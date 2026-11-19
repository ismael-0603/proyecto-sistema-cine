package ucsur.pe.repository;

import ucsur.pe.model.Promocion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Promocion: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumple PromocionJdbcRepository (MySQL).
 */
public interface PromocionRepository {
    void guardar(Promocion promocion);
    Optional<Promocion> buscarPorId(int idPromocion);
    List<Promocion> listarTodas();
}
