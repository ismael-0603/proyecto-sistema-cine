package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Calificacion {
    private int idClasificacion;
    private int puntuacion;
    private String comentario;
    private DateTime fecha;

    public Calificacion(int idClasificacion, int puntuacion, String comentario, DateTime fecha) {
        this.idClasificacion = idClasificacion;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }
    
    public int getIdClasificacion() {
        return idClasificacion;
    }

    public void setIdClasificacion(int idClasificacion) {
        this.idClasificacion = idClasificacion;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public DateTime getFecha() {
        return fecha;
    }

    public void setFecha(DateTime fecha) {
        this.fecha = fecha;
    }
    
}
