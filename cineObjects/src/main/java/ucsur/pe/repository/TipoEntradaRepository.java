package ucsur.pe.repository;

import ucsur.pe.model.TipoEntrada;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de TipoEntrada (General, Socio, Niño...): define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen TipoEntradaMemoriaRepository y TipoEntradaJdbcRepository.
 */
public interface TipoEntradaRepository {
    void guardar(TipoEntrada tipoEntrada);
    Optional<TipoEntrada> buscarPorId(int idTipoEntrada);
    /** Tarifas que se pueden elegir al comprar (las activas). */
    List<TipoEntrada> listarActivos();
    List<TipoEntrada> listarTodos();
}
