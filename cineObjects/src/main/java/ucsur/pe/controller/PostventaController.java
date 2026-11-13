package ucsur.pe.controller;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import ucsur.pe.service.PostventaService;
import java.util.List;

/** Acciones de postventa: reclamos, calificación e historial de compras. */
public class PostventaController extends ControllerBase {
    private final PostventaService servicio;

    public PostventaController(PostventaService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Reclamo> presentarReclamo(Compra compra, String tipo, String descripcion) {
        return ejecutar("Reclamo registrado", () -> servicio.presentarReclamo(compra, tipo, descripcion));
    }

    public Respuesta<Void> iniciarRevision(Reclamo reclamo) {
        return ejecutar("Reclamo en revisión", () -> servicio.iniciarRevision(reclamo));
    }

    public Respuesta<Void> responderReclamo(Reclamo reclamo, String respuesta) {
        return ejecutar("Reclamo resuelto", () -> servicio.responderReclamo(reclamo, respuesta));
    }

    public Respuesta<Void> rechazarReclamo(Reclamo reclamo, String motivo) {
        return ejecutar("Reclamo rechazado", () -> servicio.rechazarReclamo(reclamo, motivo));
    }

    public Respuesta<Calificacion> calificar(Compra compra, int puntuacion, String comentario) {
        return ejecutar("Gracias por calificar", () -> servicio.calificar(compra, puntuacion, comentario));
    }

    public Respuesta<List<Compra>> consultarHistorial(Cliente cliente) {
        return ejecutar("", () -> servicio.consultarHistorial(cliente));
    }
}
