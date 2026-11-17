package ucsur.pe.model;

/**
 * Por qué existe: es el formato de una sala (2D, 3D...) y el recargo que suma al precio de la entrada.
 */
public class TipoSala {
    private int idTipoSala;
    private String formato;
    private String descripcion;
    private long tarifaAdicionalCentavos; // recargo en céntimos que se suma a la tarifa de entrada

    public TipoSala(int idTipoSala, String formato, String descripcion, long tarifaAdicionalCentavos) {
        if (formato == null || formato.trim().isEmpty() || formato.trim().length() > 30) {
            throw new IllegalArgumentException("El formato es obligatorio (máx. 30 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 150) {
            throw new IllegalArgumentException("La descripción no puede superar 150 caracteres");
        }
        if (tarifaAdicionalCentavos < 0) {
            throw new IllegalArgumentException("La tarifa adicional no puede ser negativa");
        }
        this.idTipoSala = idTipoSala;
        this.formato = formato.trim();
        this.descripcion = descripcion;
        this.tarifaAdicionalCentavos = tarifaAdicionalCentavos;
    }

    /** Devuelve el recargo en céntimos que este formato añade al precio de la entrada. */
    public long obtenerRecargoFormatoCentavos() {
        return this.tarifaAdicionalCentavos;
    }

    public int getIdTipoSala() { return idTipoSala; }
    public String getFormato() { return formato; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return formato;
    }
    
}
