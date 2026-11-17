package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import ucsur.pe.repository.CalificacionRepository;
import ucsur.pe.repository.ReclamoRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso de postventa (RU-08): reclamos, calificación e historial.
 * La respuesta o el rechazo de un reclamo se envían al correo del cliente (wireframe 21).
 */
public class PostventaService {
    private final ReclamoRepository reclamos;
    private final CalificacionRepository calificaciones;
    private final ServicioCorreo correo;

    public PostventaService(ReclamoRepository reclamos, CalificacionRepository calificaciones, ServicioCorreo correo) {
        this.reclamos = reclamos;
        this.calificaciones = calificaciones;
        this.correo = correo;
    }

    public Reclamo presentarReclamo(Compra compra, String tipo, String descripcion) {
        Reclamo reclamo = new Reclamo(0, tipo, descripcion, compra);
        reclamo.registrar();
        reclamos.guardar(reclamo);
        return reclamo;
    }

    public void iniciarRevision(Reclamo reclamo) {
        reclamo.iniciarRevision();
        reclamos.guardar(reclamo);
    }

    public void responderReclamo(Reclamo reclamo, String respuesta) {
        reclamo.responderReclamo(respuesta);
        reclamos.guardar(reclamo);
        avisarCliente(reclamo, "Respuesta a tu reclamo #" + reclamo.getIdReclamo());
    }

    public void rechazarReclamo(Reclamo reclamo, String motivo) {
        reclamo.rechazarReclamo(motivo);
        reclamos.guardar(reclamo);
        avisarCliente(reclamo, "Tu reclamo #" + reclamo.getIdReclamo() + " fue rechazado");
    }

    /**
     * El reclamo ya quedó cerrado; si el correo falla no se deshace (el cliente igual lo ve en Mis compras),
     * por eso no se lanza error.
     */
    private void avisarCliente(Reclamo reclamo, String asunto) {
        correo.enviar(reclamo.getCompra().getCliente().getCorreo(), asunto, reclamo.getRespuesta());
    }

    /** Reclamos de una compra con su estado, para Mis compras (RF-35). */
    public List<Reclamo> consultarReclamos(Compra compra) {
        return reclamos.listarPorCompra(compra);
    }

    /** Bandeja del administrador (wireframe 21); con estado null lista todos. */
    public List<Reclamo> listarReclamos(String estado) {
        return reclamos.listarTodos().stream()
                .filter(r -> estado == null || estado.equals(r.getEstado()))
                .toList();
    }

    /** Califica una compra confirmada; solo se permite una calificación por compra. */
    public Calificacion calificar(Compra compra, int puntuacion, String comentario) {
        if (calificaciones.existePorCompra(compra)) {
            throw new ReglaNegocioException("Esta compra ya fue calificada");
        }
        Calificacion calificacion = new Calificacion(0, puntuacion, comentario, compra);
        calificacion.registrar();
        calificaciones.guardar(calificacion);
        return calificacion;
    }

    public List<Compra> consultarHistorial(Cliente cliente) {
        return cliente.consultarHistorial();
    }
}
