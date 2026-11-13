package ucsur.pe.exception;

/**
 * Por qué existe: error propio de la capa de persistencia. Las capas superiores no necesitan saber
 * si el fallo vino de la memoria o de MySQL (cuando exista la base de datos).
 */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String mensaje) {
        super(mensaje);
    }

    /** Conserva el error original (la causa), útil para depurar sin exponerlo a las capas superiores. */
    public RepositoryException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
