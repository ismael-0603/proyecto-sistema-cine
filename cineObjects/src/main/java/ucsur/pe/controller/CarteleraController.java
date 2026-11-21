package ucsur.pe.controller;

import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.service.CarteleraService;
import java.util.List;

/** Acciones de la cartelera: elegir sede, ver y filtrar funciones, ver butacas y promociones. */
public class CarteleraController extends ControllerBase {
    private final CarteleraService servicio;

    public CarteleraController(CarteleraService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<Sede>> listarSedes() {
        return ejecutar("", servicio::listarSedes);
    }

    public Respuesta<List<Funcion>> consultarCartelera(Sede sede) {
        return ejecutar("", () -> servicio.consultarCartelera(sede));
    }

    public Respuesta<List<Funcion>> filtrarCartelera(Sede sede, DateTime dia, Genero genero, String idioma, String formato,
                                                     String clasificacion) {
        return ejecutar("", () -> servicio.filtrarCartelera(sede, dia, genero, idioma, formato, clasificacion));
    }

    public Respuesta<List<Pelicula>> listarProximamente() {
        return ejecutar("", servicio::listarProximamente);
    }

    public Respuesta<List<Genero>> listarGeneros() {
        return ejecutar("", servicio::listarGeneros);
    }

    public Respuesta<List<Promocion>> consultarPromociones() {
        return ejecutar("", servicio::consultarPromociones);
    }

    /** Todas las promociones vigentes, apliquen o no hoy (pantalla «Promociones»). */
    public Respuesta<List<Promocion>> listarPromocionesVigentes() {
        return ejecutar("", servicio::listarPromocionesVigentes);
    }
}
