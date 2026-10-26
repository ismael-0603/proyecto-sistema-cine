package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: para que pedidos, reclamos y calificaciones se registren con un mismo método,
 * que devuelve false cuando no se cumplen sus reglas.
 */
public interface Registrable {
    /** Intenta completar el registro y devuelve si se cumplieron sus reglas. */
    boolean registrar();
}
