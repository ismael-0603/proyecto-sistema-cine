package ucsur.pe.cineobjects;

/**
 * Simula el procesamiento de un pago y conserva el resultado asociado a una compra.
 * El monto debe ser no negativo, el medio de pago reconocido y la compra obligatoria.
 */
public class TransaccionPago {
    public static final String TARJETA = "TARJETA";
    public static final String BILLETERA_DIGITAL = "BILLETERA_DIGITAL";
    public static final String QR = "QR";

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";

    private static int contadorReferencia = 0; // simula la referencia que daría una pasarela de pago real

    private int idTransaccion;
    private double monto;
    private String medioPago;
    private String estadoPago;
    private DateTime fechaHora;
    private String referenciaPasarela; // solo existe si el pago fue aprobado
    private String motivoRechazo; // solo existe si el pago fue rechazado
    private Compra compra;

    public TransaccionPago(int idTransaccion, double monto, String medioPago, Compra compra) {
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        if (!TARJETA.equals(medioPago) && !BILLETERA_DIGITAL.equals(medioPago) && !QR.equals(medioPago)) {
            throw new IllegalArgumentException("Medio de pago inválido: " + medioPago);
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        this.idTransaccion = idTransaccion;
        this.monto = monto;
        this.medioPago = medioPago;
        this.estadoPago = PENDIENTE;
        this.fechaHora = DateTime.ahora();
        this.compra = compra;
    }

    /**
     * Procesa una transacción pendiente; aprueba solo si el monto coincide y la compra se confirma.
     * Una transacción procesada no se puede volver a procesar.
     */
    public boolean procesarPago() {
        if (!PENDIENTE.equals(estadoPago)) {
            throw new IllegalStateException("La transacción ya fue procesada (" + estadoPago + ")");
        }
        this.fechaHora = DateTime.ahora();
        compra.calcularTotal();

        if (monto <= 0) {
            return rechazar("Monto inválido");
        }
        if (Math.abs(monto - compra.getTotal()) > 0.01) {
            return rechazar("El monto no coincide con el total de la compra");
        }
        if (!compra.confirmar()) {
            return rechazar("No se pudo confirmar la compra (estado: " + compra.getEstado() + ")");
        }
        this.estadoPago = APROBADO;
        this.referenciaPasarela = "PAS-" + (++contadorReferencia) + "-" + fechaHora.toValorComparable();
        return true;
    }

    /** Marca el pago como RECHAZADO, guarda el motivo y devuelve false. */
    private boolean rechazar(String motivo) {
        this.estadoPago = RECHAZADO;
        this.motivoRechazo = motivo;
        return false;
    }

    /** Devuelve el estado del pago: PENDIENTE, APROBADO o RECHAZADO. */
    public String verificarEstado() {
        return this.estadoPago;
    }

    /** Solicita el reembolso de la compra solo cuando este pago fue aprobado. */
    public boolean reembolsar() {
        if (!APROBADO.equals(estadoPago)) return false;
        return compra.reembolsar();
    }

    public int getIdTransaccion() { return idTransaccion; }
    public double getMonto() { return monto; }
    public String getMedioPago() { return medioPago; }
    public String getEstadoPago() { return estadoPago; }
    public DateTime getFechaHora() { return fechaHora; }
    public String getReferenciaPasarela() { return referenciaPasarela; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public Compra getCompra() { return compra; }

    @Override
    public String toString() {
        return "Pago #" + idTransaccion + " " + medioPago + " S/ " + String.format("%.2f", monto) + " [" + estadoPago + "]";
    }
}
