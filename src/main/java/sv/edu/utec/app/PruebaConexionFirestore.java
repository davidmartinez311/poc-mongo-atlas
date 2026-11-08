package sv.edu.utec.app;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import sv.edu.utec.datos.ConexionFirestore;
import java.util.concurrent.ExecutionException;
public class PruebaConexionFirestore {
    public static void main(String[] args) {
        try {
            Firestore db = ConexionFirestore.getFirestore();
// Firestore no tiene "ping": leer un documento que no existe
// comprueba red, credenciales y permisos sin gastar escrituras.
            DocumentSnapshot d = db.collection("productos")
                    .document("P-000").get().get();
            System.out.println("Conexion exitosa a Firestore. Proyecto: "
                    + db.getOptions().getProjectId());
            System.out.println("Documento de prueba P-000 existe: " + d.exists());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("No se pudo leer de Firestore: " + e.getMessage());
        } finally {
            ConexionFirestore.cerrar();
        }
    }
}