package ucsur.pe.repository;

import ucsur.pe.model.Pelicula;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Pelicula: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple PeliculaMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface PeliculaRepository {
    void guardar(Pelicula pelicula);
    Optional<Pelicula> buscarPorId(int idPelicula);
    List<Pelicula> listarTodas();
}
