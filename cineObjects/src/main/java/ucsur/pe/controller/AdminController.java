package ucsur.pe.controller;

import ucsur.pe.model.Activable;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import ucsur.pe.service.AdministracionService;
import ucsur.pe.service.ResultadoCancelacion;
import ucsur.pe.tipo.CalculoDescuento;
import java.util.List;

/**
 * Acciones del administrador (wireframes 17 a 21 y las pantallas de Sedes y Salas): listar, crear, editar
 * y dar de baja películas, funciones, productos, cupones, promociones, sedes y salas.
 */
public class AdminController extends ControllerBase {
    private final AdministracionService servicio;

    public AdminController(AdministracionService servicio) {
        this.servicio = servicio;
    }

    // ---------- Películas ----------
    public Respuesta<List<Pelicula>> listarPeliculas(Empleado admin) {
        return ejecutar("", () -> servicio.listarPeliculas(admin));
    }

    public Respuesta<Void> registrarPelicula(Empleado admin, Pelicula pelicula) {
        return ejecutar("Película registrada", () -> servicio.registrarPelicula(admin, pelicula));
    }

    public Respuesta<Void> actualizarPelicula(Empleado admin, Pelicula pelicula, String titulo, String sinopsis,
                                              String clasificacion, String trailer, String poster, DateTime estreno) {
        return ejecutar("Película actualizada", () -> servicio.actualizarPelicula(admin, pelicula, titulo, sinopsis,
                clasificacion, trailer, poster, estreno));
    }

    public Respuesta<Void> retirarPelicula(Empleado admin, Pelicula pelicula) {
        return ejecutar("Película retirada de cartelera", () -> servicio.retirarPelicula(admin, pelicula));
    }

    // ---------- Funciones ----------
    public Respuesta<List<Funcion>> listarFunciones(Empleado admin) {
        return ejecutar("", () -> servicio.listarFunciones(admin));
    }

