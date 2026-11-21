package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.Descuento;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.ResumenImporte;
import ucsur.pe.model.SeleccionAsientos;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TransaccionPago;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Por qué existe: traduce entre las tablas de la venta (compra, entrada, pedido, detalle_pedido,
 * transaccion_pago) y los objetos del modelo. Al leer, reconstruye cada objeto con su método restaurar
 * y reutiliza los objetos que ya cargaron los otros repositorios. Al guardar una compra escribe todo
 * lo que la compra cambió en una sola transacción (RNF-10: todo o nada).
 */
public class CompraMySqlDao implements CompraDao {

    private final Connection conexion;
    private final Reloj reloj;
    private final CalculadoraImporte calculadora; // los mismos colaboradores que usa CompraService
    private final GestorPuntos gestorPuntos;

    public CompraMySqlDao(Connection conexion, Reloj reloj, CalculadoraImporte calculadora, GestorPuntos gestorPuntos) {
        this.conexion = conexion;
        this.reloj = reloj;
        this.calculadora = calculadora;
        this.gestorPuntos = gestorPuntos;
    }

    // =====================================================================
    // Lectura
    // =====================================================================

    @Override
    public Carga cargarCompras(Referencias refs) {
        try {
            Map<Integer, Pedido> pedidosPorCompra = leerPedidos(refs);
            Map<Integer, List<Entrada>> entradasPorCompra = leerEntradas(refs);
            Map<Integer, Compra> compras = leerCompras(refs, pedidosPorCompra, entradasPorCompra);
            List<TransaccionPago> pagos = leerPagos(compras);
            return new Carga(new ArrayList<>(compras.values()), pagos);
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar las compras.", e);
        }
    }

