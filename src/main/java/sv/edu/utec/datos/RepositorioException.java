package sv.edu.utec.datos;

/**
 * Error de acceso a datos expresado en el idioma del negocio.
 * Quien usa el repositorio la captura sin importar nada del motor.
 */
public class RepositorioException extends RuntimeException {

    /** Error que viene de una excepcion del motor (MongoDB, Firestore, JDBC). */
    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** NUEVO: regla de negocio que detecta el propio repositorio. */
    public RepositorioException(String mensaje) {
        super(mensaje);
    }
}