package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import java.util.List;

/**
 * Contrato de persistencia de Reclamo: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple ReclamoMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface ReclamoRepository {
    /** Guarda el reclamo; si aún no tiene id, le asigna uno (como el AUTO_INCREMENT). */
    void guardar(Reclamo reclamo);
    List<Reclamo> listarPorCompra(Compra compra);
    List<Reclamo> listarTodos();
}
