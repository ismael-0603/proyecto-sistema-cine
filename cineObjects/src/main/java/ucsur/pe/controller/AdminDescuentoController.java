package ucsur.pe.controller;

import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Promocion;
import ucsur.pe.service.DescuentoAdminService;
import java.util.List;

/**
 * Acciones del administrador sobre cupones y promociones (wireframe 19). El valor del descuento es el porcentaje
 * (10 = 10%) o el monto en céntimos (500 = S/ 5.00), según el tipo; nivelSocioMinimo: 0 = todos,
 * 1 = cualquier socio, 2 a 4 = ese nivel o más.
 */
public class AdminDescuentoController extends ControllerBase {
    private final DescuentoAdminService servicio;

    public AdminDescuentoController(DescuentoAdminService servicio) {
        this.servicio = servicio;
    }

    /** Formas de descuento para el ComboBox ("PORCENTAJE", "MONTO_FIJO"). */
    public Respuesta<List<String>> tiposDescuento() {
        return ejecutar("", servicio::tiposDescuento);
    }

    // ---------- Cupones ----------
    public Respuesta<List<Cupon>> listarCupones(Empleado admin) {
        return ejecutar("", () -> servicio.listarCupones(admin));
    }

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

    // ---------- Promociones ----------
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
}
