package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Reclamo {
    private int idClasificacion;
    private String tipo;
    private String descripcion;
    private DateTime fecha;
    private String respuesta;
    private String estado;

    public Reclamo(int idClasificacion, String tipo, String descripcion, DateTime fecha, String respuesta, String estado) {
        this.idClasificacion = idClasificacion;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.respuesta = respuesta;
        this.estado = estado;
    }
    
    public int getIdClasificacion() {
        return idClasificacion;
    }

    public void setIdClasificacion(int idClasificacion) {
        this.idClasificacion = idClasificacion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public DateTime getFecha() {
        return fecha;
    }

    public void setFecha(DateTime fecha) {
        this.fecha = fecha;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    
}
