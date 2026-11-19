package ucsur.pe.repository;

import ucsur.pe.db.CompraDao;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TransaccionPago;
import java.util.List;
import java.util.Optional;

/**
 * Implementa CompraRepository con MySQL. Al crearse carga TODAS las compras guardadas (con sus entradas,
 * su pedido y sus pagos), armadas con los mismos objetos que ya cargaron los otros repositorios:
 * así funcionan las búsquedas de sesiones anteriores (recuperar por código o correo, historial del cliente,
 * cancelar una función con compras pagadas) y las reglas que comparan con == siguen valiendo.
 * Guardar una compra escribe todo lo que cambió en una transacción (ver CompraMySqlDao.guardarCompra).
 */
public class CompraJdbcRepository extends RepositorioJdbc<Compra> implements CompraRepository {
    private final CompraDao dao;
    private final List<TransaccionPago> pagosCargados;

    public CompraJdbcRepository(CompraDao dao, ClienteRepository clientes, EmpleadoRepository empleados,
                                SedeJdbcRepository sedes, CuponRepository cupones,
                                PromocionRepository promociones, TipoEntradaRepository tiposEntrada,
                                ProductoRepository productos) {
        super("la compra", Compra::getIdCompra);
        this.dao = dao;
        CompraDao.Referencias referencias = new CompraDao.Referencias() {
            @Override public Cliente cliente(int id) { return clientes.buscarPorId(id).orElse(null); }
            @Override public Sede sede(int id) { return sedes.buscarPorId(id).orElse(null); }
            @Override public Cupon cupon(int id) { return cupones.buscarPorId(id).orElse(null); }
            @Override public Promocion promocion(int id) { return promociones.buscarPorId(id).orElse(null); }
            @Override public Funcion funcion(int id) { return sedes.buscarFuncion(id).orElse(null); }
            @Override public Butaca butaca(int id) { return sedes.buscarButaca(id).orElse(null); }
            @Override public TipoEntrada tipoEntrada(int id) { return tiposEntrada.buscarPorId(id).orElse(null); }
            @Override public Producto producto(int id) { return productos.buscarPorId(id).orElse(null); }
            @Override public Empleado empleado(int id) { return empleados.buscarPorId(id).orElse(null); }
            @Override public Entrada entradaCargada(int id) { return sedes.buscarEntrada(id).orElse(null); }
        };
        CompraDao.Carga carga = dao.cargarCompras(referencias);
        cargar(carga.compras());
        this.pagosCargados = carga.pagos();
    }

    @Override
    public void guardar(Compra compra) {
        guardarCon(compra, c -> dao.guardarCompra(c, true), c -> dao.guardarCompra(c, false));
    }

    @Override public List<Compra> listarTodas() { return todos(); }

    /** Guarda la compra y su pago aprobado en una sola transacción; lo usa PagoJdbcRepository.guardarConCompra. */
    void guardarConPago(Compra compra, TransaccionPago pago) {
        guardarCon(compra, c -> dao.guardarCompraConPago(c, true, pago), c -> dao.guardarCompraConPago(c, false, pago));
    }

    /** La compra que tiene este pedido (en la BD, pedido.id_compra). */
    Optional<Compra> buscarPorPedido(Pedido pedido) {
        return filtrar(c -> c.getPedido() == pedido).stream().findFirst();
    }

    /** Los pagos que se leyeron junto con las compras; los usa PagoJdbcRepository. */
    List<TransaccionPago> pagosCargados() { return pagosCargados; }
}
