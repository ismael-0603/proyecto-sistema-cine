package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.service.PagoService;
import java.util.List;

/**
 * Acciones del pago: medios disponibles y pagar. El cliente no pide reembolsos: se hacen solos cuando
 * el administrador cancela una función (AdminController.cancelarFuncion).
 */
public class PagoController extends ControllerBase {
    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<String>> mediosDisponibles() {
        return ejecutar("", servicio::mediosDisponibles);
    }

    /**
     * Devuelve ok solo si el pago quedó APROBADO; si se rechazó, el mensaje es el motivo.
     * El monto va en céntimos (S/ 47.20 -> 4720); si viene como texto, se convierte con ConversorDinero.aCentavos.
     */
    public Respuesta<TransaccionPago> pagar(Compra compra, long montoCentavos, String medioPago) {
        Respuesta<TransaccionPago> r = ejecutar("Pago aprobado", () -> servicio.pagar(compra, montoCentavos, medioPago));
        if (r.ok() && !TransaccionPago.APROBADO.equals(r.dato().getEstadoPago())) {
            return new Respuesta<>(false, r.dato().getMotivoRechazo(), r.dato());
        }
        return r;
    }
}
