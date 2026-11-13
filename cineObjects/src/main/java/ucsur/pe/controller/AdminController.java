package ucsur.pe.controller;

import ucsur.pe.model.Activable;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.service.AdministracionService;

/** Acciones del administrador: películas, funciones, productos, stock, cupones y promociones. */
public class AdminController extends ControllerBase {
    private final AdministracionService servicio;

    public AdminController(AdministracionService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Void> registrarPelicula(Empleado admin, Pelicula pelicula) {
        return ejecutar("Película registrada", () -> servicio.registrarPelicula(admin, pelicula));
    }

    public Respuesta<Void> actualizarPelicula(Empleado admin, Pelicula pelicula, String titulo, String sinopsis,
                                              String clasificacion, String trailer, String poster, DateTime estreno) {
        return ejecutar("Película actualizada", () -> servicio.actualizarPelicula(admin, pelicula, titulo, sinopsis,
                clasificacion, trailer, poster, estreno));
    }

    public Respuesta<Void> programarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función programada", () -> servicio.programarFuncion(admin, funcion));
    }

    public Respuesta<Void> cancelarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función cancelada", () -> servicio.cancelarFuncion(admin, funcion));
    }

    public Respuesta<Void> registrarProducto(Empleado admin, Producto producto) {
        return ejecutar("Producto registrado", () -> servicio.registrarProducto(admin, producto));
    }

    public Respuesta<Void> reponerStock(Empleado admin, Producto producto, int cantidad) {
        return ejecutar("Stock repuesto", () -> servicio.reponerStock(admin, producto, cantidad));
    }

    public Respuesta<Void> registrarCupon(Empleado admin, Cupon cupon) {
        return ejecutar("Cupón registrado", () -> servicio.registrarCupon(admin, cupon));
    }

    public Respuesta<Void> registrarPromocion(Empleado admin, Promocion promocion) {
        return ejecutar("Promoción registrada", () -> servicio.registrarPromocion(admin, promocion));
    }

    public Respuesta<Void> cambiarEstado(Empleado admin, Activable elemento, boolean activo) {
        return ejecutar(activo ? "Activado" : "Desactivado", () -> servicio.cambiarEstado(admin, elemento, activo));
    }
}
