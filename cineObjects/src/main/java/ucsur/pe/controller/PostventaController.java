package ucsur.pe.controller;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Reclamo;
import ucsur.pe.service.PostventaService;
import java.util.List;

/**
 * Acciones de postventa: reclamos, calificación e historial de compras.
 * Cada acción recibe quién la hace (cliente o administrador de la sesión) para comprobar sus permisos.
 */
public class PostventaController extends ControllerBase {
    private final PostventaService servicio;

    public PostventaController(PostventaService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Reclamo> presentarReclamo(Cliente cliente, Compra compra, String tipo, String descripcion) {
        return ejecutar("Reclamo registrado", () -> servicio.presentarReclamo(cliente, compra, tipo, descripcion));
    }

    public Respuesta<Void> iniciarRevision(Empleado admin, Reclamo reclamo) {
        return ejecutar("Reclamo en revisión", () -> servicio.iniciarRevision(admin, reclamo));
    }

    public Respuesta<Void> responderReclamo(Empleado admin, Reclamo reclamo, String respuesta) {
        return ejecutar("Reclamo resuelto", () -> servicio.responderReclamo(admin, reclamo, respuesta));
    }

    public Respuesta<Void> rechazarReclamo(Empleado admin, Reclamo reclamo, String motivo) {
        return ejecutar("Reclamo rechazado", () -> servicio.rechazarReclamo(admin, reclamo, motivo));
    }

    public Respuesta<List<Reclamo>> consultarReclamos(Cliente cliente, Compra compra) {
        return ejecutar("", () -> servicio.consultarReclamos(cliente, compra));
    }

    public Respuesta<List<Reclamo>> listarReclamos(Empleado admin, String estado) {
        return ejecutar("", () -> servicio.listarReclamos(admin, estado));
    }

    public Respuesta<Calificacion> calificar(Cliente cliente, Compra compra, int puntuacion, String comentario) {
        return ejecutar("Gracias por calificar", () -> servicio.calificar(cliente, compra, puntuacion, comentario));
    }

    /** El dato es null si la compra aún no tiene calificación. */
    public Respuesta<Calificacion> consultarCalificacion(Cliente cliente, Compra compra) {
        return ejecutar("", () -> servicio.consultarCalificacion(cliente, compra));
    }

    public Respuesta<List<Compra>> consultarHistorial(Cliente cliente) {
        return ejecutar("", () -> servicio.consultarHistorial(cliente));
    }
}
