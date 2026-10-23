package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class TipoSala {
    private int idTipoSala;
    private String formato;
    private String descripcion;
    private double tarifaAdicional;
    
    public TipoSala(){
    }

    public TipoSala(int idTipoSala, String formato, String descripcion, double tarifaAdicional) {
        this.idTipoSala = idTipoSala;
        this.formato = formato;
        this.descripcion = descripcion;
        this.tarifaAdicional = tarifaAdicional;
    }

    public int getIdTipoSala() {
        return idTipoSala;
    }

    public void setIdTipoSala(int idTipoSala) {
        this.idTipoSala = idTipoSala;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getTarifaAdicional() {
        return tarifaAdicional;
    }

    public void setTarifaAdicional(double tarifaAdicional) {
        this.tarifaAdicional = tarifaAdicional;
    }
    
}
