package ucsur.pe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import ucsur.pe.controller.AdminDescuentoController;
import ucsur.pe.controller.AdminFuncionController;
import ucsur.pe.controller.AdminInfraestructuraController;
import ucsur.pe.controller.AdminPeliculaController;
import ucsur.pe.controller.AdminProductoController;
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
import ucsur.pe.db.DBConnection;
import ucsur.pe.db.PeliculaDao;
import ucsur.pe.db.PeliculaMySqlDao;
import ucsur.pe.db.PersonaMySqlDao;
import ucsur.pe.db.PostventaMySqlDao;
import ucsur.pe.db.SedeMySqlDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.pasarela.PasarelaPago;
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
import ucsur.pe.service.DescuentoAdminService;
import ucsur.pe.service.FuncionAdminService;
import ucsur.pe.service.InfraestructuraAdminService;
import ucsur.pe.service.PeliculaAdminService;
import ucsur.pe.service.ProductoAdminService;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.CarteleraService;
import ucsur.pe.service.CompraService;
import ucsur.pe.service.EmisionService;
import ucsur.pe.service.PagoService;
import ucsur.pe.service.PedidoService;
import ucsur.pe.service.PostventaService;
import ucsur.pe.vista.Navegador;
import java.io.InputStream;
import java.sql.Connection;
import static javafx.application.Application.launch;

/**
 * Por qué existe: es la entrada de la aplicación, como App en el ejemplo MVCone.
 * Abre la conexión con DB_Cine, crea y conecta las piezas de abajo hacia arriba
 * (DAO -> repositorios -> servicios -> controllers), muestra la primera pantalla
 * y cierra la conexión al salir. Es el único lugar que decide qué implementaciones se usan:
 * repositorios JDBC, y la pasarela y el correo simulados. Los servicios solo conocen
 * las interfaces de los repositorios (DIP).
 */
public class App extends Application {

    /** Pesos de Manrope que usa el diseño; cada uno queda como su propia familia ("Manrope SemiBold"...). */
    private static final String[] FUENTES = {"Regular", "Medium", "SemiBold", "Bold", "ExtraBold"};

    private DBConnection db;
    private Reloj reloj;
    private LoginController login;
    private CarteleraController cartelera;
    private CompraController compra;
    private PagoController pago;
    private DulceriaController dulceria;
    private AdminPeliculaController adminPeliculas;
    private AdminFuncionController adminFunciones;
    private AdminProductoController adminProductos;
    private AdminDescuentoController adminDescuentos;
    private AdminInfraestructuraController adminInfraestructura;
    private PostventaController postventa;
    private EmisionController emision;

    @Override
    public void start(Stage stage) {
        cargarFuentes();
        CorreoSimulado correo = new CorreoSimulado();
        try {
            // 1. Conexión con la base de datos.
            db = DBConnection.getInstance();
            // 2. Repositorios, servicios y controllers: cada repositorio carga sus datos al crearse.
            reloj = new RelojSistema();
            PasarelaPago pasarela = new PasarelaSimulada(reloj);
            conectarCapas(db.getConexion(), pasarela, correo);
        } catch (RepositoryException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage()
                    + "\n\nRevise que el servidor MySQL esté encendido y que db.properties tenga los datos correctos.");
            error.setHeaderText("No se pudo abrir el kiosco");
            error.showAndWait();
            Platform.exit();
            return;
        }

