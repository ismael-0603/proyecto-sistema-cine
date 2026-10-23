package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Funcion {
    private int idFuncion;
    private DateTime fecha;
    private String horaInicio;
    private String idioma;
    private String estado; // Disponible, Agotado, En curso
    
    public Funcion(){
    }

    public Funcion(int idFuncion, DateTime fecha, String horaInicio, String idioma, String estado) {
        this.idFuncion = idFuncion;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.idioma = idioma;
        this.estado = estado;
    }
    
    public int getIdFuncion() {
        return idFuncion;
    }

    public void setIdFuncion(int idFuncion) {
        this.idFuncion = idFuncion;
    }

    public DateTime getFecha() {
        return fecha;
    }

    public void setFecha(DateTime fecha) {
        this.fecha = fecha;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    
}
