package ucsur.pe.repository;

import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Sede: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple SedeMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface SedeRepository {
    void guardar(Sede sede);
    Optional<Sede> buscarPorId(int idSede);
    List<Sede> listarTodas();
}
