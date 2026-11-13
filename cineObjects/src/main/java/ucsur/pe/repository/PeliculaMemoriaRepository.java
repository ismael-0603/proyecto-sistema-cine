package ucsur.pe.repository;

import ucsur.pe.model.Pelicula;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de PeliculaRepository (una lista hace de tabla). */
public class PeliculaMemoriaRepository extends RepositorioMemoria<Pelicula> implements PeliculaRepository {
    @Override public void guardar(Pelicula pelicula) { agregar(pelicula); }
    @Override public Optional<Pelicula> buscarPorId(int idPelicula) { return buscarPrimero(p -> p.getIdPelicula() == idPelicula); }
    @Override public List<Pelicula> listarTodas() { return todos(); }
}
