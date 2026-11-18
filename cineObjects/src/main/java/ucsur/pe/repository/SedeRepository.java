package ucsur.pe.repository;

import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Sede: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen SedeMemoriaRepository (demos en memoria) y SedeJdbcRepository (MySQL).
 */
public interface SedeRepository {
    void guardar(Sede sede);
    Optional<Sede> buscarPorId(int idSede);
    List<Sede> listarTodas();
}
