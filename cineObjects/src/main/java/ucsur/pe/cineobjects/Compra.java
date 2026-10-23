package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Compra { //Creo q debemos agregar el igv, o crear comprobate --> boleta y factura
    
    public static final double TASA_IGV = 0.18;
    
    private int idCompra;
    private String serieNumero; //numeracion para la boleta o factura
    private DateTime fecha; //dia de confirmacion
    private double subtotal;
    private double descuento; 
    private double igv;
    private double total;

    public Compra(int idCompra, String serieNumero, DateTime fecha, double subtotal, double descuento, double igv, double total) {
        this.idCompra = idCompra;
        this.serieNumero = serieNumero;
        this.fecha = fecha;
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.igv = igv;
        this.total = total;
    }
    
    public int getIdCompra() {
        return idCompra;
    }

    public void setIdCompra(int idCompra) {
        this.idCompra = idCompra;
    }

    public String getSerieNumero() {
        return serieNumero;
    }

    public void setSerieNumero(String serieNumero) {
        this.serieNumero = serieNumero;
    }

    public DateTime getFecha() {
        return fecha;
    }

    public void setFecha(DateTime fecha) {
        this.fecha = fecha;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public double getIgv() {
        return igv;
    }

    public void setIgv(double igv) {
        this.igv = igv;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
    
}
