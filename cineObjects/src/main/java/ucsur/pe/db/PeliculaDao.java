package ucsur.pe.db;

import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.List;
import java.util.Map;

/**
 * Contrato DAO de películas (tablas pelicula, genero y pelicula_genero), como ComprobanteDao en MVCone:
 * habla con objetos del modelo y esconde el SQL a las capas de arriba.
 */
public interface PeliculaDao {

    List<Genero> listarGeneros();

    /** Todas las películas con sus géneros; los géneros se toman del mapa (id -> Genero) para no repetir objetos. */
    List<Pelicula> listarTodas(Map<Integer, Genero> generos);

    /** Inserta la película con su id y sus géneros. */
    void insertar(Pelicula pelicula);

    /** Actualiza los datos y reemplaza sus géneros. */
    void actualizar(Pelicula pelicula);
}
