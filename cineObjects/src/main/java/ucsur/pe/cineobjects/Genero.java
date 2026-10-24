package ucsur.pe.cineobjects;

/** Representa un género cinematográfico; su nombre es obligatorio y no puede superar 20 caracteres. */
public class Genero {
    private int idGenero;
    private String nombre;

    public Genero(int idGenero, String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del género es obligatorio");
        }
        if (nombre.trim().length() > 20) {
            throw new IllegalArgumentException("El nombre del género no puede superar 20 caracteres");
        }
        this.idGenero = idGenero;
        this.nombre = nombre.trim();
    }

    public int getIdGenero() { 
        return idGenero; 
    }
    public String getNombre() { 
        return nombre; 
    }

    @Override
    public String toString() {
        return nombre;
    }
}
