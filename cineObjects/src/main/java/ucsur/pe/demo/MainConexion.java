package ucsur.pe.demo;

import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.correo.CorreoSimulado;
import ucsur.pe.db.CatalogoMySqlDao;
import ucsur.pe.db.CompraMySqlDao;
import ucsur.pe.db.DBConnection;
import ucsur.pe.db.Fechas;
import ucsur.pe.db.PeliculaMySqlDao;
import ucsur.pe.db.PersonaMySqlDao;
import ucsur.pe.db.PostventaMySqlDao;
import ucsur.pe.db.SedeMySqlDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reclamo;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import ucsur.pe.model.TransaccionPago;
import ucsur.pe.pasarela.PasarelaSimulada;
import ucsur.pe.repository.CalificacionJdbcRepository;
import ucsur.pe.repository.ClienteJdbcRepository;
import ucsur.pe.repository.CompraJdbcRepository;
import ucsur.pe.repository.CuponJdbcRepository;
import ucsur.pe.repository.EmpleadoJdbcRepository;
import ucsur.pe.repository.PagoJdbcRepository;
import ucsur.pe.repository.PedidoJdbcRepository;
import ucsur.pe.repository.PeliculaJdbcRepository;
import ucsur.pe.repository.ProductoJdbcRepository;
import ucsur.pe.repository.PromocionJdbcRepository;
import ucsur.pe.repository.ReclamoJdbcRepository;
import ucsur.pe.repository.SalaJdbcRepository;
import ucsur.pe.repository.SedeJdbcRepository;
import ucsur.pe.repository.TipoEntradaJdbcRepository;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.CarteleraService;
import ucsur.pe.service.CompraService;
import ucsur.pe.service.EmisionService;
import ucsur.pe.service.PagoService;
import ucsur.pe.service.PedidoService;
import ucsur.pe.service.PostventaService;
import ucsur.pe.tipo.CalculoPorcentaje;
import ucsur.pe.util.ConversorDinero;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

/**
 * Por qué existe: prueba por consola la migración a MySQL. Cada comprobación imprime OK o FALLA.
 * Fase 1: la conexión y las fechas. Fase 2: el catálogo (sedes, salas, butacas, funciones, películas,
 * productos, cupones, promociones y tipos de entrada) cargado una vez, con un solo objeto por id.
 * Requiere MySQL encendido y la base creada con BD_Cine.sql y BD_Cine_datos.sql.
 */
public class MainConexion {

    private static int fallas = 0;

    public static void main(String[] args) {
        System.out.println("=== CONEXIÓN A DB_Cine ===");

        probarFechasSinBD();
        probarContrasenaIncorrecta();

        DBConnection db;
        try {
            db = DBConnection.getInstance();
        } catch (RepositoryException e) {
            comprobar("conectar con db.properties", false);
            System.out.println("   " + e.getMessage());
            System.out.println("   Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "-"));
            System.out.println("   ¿Está encendido el servicio MySQL80 y es correcta la contraseña?");
            terminar();
            return;
        }
        comprobar("conectar con db.properties", true);

        try {
            Connection cn = db.getConexion();
            comprobar("la base activa es DB_Cine", "DB_Cine".equalsIgnoreCase(textoDe(cn, "SELECT DATABASE()")));
            mostrarConteo(cn, "sede");
            mostrarConteo(cn, "sala");
            mostrarConteo(cn, "butaca");
            mostrarConteo(cn, "pelicula");
            mostrarConteo(cn, "funcion");
            mostrarConteo(cn, "producto");
            mostrarConteo(cn, "cliente");
            probarHoraDeLaSesion(cn);
            probarFechaIdaYVuelta(cn);
            probarLeerUnaFuncion(cn);
            probarPersonas(cn, probarCatalogo(cn));
            probarVentas(cn);
            probarApp(cn);
        } catch (SQLException | RepositoryException e) {
            comprobar("consultas de prueba", false);
            System.out.println("   " + e.getMessage());
            if (e.getCause() != null) System.out.println("   Causa: " + e.getCause().getMessage());
        } finally {
            db.cerrar();
        }
        terminar();
    }

    /** Fechas convierte DateTime a JDBC y de vuelta sin perder nada (no necesita la BD). */
    private static void probarFechasSinBD() {
        DateTime fechaHora = new DateTime(17, 11, 2026, 21, 5, 9);
        DateTime fecha = new DateTime(29, 2, 2028);
        DateTime hora = new DateTime(18, 45);
        comprobar("Fechas: DATETIME ida y vuelta", fechaHora.equals(Fechas.desdeTimestamp(Fechas.aTimestamp(fechaHora))));
        comprobar("Fechas: DATE ida y vuelta (29/02 bisiesto)", fecha.equals(Fechas.desdeDate(Fechas.aDate(fecha))));
        comprobar("Fechas: TIME ida y vuelta", hora.equals(Fechas.desdeTime(Fechas.aTime(hora))));
        comprobar("Fechas: null devuelve null", Fechas.aTimestamp(null) == null && Fechas.desdeDate(null) == null);
    }

    /** Con una contraseña mala debe salir el mensaje en español, no una traza de MySQL. */
    private static void probarContrasenaIncorrecta() {
        Properties props;
        try {
            props = DBConnection.leerPropiedades();
        } catch (RepositoryException e) {
            comprobar("leer db.properties", false);
            System.out.println("   " + e.getMessage());
            return;
        }
        props.setProperty("db.password", props.getProperty("db.password") + "_incorrecta");
        try {
            DBConnection.abrir(props).close();
            comprobar("contraseña incorrecta es rechazada", false);
        } catch (RepositoryException e) {
            comprobar("contraseña incorrecta -> \"" + e.getMessage() + "\"",
                    e.getMessage().startsWith("No se pudo conectar"));
        } catch (SQLException e) {
            comprobar("contraseña incorrecta es rechazada", false);
        }
    }

    /** NOW() de la sesión debe coincidir con DateTime.ahora() (los dos en hora de Perú). */
    private static void probarHoraDeLaSesion(Connection cn) throws SQLException {
        DateTime antes = DateTime.ahora();
        DateTime enBD;
        try (PreparedStatement ps = cn.prepareStatement("SELECT NOW()");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            enBD = Fechas.desdeTimestamp(rs.getTimestamp(1));
        }
        DateTime despues = DateTime.ahora();
        boolean coincide = !enBD.esDespuesDe(despues.sumarMinutos(1)) && !antes.esDespuesDe(enBD.sumarMinutos(1));
        comprobar("NOW() de MySQL (" + enBD + ") = DateTime.ahora() (" + antes + ")", coincide);
    }

    /** Una fecha enviada con PreparedStatement vuelve igual (no se corre por la zona horaria). */
    private static void probarFechaIdaYVuelta(Connection cn) throws SQLException {
        DateTime original = new DateTime(17, 11, 2026, 23, 59, 30);
        try (PreparedStatement ps = cn.prepareStatement("SELECT CAST(? AS DATETIME)")) {
            ps.setTimestamp(1, Fechas.aTimestamp(original));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                DateTime leida = Fechas.desdeTimestamp(rs.getTimestamp(1));
                comprobar("DATETIME por la BD ida y vuelta (" + leida + ")", original.equals(leida));
            }
        }
    }

