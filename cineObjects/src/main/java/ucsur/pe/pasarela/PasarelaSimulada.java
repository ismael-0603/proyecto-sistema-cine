package ucsur.pe.pasarela;

import ucsur.pe.model.Reloj;
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
        cobrosAprobados.add(referencia);
        return ResultadoPago.aprobado(referencia);
    }

    @Override
    public boolean reembolsar(String referenciaPasarela, long montoCentavos) {
        // Solo se devuelve un cobro que esta pasarela aprobó, y una sola vez.
        return referenciaPasarela != null && montoCentavos > 0 && cobrosAprobados.remove(referenciaPasarela);
    }
}
