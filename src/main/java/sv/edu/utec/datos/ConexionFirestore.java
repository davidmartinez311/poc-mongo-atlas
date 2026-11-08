package sv.edu.utec.datos;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Punto unico de acceso a Cloud Firestore.
 * La llave de la cuenta de servicio NUNCA va dentro del proyecto: se lee
 * del archivo que indica la variable de entorno GOOGLE_APPLICATION_CREDENTIALS.
 */
public final class ConexionFirestore {

    private static Firestore db;                 // un solo cliente para toda la app

    private ConexionFirestore() { }              // nadie puede hacer new ConexionFirestore()

    public static synchronized Firestore getFirestore() {
        if (db == null) {
            String ruta = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            if (ruta == null || ruta.isBlank()) {
                throw new IllegalStateException(
                        "Falta la variable de entorno GOOGLE_APPLICATION_CREDENTIALS "
                                + "(Run > Edit Configurations).");
            }
            if (!Files.isRegularFile(Path.of(ruta))) {
                throw new IllegalStateException(
                        "No existe el archivo de la cuenta de servicio: " + ruta);
            }
            try {
                FirebaseOptions opciones = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                FirebaseApp.initializeApp(opciones);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "No se pudo leer la cuenta de servicio: " + e.getMessage(), e);
            }
            db = FirestoreClient.getFirestore();     // fuera del catch
        }
        return db;
    }

    public static synchronized void cerrar() {
        if (db != null) {
            FirebaseApp.getInstance().delete();  // libera hilos y conexiones
            db = null;
        }
    }
}