    /** Lee la primera función para ver que DATE y TIME llegan bien como DateTime. */
    private static void probarLeerUnaFuncion(Connection cn) throws SQLException {
        String sql = """
                SELECT f.id_funcion, f.fecha, f.hora_inicio, p.titulo
                FROM funcion f
                JOIN pelicula p ON p.id_pelicula = f.id_pelicula
                ORDER BY f.id_funcion
                LIMIT 1
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                comprobar("leer una función (la tabla está vacía)", false);
                return;
            }
            DateTime fecha = Fechas.desdeDate(rs.getDate("fecha"));
            DateTime hora = Fechas.desdeTime(rs.getTime("hora_inicio"));
            comprobar("leer función " + rs.getInt("id_funcion") + ": " + rs.getString("titulo")
                    + " el " + fecha.soloFecha() + " a las " + hora.soloHora(), true);
        }
    }

    // ---------- Fase 2: catálogo ----------

    /** Carga el catálogo con los repositorios Jdbc y lo compara con lo que dice la BD por SQL. */
    private static SedeJdbcRepository probarCatalogo(Connection cn) throws SQLException {
        System.out.println("\n--- Catálogo (repositorios Jdbc) ---");
        Reloj reloj = new RelojSistema();
        long inicio = System.currentTimeMillis();
        CatalogoMySqlDao catalogo = new CatalogoMySqlDao(cn);
        SedeMySqlDao sedeDao = new SedeMySqlDao(cn, reloj);
        PeliculaJdbcRepository peliculas = new PeliculaJdbcRepository(new PeliculaMySqlDao(cn));
        TipoEntradaJdbcRepository tipos = new TipoEntradaJdbcRepository(catalogo);
        SedeJdbcRepository sedes = new SedeJdbcRepository(sedeDao, peliculas, tipos);
        SalaJdbcRepository salas = new SalaJdbcRepository(sedeDao, sedes, sedes.listarTiposSala());
        ProductoJdbcRepository productos = new ProductoJdbcRepository(catalogo, sedes);
        CuponJdbcRepository cupones = new CuponJdbcRepository(catalogo);
        PromocionJdbcRepository promociones = new PromocionJdbcRepository(catalogo);
        int funciones = sedes.listarTodas().stream().mapToInt(s -> s.getCartelera().size()).sum();
        int butacas = sedes.listarTodas().stream().flatMap(s -> s.getSalas().stream())
                .mapToInt(s -> s.obtenerMapaButacas().size()).sum();
        comprobar("catálogo cargado en " + (System.currentTimeMillis() - inicio) + " ms", true);

        // Lo cargado coincide con las filas de cada tabla
        comprobar("sedes: " + sedes.listarTodas().size(), sedes.listarTodas().size() == entero(cn, "SELECT COUNT(*) FROM sede"));
        comprobar("funciones: " + funciones, funciones == entero(cn, "SELECT COUNT(*) FROM funcion"));
        comprobar("butacas: " + butacas, butacas == entero(cn, "SELECT COUNT(*) FROM butaca"));
        comprobar("películas: " + peliculas.listarTodas().size(),
                peliculas.listarTodas().size() == entero(cn, "SELECT COUNT(*) FROM pelicula"));
        int generos = peliculas.listarTodas().stream().mapToInt(p -> p.getGeneros().size()).sum();
        comprobar("géneros de películas: " + generos, generos == entero(cn, "SELECT COUNT(*) FROM pelicula_genero"));
        comprobar("productos: " + productos.listarTodos().size(),
                productos.listarTodos().size() == entero(cn, "SELECT COUNT(*) FROM producto"));
        comprobar("cupones: " + cupones.listarTodos().size(),
                cupones.listarTodos().size() == entero(cn, "SELECT COUNT(*) FROM cupon"));
        comprobar("promociones: " + promociones.listarTodas().size(),
                promociones.listarTodas().size() == entero(cn, "SELECT COUNT(*) FROM promocion"));
        comprobar("tipos de entrada activos: " + tipos.listarActivos().size(),
                tipos.listarActivos().size() == entero(cn, "SELECT COUNT(*) FROM tipo_entrada WHERE estado"));

        // Cada sala tiene tantas butacas como su capacidad
        String salaIncompleta = sedes.listarTodas().stream().flatMap(s -> s.getSalas().stream())
                .filter(s -> s.obtenerMapaButacas().size() != s.getCapacidad())
                .map(s -> s.getSede().getNombre() + " sala " + s.getNumero()).findFirst().orElse(null);
        comprobar("butacas = capacidad en todas las salas" + (salaIncompleta != null ? " (no en " + salaIncompleta + ")" : ""),
                salaIncompleta == null);

        // Cartelera de la primera sede abierta, como la vería el cliente
        CarteleraService cartelera = new CarteleraService(sedes, promociones, peliculas, reloj);
        List<Sede> abiertas = cartelera.listarSedes();
        if (abiertas.isEmpty()) {
            comprobar("hay sedes abiertas", false);
            return sedes;
        }
        Sede sede = abiertas.get(0);
        int enCartelera = cartelera.consultarCartelera(sede).size();
        comprobar("cartelera de " + sede.getNombre() + ": " + enCartelera + " funciones", enCartelera > 0);

        // Un solo objeto por id
        Sala sala = sede.getSalas().get(0);
        comprobar("la sala del repositorio es el mismo objeto que la de la sede",
                salas.buscarPorId(sala.getIdSala()).orElse(null) == sala);
        Funcion funcion = sede.getCartelera().get(0);
        comprobar("la función usa la misma película que el repositorio",
                peliculas.buscarPorId(funcion.getPelicula().getIdPelicula()).orElse(null) == funcion.getPelicula());

        // Review Focus 1: una butaca VENDIDA en la BD se ve ocupada
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_entrada, id_funcion, id_butaca FROM entrada WHERE estado = 'VENDIDA' ORDER BY id_entrada DESC LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Entrada e = sedes.buscarEntrada(rs.getInt("id_entrada")).orElse(null);
                boolean ok = e != null && e.getFuncion().getIdFuncion() == rs.getInt("id_funcion")
                        && e.getButaca().getIdButaca() == rs.getInt("id_butaca")
                        && !e.getFuncion().esButacaDisponible(e.getButaca());
                comprobar("butaca vendida " + (e != null ? e.getButaca().getCodigoAsiento() : "?") + " ocupada en su función", ok);
            } else {
                comprobar("hay entradas VENDIDA para probar", false);
            }
        }

        // Stock por sede, montos y días leídos tal cual
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_sede, id_producto, stock FROM inventario_sede WHERE stock > 0 ORDER BY id_sede, id_producto LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                Producto p = productos.buscarPorId(rs.getInt("id_producto")).orElseThrow();
                Sede s = sedes.buscarPorId(rs.getInt("id_sede")).orElseThrow();
                comprobar("stock de " + p.getNombre() + " en " + s.getNombre() + ": " + p.getStock(s),
                        p.getStock(s) == rs.getInt("stock"));
            }
        }
        String codigo = textoDe(cn, "SELECT codigo FROM cupon ORDER BY id_cupon LIMIT 1");
        Cupon cupon = cupones.buscarPorCodigo("  " + codigo.toLowerCase() + " ").orElse(null);
        comprobar("cupón por código sin distinguir mayúsculas: " + cupon, cupon != null && cupon.getCodigo().equals(codigo));
        Promocion conDias = promociones.listarTodas().stream()
                .filter(p -> !p.getDiasAplicables().isEmpty()).findFirst().orElse(null);
        comprobar("promoción con días: " + conDias + " " + (conDias != null ? conDias.getDiasAplicables() : ""),
                conDias != null && conDias.getDiasAplicables().size()
                        == entero(cn, "SELECT COUNT(*) FROM promocion_dia WHERE id_promocion = " + conDias.getIdPromocion()));

        if (cupon == null || conDias == null) {
            return sedes; // sin un cupón y una promoción con días no se pueden probar las escrituras
        }

        // Guardar sin cambios no altera nada: se escribe y al volver a leer sale igual
        String antes = describir(cupon) + describir(conDias);
        cupones.guardar(cupon);
        promociones.guardar(conDias);
        productos.guardar(productos.listarTodos().get(0));
        salas.guardar(sala);
        sedes.guardar(sede);
        CatalogoMySqlDao relectura = new CatalogoMySqlDao(cn);
        Cupon cuponLeido = relectura.listarCupones().stream().filter(c -> c.getIdCupon() == cupon.getIdCupon()).findFirst().orElseThrow();
        Promocion promoLeida = relectura.listarPromociones().stream()
                .filter(p -> p.getIdPromocion() == conDias.getIdPromocion()).findFirst().orElseThrow();
        comprobar("guardar sin cambios y volver a leer da lo mismo", antes.equals(describir(cuponLeido) + describir(promoLeida)));

        // Un segundo objeto con un id ya usado se rechaza (habría dos objetos para la misma fila)
        try {
            cupones.guardar(Cupon.restaurar(cupon.getIdCupon(), "OTRO", new CalculoPorcentaje(5), cupon.getFechaInicio(),
                    cupon.getFechaFin(), 1, 0, 0, true));
            comprobar("id repetido es rechazado", false);
        } catch (RepositoryException e) {
            comprobar("id repetido -> \"" + e.getMessage() + "\"", true);
        }

        // Registrar una promoción nueva (con sus días, en una transacción) y luego borrarla.
        // Nace con id 0: la base lo genera (AUTO_INCREMENT) y el DAO se lo asigna.
        Promocion nueva = new Promocion(0, "Prueba MainConexion", "Se borra al terminar",
                new CalculoPorcentaje(5), DateTime.ahora(), DateTime.ahora().sumarDias(1), 0, List.of(1, 3), true);
        try {
            promociones.guardar(nueva);
            comprobar("promoción nueva con id generado por la base (" + nueva.getIdPromocion() + ") y 2 días",
                    nueva.getIdPromocion() > 0
                    && entero(cn, "SELECT COUNT(*) FROM promocion_dia WHERE id_promocion = " + nueva.getIdPromocion()) == 2);
            try {
                promociones.guardar(new Promocion(99999, "Id a mano", null, new CalculoPorcentaje(5),
                        DateTime.ahora(), DateTime.ahora().sumarDias(1), 0, List.of(), true));
                comprobar("un id puesto a mano es rechazado", false);
            } catch (RepositoryException e) {
                comprobar("id puesto a mano -> \"" + e.getMessage() + "\"", true);
            }
        } finally {
            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM promocion WHERE id_promocion = ? AND nombre = 'Prueba MainConexion'")) {
                ps.setInt(1, nueva.getIdPromocion());
                ps.executeUpdate(); // promocion_dia se borra en cascada
            }
        }
        return sedes;
    }

    // ---------- Fase 3: clientes y empleados ----------

    private static final String CORREO_PRUEBA = "prueba.mainconexion.%d@test.pe";

    /** Carga clientes y empleados, prueba el login con datos de la BD y registra cuentas de prueba (luego las borra). */
    private static void probarPersonas(Connection cn, SedeJdbcRepository sedes) throws SQLException {
        System.out.println("\n--- Clientes y empleados (repositorios Jdbc) ---");
        Reloj reloj = new RelojSistema();
        PersonaMySqlDao dao = new PersonaMySqlDao(cn, reloj);
        ClienteJdbcRepository clientes = new ClienteJdbcRepository(dao);
        EmpleadoJdbcRepository empleados = new EmpleadoJdbcRepository(dao, sedes);
        AutenticacionService auth = new AutenticacionService(clientes, empleados, new CorreoSimulado(), reloj);

        comprobar("clientes: " + clientes.listarTodos().size(),
                clientes.listarTodos().size() == entero(cn, "SELECT COUNT(*) FROM cliente"));
        comprobar("empleados: " + empleados.listarTodos().size(),
                empleados.listarTodos().size() == entero(cn, "SELECT COUNT(*) FROM empleado"));
        long invitados = clientes.listarTodos().stream().filter(Cliente::esInvitado).count();
        comprobar("invitados: " + invitados, invitados == entero(cn, "SELECT COUNT(*) FROM cliente WHERE es_invitado"));
        Empleado cualquiera = empleados.listarTodos().get(0);
        comprobar("la sede del empleado es el mismo objeto que la del catálogo",
                sedes.buscarPorId(cualquiera.getSede().getIdSede()).orElse(null) == cualquiera.getSede());

        // Login con cuentas de la BD
        String[] admin = fila(cn, """
                SELECT e.correo, e.contra FROM empleado e JOIN rol r ON r.id_rol = e.id_rol
                WHERE r.nivel_acceso = 2 AND e.estado ORDER BY e.id_empleado LIMIT 1""");
        ucsur.pe.service.Sesion sesionAdmin = auth.iniciarSesion(admin[0], admin[1]);
        comprobar("login de administrador " + admin[0] + " -> " + sesionAdmin.tipo(),
                sesionAdmin.tipo() == ucsur.pe.service.Sesion.TipoCuenta.ADMINISTRADOR
                && sesionAdmin.empleado().getRol().tienePermiso(Rol.ADMINISTRAR));
        String[] socio = fila(cn, """
                SELECT correo, contra, id_cliente, puntos, nivel_socio FROM cliente
                WHERE estado AND NOT es_invitado ORDER BY id_cliente LIMIT 1""");
        Cliente c = auth.iniciarSesion(socio[0], socio[1]).cliente();
        comprobar("login de cliente " + socio[0] + " con sus puntos y nivel", c != null
                && c.getIdCliente() == Integer.parseInt(socio[2]) && c.consultarPuntos() == Integer.parseInt(socio[3])
                && c.getNivelSocio() == Integer.parseInt(socio[4]));
        String[] inactivo = fila(cn, "SELECT correo, contra FROM cliente WHERE NOT estado AND NOT es_invitado LIMIT 1");
        if (inactivo != null) {
            try {
                auth.iniciarSesion(inactivo[0], inactivo[1]);
                comprobar("cuenta desactivada no inicia sesión", false);
            } catch (IllegalStateException e) {
                comprobar("cuenta desactivada -> \"" + e.getMessage() + "\"", true);
            }
        }

        // Registrar, actualizar y rechazar repetidos; las cuentas de prueba se borran al final
        String correo = String.format(CORREO_PRUEBA, System.currentTimeMillis());
        try {
            String dni = dniLibre(cn);
            Cliente nuevo = auth.registrarCliente(dni, "Prueba", "Main Conexion", new DateTime(1, 1, 2000),
                    correo, null, "clave123", "clave123");
            comprobar("cliente registrado con id " + nuevo.getIdCliente(),
                    entero(cn, "SELECT COUNT(*) FROM cliente WHERE id_cliente = " + nuevo.getIdCliente()
                            + " AND dni = '" + dni + "'") == 1);
            comprobar("buscarPorCorreo devuelve el mismo objeto", clientes.buscarPorCorreo(correo.toUpperCase()).orElse(null) == nuevo);
            auth.actualizarDatos(nuevo, "987654321", correo);
            comprobar("actualizar teléfono queda en la BD", "987654321".equals(
                    textoDe(cn, "SELECT telefono FROM cliente WHERE id_cliente = " + nuevo.getIdCliente())));
            try {
                auth.registrarCliente(dniLibre(cn), "Otra", "Persona", null, correo, null, "clave123", "clave123");
                comprobar("correo repetido es rechazado", false);
            } catch (IllegalStateException e) {
                comprobar("correo repetido -> \"" + e.getMessage() + "\"", true);
            }
            try {
                // El servicio no revisa el DNI: lo rechaza la BD (uq_cliente_dni) y debe llegar en español
                auth.registrarCliente(dni, "Otra", "Persona", null, String.format(CORREO_PRUEBA, 1L), null, "clave123", "clave123");
                comprobar("DNI repetido es rechazado", false);
            } catch (RepositoryException e) {
                comprobar("DNI repetido -> \"" + e.getMessage() + "\"", true);
            }
            Cliente inv1 = auth.ingresarComoInvitado(correo);
            Cliente inv2 = auth.ingresarComoInvitado(correo);
            comprobar("dos invitados con el mismo correo reciben ids nuevos (" + inv1.getIdCliente() + ", " + inv2.getIdCliente() + ")",
                    inv1.getIdCliente() > 0 && inv2.getIdCliente() > inv1.getIdCliente()
                            && clientes.buscarPorCorreo(correo).orElse(null) == nuevo);
        } finally {
            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM cliente WHERE correo LIKE 'prueba.mainconexion.%@test.pe'")) {
                ps.executeUpdate();
            }
        }
    }

    // ---------- Fase 4: compras, entradas, pago y pedido ----------

    /** Todos los repositorios Jdbc y los servicios de la venta, cargados desde la BD (como abrir la app). */
    private record Sesion(Reloj reloj, SedeJdbcRepository sedes, ClienteJdbcRepository clientes,
                          EmpleadoJdbcRepository empleados, ProductoJdbcRepository productos,
                          CuponJdbcRepository cupones, TipoEntradaJdbcRepository tipos, CompraJdbcRepository compras,
                          PagoJdbcRepository pagos, PedidoJdbcRepository pedidos, ReclamoJdbcRepository reclamos,
                          CalificacionJdbcRepository calificaciones, AutenticacionService auth,
                          CompraService compra, PagoService pago, PedidoService pedido, EmisionService emision,
                          PostventaService postventa) { }

    private static Sesion abrirSesion(Connection cn) {
        Reloj reloj = new RelojSistema();
        CatalogoMySqlDao catalogo = new CatalogoMySqlDao(cn);
        SedeMySqlDao sedeDao = new SedeMySqlDao(cn, reloj);
        PeliculaJdbcRepository peliculas = new PeliculaJdbcRepository(new PeliculaMySqlDao(cn));
        TipoEntradaJdbcRepository tipos = new TipoEntradaJdbcRepository(catalogo);
        SedeJdbcRepository sedes = new SedeJdbcRepository(sedeDao, peliculas, tipos);
        ProductoJdbcRepository productos = new ProductoJdbcRepository(catalogo, sedes);
        CuponJdbcRepository cupones = new CuponJdbcRepository(catalogo);
        PromocionJdbcRepository promociones = new PromocionJdbcRepository(catalogo);
        PersonaMySqlDao personaDao = new PersonaMySqlDao(cn, reloj);
        ClienteJdbcRepository clientes = new ClienteJdbcRepository(personaDao);
        EmpleadoJdbcRepository empleados = new EmpleadoJdbcRepository(personaDao, sedes);
        CalculadoraImporte calculadora = new CalculadoraImporte();
        GestorPuntos gestorPuntos = new GestorPuntos();
        CompraMySqlDao compraDao = new CompraMySqlDao(cn, reloj, calculadora, gestorPuntos);
        CompraJdbcRepository compras = new CompraJdbcRepository(compraDao, clientes, empleados, sedes, cupones,
                promociones, tipos, productos);
        PagoJdbcRepository pagos = new PagoJdbcRepository(compraDao, compras);
        PedidoJdbcRepository pedidos = new PedidoJdbcRepository(compraDao, compras);
        PostventaMySqlDao postventaDao = new PostventaMySqlDao(cn);
        ReclamoJdbcRepository reclamos = new ReclamoJdbcRepository(postventaDao, compras);
        CalificacionJdbcRepository calificaciones = new CalificacionJdbcRepository(postventaDao, compras);
        CorreoSimulado correo = new CorreoSimulado();
        return new Sesion(reloj, sedes, clientes, empleados, productos, cupones, tipos, compras, pagos, pedidos,
                reclamos, calificaciones,
                new AutenticacionService(clientes, empleados, correo, reloj),
                new CompraService(compras, cupones, tipos, calculadora, gestorPuntos, reloj),
                new PagoService(pagos, compras, pedidos, new PasarelaSimulada(reloj), reloj),
                new PedidoService(pedidos, productos, compras, reloj),
                new EmisionService(compras, pagos, correo, reloj),
                new PostventaService(reclamos, calificaciones, correo));
    }

    /**
     * Recorre una venta completa con los servicios reales y revisa por SQL lo que quedó en la BD.
     * Luego abre una segunda sesión (otra carga desde cero) para ver que todo se recupera.
     * Usa un cliente de prueba; al final borra lo creado y deja stock, usos del cupón y correlativo como estaban.
     */
    private static void probarVentas(Connection cn) throws SQLException {
        System.out.println("\n--- Compras, entradas, pago y pedido (repositorios Jdbc) ---");
        long inicio = System.currentTimeMillis();
        Sesion s = abrirSesion(cn);
        comprobar("todo cargado en " + (System.currentTimeMillis() - inicio) + " ms: " + s.compras().listarTodas().size()
                + " compras", s.compras().listarTodas().size() == entero(cn, "SELECT COUNT(*) FROM compra"));
        int entradas = s.compras().listarTodas().stream().mapToInt(c -> c.getEntradas().size()).sum();
        comprobar("entradas de las compras: " + entradas, entradas == entero(cn, "SELECT COUNT(*) FROM entrada"));
        long pedidosCargados = s.compras().listarTodas().stream().filter(c -> c.getPedido() != null).count();
        comprobar("pedidos: " + pedidosCargados, pedidosCargados == entero(cn, "SELECT COUNT(*) FROM pedido"));
        comprobar("pagos: " + entero(cn, "SELECT COUNT(*) FROM transaccion_pago"),
                s.compras().listarTodas().stream().mapToInt(c -> s.pagos().listarPorCompra(c).size()).sum()
                        == entero(cn, "SELECT COUNT(*) FROM transaccion_pago"));
        comprobar("reclamos: " + s.reclamos().listarTodos().size(),
                s.reclamos().listarTodos().size() == entero(cn, "SELECT COUNT(*) FROM reclamo")
                        && s.reclamos().listarTodos().stream().allMatch(r ->
                                s.compras().buscarPorId(r.getCompra().getIdCompra()).orElse(null) == r.getCompra()));
        long calificadas = s.compras().listarTodas().stream().filter(c -> s.calificaciones().existePorCompra(c)).count();
        comprobar("compras calificadas: " + calificadas, calificadas == entero(cn, "SELECT COUNT(*) FROM calificacion"));
        Compra vendida = s.compras().listarTodas().stream()
                .filter(c -> Compra.CONFIRMADA.equals(c.getEstado()) && !c.getEntradas().isEmpty()).findFirst().orElseThrow();
        Entrada suEntrada = vendida.getEntradas().get(0);
        comprobar("la entrada de la compra es el mismo objeto que cuelga de la función",
                s.sedes().buscarEntrada(suEntrada.getIdEntrada()).orElse(null) == suEntrada
                        && suEntrada.getFuncion().getEntradasEmitidas().contains(suEntrada));
        comprobar("la compra está en el historial de su cliente (mismo objeto)",
                vendida.getCliente().consultarHistorial().contains(vendida)
                        && s.clientes().buscarPorId(vendida.getCliente().getIdCliente()).orElse(null) == vendida.getCliente());
        Compra reembolsadaBD = s.compras().listarTodas().stream()
                .filter(c -> Compra.REEMBOLSADA.equals(c.getEstado())).findFirst().orElse(null);
        if (reembolsadaBD != null) {
            comprobar("reembolsada de la BD: devuelto " + reembolsadaBD.obtenerDetalle().getMontoReembolsadoCentavos()
                    + " céntimos (columna monto_reembolsado)", reembolsadaBD.getMontoReembolsadoCentavos() > 0);
        }

        // ---------- Datos para la venta de prueba ----------
        TipoEntrada general = s.tipos().listarActivos().stream()
                .filter(t -> !t.isRequiereSocio() && t.getPuntosCanje() == 0).findFirst().orElseThrow();
        Funcion funcion = null;
        Producto producto = null;
        for (Sede sede : s.sedes().listarTodas()) {
            if (!sede.estaOperativa()) continue;
            Producto p = s.productos().listarTodos().stream()
                    .filter(x -> x.isEstado() && x.getNivelSocioMinimo() == 0 && x.getStock(sede) >= 2).findFirst().orElse(null);
            if (p == null) continue;
            for (Funcion f : sede.consultarCartelera()) {
                if (f.estaDisponible() && f.getSala().estaOperativa() && f.consultarDisponibilidad() >= 8
                        && f.motivoVentaCerrada(DateTime.ahora().sumarMinutos(10)) == null) {
                    funcion = f;
                    producto = p;
                    break;
                }
            }
            if (funcion != null) break;
        }
        if (funcion == null) {
            comprobar("hay una función con butacas y una sede con stock para probar", false);
            return;
        }
        Sede sede = funcion.getSala().getSede();
        List<Butaca> libres = funcion.obtenerButacasDisponibles();
        Cupon cupon = s.cupones().listarTodos().stream()
                .filter(c -> c.validarVigencia(DateTime.ahora())).findFirst().orElseThrow();
        int usosAntes = cupon.getUsosRealizados();
        int stockAntes = producto.getStock(sede);
        String correo = "prueba.ventas." + System.currentTimeMillis() + "@test.pe";
        Cliente cliente = s.auth().registrarCliente(dniLibre(cn), "Prueba", "Ventas", new DateTime(1, 1, 2000),
                correo, null, "clave123", "clave123");
        System.out.println("   (función " + funcion + ", producto " + producto.getNombre() + ", cupón " + cupon.getCodigo() + ")");
        try {
            // ---------- 1. Compra con 2 entradas, dulcería y cupón ----------
            Compra c1 = s.compra().iniciarCompra(cliente, sede);
            comprobar("compra iniciada en la BD: " + estadoCompra(cn, c1), Compra.EN_PROCESO.equals(estadoCompra(cn, c1)));
            s.compra().agregarEntrada(c1, funcion, libres.get(0), general);
            s.compra().agregarEntrada(c1, funcion, libres.get(1), general);
            s.compra().confirmarAsientos(c1);
            comprobar("al confirmar asientos: 2 entradas BLOQUEADA en la BD y con su id en Java",
                    contarEntradas(cn, c1, Entrada.BLOQUEADA) == 2 && c1.getEntradas().stream().allMatch(e -> e.getIdEntrada() > 0));
            s.compra().agregarProducto(c1, producto, 1, "prueba"); // el servicio arma el pedido
            Pedido pedido = c1.getPedido();
            s.compra().aplicarCupon(c1, cupon.getCodigo().toLowerCase());
            TransaccionPago pago1 = s.pago().pagar(c1, c1.getTotalCentavos(), TransaccionPago.YAPE);
            comprobar("pago aprobado: " + pago1.getEstadoPago() + (pago1.getMotivoRechazo() != null ? " " + pago1.getMotivoRechazo() : ""),
                    TransaccionPago.APROBADO.equals(pago1.getEstadoPago()));
            comprobar("el pago aprobado se guardó junto con la compra (id " + pago1.getIdTransaccion() + " en la BD)",
                    pago1.getIdTransaccion() > 0 && "APROBADO".equals(textoDe(cn,
                            "SELECT estado_pago FROM transaccion_pago WHERE id_transaccion = " + pago1.getIdTransaccion())));
            comprobar("compra CONFIRMADA en la BD con 2 entradas VENDIDA",
                    Compra.CONFIRMADA.equals(estadoCompra(cn, c1)) && contarEntradas(cn, c1, Entrada.VENDIDA) == 2);
            String serieBD = textoDe(cn, "SELECT serie_numero FROM compra WHERE id_compra = " + c1.getIdCompra());
            comprobar("serie del trigger: " + c1.getSerieNumero() + " (Java) = " + serieBD + " (BD)",
                    c1.getSerieNumero() != null && c1.getSerieNumero().startsWith("B001-") && c1.getSerieNumero().equals(serieBD));
            comprobar("pedido registrado con su QR: " + pedido.getCodigoQR(), pedido.getIdPedido() > 0
                    && pedido.getCodigoQR().startsWith("QR-PED-" + pedido.getIdPedido() + "-")
                    && pedido.getCodigoQR().equals(textoDe(cn, "SELECT codigo_qr FROM pedido WHERE id_pedido = " + pedido.getIdPedido())));
            comprobar("stock en la BD: " + stockAntes + " -> " + stockBD(cn, sede, producto), stockBD(cn, sede, producto) == stockAntes - 1);
            comprobar("usos del cupón en la BD: " + usosAntes + " -> " + (usosAntes + 1),
                    entero(cn, "SELECT usos_realizados FROM cupon WHERE id_cupon = " + cupon.getIdCupon()) == usosAntes + 1);
            comprobar("puntos del cliente en la BD: " + cliente.consultarPuntos(), cliente.consultarPuntos() > 0
                    && entero(cn, "SELECT puntos FROM cliente WHERE id_cliente = " + cliente.getIdCliente()) == cliente.consultarPuntos());

            // ---------- 2. Otra compra: la serie es distinta ----------
            Compra c2 = s.compra().iniciarCompra(cliente, sede);
            s.compra().agregarEntrada(c2, funcion, libres.get(2), general);
            s.compra().confirmarAsientos(c2);
            s.pago().pagar(c2, c2.getTotalCentavos(), TransaccionPago.TARJETA_DEBITO);
            comprobar("dos compras seguidas tienen series distintas: " + c1.getSerieNumero() + " y " + c2.getSerieNumero(),
                    c2.getSerieNumero() != null && !c2.getSerieNumero().equals(c1.getSerieNumero()));

            // ---------- 3. La butaca vendida ya no se puede elegir en otra compra ----------
            Compra c3 = s.compra().iniciarCompra(cliente, sede);
            try {
                s.compra().agregarEntrada(c3, funcion, libres.get(0), general);
                comprobar("butaca vendida rechazada en otra compra", false);
            } catch (IllegalStateException e) {
                comprobar("butaca vendida en otra compra -> \"" + e.getMessage() + "\"", true);
            }
            s.compra().cancelar(c3);
            comprobar("compra cancelada en la BD", Compra.CANCELADA.equals(estadoCompra(cn, c3)));

            // ---------- 3b. Postventa: reclamo atendido por el administrador y calificación ----------
            Reclamo reclamo = s.postventa().presentarReclamo(cliente, c1, Reclamo.ATENCION, "Prueba de MainConexion");
            comprobar("reclamo registrado con id " + reclamo.getIdReclamo() + " (" + estadoReclamo(cn, reclamo) + " en la BD)",
                    reclamo.getIdReclamo() > 0 && Reclamo.REGISTRADO.equals(estadoReclamo(cn, reclamo)));
            Empleado admin = s.empleados().listarTodos().stream()
                    .filter(e -> e.isEstado() && e.getSede() == sede && e.getRol().tienePermiso(Rol.ADMINISTRAR))
                    .findFirst().orElseThrow();
            comprobar("la bandeja del administrador de la sede lo muestra",
                    s.postventa().listarReclamos(admin, Reclamo.REGISTRADO).contains(reclamo));
            s.postventa().iniciarRevision(admin, reclamo);
            comprobar("en revisión en la BD", Reclamo.EN_REVISION.equals(estadoReclamo(cn, reclamo)));
            s.postventa().responderReclamo(admin, reclamo, "Gracias, ya lo revisamos");
            comprobar("respondido en la BD: " + estadoReclamo(cn, reclamo) + " con su respuesta",
                    Reclamo.RESUELTO.equals(estadoReclamo(cn, reclamo)) && "Gracias, ya lo revisamos".equals(
                            textoDe(cn, "SELECT respuesta FROM reclamo WHERE id_reclamo = " + reclamo.getIdReclamo())));
            Cliente otroCliente = s.clientes().listarTodos().stream()
                    .filter(c -> c != cliente && !c.esInvitado() && c.isEstado()).findFirst().orElseThrow();
            try {
                s.postventa().presentarReclamo(otroCliente, c1, Reclamo.ATENCION, "No es mi compra");
                comprobar("un cliente no reclama sobre compra ajena", false);
            } catch (IllegalStateException e) {
                comprobar("reclamo sobre compra ajena -> \"" + e.getMessage() + "\"", true);
            }
            Calificacion calificacion = s.postventa().calificar(cliente, c1, 5, "Prueba de MainConexion");
            comprobar("calificación guardada con id " + calificacion.getIdCalificacion(), calificacion.getIdCalificacion() > 0
                    && entero(cn, "SELECT puntuacion FROM calificacion WHERE id_compra = " + c1.getIdCompra()) == 5);
            try {
                s.postventa().calificar(cliente, c1, 3, "otra vez");
                comprobar("una sola calificación por compra", false);
            } catch (IllegalStateException e) {
                comprobar("calificar dos veces -> \"" + e.getMessage() + "\"", true);
            }

            // ---------- 4. Reembolso al cancelar la función (en memoria; la función no se guarda) ----------
            funcion.cancelar();
            long devuelto = s.pago().reembolsar(c2);
            funcion.reactivar();
            comprobar("reembolso en la BD: REEMBOLSADA, devuelto " + devuelto + " céntimos y entrada ANULADA",
                    Compra.REEMBOLSADA.equals(estadoCompra(cn, c2)) && contarEntradas(cn, c2, Entrada.ANULADA) == 1
                            && centavosDe(cn, "SELECT monto_reembolsado FROM compra WHERE id_compra = " + c2.getIdCompra()) == devuelto);

            // ---------- 5. Volver al mapa, quitar una entrada y confirmar de nuevo ----------
            Compra c4 = s.compra().iniciarCompra(cliente, sede);
            s.compra().agregarEntrada(c4, funcion, libres.get(3), general);
            Entrada quitada = s.compra().agregarEntrada(c4, funcion, libres.get(4), general);
            s.compra().confirmarAsientos(c4);
            s.compra().liberarAsientos(c4);
            s.compra().quitarEntrada(c4, quitada);
            s.compra().confirmarAsientos(c4);
            comprobar("tras liberar y quitar una: 1 BLOQUEADA y ninguna preselección suelta en la BD",
                    contarEntradas(cn, c4, Entrada.BLOQUEADA) == 1 && contarEntradas(cn, c4, Entrada.PRESELECCIONADA) == 0);
            s.compra().cancelar(c4);
            comprobar("al cancelar, la entrada queda ANULADA en la BD", contarEntradas(cn, c4, Entrada.ANULADA) == 1);

            // ---------- 6. Review Focus 3: si falla al guardar después de cobrar, se deshace todo ----------
            Compra c5 = s.compra().iniciarCompra(cliente, sede);
            s.compra().agregarEntrada(c5, funcion, libres.get(5), general);
            s.compra().confirmarAsientos(c5);
            s.compra().agregarProducto(c5, producto, 1, null);
            Pedido pedido5 = c5.getPedido();
            s.compra().aplicarCupon(c5, cupon.getCodigo());
            int usosAntesC5 = cupon.getUsosRealizados();
            int stockAntesC5 = producto.getStock(sede);
            int puntosAntesC5 = cliente.consultarPuntos();
            Entrada e5 = c5.getEntradas().get(0);
            // Otra "kiosko" vende esa butaca: en la BD el bloqueo de c5 venció y la butaca quedó VENDIDA para c2
            try (PreparedStatement ps = cn.prepareStatement("UPDATE entrada SET estado = 'EXPIRADA' WHERE id_entrada = ?")) {
                ps.setInt(1, e5.getIdEntrada());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = cn.prepareStatement("""
                    INSERT INTO entrada (codigo_qr, precio, fecha_emision, estado, id_compra, id_funcion, id_butaca, id_tipo_entrada)
                    VALUES (?, 10.00, NOW(), 'VENDIDA', ?, ?, ?, ?)""")) {
                ps.setString(1, "QR-PRUEBA-CHOQUE-" + e5.getIdEntrada());
                ps.setInt(2, c2.getIdCompra());
                ps.setInt(3, funcion.getIdFuncion());
                ps.setInt(4, e5.getButaca().getIdButaca());
                ps.setInt(5, general.getIdTipoEntrada());
                ps.executeUpdate();
            }
            TransaccionPago pago5 = s.pago().pagar(c5, c5.getTotalCentavos(), TransaccionPago.YAPE);
            comprobar("choque al guardar -> pago " + pago5.getEstadoPago() + ": \"" + pago5.getMotivoRechazo() + "\"",
                    TransaccionPago.RECHAZADO.equals(pago5.getEstadoPago())
                            && pago5.getMotivoRechazo().startsWith("No se pudo guardar la compra; cobro devuelto"));
            comprobar("la compra queda CANCELADA en Java y en la BD, sin entradas vendidas",
                    Compra.CANCELADA.equals(c5.getEstado()) && Compra.CANCELADA.equals(estadoCompra(cn, c5))
                            && contarEntradas(cn, c5, Entrada.VENDIDA) == 0);
            comprobar("ningún pago APROBADO para esa compra; el guardado es RECHAZADO",
                    entero(cn, "SELECT COUNT(*) FROM transaccion_pago WHERE estado_pago = 'APROBADO' AND id_compra = " + c5.getIdCompra()) == 0
                            && entero(cn, "SELECT COUNT(*) FROM transaccion_pago WHERE estado_pago = 'RECHAZADO' AND id_compra = " + c5.getIdCompra()) == 1);
            comprobar("en Java la butaca se libera (la entrada quedó " + e5.getEstado() + ")",
                    Entrada.ANULADA.equals(e5.getEstado()) && funcion.esButacaDisponible(e5.getButaca()));
            comprobar("cupón, stock y puntos vuelven a como estaban (Java y BD)",
                    cupon.getUsosRealizados() == usosAntesC5 && producto.getStock(sede) == stockAntesC5
                            && cliente.consultarPuntos() == puntosAntesC5
                            && entero(cn, "SELECT usos_realizados FROM cupon WHERE id_cupon = " + cupon.getIdCupon()) == usosAntesC5
                            && stockBD(cn, sede, producto) == stockAntesC5
                            && entero(cn, "SELECT puntos FROM cliente WHERE id_cliente = " + cliente.getIdCliente()) == puntosAntesC5);

            // ---------- 6b. Pedido guardado como borrador, editado y luego pagado ----------
            // «Volver» desde Dulcería guarda la compra con el pedido aún sin registrar (borrador con id).
            // Si después cambia una cantidad y paga, la BD debe quedar con la cantidad nueva, no la del borrador.
            Compra c6 = s.compra().iniciarCompra(cliente, sede);
            s.compra().agregarEntrada(c6, funcion, libres.get(6), general);
            s.compra().confirmarAsientos(c6);
            DetallePedido linea6 = s.compra().agregarProducto(c6, producto, 1, null);
            s.compra().liberarAsientos(c6);
            comprobar("borrador del pedido guardado con 1 unidad", c6.getPedido().getIdPedido() > 0
                    && entero(cn, "SELECT SUM(cantidad) FROM detalle_pedido WHERE id_pedido = " + c6.getPedido().getIdPedido()) == 1);
            s.compra().confirmarAsientos(c6);
            s.compra().cambiarCantidad(c6, linea6, 2);
            TransaccionPago pago6 = s.pago().pagar(c6, c6.getTotalCentavos(), TransaccionPago.YAPE);
            comprobar("pagado el pedido editado: la BD guarda 2 unidades y el total cobrado",
                    TransaccionPago.APROBADO.equals(pago6.getEstadoPago())
                            && entero(cn, "SELECT SUM(cantidad) FROM detalle_pedido WHERE id_pedido = " + c6.getPedido().getIdPedido()) == 2
                            && centavosDe(cn, "SELECT total FROM compra WHERE id_compra = " + c6.getIdCompra()) == c6.getTotalCentavos());

            // ---------- 7. Segunda sesión: todo se recupera de la BD ----------
            Sesion s2 = abrirSesion(cn);
            Compra c1b = s2.compras().buscarPorId(c1.getIdCompra()).orElseThrow();
            Cliente clienteB = s2.clientes().buscarPorCorreo(correo).orElseThrow();
            comprobar("otra sesión: la compra trae a su cliente (mismo objeto) con " + clienteB.consultarHistorial().size()
                            + " compras en su historial",
                    c1b.getCliente() == clienteB && clienteB.consultarHistorial().size()
                            == entero(cn, "SELECT COUNT(*) FROM compra WHERE id_cliente = " + cliente.getIdCliente()));
            comprobar("otra sesión: recuperar por código y correo", s2.emision().recuperarPorCodigo(c1.getSerieNumero(), correo) != null);
            comprobar("otra sesión: las 2 entradas vendidas cuelgan de la función y ocupan la butaca",
                    c1b.getEntradas().size() == 2 && c1b.getEntradas().stream().allMatch(e ->
                            s2.sedes().buscarEntrada(e.getIdEntrada()).orElse(null) == e
                                    && !e.getFuncion().esButacaDisponible(e.getButaca())));
            Compra c2b = s2.compras().buscarPorId(c2.getIdCompra()).orElseThrow();
            comprobar("otra sesión: la reembolsada muestra lo devuelto", c2b.obtenerDetalle().getMontoReembolsadoCentavos() == devuelto);
            comprobar("otra sesión: su pago aprobado", s2.pagos().listarPorCompra(c1b).stream()
                    .anyMatch(p -> TransaccionPago.APROBADO.equals(p.getEstadoPago())));
            List<Reclamo> reclamosB = s2.postventa().consultarReclamos(clienteB, c1b);
            comprobar("otra sesión: el cliente ve su reclamo RESUELTO con la respuesta", reclamosB.size() == 1
                    && Reclamo.RESUELTO.equals(reclamosB.get(0).getEstado())
                    && "Gracias, ya lo revisamos".equals(reclamosB.get(0).getRespuesta())
                    && reclamosB.get(0).getCompra() == c1b);
            Empleado adminB = s2.empleados().listarTodos().stream()
                    .filter(e -> e.isEstado() && e.getSede() == c1b.getSede() && e.getRol().tienePermiso(Rol.ADMINISTRAR))
                    .findFirst().orElseThrow();
            comprobar("otra sesión: la bandeja del administrador lo incluye",
                    s2.postventa().listarReclamos(adminB, null).contains(reclamosB.get(0)));
            try {
                s2.postventa().calificar(clienteB, c1b, 4, "otra sesión");
                comprobar("otra sesión: la compra ya calificada no se vuelve a calificar", false);
            } catch (IllegalStateException e) {
                comprobar("otra sesión: calificar de nuevo -> \"" + e.getMessage() + "\"", s2.calificaciones().existePorCompra(c1b));
            }
            Pedido pedidoB = c1b.getPedido();
            Empleado dulceria = s2.empleados().listarTodos().stream()
                    .filter(e -> e.isEstado() && e.getSede() == c1b.getSede() && e.getRol().tienePermiso(Rol.ATENDER_PEDIDO))
                    .findFirst().orElseThrow();
            comprobar("otra sesión: el pedido está en la cola de la sede", s2.pedido().consultarPendientes(dulceria).contains(pedidoB));
            s2.pedido().prepararPedido(dulceria, pedidoB);
            s2.pedido().marcarListo(dulceria, pedidoB);
            s2.pedido().entregarPedido(dulceria, pedidoB, pedidoB.getCodigoQR());
            String[] filaPedido = fila(cn, "SELECT estado, id_empleado, fecha_entrega FROM pedido WHERE id_pedido = " + pedidoB.getIdPedido());
            comprobar("otra sesión: pedido preparado y entregado por " + dulceria.getNombreCompleto() + " (en la BD: "
                            + filaPedido[0] + ")", Pedido.ENTREGADO.equals(filaPedido[0])
                            && String.valueOf(dulceria.getIdEmpleado()).equals(filaPedido[1]) && filaPedido[2] != null);

            // Reembolso de una compra pagada en la sesión anterior (la pasarela reconoce su referencia)
            Funcion funcionB = c1b.getEntradas().get(0).getFuncion();
            funcionB.cancelar();
            long devueltoB = s2.pago().reembolsar(c1b);
            funcionB.reactivar();
            comprobar("otra sesión: reembolso de una compra pagada antes (devuelto " + devueltoB + " de "
                            + c1b.getTotalCentavos() + "; la dulcería entregada no se devuelve)",
                    Compra.REEMBOLSADA.equals(estadoCompra(cn, c1)) && devueltoB > 0 && devueltoB < c1b.getTotalCentavos()
                            && centavosDe(cn, "SELECT monto_reembolsado FROM compra WHERE id_compra = " + c1.getIdCompra()) == devueltoB);
            try {
                s2.pago().reembolsar(c1b);
                comprobar("no se reembolsa dos veces", false);
            } catch (IllegalStateException e) {
                comprobar("segundo reembolso -> \"" + e.getMessage() + "\"", true);
            }
        } finally {
            limpiarVentas(cn, cliente.getIdCliente(), cupon, usosAntes, sede, producto, stockAntes);
        }
    }

    /** Borra lo que creó probarVentas y deja stock, usos del cupón y correlativo como estaban. */
    private static void limpiarVentas(Connection cn, int idCliente, Cupon cupon, int usosAntes, Sede sede,
                                      Producto producto, int stockAntes) throws SQLException {
        borrarClienteDePrueba(cn, idCliente);
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("UPDATE cupon SET usos_realizados = " + usosAntes + " WHERE id_cupon = " + cupon.getIdCupon());
            st.executeUpdate("UPDATE inventario_sede SET stock = " + stockAntes + " WHERE id_sede = " + sede.getIdSede()
                    + " AND id_producto = " + producto.getIdProducto());
        }
    }

    /** Borra un cliente de prueba con todas sus compras, y deja el correlativo en la última serie que queda. */
    private static void borrarClienteDePrueba(Connection cn, int idCliente) throws SQLException {
        String deCliente = " WHERE id_compra IN (SELECT id_compra FROM compra WHERE id_cliente = " + idCliente + ")";
        try (Statement st = cn.createStatement()) {
            st.executeUpdate("DELETE FROM reclamo" + deCliente);
            st.executeUpdate("DELETE FROM calificacion" + deCliente);
            st.executeUpdate("DELETE FROM pedido" + deCliente); // detalle_pedido se borra en cascada
            st.executeUpdate("DELETE FROM transaccion_pago" + deCliente);
            st.executeUpdate("DELETE FROM entrada" + deCliente);
            st.executeUpdate("DELETE FROM compra WHERE id_cliente = " + idCliente);
            st.executeUpdate("DELETE FROM cliente WHERE id_cliente = " + idCliente);
            st.executeUpdate("""
                    UPDATE correlativo
                    SET ultimo_numero = (SELECT COALESCE(MAX(CAST(SUBSTRING(serie_numero, 6) AS UNSIGNED)), 0) FROM compra)
                    WHERE serie = 'B001'""");
        }
    }

    // ---------- Fase 6: App con la base de datos ----------

    /**
     * Usa App tal como la usará la interfaz: solo controllers y Respuesta. Compra una entrada con un cliente
     * de prueba, "reinicia" la aplicación (otra App) y recupera la compra por su código. Al final la borra.
     */
    private static void probarApp(Connection cn) throws SQLException {
        System.out.println("\n--- App con la base de datos (como la usará la interfaz) ---");
        Reloj reloj = new RelojSistema();
        long inicio = System.currentTimeMillis();
        App app = new App(cn, reloj, new PasarelaSimulada(reloj), new CorreoSimulado());
        comprobar("App con MySQL creada en " + (System.currentTimeMillis() - inicio) + " ms", true);

        String[] admin = fila(cn, """
                SELECT e.correo, e.contra FROM empleado e JOIN rol r ON r.id_rol = e.id_rol
                WHERE r.nivel_acceso = 2 AND e.estado ORDER BY e.id_empleado LIMIT 1""");
        Respuesta<ucsur.pe.service.Sesion> sesion = app.login().iniciarSesion(admin[0], admin[1]);
        comprobar("login del administrador por el controller", sesion.ok()
                && sesion.dato().tipo() == ucsur.pe.service.Sesion.TipoCuenta.ADMINISTRADOR);
        Respuesta<ucsur.pe.service.Sesion> mala = app.login().iniciarSesion(admin[0], "clave-incorrecta");
        comprobar("clave incorrecta -> \"" + mala.mensaje() + "\"", !mala.ok());
        Respuesta<List<Pelicula>> peliculas = app.admin().listarPeliculas(sesion.dato().empleado());
        comprobar("el administrador lista " + peliculas.dato().size() + " películas",
                peliculas.ok() && peliculas.dato().size() == entero(cn, "SELECT COUNT(*) FROM pelicula"));
        probarAdministracion(cn, app, sesion.dato().empleado());

        // Una función con butacas libres y que no cierre su venta pronto
        Funcion funcion = null;
        for (Sede s : app.cartelera().listarSedes().dato()) {
            funcion = app.cartelera().consultarCartelera(s).dato().stream()
                    .filter(f -> f.consultarDisponibilidad() >= 2 && f.motivoVentaCerrada(DateTime.ahora().sumarMinutos(10)) == null)
                    .findFirst().orElse(null);
            if (funcion != null) break;
        }
        if (funcion == null) {
            comprobar("hay una función a la venta para probar", false);
            return;
        }
        Sede sede = funcion.getSala().getSede();
        TipoEntrada tipo = app.compra().listarTiposEntrada(null).dato().stream() // sin cliente: solo tarifas abiertas
                .filter(t -> t.getPuntosCanje() == 0).findFirst().orElseThrow();
        String correo = "prueba.app." + System.currentTimeMillis() + "@test.pe";
        Respuesta<Cliente> registro = app.login().registrarCliente(dniLibre(cn), "Prueba", "App", new DateTime(1, 1, 2000),
                correo, null, "clave123", "clave123");
        comprobar("registrar cliente: " + registro.mensaje(), registro.ok());
        if (!registro.ok()) return;
        Cliente cliente = registro.dato();
        try {
            Compra compra = app.compra().iniciarCompra(cliente, sede).dato();
            Butaca butaca = app.cartelera().consultarButacasDisponibles(funcion).dato().get(0);
            Respuesta<Entrada> entrada = app.compra().agregarEntrada(compra, funcion, butaca, tipo);
            Respuesta<Void> asientos = app.compra().confirmarAsientos(compra);
            Respuesta<TransaccionPago> pago = app.pago().pagar(compra, compra.getTotalCentavos(), TransaccionPago.PLIN);
            comprobar("comprar " + butaca.getCodigoAsiento() + " en " + funcion + " y pagar: " + pago.mensaje(),
                    entrada.ok() && asientos.ok() && pago.ok()
                            && TransaccionPago.APROBADO.equals(pago.dato().getEstadoPago()));
            Respuesta<String> comprobante = app.emision().imprimir(compra);
            comprobar("comprobante " + compra.getSerieNumero(), comprobante.ok()
                    && comprobante.dato().contains(compra.getSerieNumero()));

            // "Reiniciar" la aplicación: otra App carga todo desde la BD
            App reiniciada = new App(cn, reloj, new PasarelaSimulada(reloj), new CorreoSimulado());
            Respuesta<DetalleCompra> recuperada = reiniciada.emision().recuperarPorCodigo(compra.getSerieNumero(), correo);
            comprobar("tras reiniciar, recuperar " + compra.getSerieNumero() + " con el correo: " + recuperada.mensaje(),
                    recuperada.ok() && recuperada.dato().getButacas().contains(butaca.getCodigoAsiento()));
            Respuesta<ucsur.pe.service.Sesion> otraVez = reiniciada.login().iniciarSesion(correo, "clave123");
            comprobar("tras reiniciar, el cliente inicia sesión y ve su compra en el historial",
                    otraVez.ok() && reiniciada.postventa().consultarHistorial(otraVez.dato().cliente()).dato().size() == 1);
        } finally {
            borrarClienteDePrueba(cn, cliente.getIdCliente());
        }
    }

    /**
     * El administrador registra con datos sueltos (como MVCone): el servicio crea cada objeto con id 0
     * y la base le genera el id (AUTO_INCREMENT). Activar o desactivar se guarda en la BD.
     * Todo lo que se crea aquí se borra al final.
     */
    private static void probarAdministracion(Connection cn, App app, Empleado admin) throws SQLException {
        System.out.println("\n--- Administración: registrar con datos sueltos (ids AUTO_INCREMENT) ---");
        List<Genero> generos = app.admin().listarGeneros(admin).dato();
        comprobar("géneros por el controller: " + generos.size(), generos.size() == entero(cn, "SELECT COUNT(*) FROM genero"));
        List<TipoSala> formatos = app.admin().listarTiposSala(admin).dato();
        comprobar("formatos de sala por el controller: " + formatos.size(),
                formatos.size() == entero(cn, "SELECT COUNT(*) FROM tipo_sala"));
        comprobar("formas de descuento: " + app.admin().tiposDescuento().dato(),
                app.admin().tiposDescuento().dato().contains(CalculoPorcentaje.TIPO));

        Pelicula pelicula = null;
        Funcion funcion = null;
        Producto producto = null;
        try {
            Respuesta<Pelicula> rp = app.admin().registrarPelicula(admin, "Prueba MainConexion", "Se borra al terminar",
                    90, "APT", null, null, null, Pelicula.EN_CARTELERA, generos.subList(0, Math.min(2, generos.size())));
            pelicula = rp.dato();
            comprobar("película registrada con id " + (pelicula != null ? pelicula.getIdPelicula() : 0) + " y sus géneros",
                    rp.ok() && pelicula.getIdPelicula() > 0 && entero(cn,
                            "SELECT COUNT(*) FROM pelicula_genero WHERE id_pelicula = " + pelicula.getIdPelicula())
                            == pelicula.getGeneros().size());

            Sala sala = admin.getSede().consultarSalasDisponibles().stream().findFirst().orElse(null);
            if (rp.ok() && sala != null) {
                DateTime lejos = DateTime.ahora().sumarDias(400); // una fecha sin funciones, para no chocar
                Respuesta<Funcion> rf = app.admin().programarFuncion(admin, pelicula, sala,
                        new DateTime(lejos.getDia(), lejos.getMes(), lejos.getAnio()), new DateTime(3, 0), "Doblada");
                funcion = rf.dato();
                comprobar("función programada: " + rf.mensaje() + (funcion != null ? " (id " + funcion.getIdFuncion() + ")" : ""),
                        rf.ok() && funcion.getIdFuncion() > 0
                        && entero(cn, "SELECT COUNT(*) FROM funcion WHERE id_funcion = " + funcion.getIdFuncion()) == 1);
            }

            Respuesta<Producto> rpr = app.admin().registrarProducto(admin, "Prueba MainConexion", null, "Pruebas",
                    150, false, 0, 0);
            producto = rpr.dato();
            comprobar("producto registrado con id " + (producto != null ? producto.getIdProducto() : 0),
                    rpr.ok() && producto.getIdProducto() > 0);
            if (rpr.ok()) {
                Respuesta<Void> off = app.admin().cambiarEstadoProducto(admin, producto, false);
                comprobar("desactivar el producto queda guardado en la BD", off.ok() && entero(cn,
                        "SELECT COUNT(*) FROM producto WHERE NOT estado AND id_producto = " + producto.getIdProducto()) == 1);
            }
        } finally {
            if (funcion != null && funcion.getIdFuncion() > 0) borrar(cn, "DELETE FROM funcion WHERE id_funcion = ?", funcion.getIdFuncion());
            if (pelicula != null && pelicula.getIdPelicula() > 0) borrar(cn, "DELETE FROM pelicula WHERE id_pelicula = ?", pelicula.getIdPelicula());
            if (producto != null && producto.getIdProducto() > 0) {
                borrar(cn, "DELETE FROM inventario_sede WHERE id_producto = ?", producto.getIdProducto());
                borrar(cn, "DELETE FROM producto WHERE id_producto = ?", producto.getIdProducto());
            }
        }
    }

    /** Borra un registro de prueba por su id (pelicula_genero se borra en cascada). */
    private static void borrar(Connection cn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static String estadoReclamo(Connection cn, Reclamo reclamo) throws SQLException {
        return textoDe(cn, "SELECT estado FROM reclamo WHERE id_reclamo = " + reclamo.getIdReclamo());
    }

    private static String estadoCompra(Connection cn, Compra compra) throws SQLException {
        return textoDe(cn, "SELECT estado FROM compra WHERE id_compra = " + compra.getIdCompra());
    }

    private static int contarEntradas(Connection cn, Compra compra, String estado) throws SQLException {
        return entero(cn, "SELECT COUNT(*) FROM entrada WHERE id_compra = " + compra.getIdCompra() + " AND estado = '" + estado + "'");
    }

    private static int stockBD(Connection cn, Sede sede, Producto producto) throws SQLException {
        return entero(cn, "SELECT stock FROM inventario_sede WHERE id_sede = " + sede.getIdSede()
                + " AND id_producto = " + producto.getIdProducto());
    }

    /** Un DECIMAL de la BD en céntimos. */
    private static long centavosDe(Connection cn, String sql) throws SQLException {
        return ConversorDinero.aCentavos(textoDe(cn, sql));
    }

    /** Un DNI de 8 dígitos que ningún cliente usa. */
    private static String dniLibre(Connection cn) throws SQLException {
        String dni;
        do {
            dni = String.valueOf(10_000_000 + (long) (Math.random() * 89_999_999));
        } while (entero(cn, "SELECT COUNT(*) FROM cliente WHERE dni = '" + dni + "'") > 0);
        return dni;
    }

    /** Primera fila de la consulta como textos (null si no hay filas). */
    private static String[] fila(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            String[] valores = new String[rs.getMetaData().getColumnCount()];
            for (int i = 0; i < valores.length; i++) valores[i] = rs.getString(i + 1);
            return valores;
        }
    }

    /** Los datos de un descuento que se escriben en la BD, para comparar antes y después de guardar. */
    private static String describir(Cupon c) {
        return c.getCodigo() + "|" + c.getTipoDescuento() + "|" + c.getValor() + "|" + c.getFechaInicio().soloFecha()
                + "|" + c.getFechaFin().soloFecha() + "|" + c.getLimiteUso() + "|" + c.getUsosRealizados() + "|" + c.isEstado();
    }

    private static String describir(Promocion p) {
        return p.getNombre() + "|" + p.getTipoDescuento() + "|" + p.getValor() + "|" + p.getFechaInicio().soloFecha()
                + "|" + p.getFechaFin().soloFecha() + "|" + p.isSoloSocios() + "|" + p.getDiasAplicables() + "|" + p.isEstado();
    }

    // ---------- apoyo ----------

    private static int entero(Connection cn, String sql) throws SQLException {
        return Integer.parseInt(textoDe(cn, sql));
    }

    private static void mostrarConteo(Connection cn, String tabla) throws SQLException {
        // el nombre de tabla viene de este mismo archivo, no del usuario
        String total = textoDe(cn, "SELECT COUNT(*) FROM " + tabla);
        comprobar("tabla " + tabla + ": " + total + " filas", true);
    }

    private static String textoDe(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void comprobar(String descripcion, boolean ok) {
        if (!ok) fallas++;
        System.out.println((ok ? "OK    " : "FALLA ") + descripcion);
    }

    private static void terminar() {
        System.out.println(fallas == 0 ? "\nTodo OK." : "\n" + fallas + " comprobación(es) con FALLA.");
    }
}
