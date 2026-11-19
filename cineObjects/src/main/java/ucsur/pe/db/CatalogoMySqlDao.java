package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.tipo.CalculoDescuento;
import ucsur.pe.tipo.CalculoPorcentaje;
import ucsur.pe.tipo.FabricaCalculoDescuento;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: traduce entre las tablas del catálogo de venta (tipo_entrada, producto, inventario_sede,
 * cupon, promocion, promocion_dia) y los objetos del modelo. No aplica reglas de negocio.
 * La forma de descontar (porcentaje o monto fijo) se arma con FabricaCalculoDescuento, como en MVCone
 * TipoDinamico se arma con los datos de la BD.
 */
public class CatalogoMySqlDao implements CatalogoDao {

    private final Connection conexion;

    public CatalogoMySqlDao(Connection conexion) {
        this.conexion = conexion;
    }

    // ---------- Tipos de entrada ----------

    @Override
    public List<TipoEntrada> listarTiposEntrada() {
        String sql = """
                SELECT id_tipo_entrada, nombre, descripcion, tarifa, puntos_por_entrada,
                       requiere_socio, puntos_canje, estado
                FROM tipo_entrada
                ORDER BY id_tipo_entrada
                """;
        List<TipoEntrada> tipos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.add(new TipoEntrada(rs.getInt("id_tipo_entrada"), rs.getString("nombre"),
                        rs.getString("descripcion"), Dinero.leer(rs, "tarifa"), rs.getInt("puntos_por_entrada"),
                        rs.getBoolean("requiere_socio"), rs.getBoolean("estado"), rs.getInt("puntos_canje")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los tipos de entrada.", e);
        }
        return tipos;
    }

    @Override
    public void insertarTipoEntrada(TipoEntrada tipo) {
        String sql = """
                INSERT INTO tipo_entrada (nombre, descripcion, tarifa, puntos_por_entrada,
                                          requiere_socio, puntos_canje, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        escribirTipoEntrada(sql, tipo, true, "No se pudo registrar el tipo de entrada " + tipo.getNombre() + ".");
    }

    @Override
    public void actualizarTipoEntrada(TipoEntrada tipo) {
        String sql = """
                UPDATE tipo_entrada
                SET nombre = ?, descripcion = ?, tarifa = ?, puntos_por_entrada = ?,
                    requiere_socio = ?, puntos_canje = ?, estado = ?
                WHERE id_tipo_entrada = ?
                """;
        escribirTipoEntrada(sql, tipo, false, "No se pudo actualizar el tipo de entrada " + tipo.getNombre() + ".");
    }

    /**
     * El INSERT y el UPDATE llevan los mismos datos en el mismo orden. El UPDATE agrega el id al final;
     * el INSERT no lo lleva: lo genera la base (AUTO_INCREMENT) y se asigna al objeto.
     */
    private void escribirTipoEntrada(String sql, TipoEntrada tipo, boolean nuevo, String mensajeError) {
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tipo.getNombre());
            ps.setString(2, tipo.getDescripcion());
            ps.setBigDecimal(3, Dinero.aDecimal(tipo.getTarifaCentavos()));
            ps.setInt(4, tipo.getPuntosPorEntrada());
            ps.setBoolean(5, tipo.isRequiereSocio());
            ps.setInt(6, tipo.getPuntosCanje());
            ps.setBoolean(7, tipo.isEstado());
            if (!nuevo) ps.setInt(8, tipo.getIdTipoEntrada());
            ps.executeUpdate();
            if (nuevo) tipo.asignarId(Claves.generada(ps));
        } catch (SQLException e) {
            throw new RepositoryException(mensajeError, e);
        }
    }

    // ---------- Productos ----------

    @Override
    public List<Producto> listarProductos(Map<Integer, Sede> sedes) {
        String sql = """
                SELECT id_producto, nombre, descripcion, categoria, precio, es_combo, nivel_socio_minimo, estado
                FROM producto
                ORDER BY id_producto
                """;
        Map<Integer, Producto> productos = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = new Producto(rs.getInt("id_producto"), rs.getString("nombre"), rs.getString("descripcion"),
                        rs.getString("categoria"), Dinero.leer(rs, "precio"), rs.getBoolean("es_combo"),
                        rs.getInt("nivel_socio_minimo"), rs.getBoolean("estado"));
                productos.put(p.getIdProducto(), p);
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los productos.", e);
        }
        // Stock por sede: una sola consulta; el producto parte en 0 y se le repone lo que hay en la BD
        String sqlStock = "SELECT id_sede, id_producto, stock FROM inventario_sede WHERE stock > 0";
        try (PreparedStatement ps = conexion.prepareStatement(sqlStock);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Producto p = productos.get(rs.getInt("id_producto"));
                Sede s = sedes.get(rs.getInt("id_sede"));
                if (p != null && s != null) {
                    p.reponerStock(s, rs.getInt("stock"));
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo cargar el stock de los productos.", e);
        }
        return new ArrayList<>(productos.values());
    }

    @Override
    public void insertarProducto(Producto producto, Collection<Sede> sedes) {
        String sql = """
                INSERT INTO producto (nombre, descripcion, categoria, precio, es_combo, nivel_socio_minimo, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        escribirProducto(sql, producto, sedes, true, "No se pudo registrar el producto " + producto.getNombre() + ".");
    }

    @Override
    public void actualizarProducto(Producto producto, Collection<Sede> sedes) {
        String sql = """
                UPDATE producto
                SET nombre = ?, descripcion = ?, categoria = ?, precio = ?, es_combo = ?,
                    nivel_socio_minimo = ?, estado = ?
                WHERE id_producto = ?
                """;
        escribirProducto(sql, producto, sedes, false, "No se pudo actualizar el producto " + producto.getNombre() + ".");
    }

    /**
     * Escribe el producto y su stock en cada sede en una sola transacción. Si es nuevo, el id que genera
     * la base se asigna al producto recién después del commit.
     */
    private void escribirProducto(String sql, Producto producto, Collection<Sede> sedes, boolean nuevo,
                                  String mensajeError) {
        // Si la sede ya tiene fila de inventario se actualiza; si no, se crea solo cuando hay stock
        String sqlActualizar = "UPDATE inventario_sede SET stock = ? WHERE id_sede = ? AND id_producto = ?";
        String sqlInsertar = "INSERT INTO inventario_sede (id_sede, id_producto, stock) VALUES (?, ?, ?)";
        int[] id = {producto.getIdProducto()};
        Transaccion.ejecutar(conexion, mensajeError, () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, producto.getNombre());
                ps.setString(2, producto.getDescripcion());
                ps.setString(3, producto.getCategoria());
                ps.setBigDecimal(4, Dinero.aDecimal(producto.getPrecioCentavos()));
                ps.setBoolean(5, producto.isEsCombo());
                ps.setInt(6, producto.getNivelSocioMinimo());
                ps.setBoolean(7, producto.isEstado());
                if (!nuevo) ps.setInt(8, id[0]);
                ps.executeUpdate();
                if (nuevo) id[0] = Claves.generada(ps);
            }
            try (PreparedStatement actualizar = conexion.prepareStatement(sqlActualizar);
                 PreparedStatement insertar = conexion.prepareStatement(sqlInsertar)) {
                for (Sede sede : sedes) {
                    int stock = producto.getStock(sede);
                    actualizar.setInt(1, stock);
                    actualizar.setInt(2, sede.getIdSede());
                    actualizar.setInt(3, id[0]);
                    if (actualizar.executeUpdate() == 0 && stock > 0) {
                        insertar.setInt(1, sede.getIdSede());
                        insertar.setInt(2, id[0]);
                        insertar.setInt(3, stock);
                        insertar.executeUpdate();
                    }
                }
            }
        });
        if (nuevo) producto.asignarId(id[0]);
    }

    // ---------- Cupones ----------

    @Override
    public List<Cupon> listarCupones() {
        String sql = """
                SELECT id_cupon, codigo, tipo_descuento, descuento, fecha_inicio, fecha_fin,
                       limite_uso, usos_realizados, nivel_socio_minimo, estado
                FROM cupon
                ORDER BY id_cupon
                """;
        List<Cupon> cupones = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cupones.add(Cupon.restaurar(rs.getInt("id_cupon"), rs.getString("codigo"),
                        leerCalculo(rs, "descuento"), Fechas.desdeDate(rs.getDate("fecha_inicio")),
                        Fechas.desdeDate(rs.getDate("fecha_fin")), rs.getInt("limite_uso"),
                        rs.getInt("usos_realizados"), rs.getInt("nivel_socio_minimo"), rs.getBoolean("estado")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los cupones.", e);
        }
        return cupones;
    }

    @Override
    public void insertarCupon(Cupon cupon) {
        String sql = """
                INSERT INTO cupon (codigo, tipo_descuento, descuento, fecha_inicio, fecha_fin,
                                   limite_uso, usos_realizados, nivel_socio_minimo, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        escribirCupon(sql, cupon, true, "No se pudo registrar el cupón " + cupon.getCodigo() + ".");
    }

    @Override
    public void actualizarCupon(Cupon cupon) {
        String sql = """
                UPDATE cupon
                SET codigo = ?, tipo_descuento = ?, descuento = ?, fecha_inicio = ?, fecha_fin = ?,
                    limite_uso = ?, usos_realizados = ?, nivel_socio_minimo = ?, estado = ?
                WHERE id_cupon = ?
                """;
        escribirCupon(sql, cupon, false, "No se pudo actualizar el cupón " + cupon.getCodigo() + ".");
    }

    /** Igual que escribirTipoEntrada: el UPDATE lleva el id al final y el INSERT recibe el que genera la base. */
    private void escribirCupon(String sql, Cupon cupon, boolean nuevo, String mensajeError) {
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cupon.getCodigo());
            ps.setString(2, cupon.getTipoDescuento());
            ps.setBigDecimal(3, valorParaBD(cupon.getCalculo()));
            ps.setDate(4, Fechas.aDate(cupon.getFechaInicio()));
            ps.setDate(5, Fechas.aDate(cupon.getFechaFin()));
            ps.setInt(6, cupon.getLimiteUso());
            ps.setInt(7, cupon.getUsosRealizados());
            ps.setInt(8, cupon.getNivelSocioMinimo());
            ps.setBoolean(9, cupon.isEstado());
            if (!nuevo) ps.setInt(10, cupon.getIdCupon());
            ps.executeUpdate();
            if (nuevo) cupon.asignarId(Claves.generada(ps));
        } catch (SQLException e) {
            throw new RepositoryException(mensajeError, e);
        }
    }

    // ---------- Promociones ----------

    @Override
    public List<Promocion> listarPromociones() {
        // Días de todas las promociones en una consulta: id -> [1, 2...]
        Map<Integer, List<Integer>> dias = new LinkedHashMap<>();
        String sqlDias = "SELECT id_promocion, dia_semana FROM promocion_dia ORDER BY id_promocion, dia_semana";
        try (PreparedStatement ps = conexion.prepareStatement(sqlDias);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                dias.computeIfAbsent(rs.getInt("id_promocion"), k -> new ArrayList<>()).add(rs.getInt("dia_semana"));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los días de las promociones.", e);
        }
        String sql = """
                SELECT id_promocion, nombre, descripcion, tipo_descuento, valor, fecha_inicio, fecha_fin,
                       nivel_socio_minimo, estado
                FROM promocion
                ORDER BY id_promocion
                """;
        List<Promocion> promociones = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_promocion");
                promociones.add(new Promocion(id, rs.getString("nombre"), rs.getString("descripcion"),
                        leerCalculo(rs, "valor"), Fechas.desdeDate(rs.getDate("fecha_inicio")),
                        Fechas.desdeDate(rs.getDate("fecha_fin")), rs.getInt("nivel_socio_minimo"),
                        dias.getOrDefault(id, List.of()), rs.getBoolean("estado")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar las promociones.", e);
        }
        return promociones;
    }

    @Override
    public void insertarPromocion(Promocion promocion) {
        String sql = """
                INSERT INTO promocion (nombre, descripcion, tipo_descuento, valor, fecha_inicio, fecha_fin,
                                       nivel_socio_minimo, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        escribirPromocion(sql, promocion, true, "No se pudo registrar la promoción " + promocion.getNombre() + ".");
    }

    @Override
    public void actualizarPromocion(Promocion promocion) {
        String sql = """
                UPDATE promocion
                SET nombre = ?, descripcion = ?, tipo_descuento = ?, valor = ?, fecha_inicio = ?, fecha_fin = ?,
                    nivel_socio_minimo = ?, estado = ?
                WHERE id_promocion = ?
                """;
        escribirPromocion(sql, promocion, false, "No se pudo actualizar la promoción " + promocion.getNombre() + ".");
    }

    /**
     * Escribe la promoción y reemplaza sus días en una sola transacción. Si es nueva, el id que genera
     * la base se asigna a la promoción recién después del commit.
     */
    private void escribirPromocion(String sql, Promocion promocion, boolean nuevo, String mensajeError) {
        int[] id = {promocion.getIdPromocion()};
        Transaccion.ejecutar(conexion, mensajeError, () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, promocion.getNombre());
                ps.setString(2, promocion.getDescripcion());
                ps.setString(3, promocion.getTipoDescuento());
                ps.setBigDecimal(4, valorParaBD(promocion.getCalculo()));
                ps.setDate(5, Fechas.aDate(promocion.getFechaInicio()));
                ps.setDate(6, Fechas.aDate(promocion.getFechaFin()));
                ps.setInt(7, promocion.getNivelSocioMinimo());
                ps.setBoolean(8, promocion.isEstado());
                if (!nuevo) ps.setInt(9, id[0]);
                ps.executeUpdate();
                if (nuevo) id[0] = Claves.generada(ps);
            }
            try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM promocion_dia WHERE id_promocion = ?")) {
                borrar.setInt(1, id[0]);
                borrar.executeUpdate();
            }
            try (PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO promocion_dia (id_promocion, dia_semana) VALUES (?, ?)")) {
                for (int dia : promocion.getDiasAplicables()) {
                    ps.setInt(1, id[0]);
                    ps.setInt(2, dia);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
        if (nuevo) promocion.asignarId(id[0]);
    }

    // ---------- apoyo: valor del descuento ----------

    /**
     * Columnas tipo_descuento + valor -> forma de cálculo. En la BD el valor siempre es DECIMAL(6,2):
     * un porcentaje se guarda como 30.00 (30 %) y un monto fijo como 5.00 (S/ 5.00 = 500 céntimos).
     */
    private static CalculoDescuento leerCalculo(ResultSet rs, String columnaValor) throws SQLException {
        String tipo = rs.getString("tipo_descuento");
        long centavos = Dinero.leer(rs, columnaValor);
        long valor = CalculoPorcentaje.TIPO.equals(tipo) ? centavos / 100 : centavos;
        return FabricaCalculoDescuento.crear(tipo, valor);
    }

    /** Lo inverso de leerCalculo: 30 % -> 30.00 y 500 céntimos -> 5.00. */
    private static BigDecimal valorParaBD(CalculoDescuento calculo) {
        return CalculoPorcentaje.TIPO.equals(calculo.getTipo())
                ? BigDecimal.valueOf(calculo.getValor()).setScale(2)
                : Dinero.aDecimal(calculo.getValor());
    }
}
