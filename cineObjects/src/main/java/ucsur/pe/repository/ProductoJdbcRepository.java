package ucsur.pe.repository;

import ucsur.pe.db.CatalogoDao;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/**
 * Implementa ProductoRepository con MySQL. Carga los productos con su stock en cada sede (inventario_sede);
 * guardar escribe el producto y su stock en todas las sedes.
 */
public class ProductoJdbcRepository extends RepositorioJdbc<Producto> implements ProductoRepository {
    private final CatalogoDao dao;
    private final SedeRepository sedes;

    public ProductoJdbcRepository(CatalogoDao dao, SedeRepository sedes) {
        super("el producto", Producto::getIdProducto);
        this.dao = dao;
        this.sedes = sedes;
        cargar(dao.listarProductos(porIdDe(sedes.listarTodas(), Sede::getIdSede)));
    }

    @Override
    public void guardar(Producto producto) {
        guardarCon(producto,
                p -> dao.insertarProducto(p, sedes.listarTodas()),
                p -> dao.actualizarProducto(p, sedes.listarTodas()));
    }

    @Override public Optional<Producto> buscarPorId(int idProducto) { return porId(idProducto); }
    @Override public List<Producto> listarTodos() { return todos(); }
}
