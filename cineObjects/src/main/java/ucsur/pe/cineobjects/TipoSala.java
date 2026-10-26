package ucsur.pe.cineobjects;

/**
 * Por qué existe: es el formato de una sala (2D, 3D...) y el recargo que suma al precio de la entrada.
 */
public class TipoSala {
    private int idTipoSala;
    private String formato;
    private String descripcion;
    private double tarifaAdicional; // recargo en soles que se suma a la tarifa de entrada

    public TipoSala(int idTipoSala, String formato, String descripcion, double tarifaAdicional) {
        if (formato == null || formato.trim().isEmpty() || formato.trim().length() > 30) {
            throw new IllegalArgumentException("El formato es obligatorio (máx. 30 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 150) {
            throw new IllegalArgumentException("La descripción no puede superar 150 caracteres");
        }
        if (tarifaAdicional < 0) {
            throw new IllegalArgumentException("La tarifa adicional no puede ser negativa");
        }
        this.idTipoSala = idTipoSala;
        this.formato = formato.trim();
        this.descripcion = descripcion;
        this.tarifaAdicional = tarifaAdicional;
    }

    /** Devuelve el recargo que este formato añade al precio de la entrada. */
    public double obtenerRecargoFormato() {
        return this.tarifaAdicional;
    }

    public int getIdTipoSala() { return idTipoSala; }
    public String getFormato() { return formato; }
    public String getDescripcion() { return descripcion; }
    public double getTarifaAdicional() { return tarifaAdicional; }

    @Override
    public String toString() {
        return formato;
    }
    
}
