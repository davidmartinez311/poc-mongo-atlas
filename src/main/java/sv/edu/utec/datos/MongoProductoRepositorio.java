package sv.edu.utec.datos;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import sv.edu.utec.modelo.Producto;
import sv.edu.utec.modelo.ResumenCategoria;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MongoProductoRepositorio implements ProductoRepositorio {

    private final MongoCollection<Document> coleccion;

    public MongoProductoRepositorio(MongoDatabase bd) {
        this.coleccion = bd.getCollection("productos");
        // Regla de negocio en la base de datos: el codigo no se repite
        coleccion.createIndex(Indexes.ascending("codigo"),
                new IndexOptions().unique(true));
        // Indice normal: acelera los filtros por categoria
        coleccion.createIndex(Indexes.ascending("categoria"));
    }

    // =========================== CREATE ===========================

    @Override
    public void insertar(Producto p) {
        try {
            coleccion.insertOne(aDocumento(p));
        } catch (MongoWriteException e) {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                throw new RepositorioException(
                        "Ya existe un producto con el codigo " + p.getCodigo(), e);
            }
            throw e;
        }
    }

    @Override
    public int insertarVarios(List<Producto> productos) {
        if (productos.isEmpty()) {
            return 0;                 // insertMany no acepta listas vacias
        }
        List<Document> documentos = new ArrayList<>();
        for (Producto p : productos) {
            documentos.add(aDocumento(p));
        }
        try {
            return coleccion.insertMany(documentos).getInsertedIds().size();
        } catch (MongoBulkWriteException e) {
            throw new RepositorioException("Carga detenida: "
                    + e.getWriteResult().getInsertedCount()
                    + " insertados antes de un codigo duplicado", e);
        }
    }

    // ============================ READ ============================

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        Document d = coleccion.find(Filters.eq("codigo", codigo)).first();
        return Optional.ofNullable(d).map(this::aProducto);
    }

    @Override
    public List<Producto> listarTodos() {
        return buscar(new Document(), Sorts.ascending("codigo"));
    }

    @Override
    public List<Producto> listarPorCategoria(String categoria) {
        return buscar(Filters.eq("categoria", categoria),
                Sorts.ascending("nombre"));
    }

    @Override
    public List<Producto> buscarPorNombre(String texto) {
        // Pattern.quote: lo que escribe el usuario se busca literal,
        // nunca se interpreta como expresion regular (inyeccion NoSQL)
        Bson filtro = Filters.regex("nombre", Pattern.quote(texto), "i");
        return buscar(filtro, Sorts.ascending("nombre"));
    }

    @Override
    public List<Producto> buscarPorRangoDePrecio(double minimo, double maximo) {
        Bson filtro = Filters.and(
                Filters.gte("precio", minimo),
                Filters.lte("precio", maximo));
        return buscar(filtro, Sorts.ascending("precio"));
    }

    /** Ejecuta un find con filtro y orden, y convierte cada documento. */
    private List<Producto> buscar(Bson filtro, Bson orden) {
        List<Producto> resultado = new ArrayList<>();
        for (Document d : coleccion.find(filtro).sort(orden)) {
            resultado.add(aProducto(d));
        }
        return resultado;
    }

    // =========================== UPDATE ===========================

    @Override
    public boolean actualizar(Producto p) {
        UpdateResult r = coleccion.updateOne(
                Filters.eq("codigo", p.getCodigo()),
                Updates.combine(
                        Updates.set("nombre", p.getNombre()),
                        Updates.set("categoria", p.getCategoria()),
                        Updates.set("precio", p.getPrecio()),
                        Updates.set("existencia", p.getExistencia()),
                        Updates.currentDate("actualizadoEn")));
        return r.getMatchedCount() > 0;       // false: el codigo no existe
    }

    @Override
    public boolean ajustarExistencia(String codigo, int cantidad) {
        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }
        Bson filtro = Filters.eq("codigo", codigo);
        if (cantidad < 0) {
            // Solo descuenta si alcanza. Revisar y restar ocurre en UNA
            // operacion atomica dentro del servidor.
            filtro = Filters.and(filtro, Filters.gte("existencia", -cantidad));
        }
        UpdateResult r = coleccion.updateOne(filtro, Updates.combine(
                Updates.inc("existencia", cantidad),
                Updates.currentDate("actualizadoEn")));
        return r.getModifiedCount() > 0;
    }

    // =========================== DELETE ===========================

    @Override
    public boolean eliminar(String codigo) {
        DeleteResult r = coleccion.deleteOne(Filters.eq("codigo", codigo));
        return r.getDeletedCount() > 0;
    }

    // ========================== REPORTE ===========================

    @Override
    public List<ResumenCategoria> resumenPorCategoria() {
        List<Bson> pipeline = List.of(
                Aggregates.group("$categoria",
                        Accumulators.sum("productos", 1),
                        Accumulators.sum("unidades", "$existencia"),
                        Accumulators.sum("valor", new Document("$multiply",
                                List.of("$precio", "$existencia")))),
                Aggregates.sort(Sorts.descending("valor")));

        List<ResumenCategoria> resultado = new ArrayList<>();
        for (Document d : coleccion.aggregate(pipeline)) {
            resultado.add(new ResumenCategoria(
                    d.getString("_id"),
                    d.get("productos", Number.class).intValue(),
                    d.get("unidades", Number.class).intValue(),
                    d.get("valor", Number.class).doubleValue()));
        }
        return resultado;
    }

    // ================ Traduccion objeto <-> documento ================

    private Document aDocumento(Producto p) {
        return new Document("codigo", p.getCodigo())
                .append("nombre", p.getNombre())
                .append("categoria", p.getCategoria())
                .append("precio", p.getPrecio())
                .append("existencia", p.getExistencia())
                .append("creadoEn", new Date());
    }

    private Producto aProducto(Document d) {
        return new Producto(
                d.getString("codigo"),
                d.getString("nombre"),
                d.getString("categoria"),
                d.get("precio", Number.class).doubleValue(),
                d.get("existencia", Number.class).intValue());
    }
}