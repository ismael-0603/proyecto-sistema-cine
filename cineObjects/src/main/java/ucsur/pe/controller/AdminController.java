package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import ucsur.pe.service.AdministracionService;
import ucsur.pe.service.ResultadoCancelacion;
import ucsur.pe.util.ConversorDinero;
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

    // ---------- Funciones ----------
    public Respuesta<List<Funcion>> listarFunciones(Empleado admin) {
        return ejecutar("", () -> servicio.listarFunciones(admin));
    }

    public Respuesta<Funcion> programarFuncion(Empleado admin, Pelicula pelicula, Sala sala, DateTime fecha,
                                               DateTime hora, String idioma) {
        return ejecutar("Función programada", () -> servicio.programarFuncion(admin, pelicula, sala, fecha, hora, idioma));
    }

    public Respuesta<Void> editarFuncion(Empleado admin, Funcion funcion, DateTime fecha, DateTime hora, String idioma) {
        return ejecutar("Función actualizada", () -> servicio.editarFuncion(admin, funcion, fecha, hora, idioma));
    }

    /** Cancela la función y reembolsa automáticamente a quienes compraron; el mensaje resume lo ocurrido. */
    public Respuesta<ResultadoCancelacion> cancelarFuncion(Empleado admin, Funcion funcion) {
        Respuesta<ResultadoCancelacion> r = ejecutar("Función cancelada", () -> servicio.cancelarFuncion(admin, funcion));
        return r.ok() ? Respuesta.exito("Función cancelada: " + r.dato(), r.dato()) : r;
    }

    /** «Reintentar» del aviso de cancelación: vuelve a pedir el reembolso de una compra pendiente. */
    public Respuesta<Long> reintentarReembolso(Empleado admin, Compra compra) {
        Respuesta<Long> r = ejecutar("", () -> servicio.reintentarReembolso(admin, compra));
        return r.ok() ? Respuesta.exito("Reembolso hecho: " + ConversorDinero.formatear(r.dato()), r.dato()) : r;
    }

    public Respuesta<Void> reactivarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función reactivada", () -> servicio.reactivarFuncion(admin, funcion));
    }

    // ---------- Productos ----------
    public Respuesta<List<Producto>> listarProductos(Empleado admin) {
        return ejecutar("", () -> servicio.listarProductos(admin));
    }

    /** stockInicial: unidades con las que arranca en la sede del administrador (0 = sin stock). */
    public Respuesta<Producto> registrarProducto(Empleado admin, String nombre, String descripcion, String categoria,
                                                 long precioCentavos, boolean esCombo, int nivelSocioMinimo,
                                                 int stockInicial) {
        return ejecutar("Producto registrado", () -> servicio.registrarProducto(admin, nombre, descripcion, categoria,
                precioCentavos, esCombo, nivelSocioMinimo, stockInicial));
    }

    public Respuesta<Void> actualizarProducto(Empleado admin, Producto producto, String nombre, String descripcion,
                                              String categoria, long precioCentavos, boolean esCombo, int nivelSocioMinimo) {
        return ejecutar("Producto actualizado", () -> servicio.actualizarProducto(admin, producto, nombre, descripcion,
                categoria, precioCentavos, esCombo, nivelSocioMinimo));
    }

    /** Repone en la sede del administrador; el mensaje avisa si el producto sigue en stock bajo. */
    public Respuesta<Integer> reponerStock(Empleado admin, Producto producto, int cantidad) {
        Respuesta<Integer> r = ejecutar("", () -> servicio.reponerStock(admin, producto, cantidad));
        if (!r.ok()) return r;
        String aviso = r.dato() <= Producto.UMBRAL_STOCK_BAJO ? ", sigue en stock bajo" : "";
        return Respuesta.exito("Stock repuesto: quedan " + r.dato() + aviso, r.dato());
    }

    /** Productos con stock bajo o agotados en la sede del administrador (alerta de «Productos de dulcería»). */
    public Respuesta<List<Producto>> listarProductosConStockBajo(Empleado admin) {
        Respuesta<List<Producto>> r = ejecutar("", () -> servicio.listarProductosConStockBajo(admin));
        if (!r.ok() || r.dato().isEmpty()) return r;
        return Respuesta.exito(r.dato().size() + " producto(s) con stock bajo o agotados", r.dato());
    }

    public Respuesta<Void> cambiarEstadoProducto(Empleado admin, Producto producto, boolean activo) {
        return ejecutar(activo ? "Producto activado" : "Producto desactivado",
                () -> servicio.cambiarEstadoProducto(admin, producto, activo));
    }

    // ---------- Cupones y promociones ----------
    public Respuesta<List<Cupon>> listarCupones(Empleado admin) {
        return ejecutar("", () -> servicio.listarCupones(admin));
    }

    /** Formas de descuento para el ComboBox ("PORCENTAJE", "MONTO_FIJO"). */
    public Respuesta<List<String>> tiposDescuento() {
        return ejecutar("", servicio::tiposDescuento);
    }

    /**
     * El valor es el porcentaje (10 = 10%) o el monto en céntimos (500 = S/ 5.00), según el tipo.
     * nivelSocioMinimo: 0 = todos, 1 = cualquier socio, 2 a 4 = ese nivel o más.
     */
    public Respuesta<Cupon> registrarCupon(Empleado admin, String codigo, String tipoDescuento, long valor,
                                           DateTime inicio, DateTime fin, int limiteUso, int nivelSocioMinimo) {
        return ejecutar("Cupón registrado", () -> servicio.registrarCupon(admin, codigo, tipoDescuento, valor,
                inicio, fin, limiteUso, nivelSocioMinimo));
    }

    public Respuesta<Void> actualizarCupon(Empleado admin, Cupon cupon, String codigo, String tipoDescuento, long valor,
                                           DateTime inicio, DateTime fin, int limiteUso, int nivelSocioMinimo) {
        return ejecutar("Cupón actualizado", () -> servicio.actualizarCupon(admin, cupon, codigo, tipoDescuento, valor,
                inicio, fin, limiteUso, nivelSocioMinimo));
    }

    public Respuesta<Void> cambiarEstadoCupon(Empleado admin, Cupon cupon, boolean activo) {
        return ejecutar(activo ? "Cupón activado" : "Cupón desactivado",
                () -> servicio.cambiarEstadoCupon(admin, cupon, activo));
    }

    public Respuesta<List<Promocion>> listarPromociones(Empleado admin) {
        return ejecutar("", () -> servicio.listarPromociones(admin));
    }

    public Respuesta<Promocion> registrarPromocion(Empleado admin, String nombre, String descripcion,
                                                   String tipoDescuento, long valor, DateTime inicio, DateTime fin,
                                                   int nivelSocioMinimo, List<Integer> dias) {
        return ejecutar("Promoción registrada", () -> servicio.registrarPromocion(admin, nombre, descripcion,
                tipoDescuento, valor, inicio, fin, nivelSocioMinimo, dias));
    }

    public Respuesta<Void> actualizarPromocion(Empleado admin, Promocion promocion, String nombre, String descripcion,
                                               String tipoDescuento, long valor, DateTime inicio, DateTime fin,
                                               int nivelSocioMinimo, List<Integer> dias) {
        return ejecutar("Promoción actualizada", () -> servicio.actualizarPromocion(admin, promocion, nombre, descripcion,
                tipoDescuento, valor, inicio, fin, nivelSocioMinimo, dias));
    }

    public Respuesta<Void> cambiarEstadoPromocion(Empleado admin, Promocion promocion, boolean activo) {
        return ejecutar(activo ? "Promoción activada" : "Promoción desactivada",
                () -> servicio.cambiarEstadoPromocion(admin, promocion, activo));
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
    public Respuesta<List<TipoSala>> listarTiposSala(Empleado admin) {
        return ejecutar("", () -> servicio.listarTiposSala(admin));
    }

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
