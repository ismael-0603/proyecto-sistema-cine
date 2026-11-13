package ucsur.pe.controller;

import ucsur.pe.model.Butaca;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
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

    public Respuesta<List<Funcion>> filtrarCartelera(Sede sede, String idioma, String clasificacion, Genero genero) {
        return ejecutar("", () -> servicio.filtrarCartelera(sede, idioma, clasificacion, genero));
    }

    public Respuesta<List<Butaca>> consultarButacasDisponibles(Funcion funcion) {
        return ejecutar("", () -> servicio.consultarButacasDisponibles(funcion));
    }

    public Respuesta<List<Promocion>> consultarPromociones() {
        return ejecutar("", servicio::consultarPromociones);
    }
}
