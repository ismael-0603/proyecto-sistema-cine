package ucsur.pe.db;

import ucsur.pe.model.Cupon;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Contrato DAO de lo que se vende y se descuenta: tipo_entrada, producto (con inventario_sede),
 * cupon y promocion (con promocion_dia).
 */
public interface CatalogoDao {

    List<TipoEntrada> listarTiposEntrada();
    void insertarTipoEntrada(TipoEntrada tipo);
    void actualizarTipoEntrada(TipoEntrada tipo);

    /** Productos con su stock en cada sede; las sedes se toman del mapa (id -> Sede). */
    List<Producto> listarProductos(Map<Integer, Sede> sedes);
    /** Inserta el producto y su stock en las sedes indicadas. */
    void insertarProducto(Producto producto, Collection<Sede> sedes);
    /** Actualiza el producto y su stock en las sedes indicadas. */
    void actualizarProducto(Producto producto, Collection<Sede> sedes);

    List<Cupon> listarCupones();
    void insertarCupon(Cupon cupon);
    /** Guarda también los usos realizados (cambian al confirmar o reembolsar una compra). */
    void actualizarCupon(Cupon cupon);

    List<Promocion> listarPromociones();
    void insertarPromocion(Promocion promocion);
    void actualizarPromocion(Promocion promocion);
}