    public Respuesta<Void> programarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función programada", () -> servicio.programarFuncion(admin, funcion));
    }

    public Respuesta<Void> editarFuncion(Empleado admin, Funcion funcion, DateTime fecha, DateTime hora, String idioma) {
        return ejecutar("Función actualizada", () -> servicio.editarFuncion(admin, funcion, fecha, hora, idioma));
    }

    /** Cancela la función y reembolsa automáticamente a quienes compraron; el mensaje resume lo ocurrido. */
    public Respuesta<ResultadoCancelacion> cancelarFuncion(Empleado admin, Funcion funcion) {
        Respuesta<ResultadoCancelacion> r = ejecutar("Función cancelada", () -> servicio.cancelarFuncion(admin, funcion));
        return r.ok() ? Respuesta.exito("Función cancelada: " + r.dato(), r.dato()) : r;
    }

    public Respuesta<Void> reactivarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función reactivada", () -> servicio.reactivarFuncion(admin, funcion));
    }

    // ---------- Productos ----------
    public Respuesta<List<Producto>> listarProductos(Empleado admin) {
        return ejecutar("", () -> servicio.listarProductos(admin));
    }

    public Respuesta<Void> registrarProducto(Empleado admin, Producto producto) {
        return ejecutar("Producto registrado", () -> servicio.registrarProducto(admin, producto));
    }

    public Respuesta<Void> actualizarProducto(Empleado admin, Producto producto, String nombre, String descripcion,
                                              String categoria, long precioCentavos, boolean esCombo, int nivelSocioMinimo) {
        return ejecutar("Producto actualizado", () -> servicio.actualizarProducto(admin, producto, nombre, descripcion,
                categoria, precioCentavos, esCombo, nivelSocioMinimo));
    }

    public Respuesta<Void> reponerStock(Empleado admin, Producto producto, int cantidad) {
        return ejecutar("Stock repuesto", () -> servicio.reponerStock(admin, producto, cantidad));
    }

    // ---------- Cupones y promociones ----------
    public Respuesta<List<Cupon>> listarCupones(Empleado admin) {
        return ejecutar("", () -> servicio.listarCupones(admin));
    }

    public Respuesta<Void> registrarCupon(Empleado admin, Cupon cupon) {
        return ejecutar("Cupón registrado", () -> servicio.registrarCupon(admin, cupon));
    }

    public Respuesta<Void> actualizarCupon(Empleado admin, Cupon cupon, String codigo, CalculoDescuento calculo,
                                           DateTime inicio, DateTime fin, int limiteUso) {
        return ejecutar("Cupón actualizado", () -> servicio.actualizarCupon(admin, cupon, codigo, calculo, inicio, fin, limiteUso));
    }

    public Respuesta<List<Promocion>> listarPromociones(Empleado admin) {
        return ejecutar("", () -> servicio.listarPromociones(admin));
    }

    public Respuesta<Void> registrarPromocion(Empleado admin, Promocion promocion) {
        return ejecutar("Promoción registrada", () -> servicio.registrarPromocion(admin, promocion));
    }

    public Respuesta<Void> actualizarPromocion(Empleado admin, Promocion promocion, String nombre, String descripcion,
                                               CalculoDescuento calculo, DateTime inicio, DateTime fin,
                                               boolean soloSocios, List<Integer> dias) {
        return ejecutar("Promoción actualizada", () -> servicio.actualizarPromocion(admin, promocion, nombre, descripcion,
                calculo, inicio, fin, soloSocios, dias));
    }

    public Respuesta<Void> cambiarEstado(Empleado admin, Activable elemento, boolean activo) {
        return ejecutar(activo ? "Activado" : "Desactivado", () -> servicio.cambiarEstado(admin, elemento, activo));
    }

    // ---------- Sedes ----------
    public Respuesta<List<Sede>> listarSedes(Empleado admin) {
        return ejecutar("", () -> servicio.listarSedes(admin));
    }

    public Respuesta<Sede> registrarSede(Empleado admin, String nombre, String direccion, String distrito,
                                         String telefono, String horario) {
        return ejecutar("Sede registrada", () -> servicio.registrarSede(admin, nombre, direccion, distrito, telefono, horario));
    }

    public Respuesta<Void> actualizarSede(Empleado admin, Sede sede, String nombre, String direccion, String distrito,
                                          String telefono, String horario) {
        return ejecutar("Sede actualizada", () -> servicio.actualizarSede(admin, sede, nombre, direccion, distrito, telefono, horario));
    }

    public Respuesta<Void> cambiarEstadoSede(Empleado admin, Sede sede, String estado) {
        return ejecutar("Estado de la sede: " + estado, () -> servicio.cambiarEstadoSede(admin, sede, estado));
    }

    public Respuesta<Void> darDeBajaSede(Empleado admin, Sede sede) {
        return ejecutar("Sede dada de baja", () -> servicio.darDeBajaSede(admin, sede));
    }

    // ---------- Salas ----------
    public Respuesta<List<Sala>> listarSalas(Empleado admin, Sede sede) {
        return ejecutar("", () -> servicio.listarSalas(admin, sede));
    }

    public Respuesta<Sala> registrarSala(Empleado admin, Sede sede, int numero, TipoSala tipoSala,
                                         int filas, int lateral, int central) {
        return ejecutar("Sala registrada", () -> servicio.registrarSala(admin, sede, numero, tipoSala, filas, lateral, central));
    }

    public Respuesta<Void> actualizarSala(Empleado admin, Sala sala, int numero, TipoSala tipoSala) {
        return ejecutar("Sala actualizada", () -> servicio.actualizarSala(admin, sala, numero, tipoSala));
    }

    public Respuesta<Void> redistribuirSala(Empleado admin, Sala sala, int filas, int lateral, int central) {
        return ejecutar("Butacas redistribuidas", () -> servicio.redistribuirSala(admin, sala, filas, lateral, central));
    }

    public Respuesta<Void> cambiarEstadoSala(Empleado admin, Sala sala, String estado) {
        return ejecutar("Estado de la sala: " + estado, () -> servicio.cambiarEstadoSala(admin, sala, estado));
    }

    public Respuesta<Void> darDeBajaSala(Empleado admin, Sala sala) {
        return ejecutar("Sala dada de baja", () -> servicio.darDeBajaSala(admin, sala));
    }
}
