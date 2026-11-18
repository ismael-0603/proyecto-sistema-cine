package ucsur.pe.db;

import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import java.util.List;
import java.util.Map;

/**
 * Contrato DAO de la infraestructura del cine: sede, sala, butaca, funcion y tipo_sala,
 * más las entradas que hoy ocupan una butaca (BLOQUEADA o VENDIDA).
 * Se agrupan porque se leen juntas: una sede trae sus salas, sus butacas y su cartelera.
 */
public interface SedeDao {

    /** Lo que se carga al arrancar: las sedes completas y las entradas que ocupan butacas (por id). */
    record Carga(List<Sede> sedes, Map<Integer, Entrada> entradas) { }

    List<TipoSala> listarTiposSala();

    /**
     * Arma cada sede con sus salas, butacas y funciones, y cuelga de cada función sus entradas BLOQUEADA
     * o VENDIDA (así esas butacas se ven ocupadas). Los tipos de sala, las películas y los tipos de entrada
     * se toman de los mapas (id -> objeto) para que haya un solo objeto por id en toda la aplicación.
     */
    Carga cargarSedes(Map<Integer, TipoSala> tiposSala, Map<Integer, Pelicula> peliculas,
                      Map<Integer, TipoEntrada> tiposEntrada);

    /** Inserta la sede y le asigna el id que genera la base (AUTO_INCREMENT). */
    void insertarSede(Sede sede);
    void actualizarSede(Sede sede);

    /** Inserta la sala y sus butacas; a la sala y a cada butaca les asigna el id que genera la base. */
    void insertarSala(Sala sala);
    /** Actualiza la sala; si se redistribuyó (butacas nuevas con id 0), reemplaza sus butacas. */
    void actualizarSala(Sala sala);

    /** Inserta la función y le asigna el id que genera la base. */
    void insertarFuncion(Funcion funcion);
    /** Guarda fecha, hora, idioma y estado (lo que puede cambiar al reprogramar, cancelar o reactivar). */
    void actualizarFuncion(Funcion funcion);
}
