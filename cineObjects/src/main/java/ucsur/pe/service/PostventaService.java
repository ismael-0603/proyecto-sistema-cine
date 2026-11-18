package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Reclamo;
import ucsur.pe.repository.CalificacionRepository;
import ucsur.pe.repository.ReclamoRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso de postventa (RU-08): reclamos, calificación e historial.
 * La respuesta o el rechazo de un reclamo se envían al correo del cliente (wireframe 21).
 *
 * Cada operación recibe a quien la hace (el cliente o el empleado de la sesión):
 * - el cliente solo reclama, califica o consulta sus propias compras;
 * - solo un administrador activo atiende reclamos, y solo los de compras de su sede.
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

    public Reclamo presentarReclamo(Cliente cliente, Compra compra, String tipo, String descripcion) {
        exigirTitular(cliente, compra);
        Reclamo reclamo = new Reclamo(0, tipo, descripcion, compra);
        reclamo.registrar();
        reclamos.guardar(reclamo);
        return reclamo;
    }

    public void iniciarRevision(Empleado admin, Reclamo reclamo) {
        exigirAdminDeLaSede(admin, reclamo);
        reclamo.iniciarRevision();
        reclamos.guardar(reclamo);
    }

    public void responderReclamo(Empleado admin, Reclamo reclamo, String respuesta) {
        exigirAdminDeLaSede(admin, reclamo);
        reclamo.responderReclamo(respuesta);
        reclamos.guardar(reclamo);
        avisarCliente(reclamo, "Respuesta a tu reclamo #" + reclamo.getIdReclamo());
    }

    public void rechazarReclamo(Empleado admin, Reclamo reclamo, String motivo) {
        exigirAdminDeLaSede(admin, reclamo);
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

    /** Reclamos de una compra con su estado, para Mis compras (RF-35); solo los ve el dueño de la compra. */
    public List<Reclamo> consultarReclamos(Cliente cliente, Compra compra) {
        exigirTitular(cliente, compra);
        return reclamos.listarPorCompra(compra);
    }

    /** Bandeja del administrador (wireframe 21): reclamos de su sede; con estado null lista todos. */
    public List<Reclamo> listarReclamos(Empleado admin, String estado) {
        exigirAdmin(admin);
        return reclamos.listarTodos().stream()
                .filter(r -> r.getCompra().getSede().equals(admin.getSede()))
                .filter(r -> estado == null || estado.equals(r.getEstado()))
                .toList();
    }

    /** Califica una compra confirmada propia; solo se permite una calificación por compra. */
    public Calificacion calificar(Cliente cliente, Compra compra, int puntuacion, String comentario) {
        exigirTitular(cliente, compra);
        if (calificaciones.existePorCompra(compra)) {
            throw new ReglaNegocioException("Esta compra ya fue calificada");
        }
        Calificacion calificacion = new Calificacion(0, puntuacion, comentario, compra);
        calificacion.registrar();
        calificaciones.guardar(calificacion);
        return calificacion;
    }

    public List<Compra> consultarHistorial(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("El cliente es obligatorio");
        return cliente.consultarHistorial();
    }

    // ---------- Validaciones comunes ----------
    /** La compra debe ser del cliente de la sesión, y su cuenta debe estar activa. */
    private void exigirTitular(Cliente cliente, Compra compra) {
        if (cliente == null || !cliente.isEstado()) {
            throw new ReglaNegocioException("La cuenta del cliente no está activa");
        }
        if (compra == null) throw new IllegalArgumentException("La compra es obligatoria");
        if (compra.getCliente() != cliente) {
            throw new ReglaNegocioException("La compra no pertenece a este cliente");
        }
    }

    private void exigirAdmin(Empleado admin) {
        if (admin == null || !admin.esAdministradorActivo()) {
            throw new ReglaNegocioException("Solo un administrador activo puede atender reclamos");
        }
    }

    private void exigirAdminDeLaSede(Empleado admin, Reclamo reclamo) {
        exigirAdmin(admin);
        if (reclamo == null) throw new IllegalArgumentException("El reclamo es obligatorio");
        if (!reclamo.getCompra().getSede().equals(admin.getSede())) {
            throw new ReglaNegocioException("El reclamo es de otra sede; el administrador gestiona " + admin.getSede().getNombre());
        }
    }
}
