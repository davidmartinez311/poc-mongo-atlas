package sv.edu.utec.datos;

public class RepositorioException extends RuntimeException {
    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}