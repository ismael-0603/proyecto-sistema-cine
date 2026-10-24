package ucsur.pe.cineobjects.cineInterfaz;

/** Contrato para objetos que pueden completar un registro sujeto a reglas de negocio. */
public interface Registrable {
    /** Intenta completar el registro y devuelve si se cumplieron sus reglas. */
    boolean registrar();
}
