package ucsur.pe.repository;

import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Pelicula: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumple PeliculaJdbcRepository (MySQL).
 */
public interface PeliculaRepository {
    void guardar(Pelicula pelicula);
    Optional<Pelicula> buscarPorId(int idPelicula);
    List<Pelicula> listarTodas();
    /** Géneros que se pueden asignar a una película (y usar en el filtro de la cartelera). */
    List<Genero> listarGeneros();
}
