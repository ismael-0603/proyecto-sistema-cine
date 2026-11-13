package ucsur.pe.repository;

import ucsur.pe.model.Promocion;
import java.util.List;

/**
 * Contrato de persistencia de Promocion: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple PromocionMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface PromocionRepository {
    void guardar(Promocion promocion);
    List<Promocion> listarTodas();
}
