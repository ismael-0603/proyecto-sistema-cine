package ucsur.pe.cineobjects;

import java.util.List;
import ucsur.pe.cineInterfaz.PasarelaPago;
import ucsur.pe.cineInterfaz.ResultadoPago;

/**
 * Por qué existe: es el pago de una compra. Primero cobra en la pasarela y, solo si el cobro se aprueba,
 * confirma la venta. Si la compra no se puede confirmar después de cobrar, devuelve el dinero.
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

    /** Pasarela que se usa si no se indica otra; compartida para que las referencias no se repitan. */
    private static final PasarelaPago PASARELA_POR_DEFECTO = new PasarelaSimulada();

    private int idTransaccion;
    private double monto;
    private String medioPago;
    private String estadoPago;
    private DateTime fechaHora;
    private String referenciaPasarela; // existe si la pasarela aprobó el cobro
    private String motivoRechazo; // existe si el pago fue rechazado o devuelto
    private Compra compra;
    private final PasarelaPago pasarela; // actor externo; llega por el constructor (inversión de dependencias)

    /** Constructor de uso normal: cobra con la pasarela simulada. */
    public TransaccionPago(int idTransaccion, double monto, String medioPago, Compra compra) {
        this(idTransaccion, monto, medioPago, compra, PASARELA_POR_DEFECTO);
    }

    /** Recibe la pasarela con la que se cobrará (la real, la simulada o una falsa en pruebas). */
    public TransaccionPago(int idTransaccion, double monto, String medioPago, Compra compra, PasarelaPago pasarela) {
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }
        if (!esMedioValido(medioPago)) {
            throw new IllegalArgumentException("Medio de pago inválido: " + medioPago + ". Medios aceptados: " + MEDIOS_DISPONIBLES);
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        if (pasarela == null) {
            throw new IllegalArgumentException("La pasarela de pago es obligatoria");
        }
        this.idTransaccion = idTransaccion;
        this.monto = monto;
        this.medioPago = medioPago;
        this.estadoPago = PENDIENTE;
        this.fechaHora = compra.getReloj().ahora();
        this.compra = compra;
        this.pasarela = pasarela;
    }

    /** Indica si el medio está entre los que acepta el kiosko. */
    public static boolean esMedioValido(String medioPago) {
        return medioPago != null && MEDIOS_DISPONIBLES.contains(medioPago);
    }

    /**
     * Procesa una transacción pendiente en este orden:
     * 1) revisa, sin cambiar nada, que la compra se pueda confirmar y que el monto coincida;
     *    si algo falla, se rechaza SIN COBRAR;
     * 2) cobra en la pasarela; si se rechaza, la compra sigue EN_PROCESO para reintentar (RF-18);
     * 3) con el cobro aprobado confirma la compra usando el mismo instante del paso 1.
     * Una transacción procesada no se puede volver a procesar.
     */
    public boolean procesarPago() {
        if (!PENDIENTE.equals(estadoPago)) {
            throw new IllegalStateException("La transacción ya fue procesada (" + estadoPago + ")");
        }
        // Un único instante para validar y confirmar: lo que se validó no puede vencer a mitad de camino.
        DateTime ahora = compra.getReloj().ahora(); // el mismo reloj que usa la compra para su plazo
        this.fechaHora = ahora;

        // 1. Validaciones antes de cobrar
        if (compra.verificarExpiracion()) {
            return rechazar("La compra expiró: se acabaron los " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos");
        }
        compra.calcularTotal();
        if (monto <= 0) {
            return rechazar("Monto inválido");
        }
        if (Math.abs(monto - compra.getTotal()) > 0.01) {
            return rechazar("El monto no coincide con el total de la compra");
        }
        String motivo = compra.motivoNoConfirmable(ahora);
        if (motivo != null) {
            return rechazar("La compra no puede pagarse: " + motivo); // no se llega a cobrar
        }

        // 2. Cobro en la pasarela
        ResultadoPago resultado = pasarela.cobrar(medioPago, monto, "COMPRA-" + compra.getIdCompra());
        if (resultado == null || !resultado.aprobado()) {
            return rechazar(resultado == null ? "La pasarela no respondió" : resultado.motivo());
        }
        this.referenciaPasarela = resultado.referencia();

        // 3. Con el dinero cobrado se confirma la venta, con el mismo instante validado en el paso 1
        try {
            compra.confirmar(this, ahora);
        } catch (RuntimeException ex) {
            // No debería pasar porque ya se validó todo; si pasa, no se queda con el dinero del cliente.
            boolean devuelto = pasarela.reembolsar(referenciaPasarela, monto);
            return rechazar((devuelto ? "Cobro devuelto" : "Cobro NO devuelto, revisar")
                    + ": " + ex.getMessage());
        }
        this.estadoPago = APROBADO;
        return true;
    }

    /** Marca el pago como RECHAZADO, guarda el motivo y devuelve false. */
    private boolean rechazar(String motivo) {
        this.estadoPago = RECHAZADO;
        this.motivoRechazo = (motivo != null && motivo.length() > MAX_MOTIVO) ? motivo.substring(0, MAX_MOTIVO) : motivo;
        return false;
    }

    /**
     * Reembolsa un pago aprobado: primero la compra comprueba que se puede (funciones canceladas
     * y pedido no entregado) y luego la pasarela devuelve el dinero.
     */
    public boolean reembolsar() {
        if (!APROBADO.equals(estadoPago)) return false;
        if (!compra.reembolsar()) return false;
        return pasarela.reembolsar(referenciaPasarela, monto);
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
