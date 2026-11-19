package ucsur.pe.repository;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;
import java.util.Optional;

/**
 * Contrato de persistencia de Calificacion: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen CalificacionMemoriaRepository (demos en memoria) y CalificacionJdbcRepository (MySQL).
 */
public interface CalificacionRepository {
    /** Guarda la calificación; si aún no tiene id, le asigna uno (como el AUTO_INCREMENT). */
    void guardar(Calificacion calificacion);
    /** En la BD hay una sola calificación por compra (uq_calificacion_compra). */
    boolean existePorCompra(Compra compra);
    /** La calificación de la compra, si ya la calificaron (para mostrar «Calificado ★ n»). */
    Optional<Calificacion> buscarPorCompra(Compra compra);
}
