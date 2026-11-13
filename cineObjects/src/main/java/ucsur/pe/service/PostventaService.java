package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import ucsur.pe.repository.CalificacionRepository;
import ucsur.pe.repository.ReclamoRepository;
import java.util.List;

/** Por qué existe: casos de uso de postventa (RU-08): reclamos, calificación e historial. */
public class PostventaService {
    private final ReclamoRepository reclamos;
    private final CalificacionRepository calificaciones;

    public PostventaService(ReclamoRepository reclamos, CalificacionRepository calificaciones) {
        this.reclamos = reclamos;
        this.calificaciones = calificaciones;
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
    }

    public void rechazarReclamo(Reclamo reclamo, String motivo) {
        reclamo.rechazarReclamo(motivo);
        reclamos.guardar(reclamo);
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
