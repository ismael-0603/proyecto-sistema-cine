package ucsur.pe.cineobjects;

/**
 * Por qué existe: simula el pago de una compra (tarjeta, billetera digital o QR):
 * lo aprueba o lo rechaza y permite reembolsar.
 */
public class TransaccionPago {
    // Medios de pago (constantes: se usan como TransaccionPago.NOMBRE y su valor no cambia).
    public static final String TARJETA = "TARJETA"; // tarjeta de crédito o débito
    public static final String BILLETERA_DIGITAL = "BILLETERA_DIGITAL"; // billetera del celular (por ejemplo Yape o Plin)
    public static final String QR = "QR"; // pago escaneando un código QR

    // Estados de un pago (constantes: se usan como TransaccionPago.NOMBRE y su valor no cambia).
    public static final String PENDIENTE = "PENDIENTE"; // creado, aún sin procesar
    public static final String APROBADO = "APROBADO"; // pago aceptado; la compra queda confirmada
    public static final String RECHAZADO = "RECHAZADO"; // pago no aceptado; se guarda el motivo

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
