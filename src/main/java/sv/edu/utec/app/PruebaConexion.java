package sv.edu.utec.app;
import com.mongodb.MongoException;
import org.bson.Document;
import sv.edu.utec.datos.ConexionMongo;
public class PruebaConexion {
    public static void main(String[] args) {
        try {
            Document respuesta = ConexionMongo.getBaseDeDatos()
                    .runCommand(new Document("ping", 1));
            System.out.println("Conexion exitosa a MongoDB Atlas. ok = "
                    + respuesta.get("ok"));
        } catch (MongoException e) {
            System.err.println("No se pudo conectar: " + e.getMessage());
        } finally {
            ConexionMongo.cerrar();
        }
    }
}
