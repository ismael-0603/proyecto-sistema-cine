package ucsur.pe.db;

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

/**
 * Contrato DAO de la venta: compra, entrada, pedido, detalle_pedido y transaccion_pago.
 * Se agrupan porque una compra se guarda junto con sus entradas y su pedido, en una sola transacción.
 */
public interface CompraDao {

    /**
     * Cómo encontrar los objetos que ya cargaron los otros repositorios (un solo objeto por id).
     * Cada método devuelve null si el id no existe.
     */
    interface Referencias {
        Cliente cliente(int idCliente);
        Sede sede(int idSede);
        Cupon cupon(int idCupon);
        Promocion promocion(int idPromocion);
        Funcion funcion(int idFuncion);
        Butaca butaca(int idButaca);
        TipoEntrada tipoEntrada(int idTipoEntrada);
        Producto producto(int idProducto);
        Empleado empleado(int idEmpleado);
        /** Una entrada BLOQUEADA o VENDIDA que ya cuelga de su función, para reutilizarla. */
        Entrada entradaCargada(int idEntrada);
    }

    /** Lo que se carga al arrancar: todas las compras (con entradas y pedido) y todos los pagos. */
    record Carga(List<Compra> compras, List<TransaccionPago> pagos) { }

    /** Arma todas las compras con sus entradas y su pedido, y los pagos de cada una. */
    Carga cargarCompras(Referencias referencias);

    /**
     * Guarda la compra en una transacción: la fila de compra, sus entradas, su pedido, los puntos y el nivel
     * del cliente, los usos del cupón y el stock de la dulcería. Si algo falla, no se guarda nada.
     * Después relee la serie que puso el trigger trg_compra_serie y se la asigna a la compra.
     * Una compra nueva (id 0) se inserta y recibe el id que genera la base (AUTO_INCREMENT).
     */
    void guardarCompra(Compra compra, boolean nueva);

    /**
     * Igual que guardarCompra y, en la MISMA transacción, inserta el pago aprobado de esa compra:
     * quedan las dos cosas o ninguna. Así nunca hay una venta confirmada sin el pago que permite reembolsarla.
     * El id del pago se le asigna recién después del commit.
     */
    void guardarCompraConPago(Compra compra, boolean nueva, TransaccionPago pago);

    /** Inserta el pago sin id y le asigna el que genera la base. */
    void insertarPago(TransaccionPago pago);
    void actualizarPago(TransaccionPago pago);

    /** Lo que cambia en la dulcería: estado, empleado y fecha de entrega. */
    void actualizarPedido(Pedido pedido);
}
