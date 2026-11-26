package ucsur.pe.db;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import java.util.List;
import java.util.Map;

/** Contrato DAO de la postventa: reclamo y calificacion. */
public interface PostventaDao {

    /** Todos los reclamos; su compra se toma del mapa (id -> Compra) para que sea el mismo objeto. */
    List<Reclamo> listarReclamos(Map<Integer, Compra> compras);
    /** Inserta el reclamo (id 0) y le asigna el id generado por la base con Reclamo.asignarId. */
    void insertarReclamo(Reclamo reclamo);
    /** Guarda lo que cambia al atenderlo: estado y respuesta. */
    void actualizarReclamo(Reclamo reclamo);

    List<Calificacion> listarCalificaciones(Map<Integer, Compra> compras);
    /** Inserta la calificación (id 0) y le asigna el id generado por la base. Una compra solo se califica una vez. */
    void insertarCalificacion(Calificacion calificacion);
}
