package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.PeliculaRepository;
import ucsur.pe.repository.SedeRepository;
import java.util.List;

import static ucsur.pe.service.PermisosAdmin.exigirAdmin;

/**
 * Por qué existe: casos de uso del administrador sobre el catálogo de películas (wireframe 17, RF-37).
 * Todas las operaciones exigen el permiso ADMINISTRAR. «Eliminar» es una baja lógica (FUERA_DE_CARTELERA):
 * las funciones y compras antiguas siguen apuntando a la película y el historial no se pierde.
 * Como ComprobanteService de MVCone, al registrar recibe los datos sueltos y crea el objeto aquí:
 * la vista no construye modelos ni maneja ids (los genera la base, AUTO_INCREMENT).
 */
public class PeliculaAdminService {
    private final PeliculaRepository peliculas;
    private final SedeRepository sedes; // para saber si la película tiene funciones pendientes en alguna sede

    public PeliculaAdminService(PeliculaRepository peliculas, SedeRepository sedes) {
        this.peliculas = peliculas;
        this.sedes = sedes;
    }

    /** Crea la película con sus géneros y la guarda; la base le genera el id. */
    public Pelicula registrarPelicula(Empleado admin, String titulo, String sinopsis, int duracion,
                                      String clasificacion, String trailer, String poster, DateTime fechaEstreno,
                                      String estado, List<Genero> generos) {
        exigirAdmin(admin);
        Pelicula pelicula = new Pelicula(0, titulo, sinopsis, duracion, clasificacion, trailer, poster, fechaEstreno, estado);
        pelicula.cambiarGeneros(generos);
        peliculas.guardar(pelicula);
        return pelicula;
    }

    /**
     * «Guardar película» al editar (wireframe 17.1): datos, estado y géneros. La duración no se edita.
     * Sacarla de la cartelera (Próximamente o Fuera de cartelera) exige que no tenga funciones pendientes,
     * igual que «Retirar».
     */
    public void actualizarPelicula(Empleado admin, Pelicula pelicula, String titulo, String sinopsis,
                                   String clasificacion, String trailer, String poster, DateTime fechaEstreno,
                                   String estado, List<Genero> generos) {
        exigirAdmin(admin);
        if (pelicula == null) throw new IllegalArgumentException("La película es obligatoria");
        boolean saleDeCartelera = !Pelicula.EN_CARTELERA.equals(estado) && !Pelicula.PREESTRENO.equals(estado)
                && !Pelicula.REESTRENO.equals(estado);
        if (pelicula.estaEnCartelera() && saleDeCartelera) {
            exigirSinFuncionesPendientes(pelicula);
        }
        pelicula.actualizarInformacion(titulo, sinopsis, clasificacion, trailer, poster, fechaEstreno, estado);
        pelicula.cambiarGeneros(generos);
        peliculas.guardar(pelicula);
    }

    /** Géneros para el formulario de películas. */
    public List<Genero> listarGeneros(Empleado admin) {
        exigirAdmin(admin);
        return peliculas.listarGeneros();
    }

    /** «Eliminar» película: pasa a FUERA_DE_CARTELERA si no tiene funciones pendientes en ninguna sede. */
    public void retirarPelicula(Empleado admin, Pelicula pelicula) {
        exigirAdmin(admin);
        if (pelicula == null) throw new IllegalArgumentException("La película es obligatoria");
        exigirSinFuncionesPendientes(pelicula);
        pelicula.setEstado(Pelicula.FUERA_DE_CARTELERA);
        peliculas.guardar(pelicula);
    }

    public List<Pelicula> listarPeliculas(Empleado admin) {
        exigirAdmin(admin);
        return peliculas.listarTodas();
    }

    /** Una película con funciones pendientes en alguna sede no puede salir de la cartelera (wireframe 17.3). */
    private void exigirSinFuncionesPendientes(Pelicula pelicula) {
        for (Sede sede : sedes.listarTodas()) {
            for (Funcion f : sede.getCartelera()) {
                if (f.getPelicula() == pelicula && f.estaPendiente()) {
                    throw new ReglaNegocioException("La película tiene funciones pendientes en " + sede.getNombre()
                            + "; cancélelas antes de retirarla");
                }
            }
        }
    }
}
