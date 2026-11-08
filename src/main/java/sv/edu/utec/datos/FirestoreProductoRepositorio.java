package sv.edu.utec.datos;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import sv.edu.utec.modelo.Producto;
import sv.edu.utec.modelo.ResumenCategoria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FirestoreProductoRepositorio implements ProductoRepositorio {

    private final Firestore db;
    private final CollectionReference coleccion;

    public FirestoreProductoRepositorio(Firestore db) {
        this.db = db;
        this.coleccion = db.collection("productos");
    }

    // ===================== PARTE C: CREATE =====================

    @Override
    public void insertar(Producto p) {
        DocumentReference ref = referencia(p.getCodigo());
        boolean creado = esperar(db.runTransaction(tx -> {
            if (tx.get(ref).get().exists()) {
                return false;                       // el codigo ya esta ocupado
            }
            tx.create(ref, aMapa(p));
            return true;
        }), "insertar");
        if (!creado) {
            throw new RepositorioException(
                    "Ya existe un producto con el codigo " + p.getCodigo());
        }
    }

    @Override
    public int insertarVarios(List<Producto> productos) {
        if (productos.isEmpty()) {
            return 0;
        }
        DocumentReference[] refs = new DocumentReference[productos.size()];
        for (int i = 0; i < refs.length; i++) {
            refs[i] = referencia(productos.get(i).getCodigo());
        }
        String repetido = esperar(db.runTransaction(tx -> {
            for (DocumentSnapshot d : tx.getAll(refs).get()) {
                if (d.exists()) {
                    return d.getId();               // uno ocupado: no se escribe nada
                }
            }
            for (int i = 0; i < refs.length; i++) {
                tx.create(refs[i], aMapa(productos.get(i)));
            }
            return null;
        }), "insertar el lote");
        if (repetido != null) {
            throw new RepositorioException("Carga cancelada: el codigo " + repetido
                    + " ya existe; no se inserto ningun producto");
        }
        return productos.size();
    }

    // ===================== PARTE C: DELETE =====================

    @Override
    public boolean eliminar(String codigo) {
        DocumentReference ref = referencia(codigo);
        return esperar(db.runTransaction(tx -> {
            if (!tx.get(ref).get().exists()) {
                return false;                       // no habia nada que borrar
            }
            tx.delete(ref);
            return true;
        }), "eliminar");
    }

    // ===================== PARTE C y D: READ =====================

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        DocumentSnapshot d = esperar(referencia(codigo).get(), "buscar " + codigo);
        return d.exists() ? Optional.of(aProducto(d)) : Optional.empty();
    }

    @Override
    public List<Producto> listarTodos() {
        return buscar(coleccion.orderBy("codigo"));
    }

    @Override
    public List<Producto> buscarPorRangoDePrecio(double minimo, double maximo) {
        return buscar(coleccion
                .whereGreaterThanOrEqualTo("precio", minimo)
                .whereLessThanOrEqualTo("precio", maximo)
                .orderBy("precio"));
    }

    @Override
    public List<Producto> buscarPorNombre(String texto) {
        // Firestore no tiene LIKE ni $regex: se filtra en Java.
        // Cuesta una lectura por cada documento de la coleccion.
        String buscado = texto.toLowerCase(Locale.ROOT);
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : listarTodos()) {
            if (p.getNombre().toLowerCase(Locale.ROOT).contains(buscado)) {
                resultado.add(p);
            }
        }
        resultado.sort(Comparator.comparing(Producto::getNombre));
        return resultado;
    }

    @Override
    public List<Producto> listarPorCategoria(String categoria) {
        // Igualdad en un campo + orden por otro: necesita un indice compuesto
        return buscar(coleccion
                .whereEqualTo("categoria", categoria)
                .orderBy("nombre"));
    }

    /** Ejecuta una consulta y convierte cada documento. */
    private List<Producto> buscar(Query consulta) {
        List<Producto> resultado = new ArrayList<>();
        for (QueryDocumentSnapshot d : esperar(consulta.get(), "consultar").getDocuments()) {
            resultado.add(aProducto(d));
        }
        return resultado;
    }

    // ===================== PARTE E: UPDATE =====================

    @Override
    public boolean actualizar(Producto p) {
        DocumentReference ref = referencia(p.getCodigo());
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("nombre", p.getNombre());
        cambios.put("categoria", p.getCategoria());
        cambios.put("precio", p.getPrecio());
        cambios.put("existencia", p.getExistencia());
        cambios.put("actualizadoEn", FieldValue.serverTimestamp());
        return esperar(db.runTransaction(tx -> {
            if (!tx.get(ref).get().exists()) {
                return false;                       // el codigo no existe
            }
            tx.update(ref, cambios);
            return true;
        }), "actualizar");
    }

    @Override
    public boolean ajustarExistencia(String codigo, int cantidad) {
        if (cantidad == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero");
        }
        DocumentReference ref = referencia(codigo);
        return esperar(db.runTransaction(tx -> {
            DocumentSnapshot d = tx.get(ref).get();   // bloquea el documento
            if (!d.exists()) {
                return false;
            }
            long nueva = d.getLong("existencia") + cantidad;
            if (nueva < 0) {
                return false;                       // no alcanza: no se toca nada
            }
            tx.update(ref, "existencia", nueva,
                    "actualizadoEn", FieldValue.serverTimestamp());
            return true;
        }), "ajustar existencia");
    }

    // ===================== PARTE E: REPORTE =====================

    @Override
    public List<ResumenCategoria> resumenPorCategoria() {
        // Firestore cuenta, suma y promedia, pero no agrupa (no hay GROUP BY):
        // se leen los productos y se agrupan en Java con streams.
        Map<String, List<Producto>> grupos = listarTodos().stream()
                .collect(Collectors.groupingBy(Producto::getCategoria));

        List<ResumenCategoria> resultado = new ArrayList<>();
        for (Map.Entry<String, List<Producto>> g : grupos.entrySet()) {
            List<Producto> lista = g.getValue();
            int unidades = lista.stream().mapToInt(Producto::getExistencia).sum();
            double valor = lista.stream()
                    .mapToDouble(p -> p.getPrecio() * p.getExistencia()).sum();
            resultado.add(new ResumenCategoria(g.getKey(), lista.size(), unidades, valor));
        }
        resultado.sort(Comparator.comparingDouble(ResumenCategoria::valorInventario)
                .reversed());
        return resultado;
    }

    // ================ Utilidades y traduccion objeto <-> documento ================

    /** Espera la respuesta del servidor y traduce sus errores. */
    private static <T> T esperar(ApiFuture<T> futuro, String operacion) {
        try {
            return futuro.get();                    // aqui el programa espera la red
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositorioException("Operacion interrumpida: " + operacion, e);
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();         // el error real del servidor
            throw new RepositorioException(
                    "Firestore no pudo " + operacion + ": " + causa.getMessage(), causa);
        }
    }

    /** El codigo se usa como ruta del documento: se valida antes de usarlo. */
    private DocumentReference referencia(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.contains("/")
                || codigo.equals(".") || codigo.equals("..")
                || codigo.matches("__.*__")) {
            throw new IllegalArgumentException("Codigo no valido para Firestore: " + codigo);
        }
        return coleccion.document(codigo);
    }

    private Map<String, Object> aMapa(Producto p) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("codigo", p.getCodigo());
        datos.put("nombre", p.getNombre());
        datos.put("categoria", p.getCategoria());
        datos.put("precio", p.getPrecio());
        datos.put("existencia", p.getExistencia());
        datos.put("creadoEn", FieldValue.serverTimestamp());  // hora del servidor
        return datos;
    }

    private Producto aProducto(DocumentSnapshot d) {
        return new Producto(
                d.getId(),                          // el ID del documento ES el codigo
                d.getString("nombre"),
                d.getString("categoria"),
                d.getDouble("precio"),
                d.getLong("existencia").intValue());
    }
}