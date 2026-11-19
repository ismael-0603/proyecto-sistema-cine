package ucsur.pe.exception;

/**
 * Por qué existe: error propio de la capa de persistencia. Las capas superiores no necesitan saber
 * los detalles de MySQL (SQLException), solo el mensaje.
 */
public class RepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryException(String mensaje) {
        super(mensaje);
    }

    /** Conserva el error original (la causa), útil para depurar sin exponerlo a las capas superiores. */
    public RepositoryException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
