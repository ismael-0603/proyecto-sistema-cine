package ucsur.pe.repository;

import ucsur.pe.model.Sala;
import ucsur.pe.model.TipoSala;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Sala (con sus butacas): define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen SalaMemoriaRepository (demos en memoria) y SalaJdbcRepository (MySQL).
 */
public interface SalaRepository {
    void guardar(Sala sala);
    Optional<Sala> buscarPorId(int idSala);
    /** Formatos de sala (2D, 3D...) que se pueden elegir al crear o editar una sala. */
    List<TipoSala> listarTiposSala();
}
