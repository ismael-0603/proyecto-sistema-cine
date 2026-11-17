package ucsur.pe.repository;

import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Sala (con sus butacas): define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple SalaMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface SalaRepository {
    void guardar(Sala sala);
    Optional<Sala> buscarPorId(int idSala);
    List<Sala> listarPorSede(Sede sede);
    int siguienteId();
}
