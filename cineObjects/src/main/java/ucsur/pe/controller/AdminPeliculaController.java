package ucsur.pe.controller;

import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.service.PeliculaAdminService;
import java.util.List;

/** Acciones del administrador sobre las películas (wireframe 17): listar, registrar, editar y retirar. */
public class AdminPeliculaController extends ControllerBase {
    private final PeliculaAdminService servicio;

    public AdminPeliculaController(PeliculaAdminService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<Pelicula>> listarPeliculas(Empleado admin) {
        return ejecutar("", () -> servicio.listarPeliculas(admin));
    }

    public Respuesta<List<Genero>> listarGeneros(Empleado admin) {
        return ejecutar("", () -> servicio.listarGeneros(admin));
    }

    public Respuesta<Pelicula> registrarPelicula(Empleado admin, String titulo, String sinopsis, int duracion,
                                                 String clasificacion, String trailer, String poster,
                                                 DateTime estreno, String estado, List<Genero> generos) {
        return ejecutar("Película registrada", () -> servicio.registrarPelicula(admin, titulo, sinopsis, duracion,
                clasificacion, trailer, poster, estreno, estado, generos));
    }

    public Respuesta<Void> actualizarPelicula(Empleado admin, Pelicula pelicula, String titulo, String sinopsis,
                                              String clasificacion, String trailer, String poster, DateTime estreno,
                                              String estado, List<Genero> generos) {
        return ejecutar("Película actualizada", () -> servicio.actualizarPelicula(admin, pelicula, titulo, sinopsis,
                clasificacion, trailer, poster, estreno, estado, generos));
    }

    public Respuesta<Void> retirarPelicula(Empleado admin, Pelicula pelicula) {
        return ejecutar("Película retirada de cartelera", () -> servicio.retirarPelicula(admin, pelicula));
    }
}
