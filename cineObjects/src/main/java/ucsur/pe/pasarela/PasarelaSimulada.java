package ucsur.pe.pasarela;

import ucsur.pe.model.DateTime;
import ucsur.pe.model.TransaccionPago;
import java.util.HashSet;
import java.util.Set;

/**
 * Por qué existe: simula la pasarela de pagos mientras no haya una real.
 * Aprueba todo cobro con monto positivo y un medio válido, y genera una referencia como la de una pasarela.
 */
public class PasarelaSimulada implements PasarelaPago {
    private int contadorReferencia = 0;
    /** Cobros aprobados que todavía se pueden devolver. */
    private final Set<String> cobrosAprobados = new HashSet<>();

    @Override
    public ResultadoPago cobrar(String medioPago, double monto, String referenciaCompra) {
        if (!TransaccionPago.esMedioValido(medioPago)) {
            return ResultadoPago.rechazado("Medio de pago no aceptado: " + medioPago);
        }
        if (monto <= 0) {
            return ResultadoPago.rechazado("Monto inválido");
        }
        contadorReferencia++;
        String referencia = "PAS-" + contadorReferencia + "-" + DateTime.ahora().toValorComparable();
        cobrosAprobados.add(referencia);
        return ResultadoPago.aprobado(referencia);
    }

    @Override
    public boolean reembolsar(String referenciaPasarela, double monto) {
        // Solo se devuelve un cobro que esta pasarela aprobó, y una sola vez.
        return referenciaPasarela != null && monto > 0 && cobrosAprobados.remove(referenciaPasarela);
    }
}
