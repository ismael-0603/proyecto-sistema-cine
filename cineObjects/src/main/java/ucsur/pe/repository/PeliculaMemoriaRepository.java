package ucsur.pe.repository;

import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de PeliculaRepository (una lista hace de tabla). */
public class PeliculaMemoriaRepository extends RepositorioMemoria<Pelicula> implements PeliculaRepository {
    public PeliculaMemoriaRepository() { super(Pelicula::getIdPelicula, Pelicula::asignarId); }

    @Override public void guardar(Pelicula pelicula) { agregar(pelicula); }
    @Override public Optional<Pelicula> buscarPorId(int idPelicula) { return porId(idPelicula); }
    @Override public List<Pelicula> listarTodas() { return todos(); }

    /** En memoria no hay tabla de géneros: son los de las películas guardadas, sin repetir. */
    @Override
    public List<Genero> listarGeneros() {
        List<Genero> generos = new ArrayList<>();
        for (Pelicula p : todos()) {
            for (Genero g : p.getGeneros()) {
                if (generos.stream().noneMatch(x -> x.getIdGenero() == g.getIdGenero())) generos.add(g);
            }
        }
        return generos;
    }
}
