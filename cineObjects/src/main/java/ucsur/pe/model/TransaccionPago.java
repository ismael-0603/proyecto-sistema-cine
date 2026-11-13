package ucsur.pe.model;

import java.util.List;

/**
 * Por qué existe: es el pago de una compra (tabla transaccion_pago): su monto, medio, estado y,
 * según cómo terminó, la referencia de la pasarela o el motivo del rechazo.
 * El flujo del pago (validar, cobrar en la pasarela y confirmar la compra) lo coordina PagoService.
 */
public class TransaccionPago {
    // Medios de pago, como en los sistemas de venta en Perú (constantes: se usan como TransaccionPago.NOMBRE).
    public static final String TARJETA_CREDITO = "TARJETA_CREDITO"; // Visa, Mastercard, Amex, Diners
    public static final String TARJETA_DEBITO = "TARJETA_DEBITO";   // tarjeta de débito del banco
    public static final String YAPE = "YAPE";                       // billetera digital del BCP
    public static final String PLIN = "PLIN";                       // billetera de Interbank, BBVA, Scotiabank...
    public static final String PAGO_QR = "PAGO_QR";                 // QR interoperable que se escanea con la app del banco

    /** Medios que acepta el kiosko, en el orden en que la vista los muestra. */
    public static final List<String> MEDIOS_DISPONIBLES =
            List.of(TARJETA_CREDITO, TARJETA_DEBITO, YAPE, PLIN, PAGO_QR);

    // Estados de un pago (constantes: se usan como TransaccionPago.NOMBRE y su valor no cambia).
    public static final String PENDIENTE = "PENDIENTE"; // creado, aún sin procesar
    public static final String APROBADO = "APROBADO"; // cobrado y con la compra confirmada
    public static final String RECHAZADO = "RECHAZADO"; // no cobrado, o cobrado y devuelto; se guarda el motivo

    private static final int MAX_MOTIVO = 150; // en la BD motivo_rechazo es VARCHAR(150)

    private int idTransaccion;
    private double monto;
    private String medioPago;
    private String estadoPago;
    private DateTime fechaHora;
    private String referenciaPasarela; // existe si la pasarela aprobó el cobro
    private String motivoRechazo; // existe si el pago fue rechazado o devuelto
    private Compra compra;

    public TransaccionPago(int idTransaccion, double monto, String medioPago, Compra compra) {
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        if (!esMedioValido(medioPago)) {
            throw new IllegalArgumentException("Medio de pago inválido: " + medioPago + ". Medios aceptados: " + MEDIOS_DISPONIBLES);
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        this.idTransaccion = idTransaccion;
        this.monto = monto;
        this.medioPago = medioPago;
        this.estadoPago = PENDIENTE;
        this.fechaHora = compra.getReloj().ahora();
        this.compra = compra;
    }

    /** Indica si el medio está entre los que acepta el kiosko. */
    public static boolean esMedioValido(String medioPago) {
        return medioPago != null && MEDIOS_DISPONIBLES.contains(medioPago);
    }

    /** Guarda la referencia del cobro que aprobó la pasarela; el pago sigue PENDIENTE hasta confirmar la compra. */
    public void registrarCobro(String referenciaPasarela, DateTime fechaHora) {
        exigirPendiente();
        if (referenciaPasarela == null || referenciaPasarela.isBlank()) {
            throw new IllegalArgumentException("La referencia de la pasarela es obligatoria");
        }
        this.referenciaPasarela = referenciaPasarela;
        this.fechaHora = fechaHora;
    }

    /** Marca el pago como APROBADO; exige que antes se haya registrado el cobro. */
    public void aprobar() {
        exigirPendiente();
        if (referenciaPasarela == null) {
            throw new IllegalStateException("No se puede aprobar un pago sin el cobro de la pasarela");
        }
        this.estadoPago = APROBADO;
    }

    /** Marca el pago como RECHAZADO y guarda el motivo (recortado a lo que admite la BD). */
    public void rechazar(String motivo, DateTime fechaHora) {
        exigirPendiente();
        this.estadoPago = RECHAZADO;
        this.fechaHora = fechaHora;
        this.motivoRechazo = (motivo != null && motivo.length() > MAX_MOTIVO) ? motivo.substring(0, MAX_MOTIVO) : motivo;
    }

    /** Un pago ya procesado (aprobado o rechazado) no se vuelve a procesar. */
    private void exigirPendiente() {
        if (!PENDIENTE.equals(estadoPago)) {
            throw new IllegalStateException("La transacción ya fue procesada (" + estadoPago + ")");
        }
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
