package ucsur.pe.controller;

import ucsur.pe.model.Empleado;
import ucsur.pe.model.Producto;
import ucsur.pe.service.ProductoAdminService;
import java.util.List;

/** Acciones del administrador sobre los productos de dulcería (wireframe 20): registrar, editar, reponer y activar. */
public class AdminProductoController extends ControllerBase {
    private final ProductoAdminService servicio;

    public AdminProductoController(ProductoAdminService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<Producto>> listarProductos(Empleado admin) {
        return ejecutar("", () -> servicio.listarProductos(admin));
    }

    /** stockInicial: unidades con las que arranca en la sede del administrador (0 = sin stock). */
    public Respuesta<Producto> registrarProducto(Empleado admin, String nombre, String descripcion, String categoria,
                                                 long precioCentavos, boolean esCombo, int nivelSocioMinimo,
                                                 String imagen, int stockInicial) {
        return ejecutar("Producto registrado", () -> servicio.registrarProducto(admin, nombre, descripcion, categoria,
                precioCentavos, esCombo, nivelSocioMinimo, imagen, stockInicial));
    }

    public Respuesta<Void> actualizarProducto(Empleado admin, Producto producto, String nombre, String descripcion,
                                              String categoria, long precioCentavos, boolean esCombo, int nivelSocioMinimo,
                                              String imagen) {
        return ejecutar("Producto actualizado", () -> servicio.actualizarProducto(admin, producto, nombre, descripcion,
                categoria, precioCentavos, esCombo, nivelSocioMinimo, imagen));
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
}
