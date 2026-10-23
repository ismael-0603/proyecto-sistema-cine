package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class TransaccionPago {
    private int idTransaccion;
    private double monto;
    private String medioPago; // Tarjeta Credito o debito, Yape, Plin, ...
    private String estadoPago; // aprobado, rechazado, pendiente
    private DateTime fechaHora;
    private String referenciaPasarela; //codigo de operacion o voucher del POS

    public TransaccionPago(int idTransaccion, double monto, String medioPago, String estadoPago, DateTime fechaHora, String referenciaPasarela) {
        this.idTransaccion = idTransaccion;
        this.monto = monto;
        this.medioPago = medioPago;
        this.estadoPago = estadoPago;
        this.fechaHora = fechaHora;
        this.referenciaPasarela = referenciaPasarela;
    }
    
    public int getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(int idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public double getMonto() {
        return monto;
    }

    public void setMonto(double monto) {
        this.monto = monto;
    }

    public String getMedioPago() {
        return medioPago;
    }

    public void setMedioPago(String medioPago) {
        this.medioPago = medioPago;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public DateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(DateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    public void setReferenciaPasarela(String referenciaPasarela) {
        this.referenciaPasarela = referenciaPasarela;
    }
    
}
