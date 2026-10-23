package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Pedido {
    private int idPedido;
    private String codigoQR;
    private DateTime fecha; // tambien deberiamos agregar atributo hora?:v o con fecha ta bien
    private String estado; // pendiente, en preparacion, listo, entragado
    private double totalDulceria;

    public Pedido(int idPedido, String codigoQR, DateTime fecha, String estado, double totalDulceria) {
        this.idPedido = idPedido;
        this.codigoQR = codigoQR;
        this.fecha = fecha;
        this.estado = estado;
        this.totalDulceria = totalDulceria;
    }
    
    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public String getCodigoQR() {
        return codigoQR;
    }

    public void setCodigoQR(String codigoQR) {
        this.codigoQR = codigoQR;
    }

    public DateTime getFecha() {
        return fecha;
    }

    public void setFecha(DateTime fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getTotalDulceria() {
        return totalDulceria;
    }

    public void setTotalDulceria(double totalDulceria) {
        this.totalDulceria = totalDulceria;
    }
    
}
