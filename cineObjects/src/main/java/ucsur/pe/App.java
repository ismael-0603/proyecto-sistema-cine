package ucsur.pe;

import ucsur.pe.controller.AdminController;
import ucsur.pe.controller.CarteleraController;
import ucsur.pe.controller.CompraController;
import ucsur.pe.controller.DulceriaController;
import ucsur.pe.controller.EmisionController;
import ucsur.pe.controller.LoginController;
import ucsur.pe.controller.PagoController;
import ucsur.pe.controller.PostventaController;
import ucsur.pe.correo.CorreoSimulado;
import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.db.CatalogoMySqlDao;
import ucsur.pe.db.CompraMySqlDao;
import ucsur.pe.db.PeliculaMySqlDao;
import ucsur.pe.db.PersonaMySqlDao;
import ucsur.pe.db.PostventaMySqlDao;
import ucsur.pe.db.SedeMySqlDao;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.pasarela.PasarelaPago;
import ucsur.pe.pasarela.PasarelaSimulada;
import ucsur.pe.repository.*;
import ucsur.pe.service.AdministracionService;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.CarteleraService;
import ucsur.pe.service.CompraService;
import ucsur.pe.service.EmisionService;
import ucsur.pe.service.PagoService;
import ucsur.pe.service.PedidoService;
import ucsur.pe.service.PostventaService;
import java.sql.Connection;

/**
 * Por qué existe: crea y conecta las piezas de abajo hacia arriba (repositorios -> servicios -> controllers),
 * como App en el ejemplo del curso. Es el único lugar que decide qué implementaciones se usan:
 * repositorios en memoria (las demos por consola) o en MySQL (la aplicación con la base DB_Cine),
 * y la pasarela y el correo simulados. Los servicios y controllers son los mismos en los dos casos,
 * porque solo conocen las interfaces de los repositorios (DIP).
 */
public class App {

    /** Los repositorios que usa la aplicación; se arman todos juntos, en memoria o en MySQL. */
    private record Repositorios(ClienteRepository clientes, EmpleadoRepository empleados, SedeRepository sedes,
                                SalaRepository salas, PeliculaRepository peliculas, ProductoRepository productos,
                                CuponRepository cupones, PromocionRepository promociones, CompraRepository compras,
                                PedidoRepository pedidos, PagoRepository pagos, ReclamoRepository reclamos,
                                CalificacionRepository calificaciones, TipoEntradaRepository tiposEntrada) {

        /** Listas en memoria: cada demo empieza vacía y carga sus propios datos de prueba. */
        static Repositorios enMemoria() {
            CompraMemoriaRepository compras = new CompraMemoriaRepository(); // los pagos aprobados se guardan con su compra
            return new Repositorios(new ClienteMemoriaRepository(), new EmpleadoMemoriaRepository(),
                    new SedeMemoriaRepository(), new SalaMemoriaRepository(), new PeliculaMemoriaRepository(),
                    new ProductoMemoriaRepository(), new CuponMemoriaRepository(), new PromocionMemoriaRepository(),
                    compras, new PedidoMemoriaRepository(), new PagoMemoriaRepository(compras),
                    new ReclamoMemoriaRepository(), new CalificacionMemoriaRepository(),
                    new TipoEntradaMemoriaRepository());
        }

        /**
         * MySQL: cada repositorio carga sus datos al crearse. El orden importa, porque cada uno reutiliza
         * los objetos que ya cargaron los anteriores (un solo objeto por id en toda la aplicación):
         * catálogo -> sedes (con salas, butacas y funciones) -> personas -> compras -> pagos, pedidos y postventa.
         */
        static Repositorios enMySql(Connection conexion, Reloj reloj) {
            CatalogoMySqlDao catalogo = new CatalogoMySqlDao(conexion);
            SedeMySqlDao sedeDao = new SedeMySqlDao(conexion, reloj);
            PeliculaJdbcRepository peliculas = new PeliculaJdbcRepository(new PeliculaMySqlDao(conexion));
            TipoEntradaJdbcRepository tiposEntrada = new TipoEntradaJdbcRepository(catalogo);
            SedeJdbcRepository sedes = new SedeJdbcRepository(sedeDao, peliculas, tiposEntrada);
            SalaJdbcRepository salas = new SalaJdbcRepository(sedeDao, sedes, sedes.listarTiposSala());
            ProductoJdbcRepository productos = new ProductoJdbcRepository(catalogo, sedes);
            CuponJdbcRepository cupones = new CuponJdbcRepository(catalogo);
            PromocionJdbcRepository promociones = new PromocionJdbcRepository(catalogo);

            PersonaMySqlDao personaDao = new PersonaMySqlDao(conexion, reloj);
            ClienteJdbcRepository clientes = new ClienteJdbcRepository(personaDao);
            EmpleadoJdbcRepository empleados = new EmpleadoJdbcRepository(personaDao, sedes);

            // Calculadora y gestor de puntos no guardan estado: las compras leídas de la BD calculan igual
            CompraMySqlDao compraDao = new CompraMySqlDao(conexion, reloj, new CalculadoraImporte(), new GestorPuntos());
            CompraJdbcRepository compras = new CompraJdbcRepository(compraDao, clientes, empleados, sedes, cupones,
                    promociones, tiposEntrada, productos);
            PagoJdbcRepository pagos = new PagoJdbcRepository(compraDao, compras);
            PedidoJdbcRepository pedidos = new PedidoJdbcRepository(compraDao, compras);

            PostventaMySqlDao postventaDao = new PostventaMySqlDao(conexion);
            ReclamoJdbcRepository reclamos = new ReclamoJdbcRepository(postventaDao, compras);
            CalificacionJdbcRepository calificaciones = new CalificacionJdbcRepository(postventaDao, compras);

            return new Repositorios(clientes, empleados, sedes, salas, peliculas, productos, cupones, promociones,
                    compras, pedidos, pagos, reclamos, calificaciones, tiposEntrada);
        }
    }