        // 3. El navegador carga cada pantalla (FXML) en esta misma ventana y le pasa sus controllers.
        Navegador navegador = new Navegador(stage, this, getHostServices());
        // El correo es simulado: cada correo "enviado" aparece como aviso en la pantalla del kiosco
        correo.alEnviar(m -> navegador.mostrarCorreo(m.destinatario(), m.asunto(), m.cuerpo()));
        stage.setTitle("Cineplanet · Autoservicio");
        InputStream icono = App.class.getResourceAsStream("img/logo-emblema.png");
        if (icono != null) stage.getIcons().add(new Image(icono));
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setMaximized(true);
        navegador.empezar();
        stage.show();
    }

    @Override
    public void stop() {
        // 4. Cerrar la conexión al salir (como setOnCloseRequest en MVCone).
        if (db != null) {
            try {
                db.cerrar();
            } catch (RepositoryException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Arma las capas de abajo hacia arriba. El orden de los repositorios importa, porque cada uno reutiliza
     * los objetos que ya cargaron los anteriores (un solo objeto por id en toda la aplicación):
     * catálogo -> sedes (con salas, butacas y funciones) -> personas -> compras -> pagos, pedidos y postventa.
     */
    private void conectarCapas(Connection conexion, PasarelaPago pasarela, ServicioCorreo correo) {
        // DAO y repositorios
        CatalogoMySqlDao catalogo = new CatalogoMySqlDao(conexion);
        SedeMySqlDao sedeDao = new SedeMySqlDao(conexion, reloj);
        PeliculaDao peliculaDao = new PeliculaMySqlDao(conexion);
        PeliculaJdbcRepository peliculas = new PeliculaJdbcRepository(peliculaDao);
        TipoEntradaJdbcRepository tiposEntrada = new TipoEntradaJdbcRepository(catalogo);
        SedeJdbcRepository sedes = new SedeJdbcRepository(sedeDao, peliculas, tiposEntrada);
        SalaJdbcRepository salas = new SalaJdbcRepository(sedeDao, sedes, sedes.listarTiposSala());
        ProductoJdbcRepository productos = new ProductoJdbcRepository(catalogo, sedes);
        CuponJdbcRepository cupones = new CuponJdbcRepository(catalogo);
        PromocionJdbcRepository promociones = new PromocionJdbcRepository(catalogo);

        PersonaMySqlDao personaDao = new PersonaMySqlDao(conexion, reloj);
        ClienteJdbcRepository clientes = new ClienteJdbcRepository(personaDao);
        EmpleadoJdbcRepository empleados = new EmpleadoJdbcRepository(personaDao, sedes, clientes);

        // Calculadora y gestor de puntos no guardan estado: las compras leídas de la BD calculan igual
        CalculadoraImporte calculadora = new CalculadoraImporte();
        GestorPuntos gestorPuntos = new GestorPuntos();
        CompraMySqlDao compraDao = new CompraMySqlDao(conexion, reloj, calculadora, gestorPuntos);
        CompraJdbcRepository compras = new CompraJdbcRepository(compraDao, clientes, empleados, sedes, cupones,
                promociones, tiposEntrada, productos);
        PagoJdbcRepository pagos = new PagoJdbcRepository(compraDao, compras);
        PedidoJdbcRepository pedidos = new PedidoJdbcRepository(compraDao, compras);

        PostventaMySqlDao postventaDao = new PostventaMySqlDao(conexion);
        ReclamoJdbcRepository reclamos = new ReclamoJdbcRepository(postventaDao, compras);
        CalificacionJdbcRepository calificaciones = new CalificacionJdbcRepository(postventaDao, compras);

        // Servicios y controllers
        AutenticacionService autenticacionService = new AutenticacionService(clientes, empleados, correo, reloj);
        login = new LoginController(autenticacionService);

        CarteleraService carteleraService = new CarteleraService(sedes, promociones, peliculas, reloj);
        cartelera = new CarteleraController(carteleraService);

        CompraService compraService = new CompraService(compras, cupones, tiposEntrada,
                calculadora, gestorPuntos, reloj);
        compra = new CompraController(compraService);

        PagoService pagoService = new PagoService(pagos, compras, pasarela, reloj);
        pago = new PagoController(pagoService);

        PedidoService pedidoService = new PedidoService(pedidos, productos, compras, reloj);
        dulceria = new DulceriaController(pedidoService);

        // Administración: un servicio y un controller por tema (películas, funciones, productos, descuentos, sedes y salas)
        PeliculaAdminService peliculaAdminService = new PeliculaAdminService(peliculas, sedes);
        adminPeliculas = new AdminPeliculaController(peliculaAdminService);

        FuncionAdminService funcionAdminService = new FuncionAdminService(sedes, compras, pagoService, reloj);
        adminFunciones = new AdminFuncionController(funcionAdminService);

        ProductoAdminService productoAdminService = new ProductoAdminService(productos);
        adminProductos = new AdminProductoController(productoAdminService);

        DescuentoAdminService descuentoAdminService = new DescuentoAdminService(cupones, promociones);
        adminDescuentos = new AdminDescuentoController(descuentoAdminService);

        InfraestructuraAdminService infraestructuraAdminService = new InfraestructuraAdminService(sedes, salas);
        adminInfraestructura = new AdminInfraestructuraController(infraestructuraAdminService);

        PostventaService postventaService = new PostventaService(reclamos, calificaciones, correo);
        postventa = new PostventaController(postventaService);

        EmisionService emisionService = new EmisionService(compras, pagos, correo, reloj);
        emision = new EmisionController(emisionService);
    }

    private static void cargarFuentes() {
        for (String peso : FUENTES) {
            try (InputStream in = App.class.getResourceAsStream("fonts/Manrope-" + peso + ".ttf")) {
                if (in != null) Font.loadFont(in, 14);
            } catch (Exception e) {
                System.err.println("No se pudo cargar Manrope " + peso + ": " + e.getMessage());
            }
        }
    }

    // ---------- Controllers que usan las pantallas ----------

    public LoginController login() { return login; }
    public CarteleraController cartelera() { return cartelera; }
    public CompraController compra() { return compra; }
    public PagoController pago() { return pago; }
    public DulceriaController dulceria() { return dulceria; }
    public AdminPeliculaController adminPeliculas() { return adminPeliculas; }
    public AdminFuncionController adminFunciones() { return adminFunciones; }
    public AdminProductoController adminProductos() { return adminProductos; }
    public AdminDescuentoController adminDescuentos() { return adminDescuentos; }
    public AdminInfraestructuraController adminInfraestructura() { return adminInfraestructura; }
    public PostventaController postventa() { return postventa; }
    public EmisionController emision() { return emision; }

    public static void main(String[] args) {
        launch(args);
    }
}
