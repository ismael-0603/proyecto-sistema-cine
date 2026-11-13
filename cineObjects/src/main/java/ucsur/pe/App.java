package ucsur.pe;

import ucsur.pe.controller.AdminController;
import ucsur.pe.controller.CarteleraController;
import ucsur.pe.controller.CompraController;
import ucsur.pe.controller.DulceriaController;
import ucsur.pe.controller.LoginController;
import ucsur.pe.controller.PagoController;
import ucsur.pe.controller.PostventaController;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.pasarela.PasarelaPago;
import ucsur.pe.pasarela.PasarelaSimulada;
import ucsur.pe.repository.*;
import ucsur.pe.service.AdministracionService;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.CarteleraService;
import ucsur.pe.service.CompraService;
import ucsur.pe.service.PagoService;
import ucsur.pe.service.PedidoService;
import ucsur.pe.service.PostventaService;

/**
 * Por qué existe: crea y conecta las piezas de abajo hacia arriba (repositorios -> servicios -> controllers),
 * como App en el ejemplo del curso. Es el único lugar que decide qué implementaciones se usan:
 * hoy repositorios en memoria y la pasarela simulada; mañana, repositorios Jdbc y una pasarela real.
 */
public class App {
    // Repositorios (hoy en memoria; con la BD se cambian aquí por sus versiones Jdbc)
    private final ClienteRepository clientes = new ClienteMemoriaRepository();
    private final EmpleadoRepository empleados = new EmpleadoMemoriaRepository();
    private final SedeRepository sedes = new SedeMemoriaRepository();
    private final PeliculaRepository peliculas = new PeliculaMemoriaRepository();
    private final ProductoRepository productos = new ProductoMemoriaRepository();
    private final CuponRepository cupones = new CuponMemoriaRepository();
    private final PromocionRepository promociones = new PromocionMemoriaRepository();
    private final CompraRepository compras = new CompraMemoriaRepository();
    private final PedidoRepository pedidos = new PedidoMemoriaRepository();
    private final PagoRepository pagos = new PagoMemoriaRepository();
    private final ReclamoRepository reclamos = new ReclamoMemoriaRepository();
    private final CalificacionRepository calificaciones = new CalificacionMemoriaRepository();

    private final LoginController login;
    private final CarteleraController cartelera;
    private final CompraController compra;
    private final PagoController pago;
    private final DulceriaController dulceria;
    private final AdminController admin;
    private final PostventaController postventa;

    /** Uso normal: reloj del sistema y pasarela simulada. */
    public App() {
        this(new RelojSistema(), new PasarelaSimulada());
    }

    /** Permite cambiar el reloj (por ejemplo, uno fijo en pruebas) y la pasarela. */
    public App(Reloj reloj, PasarelaPago pasarela) {
        this.login = new LoginController(new AutenticacionService(clientes, empleados));
        this.cartelera = new CarteleraController(new CarteleraService(sedes, promociones, reloj));
        this.compra = new CompraController(new CompraService(compras, cupones, reloj));
        this.pago = new PagoController(new PagoService(pagos, compras, pedidos, pasarela, reloj));
        this.dulceria = new DulceriaController(new PedidoService(pedidos, productos));
        this.admin = new AdminController(new AdministracionService(sedes, peliculas, productos, cupones, promociones));
        this.postventa = new PostventaController(new PostventaService(reclamos, calificaciones));
    }

    public LoginController login() { return login; }
    public CarteleraController cartelera() { return cartelera; }
    public CompraController compra() { return compra; }
    public PagoController pago() { return pago; }
    public DulceriaController dulceria() { return dulceria; }
    public AdminController admin() { return admin; }
    public PostventaController postventa() { return postventa; }

    // Acceso a los repositorios solo para cargar datos de prueba (sedes, empleados); con la BD vendrán de ella.
    public SedeRepository sedes() { return sedes; }
    public EmpleadoRepository empleados() { return empleados; }
}