    private final Repositorios repos;
    private final LoginController login;
    private final CarteleraController cartelera;
    private final CompraController compra;
    private final PagoController pago;
    private final DulceriaController dulceria;
    private final AdminController admin;
    private final PostventaController postventa;
    private final EmisionController emision;
    private final Reloj reloj;
    private final ServicioCorreo correo;

    // ---------- En memoria (demos por consola) ----------

    /** Uso normal: reloj del sistema y pasarela simulada. */
    public App() {
        this(new RelojSistema());
    }

    /** Cambia solo el reloj (por ejemplo, uno fijo en pruebas); la pasarela simulada usa ese mismo reloj. */
    public App(Reloj reloj) {
        this(reloj, new PasarelaSimulada(reloj));
    }

    /** Permite cambiar el reloj (por ejemplo, uno fijo en pruebas) y la pasarela. */
    public App(Reloj reloj, PasarelaPago pasarela) {
        this(reloj, pasarela, new CorreoSimulado());
    }

    /** Permite cambiar también el proveedor de correo (hoy simulado). */
    public App(Reloj reloj, PasarelaPago pasarela, ServicioCorreo correo) {
        this(Repositorios.enMemoria(), reloj, pasarela, correo);
    }

    // ---------- Con la base de datos ----------

    /**
     * La aplicación con MySQL, como App.start en el ejemplo del curso: recibe la conexión
     * (DBConnection.getInstance().getConexion()) y carga los datos de DB_Cine al crearse.
     * Quien abre la conexión es quien la cierra al salir (DBConnection.cerrar()).
     */
    public App(Connection conexion, Reloj reloj, PasarelaPago pasarela, ServicioCorreo correo) {
        this(Repositorios.enMySql(exigirConexion(conexion), reloj), reloj, pasarela, correo);
    }

    private static Connection exigirConexion(Connection conexion) {
        if (conexion == null) throw new IllegalArgumentException("La conexión a la base de datos es obligatoria");
        return conexion;
    }

    /** Constructor común: recibe los repositorios ya armados (en memoria o en MySQL) y conecta lo demás. */
    private App(Repositorios repos, Reloj reloj, PasarelaPago pasarela, ServicioCorreo correo) {
        this.repos = repos;
        this.reloj = reloj;
        this.correo = correo;
        // Colaboradores sin estado de la compra: App decide cuáles se usan (DIP).
        CalculadoraImporte calculadora = new CalculadoraImporte();
        GestorPuntos gestorPuntos = new GestorPuntos();

        this.login = new LoginController(new AutenticacionService(repos.clientes(), repos.empleados(), correo, reloj));
        this.cartelera = new CarteleraController(new CarteleraService(repos.sedes(), repos.promociones(),
                repos.peliculas(), reloj));
        this.compra = new CompraController(new CompraService(repos.compras(), repos.cupones(), repos.tiposEntrada(),
                calculadora, gestorPuntos, reloj));
        PagoService pagoService = new PagoService(repos.pagos(), repos.compras(), repos.pedidos(), pasarela, reloj);
        this.pago = new PagoController(pagoService);
        this.dulceria = new DulceriaController(new PedidoService(repos.pedidos(), repos.productos()));
        this.admin = new AdminController(new AdministracionService(repos.sedes(), repos.salas(), repos.peliculas(),
                repos.productos(), repos.cupones(), repos.promociones(), repos.compras(), pagoService, reloj));
        this.postventa = new PostventaController(new PostventaService(repos.reclamos(), repos.calificaciones(), correo));
        this.emision = new EmisionController(new EmisionService(repos.compras(), correo, reloj));
    }

    public LoginController login() { return login; }
    public CarteleraController cartelera() { return cartelera; }
    public CompraController compra() { return compra; }
    public PagoController pago() { return pago; }
    public DulceriaController dulceria() { return dulceria; }
    public AdminController admin() { return admin; }
    public PostventaController postventa() { return postventa; }
    public EmisionController emision() { return emision; }

    // ---------- Solo para las demos por consola ----------
    // La interfaz gráfica usa únicamente los controllers de arriba: nunca toca repositorios ni crea modelos.
    // Las demos en memoria sí necesitan cargar datos de prueba (sedes, salas, empleados) que no tienen pantalla.

    /** Solo demos: para cargar sedes y salas de prueba en memoria; con la BD ya vienen cargadas. */
    public SedeRepository sedes() { return repos.sedes(); }
    /** Solo demos: para cargar empleados de prueba en memoria; con la BD ya vienen cargados. */
    public EmpleadoRepository empleados() { return repos.empleados(); }
    /** Solo demos: el reloj de toda la aplicación, para crear datos de prueba con la misma hora. */
    public Reloj reloj() { return reloj; }
    /** Solo demos: el proveedor de correo, para revisar qué se "envió". */
    public ServicioCorreo correo() { return correo; }
}
