package ucsur.pe.repository;

import ucsur.pe.model.Cupon;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Cupon: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple CuponMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface CuponRepository {
    void guardar(Cupon cupon);
    /** Busca por el código que escribe el cliente; no distingue mayúsculas. */
    Optional<Cupon> buscarPorCodigo(String codigo);
    List<Cupon> listarTodos();
}
