package ucsur.pe.service;

import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.PeliculaRepository;
import ucsur.pe.repository.PromocionRepository;
import ucsur.pe.repository.SedeRepository;
import java.util.Comparator;
import java.util.List;

/**
 * Por qué existe: casos de uso "Seleccionar sede", "Consultar cartelera", "Filtrar cartelera"
 * y "Consultar promociones" (RF-02 a RF-04, RF-28, RF-36).
 */
public class CarteleraService {
    private final SedeRepository sedes;
    private final PromocionRepository promociones;
    private final PeliculaRepository peliculas;
    private final Reloj reloj;

    public CarteleraService(SedeRepository sedes, PromocionRepository promociones, PeliculaRepository peliculas,
                            Reloj reloj) {
        this.sedes = sedes;
        this.promociones = promociones;
        this.peliculas = peliculas;
        this.reloj = reloj;
    }

    /** Sedes entre las que elige el cliente al empezar (RF-36). */
    public List<Sede> listarSedes() {
        return sedes.listarTodas().stream().filter(Sede::estaOperativa).toList();
    }

    public List<Funcion> consultarCartelera(Sede sede) {
        return exigirSede(sede).consultarCartelera();
    }

    /** Filtros de la cartelera (RF-03): día, género, idioma, formato de sala y clasificación; los nulos no filtran. */
    public List<Funcion> filtrarCartelera(Sede sede, DateTime dia, Genero genero, String idioma, String formato,
                                          String clasificacion) {
        return exigirSede(sede).filtrarCartelera(dia, genero, idioma, formato, clasificacion);
    }

    /**
     * Películas anunciadas que todavía no tienen funciones (pestaña «Próximamente»), de la que se estrena
     * antes a la que se estrena después; las que no tienen fecha van al final.
     */
    public List<Pelicula> listarProximamente() {
        return peliculas.listarTodas().stream()
                .filter(p -> Pelicula.PROXIMAMENTE.equals(p.getEstado()))
                .sorted(Comparator.comparingLong(p -> p.getFechaEstreno() == null
                        ? Long.MAX_VALUE : p.getFechaEstreno().toValorSoloFecha()))
                .toList();
    }

    /** Géneros para el filtro de la cartelera (RF-03), sin escribirlos en la vista. */
    public List<Genero> listarGeneros() {
        return peliculas.listarGeneros();
    }

    /** Promociones que el cliente puede aprovechar hoy: activas, dentro de sus fechas y válidas para este día (RF-28). */
    public List<Promocion> consultarPromociones() {
        DateTime hoy = reloj.ahora();
        return promociones.listarTodas().stream()
                .filter(p -> p.validarVigencia(hoy) && p.aplicaEnDia(hoy))
                .toList();
    }

    /**
     * Promociones vigentes (activas y dentro de sus fechas), aunque hoy no sea su día: es lo que muestra
     * la pantalla «Promociones» (wireframe 10), donde cada una dice qué días aplica.
     */
    public List<Promocion> listarPromocionesVigentes() {
        DateTime hoy = reloj.ahora();
        return promociones.listarTodas().stream()
                .filter(p -> p.validarVigencia(hoy))
                .toList();
    }

    private Sede exigirSede(Sede sede) {
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return sede;
    }
}
