package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Producto;
import ucsur.pe.repository.ProductoRepository;
import java.util.List;

import static ucsur.pe.service.PermisosAdmin.exigirAdmin;

/**
 * Por qué existe: casos de uso del administrador sobre los productos de dulcería (wireframe 20, RF-38):
 * registrar, editar, reponer stock y activar o desactivar. Cada administrador solo maneja el inventario
 * de la sede donde trabaja. «Eliminar» es desactivar: los pedidos antiguos siguen apuntando al producto.
 */
public class ProductoAdminService {
    private final ProductoRepository productos;

    public ProductoAdminService(ProductoRepository productos) {
        this.productos = productos;
    }

    /**
     * Crea el producto activo con su stock inicial en la sede del administrador (0 = sin stock) y su foto
     * opcional. La base le genera el id.
     */
    public Producto registrarProducto(Empleado admin, String nombre, String descripcion, String categoria,
                                      long precioCentavos, boolean esCombo, int nivelSocioMinimo, String imagen,
                                      int stockInicial) {
        exigirAdmin(admin);
        if (stockInicial < 0) throw new IllegalArgumentException("El stock inicial no puede ser negativo");
        exigirNombreLibre(nombre, null);
        Producto producto = new Producto(0, nombre, descripcion, categoria, precioCentavos, esCombo, nivelSocioMinimo, true);
        producto.asignarImagen(imagen);
        if (stockInicial > 0) producto.reponerStock(admin.getSede(), stockInicial);
        productos.guardar(producto);
        return producto;
    }

    /** La foto es opcional: vacía o null deja el producto sin foto (la dulcería muestra su categoría). */
    public void actualizarProducto(Empleado admin, Producto producto, String nombre, String descripcion,
                                   String categoria, long precioCentavos, boolean esCombo, int nivelSocioMinimo,
                                   String imagen) {
        exigirAdmin(admin);
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        exigirNombreLibre(nombre, producto);
        producto.actualizar(nombre, descripcion, categoria, precioCentavos, esCombo, nivelSocioMinimo);
        producto.asignarImagen(imagen);
        productos.guardar(producto);
    }

    private void exigirNombreLibre(String nombre, Producto propio) {
        if (nombre == null || nombre.isBlank()) return;
        String buscado = nombre.trim();
        for (Producto p : productos.listarTodos()) {
            if (p != propio && p.getNombre().equalsIgnoreCase(buscado)) {
                throw new ReglaNegocioException("Ya existe un producto con el nombre " + buscado);
            }
        }
    }

    /**
     * Repone stock del producto solo en la sede donde trabaja el administrador (no en otras sedes).
     * Devuelve el stock que queda, para avisar si sigue en stock bajo.
     */
    public int reponerStock(Empleado admin, Producto producto, int cantidad) {
        exigirAdmin(admin);
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        producto.reponerStock(admin.getSede(), cantidad);
        productos.guardar(producto);
        return producto.getStock(admin.getSede());
    }

    /** Habilita o retira un producto de la venta y lo guarda. */
    public void cambiarEstadoProducto(Empleado admin, Producto producto, boolean activo) {
        exigirAdmin(admin);
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        producto.cambiarEstado(activo);
        productos.guardar(producto);
    }

    /** Alerta del wireframe 20: productos activos con stock bajo o agotados en la sede del administrador. */
    public List<Producto> listarProductosConStockBajo(Empleado admin) {
        exigirAdmin(admin);
        return productos.listarTodos().stream().filter(p -> p.requiereReposicion(admin.getSede())).toList();
    }

    public List<Producto> listarProductos(Empleado admin) {
        exigirAdmin(admin);
        return productos.listarTodos();
    }
}
