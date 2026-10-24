package ucsur.pe.cineobjects.cineInterfaz;

/** Regla común para objetos que suman importes y devuelven el total actualizado. */
public interface Totalizable {
    /** Calcula y devuelve el importe total actual. */
    double calcularTotal();
}
