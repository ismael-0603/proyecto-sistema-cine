package ucsur.pe.service;

import ucsur.pe.model.Butaca;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.PromocionRepository;
import ucsur.pe.repository.SedeRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso "Seleccionar sede", "Consultar cartelera", "Filtrar cartelera"
 * y "Consultar promociones" (RF-02 a RF-04, RF-28, RF-36).
 */
public class CarteleraService {
    private final SedeRepository sedes;
    private final PromocionRepository promociones;
    private final Reloj reloj;

    public CarteleraService(SedeRepository sedes, PromocionRepository promociones, Reloj reloj) {
        this.sedes = sedes;
        this.promociones = promociones;
        this.reloj = reloj;
    }

    /** Sedes entre las que elige el cliente al empezar (RF-36). */
    public List<Sede> listarSedes() {
        return sedes.listarTodas().stream().filter(Sede::estaOperativa).toList();
    }

    public List<Funcion> consultarCartelera(Sede sede) {
        return exigirSede(sede).consultarCartelera();
    }

    public List<Funcion> filtrarCartelera(Sede sede, String idioma, String clasificacion, Genero genero) {
        return exigirSede(sede).filtrarCartelera(idioma, clasificacion, genero);
    }

    /** Mapa de butacas libres de una función. */
    public List<Butaca> consultarButacasDisponibles(Funcion funcion) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        return funcion.obtenerButacasDisponibles();
    }

    /** Promociones que el cliente puede aprovechar hoy: activas, dentro de sus fechas y válidas para este día (RF-28). */
    public List<Promocion> consultarPromociones() {
        DateTime hoy = reloj.ahora();
        return promociones.listarTodas().stream()
                .filter(p -> p.validarVigencia(hoy) && p.aplicaEnDia(hoy))
                .toList();
    }

    private Sede exigirSede(Sede sede) {
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return sede;
    }
}
