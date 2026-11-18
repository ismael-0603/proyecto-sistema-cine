package ucsur.pe.repository;

import ucsur.pe.db.PeliculaDao;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.List;
import java.util.Optional;

/**
 * Implementa PeliculaRepository con MySQL (por medio del DAO), como ComprobanteJdbcRepository en MVCone.
 * Carga géneros y películas una vez al crearse; los servicios no saben que hay una BD detrás.
 */
public class PeliculaJdbcRepository extends RepositorioJdbc<Pelicula> implements PeliculaRepository {
    private final PeliculaDao dao;
    private final List<Genero> generos;

    public PeliculaJdbcRepository(PeliculaDao dao) {
        super("la película", Pelicula::getIdPelicula);
        this.dao = dao;
        this.generos = dao.listarGeneros();
        cargar(dao.listarTodas(porIdDe(generos, Genero::getIdGenero)));
    }

    @Override public void guardar(Pelicula pelicula) { guardarCon(pelicula, dao::insertar, dao::actualizar); }
    @Override public Optional<Pelicula> buscarPorId(int idPelicula) { return porId(idPelicula); }
    @Override public List<Pelicula> listarTodas() { return todos(); }

    /** Géneros de la BD, para los filtros de la cartelera y el formulario de películas. */
    @Override public List<Genero> listarGeneros() { return List.copyOf(generos); }
}
