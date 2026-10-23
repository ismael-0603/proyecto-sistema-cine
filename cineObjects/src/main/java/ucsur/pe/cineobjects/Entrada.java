package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Entrada {
    private int idEntrada;
    private String codigoQR;
    private double precio;
    private String estado; // activa, usada, cancelada, vencida
    private DateTime fechaEmision;

    public Entrada(int idEntrada, String codigoQR, double precio, String estado, DateTime fechaEmision) {
        this.idEntrada = idEntrada;
        this.codigoQR = codigoQR;
        this.precio = precio;
        this.estado = estado;
        this.fechaEmision = fechaEmision;
    }
    
    public int getIdEntrada() {
        return idEntrada;
    }

    public void setIdEntrada(int idEntrada) {
        this.idEntrada = idEntrada;
    }

    public String getCodigoQR() {
        return codigoQR;
    }

    public void setCodigoQR(String codigoQR) {
        this.codigoQR = codigoQR;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public DateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(DateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }
    
}
