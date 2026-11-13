package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.service.PagoService;
import java.util.List;

/** Acciones del pago: medios disponibles, pagar y reembolsar. */
public class PagoController extends ControllerBase {
    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<String>> mediosDisponibles() {
        return ejecutar("", servicio::mediosDisponibles);
    }

    /** Devuelve ok solo si el pago quedó APROBADO; si se rechazó, el mensaje es el motivo. */
    public Respuesta<TransaccionPago> pagar(Compra compra, double monto, String medioPago) {
        Respuesta<TransaccionPago> r = ejecutar("Pago aprobado", () -> servicio.pagar(compra, monto, medioPago));
        if (r.ok() && !TransaccionPago.APROBADO.equals(r.dato().getEstadoPago())) {
            return new Respuesta<>(false, r.dato().getMotivoRechazo(), r.dato());
        }
        return r;
    }

    public Respuesta<Void> reembolsar(TransaccionPago pago) {
        return ejecutar("Reembolso realizado", () -> servicio.reembolsar(pago));
    }
}
