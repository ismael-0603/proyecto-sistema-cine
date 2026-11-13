package ucsur.pe.repository;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;

/**
 * Contrato de persistencia de Calificacion: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple CalificacionMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface CalificacionRepository {
    void guardar(Calificacion calificacion);
    /** En la BD hay una sola calificación por compra (uq_calificacion_compra). */
    boolean existePorCompra(Compra compra);
}
