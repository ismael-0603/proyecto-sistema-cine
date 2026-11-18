package ucsur.pe.pasarela;

import ucsur.pe.model.Reloj;
import ucsur.pe.model.TransaccionPago;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Por qué existe: simula la pasarela de pagos mientras no haya una real.
 * Aprueba todo cobro con monto positivo y un medio válido, y genera una referencia como la de una pasarela.
 * Una pasarela real guarda su historial de cobros; esta no tiene memoria entre sesiones, así que reconoce
 * sus cobros por el formato de la referencia (PAS-numero-fecha). Así se puede reembolsar al día siguiente
 * una compra que se pagó antes de reiniciar la aplicación.
 */
public class PasarelaSimulada implements PasarelaPago {
    /** Formato de las referencias que genera cobrar: "PAS-" + contador + "-" + fecha (yyyyMMddHHmmss). */
    private static final Pattern FORMATO_REFERENCIA = Pattern.compile("PAS-\\d+-\\d{14}");

    private int contadorReferencia = 0;
    /** Cobros ya devueltos en esta sesión: un cobro no se devuelve dos veces. */
    private final Set<String> devueltos = new HashSet<>();
    private final Reloj reloj; // para la fecha de la referencia

    public PasarelaSimulada(Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
        this.reloj = reloj;
    }

    @Override
    public ResultadoPago cobrar(String medioPago, long montoCentavos, String referenciaCompra) {
        if (!TransaccionPago.esMedioValido(medioPago)) {
            return ResultadoPago.rechazado("Medio de pago no aceptado: " + medioPago);
        }
        if (montoCentavos <= 0) {
            return ResultadoPago.rechazado("Monto inválido");
        }
        contadorReferencia++;
        String referencia = "PAS-" + contadorReferencia + "-" + reloj.ahora().toValorComparable();
        return ResultadoPago.aprobado(referencia);
    }

    /**
     * Devuelve un cobro de esta pasarela (de esta sesión o de una anterior), una sola vez por sesión.
     * Que una compra no se reembolse dos veces entre sesiones lo asegura la propia compra:
     * solo una compra CONFIRMADA se reembolsa, y después queda REEMBOLSADA.
     */
    @Override
    public boolean reembolsar(String referenciaPasarela, long montoCentavos) {
        return referenciaPasarela != null && montoCentavos > 0
                && FORMATO_REFERENCIA.matcher(referenciaPasarela).matches()
                && devueltos.add(referenciaPasarela);
    }
}