    /** Pedidos con sus detalles, por id de compra. La sede del pedido es la de su compra. */
    private Map<Integer, Pedido> leerPedidos(Referencias refs) throws SQLException {
        Map<Integer, List<DetallePedido>> detalles = new LinkedHashMap<>();
        String sqlDetalles = """
                SELECT id_detalle, cantidad, precio_unitario, personalizacion, id_pedido, id_producto
                FROM detalle_pedido
                ORDER BY id_pedido, id_detalle
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sqlDetalles);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto producto = exigir(refs.producto(rs.getInt("id_producto")), "el producto", rs.getInt("id_producto"));
                detalles.computeIfAbsent(rs.getInt("id_pedido"), k -> new ArrayList<>())
                        .add(DetallePedido.restaurar(rs.getInt("id_detalle"), producto, rs.getInt("cantidad"),
                                Dinero.leer(rs, "precio_unitario"), rs.getString("personalizacion")));
            }
        }
        String sql = """
                SELECT p.id_pedido, p.codigo_qr, p.fecha_hora, p.fecha_entrega, p.estado, p.id_empleado,
                       p.id_compra, c.id_sede
                FROM pedido p
                JOIN compra c ON c.id_compra = p.id_compra
                ORDER BY p.id_pedido
                """;
        Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_pedido");
                int idEmpleado = rs.getInt("id_empleado");
                Empleado empleado = rs.wasNull() ? null : exigir(refs.empleado(idEmpleado), "el empleado", idEmpleado);
                Sede sede = exigir(refs.sede(rs.getInt("id_sede")), "la sede", rs.getInt("id_sede"));
                try {
                    pedidos.put(rs.getInt("id_compra"), Pedido.restaurar(id, rs.getString("codigo_qr"),
                            Fechas.desdeTimestamp(rs.getTimestamp("fecha_hora")),
                            Fechas.desdeTimestamp(rs.getTimestamp("fecha_entrega")), rs.getString("estado"),
                            sede, empleado, detalles.getOrDefault(id, List.of()), reloj));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw new RepositoryException("El pedido #" + id + " de la base de datos no es válido: " + e.getMessage(), e);
                }
            }
        }
        return pedidos;
    }

    /** Entradas por id de compra; las BLOQUEADA o VENDIDA ya cargadas se reutilizan (mismo objeto). */
    private Map<Integer, List<Entrada>> leerEntradas(Referencias refs) throws SQLException {
        String sql = """
                SELECT id_entrada, codigo_qr, precio, fecha_emision, expira_bloqueo, estado,
                       id_compra, id_funcion, id_butaca, id_tipo_entrada
                FROM entrada
                ORDER BY id_compra, id_entrada
                """;
        Map<Integer, List<Entrada>> porCompra = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_entrada");
                Entrada e = refs.entradaCargada(id);
                if (e == null) {
                    e = Entrada.restaurar(id,
                            exigir(refs.funcion(rs.getInt("id_funcion")), "la función", rs.getInt("id_funcion")),
                            exigir(refs.butaca(rs.getInt("id_butaca")), "la butaca", rs.getInt("id_butaca")),
                            exigir(refs.tipoEntrada(rs.getInt("id_tipo_entrada")), "el tipo de entrada", rs.getInt("id_tipo_entrada")),
                            rs.getString("estado"), Dinero.leer(rs, "precio"), rs.getString("codigo_qr"),
                            Fechas.desdeTimestamp(rs.getTimestamp("fecha_emision")),
                            Fechas.desdeTimestamp(rs.getTimestamp("expira_bloqueo")));
                }
                porCompra.computeIfAbsent(rs.getInt("id_compra"), k -> new ArrayList<>()).add(e);
            }
        }
        return porCompra;
    }

    /** Compras en orden de fecha, así el historial de cada cliente queda del más antiguo al más reciente. */
    private Map<Integer, Compra> leerCompras(Referencias refs, Map<Integer, Pedido> pedidos,
                                             Map<Integer, List<Entrada>> entradas) throws SQLException {
        String sql = """
                SELECT id_compra, fecha_hora, expira_compra, serie_numero, subtotal, descuento, igv, total,
                       puntos_canjeados, puntos_otorgados, monto_reembolsado, estado,
                       id_cliente, id_sede, id_cupon, id_promocion
                FROM compra
                ORDER BY fecha_hora, id_compra
                """;
        Map<Integer, Compra> compras = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_compra");
                Cliente cliente = exigir(refs.cliente(rs.getInt("id_cliente")), "el cliente", rs.getInt("id_cliente"));
                Sede sede = exigir(refs.sede(rs.getInt("id_sede")), "la sede", rs.getInt("id_sede"));
                int idCupon = rs.getInt("id_cupon");
                Cupon cupon = rs.wasNull() ? null : exigir(refs.cupon(idCupon), "el cupón", idCupon);
                int idPromocion = rs.getInt("id_promocion");
                Promocion promocion = rs.wasNull() ? null : exigir(refs.promocion(idPromocion), "la promoción", idPromocion);
                ResumenImporte importe = new ResumenImporte(Dinero.leer(rs, "subtotal"), Dinero.leer(rs, "descuento"),
                        Dinero.leer(rs, "igv"), Dinero.leer(rs, "total"));
                try {
                    compras.put(id, Compra.restaurar(id, Fechas.desdeTimestamp(rs.getTimestamp("fecha_hora")),
                            Fechas.desdeTimestamp(rs.getTimestamp("expira_compra")), rs.getString("serie_numero"),
                            rs.getString("estado"), importe, rs.getInt("puntos_canjeados"), rs.getInt("puntos_otorgados"),
                            Dinero.leer(rs, "monto_reembolsado"), cliente, sede, entradas.getOrDefault(id, List.of()),
                            cupon, promocion, pedidos.get(id),
                            new SeleccionAsientos(), calculadora, gestorPuntos, reloj));
                } catch (IllegalArgumentException | IllegalStateException e) {
                    throw new RepositoryException("La compra #" + id + " de la base de datos no es válida: " + e.getMessage(), e);
                }
            }
        }
        return compras;
    }

    private List<TransaccionPago> leerPagos(Map<Integer, Compra> compras) throws SQLException {
        String sql = """
                SELECT id_transaccion, monto, medio_pago, estado_pago, fecha_hora, referencia_pasarela,
                       motivo_rechazo, id_compra
                FROM transaccion_pago
                ORDER BY id_transaccion
                """;
        List<TransaccionPago> pagos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Compra compra = exigir(compras.get(rs.getInt("id_compra")), "la compra", rs.getInt("id_compra"));
                pagos.add(TransaccionPago.restaurar(rs.getInt("id_transaccion"), Dinero.leer(rs, "monto"),
                        rs.getString("medio_pago"), rs.getString("estado_pago"),
                        Fechas.desdeTimestamp(rs.getTimestamp("fecha_hora")), rs.getString("referencia_pasarela"),
                        rs.getString("motivo_rechazo"), compra));
            }
        }
        return pagos;
    }

    // =====================================================================
    // Escritura de la compra (una transacción)
    // =====================================================================

    @Override
    public void guardarCompra(Compra compra, boolean nueva) {
        escribirCompra(compra, nueva, null);
    }

    @Override
    public void guardarCompraConPago(Compra compra, boolean nueva, TransaccionPago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago es obligatorio");
        }
        escribirCompra(compra, nueva, pago);
    }

    /** Escribe la compra y todo lo que cuelga de ella (y el pago, si llega) en una sola transacción. */
    private void escribirCompra(Compra compra, boolean nueva, TransaccionPago pago) {
        // Los ids que genera la base se asignan a los objetos recién después del commit:
        // si la transacción se revierte, ningún objeto Java queda con el id de una fila que no existe.
        Map<Entrada, Integer> idsEntradas = new LinkedHashMap<>();
        int[] idCompra = {compra.getIdCompra()};
        int[] idPedidoNuevo = {0};
        int[] idPago = {0};
        Transaccion.ejecutar(conexion, "No se pudo guardar la compra #" + compra.getIdCompra() + ".", () -> {
            if (nueva) {
                idCompra[0] = insertarCompra(compra);
            } else {
                actualizarCompra(compra);
            }
            guardarEntradas(compra, idCompra[0], idsEntradas);
            if (compra.getPedido() != null) {
                idPedidoNuevo[0] = guardarPedido(compra.getPedido(), idCompra[0]);
            }
            actualizarPuntosCliente(compra.getCliente());
            actualizarUsosCupon(compra);
            actualizarStock(compra);
            if (pago != null) {
                idPago[0] = insertarFilaPago(pago, idCompra[0]); // misma transacción: compra y pago van juntos
            }
        });
        if (nueva) {
            compra.asignarId(idCompra[0]);
        }
        if (pago != null) {
            pago.asignarId(idPago[0]);
        }
        idsEntradas.forEach(Entrada::asignarId);
        if (idPedidoNuevo[0] > 0) {
            compra.getPedido().asignarId(idPedidoNuevo[0]); // rehace en el objeto el mismo QR que ya se guardó
        }
        releerSerie(compra);
    }

    /** Inserta la fila de compra sin id y devuelve el que generó la base (AUTO_INCREMENT). */
    private int insertarCompra(Compra c) throws SQLException {
        String sql = """
                INSERT INTO compra (fecha_hora, expira_compra, subtotal, descuento, igv, total,
                                    puntos_canjeados, puntos_otorgados, monto_reembolsado, estado,
                                    id_cupon, id_promocion, id_cliente, id_sede)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarCompra(ps, c);
            ps.setInt(13, c.getCliente().getIdCliente());
            ps.setInt(14, c.getSede().getIdSede());
            ps.executeUpdate();
            return Claves.generada(ps);
        }
    }

    /** No escribe serie_numero: la pone el trigger trg_compra_serie cuando la compra pasa a CONFIRMADA. */
    private void actualizarCompra(Compra c) throws SQLException {
        String sql = """
                UPDATE compra
                SET fecha_hora = ?, expira_compra = ?, subtotal = ?, descuento = ?, igv = ?, total = ?,
                    puntos_canjeados = ?, puntos_otorgados = ?, monto_reembolsado = ?, estado = ?,
                    id_cupon = ?, id_promocion = ?
                WHERE id_compra = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            llenarCompra(ps, c);
            ps.setInt(13, c.getIdCompra());
            ps.executeUpdate();
        }
    }

    /** Los 12 datos que comparten el INSERT y el UPDATE de compra. */
    private void llenarCompra(PreparedStatement ps, Compra c) throws SQLException {
        ResumenImporte importe = c.getImporte();
        ps.setTimestamp(1, Fechas.aTimestamp(c.getFechaHora()));
        ps.setTimestamp(2, Fechas.aTimestamp(c.getExpiraCompra()));
        ps.setBigDecimal(3, Dinero.aDecimal(importe.getSubtotalCentavos()));
        ps.setBigDecimal(4, Dinero.aDecimal(importe.getDescuentoCentavos()));
        ps.setBigDecimal(5, Dinero.aDecimal(importe.getIgvCentavos()));
        ps.setBigDecimal(6, Dinero.aDecimal(importe.getTotalCentavos()));
        ps.setInt(7, c.getPuntosCanjeados());
        ps.setInt(8, c.getPuntosOtorgados());
        ps.setBigDecimal(9, Dinero.aDecimal(c.getMontoReembolsadoCentavos()));
        ps.setString(10, c.getEstado());
        Integer idCupon = null;
        Integer idPromocion = null;
        for (Descuento d : c.getDescuentos()) {
            if (d instanceof Cupon cupon) idCupon = cupon.getIdCupon();
            if (d instanceof Promocion promocion) idPromocion = promocion.getIdPromocion();
        }
        enteroOpcional(ps, 11, idCupon);
        enteroOpcional(ps, 12, idPromocion);
    }

    /**
     * Entradas de la compra: inserta las que acaban de bloquearse o venderse (las preseleccionadas viven solo
     * en memoria), actualiza las que ya existen, y borra las preselecciones que el cliente quitó después de
     * volver al mapa de butacas.
     */
    private void guardarEntradas(Compra compra, int idCompra, Map<Entrada, Integer> idsNuevos) throws SQLException {
        String insertar = """
                INSERT INTO entrada (codigo_qr, precio, fecha_emision, expira_bloqueo, estado, id_tipo_entrada,
                                     id_compra, id_funcion, id_butaca)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String actualizar = """
                UPDATE entrada
                SET codigo_qr = ?, precio = ?, fecha_emision = ?, expira_bloqueo = ?, estado = ?, id_tipo_entrada = ?
                WHERE id_entrada = ?
                """;
        List<Integer> conservadas = new ArrayList<>();
        try (PreparedStatement ins = conexion.prepareStatement(insertar, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement upd = conexion.prepareStatement(actualizar)) {
            for (Entrada e : compra.getEntradas()) {
                if (e.getIdEntrada() > 0) {
                    llenarEntrada(upd, e);
                    upd.setInt(7, e.getIdEntrada());
                    upd.executeUpdate();
                    conservadas.add(e.getIdEntrada());
                } else if (Entrada.BLOQUEADA.equals(e.getEstado()) || Entrada.VENDIDA.equals(e.getEstado())) {
                    llenarEntrada(ins, e);
                    ins.setInt(7, idCompra);
                    ins.setInt(8, e.getFuncion().getIdFuncion());
                    ins.setInt(9, e.getButaca().getIdButaca());
                    ins.executeUpdate();
                    idsNuevos.put(e, Claves.generada(ins));
                }
            }
        }
        // Solo marcadores "?" en el NOT IN: ningún dato se concatena al SQL
        String borrar = "DELETE FROM entrada WHERE id_compra = ? AND estado = 'PRESELECCIONADA'"
                + (conservadas.isEmpty() ? "" : " AND id_entrada NOT IN (" + "?,".repeat(conservadas.size() - 1) + "?)");
        try (PreparedStatement ps = conexion.prepareStatement(borrar)) {
            ps.setInt(1, idCompra);
            for (int i = 0; i < conservadas.size(); i++) {
                ps.setInt(i + 2, conservadas.get(i));
            }
            ps.executeUpdate();
        }
    }

    /** Los 6 datos que comparten el INSERT y el UPDATE de entrada. */
    private void llenarEntrada(PreparedStatement ps, Entrada e) throws SQLException {
        ps.setString(1, e.getCodigoQR());
        ps.setBigDecimal(2, Dinero.aDecimal(e.getPrecioCentavos()));
        ps.setTimestamp(3, Fechas.aTimestamp(e.getFechaEmision()));
        ps.setTimestamp(4, Fechas.aTimestamp(e.getExpiraBloqueo()));
        ps.setString(5, e.getEstado());
        ps.setInt(6, e.getTipoEntrada().getIdTipoEntrada());
    }

    /**
     * Inserta o actualiza el pedido de la compra; devuelve el id generado si es nuevo (si no, 0).
     * Mientras el pedido no esté registrado sus líneas pueden cambiar, así que se reemplazan.
     */
    private int guardarPedido(Pedido p, int idCompra) throws SQLException {
        int idNuevo = 0;
        if (p.getIdPedido() <= 0) {
            // Si la compra tenía otro pedido sin registrar (el cliente lo rehízo), ese ya no vale
            try (PreparedStatement ps = conexion.prepareStatement(
                    "DELETE FROM pedido WHERE id_compra = ? AND codigo_qr IS NULL")) {
                ps.setInt(1, idCompra);
                ps.executeUpdate();
            }
            String sql = """
                    INSERT INTO pedido (codigo_qr, fecha_hora, fecha_entrega, estado, id_empleado, id_compra)
                    VALUES (NULL, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setTimestamp(1, Fechas.aTimestamp(p.getFechaHora()));
                ps.setTimestamp(2, Fechas.aTimestamp(p.getFechaEntrega()));
                ps.setString(3, p.getEstado());
                enteroOpcional(ps, 4, p.getEmpleado() != null ? p.getEmpleado().getIdEmpleado() : null);
                ps.setInt(5, idCompra);
                ps.executeUpdate();
                idNuevo = Claves.generada(ps);
            }
            insertarDetalles(p, idNuevo);
            if (p.getCodigoQR() != null) {
                // El QR lleva el id que la base acaba de generar: se escribe aquí, dentro de la misma transacción
                try (PreparedStatement ps = conexion.prepareStatement("UPDATE pedido SET codigo_qr = ? WHERE id_pedido = ?")) {
                    ps.setString(1, Pedido.formarCodigoQR(idNuevo, p.getFechaHora()));
                    ps.setInt(2, idNuevo);
                    ps.executeUpdate();
                }
            }
        } else {
            // Lo que dice la BD, no el objeto: si al pagar el pedido se registra, el objeto ya figura registrado,
            // pero en la BD sigue como borrador (sin QR) y sus líneas pudieron cambiar desde el último guardado.
            boolean borradorEnBD = esBorradorEnBD(p.getIdPedido());
            String sql = """
                    UPDATE pedido SET codigo_qr = ?, fecha_hora = ?, fecha_entrega = ?, estado = ?, id_empleado = ?
                    WHERE id_pedido = ?
                    """;
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, p.getCodigoQR());
                ps.setTimestamp(2, Fechas.aTimestamp(p.getFechaHora()));
                ps.setTimestamp(3, Fechas.aTimestamp(p.getFechaEntrega()));
                ps.setString(4, p.getEstado());
                enteroOpcional(ps, 5, p.getEmpleado() != null ? p.getEmpleado().getIdEmpleado() : null);
                ps.setInt(6, p.getIdPedido());
                ps.executeUpdate();
            }
            if (borradorEnBD) {
                try (PreparedStatement ps = conexion.prepareStatement("DELETE FROM detalle_pedido WHERE id_pedido = ?")) {
                    ps.setInt(1, p.getIdPedido());
                    ps.executeUpdate();
                }
                insertarDetalles(p, p.getIdPedido());
            }
        }
        return idNuevo;
    }

    /** Un pedido guardado sin QR todavía no se registró: sus líneas aún pueden cambiar. */
    private boolean esBorradorEnBD(int idPedido) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("SELECT codigo_qr FROM pedido WHERE id_pedido = ?")) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString(1) == null;
            }
        }
    }

    private void insertarDetalles(Pedido p, int idPedido) throws SQLException {
        String sql = """
                INSERT INTO detalle_pedido (cantidad, precio_unitario, personalizacion, id_pedido, id_producto)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (DetallePedido d : p.getDetalles()) {
                ps.setInt(1, d.getCantidad());
                ps.setBigDecimal(2, Dinero.aDecimal(d.getPrecioUnitarioCentavos()));
                ps.setString(3, d.getPersonalizacion());
                ps.setInt(4, idPedido);
                ps.setInt(5, d.getProducto().getIdProducto());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Puntos y nivel cambian al confirmar (canje y puntos ganados) y al reembolsar. */
    private void actualizarPuntosCliente(Cliente cliente) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE cliente SET puntos = ?, nivel_socio = ? WHERE id_cliente = ?")) {
            ps.setInt(1, cliente.consultarPuntos());
            ps.setInt(2, cliente.getNivelSocio());
            ps.setInt(3, cliente.getIdCliente());
            ps.executeUpdate();
        }
    }

    /** El cupón suma un uso al confirmar y lo devuelve al reembolsar. */
    private void actualizarUsosCupon(Compra compra) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE cupon SET usos_realizados = ? WHERE id_cupon = ?")) {
            for (Descuento d : compra.getDescuentos()) {
                if (d instanceof Cupon cupon) {
                    ps.setInt(1, cupon.getUsosRealizados());
                    ps.setInt(2, cupon.getIdCupon());
                    ps.executeUpdate();
                }
            }
        }
    }

    /** El stock de la sede baja al registrar el pedido y vuelve si se anula sin haberse preparado. */
    private void actualizarStock(Compra compra) throws SQLException {
        Pedido pedido = compra.getPedido();
        if (pedido == null || !pedido.isRegistrado()) return;
        Set<Producto> productos = new LinkedHashSet<>();
        for (DetallePedido d : pedido.getDetalles()) productos.add(d.getProducto());
        Sede sede = compra.getSede();
        try (PreparedStatement upd = conexion.prepareStatement(
                     "UPDATE inventario_sede SET stock = ? WHERE id_sede = ? AND id_producto = ?");
             PreparedStatement ins = conexion.prepareStatement(
                     "INSERT INTO inventario_sede (id_sede, id_producto, stock) VALUES (?, ?, ?)")) {
            for (Producto p : productos) {
                int stock = p.getStock(sede);
                upd.setInt(1, stock);
                upd.setInt(2, sede.getIdSede());
                upd.setInt(3, p.getIdProducto());
                if (upd.executeUpdate() == 0 && stock > 0) {
                    ins.setInt(1, sede.getIdSede());
                    ins.setInt(2, p.getIdProducto());
                    ins.setInt(3, stock);
                    ins.executeUpdate();
                }
            }
        }
    }

    /** Review Focus 4: la serie la inventa la base (trigger), Java solo la relee. */
    private void releerSerie(Compra compra) {
        boolean pagada = Compra.CONFIRMADA.equals(compra.getEstado()) || Compra.REEMBOLSADA.equals(compra.getEstado());
        if (!pagada || compra.getSerieNumero() != null) return;
        try (PreparedStatement ps = conexion.prepareStatement("SELECT serie_numero FROM compra WHERE id_compra = ?")) {
            ps.setInt(1, compra.getIdCompra());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    compra.asignarSerie(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo leer la serie de la compra #" + compra.getIdCompra() + ".", e);
        }
    }

    // =====================================================================
    // Pagos y pedidos
    // =====================================================================

    @Override
    public void insertarPago(TransaccionPago pago) {
        try {
            pago.asignarId(insertarFilaPago(pago, pago.getCompra().getIdCompra()));
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar el pago de la compra #" + pago.getCompra().getIdCompra() + ".", e);
        }
    }

    /** Inserta la fila del pago y devuelve el id que generó la base (no se lo asigna al objeto). */
    private int insertarFilaPago(TransaccionPago pago, int idCompra) throws SQLException {
        String sql = """
                INSERT INTO transaccion_pago (monto, medio_pago, estado_pago, fecha_hora, referencia_pasarela,
                                              motivo_rechazo, id_compra)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarPago(ps, pago);
            ps.setInt(7, idCompra);
            ps.executeUpdate();
            return Claves.generada(ps);
        }
    }

    @Override
    public void actualizarPago(TransaccionPago pago) {
        String sql = """
                UPDATE transaccion_pago
                SET monto = ?, medio_pago = ?, estado_pago = ?, fecha_hora = ?, referencia_pasarela = ?, motivo_rechazo = ?
                WHERE id_transaccion = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            llenarPago(ps, pago);
            ps.setInt(7, pago.getIdTransaccion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar el pago #" + pago.getIdTransaccion() + ".", e);
        }
    }

    private void llenarPago(PreparedStatement ps, TransaccionPago p) throws SQLException {
        ps.setBigDecimal(1, Dinero.aDecimal(p.getMontoCentavos()));
        ps.setString(2, p.getMedioPago());
        ps.setString(3, p.getEstadoPago());
        ps.setTimestamp(4, Fechas.aTimestamp(p.getFechaHora()));
        ps.setString(5, p.getReferenciaPasarela());
        ps.setString(6, p.getMotivoRechazo());
    }

    @Override
    public void actualizarPedido(Pedido pedido) {
        String sql = "UPDATE pedido SET estado = ?, fecha_entrega = ?, id_empleado = ? WHERE id_pedido = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, pedido.getEstado());
            ps.setTimestamp(2, Fechas.aTimestamp(pedido.getFechaEntrega()));
            enteroOpcional(ps, 3, pedido.getEmpleado() != null ? pedido.getEmpleado().getIdEmpleado() : null);
            ps.setInt(4, pedido.getIdPedido());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar el pedido #" + pedido.getIdPedido() + ".", e);
        }
    }

    // =====================================================================
    // apoyo
    // =====================================================================

    /** Columna INT que admite NULL (id_cupon, id_promocion, id_empleado). */
    private static void enteroOpcional(PreparedStatement ps, int posicion, Integer valor) throws SQLException {
        if (valor == null) ps.setNull(posicion, Types.INTEGER);
        else ps.setInt(posicion, valor);
    }

    /** El objeto que la fila referencia debe existir; si no, la base tiene una referencia rota. */
    private static <T> T exigir(T objeto, String que, int id) {
        if (objeto == null) {
            throw new RepositoryException("No se encontró " + que + " con id " + id + " en la base de datos.");
        }
        return objeto;
    }
}
