package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import ucsur.pe.service.Sesion;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Encabezado compartido del kiosco. En modo cliente muestra sede, «¿Perdiste tus entradas?», «Promociones»
 * y sesión; las pantallas lo ajustan con {@link #sinSede()}, {@link #soloVolver(String, Runnable)},
 * {@link #modoPanel(String, String)} o {@link #configurarMenuLateral(boolean, Runnable)}.
 */
public class EncabezadoVista extends Pantalla {

    @FXML private Button btnSede;
    @FXML private Button btnMenuAdmin;
    @FXML private Button btnPerdiste;
    @FXML private Button btnPromociones;
    @FXML private Button btnSesion;
    @FXML private Button btnVolver;
    @FXML private VBox cajaTitulo;
    @FXML private Label lblTituloPanel;
    @FXML private Label lblSubtituloPanel;
    @FXML private Button btnComprar;
    @FXML private HBox logo;

    private Runnable accionVolver = () -> { };
    private Runnable accionMenuAdmin = () -> { };
    /** Al tocar el logo se va a la cartelera; los paneles y el comprobante lo cambian. */
    private Runnable accionLogo = () -> nav.ir("cartelera");

    public EncabezadoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        ocultar(btnVolver);
        ocultar(btnMenuAdmin);
        logo.setOnMouseClicked(e -> accionLogo.run());
        if (estado().getSede() != null) {
            btnSede.setText("Sede: " + estado().getSede().getNombre() + " · Cambiar");
            btnSede.setGraphic(Iconos.crear(Iconos.UBICACION, 15, "icono-rojo"));
            btnSede.setGraphicTextGap(8);
        } else {
            ocultar(btnSede);
        }
        if (estado().getSesion() != null) {
            btnSesion.setText("Hola, " + estado().getSesion().cuenta().getNombres().split(" ")[0] + "  ▾");
        } else {
            btnSesion.setText("Iniciar sesión");
        }
        // Con sesión de cliente abierta puede consultar sus compras desde «Mi cuenta»
        if (estado().clienteConSesion() != null) {
            ocultar(btnPerdiste);
        }
    }

    /** Para «Elegir sede»: todavía no hay sede ni entradas que recuperar. */
    public void sinSede() {
        ocultar(btnSede);
        ocultar(btnMenuAdmin);
        ocultar(btnPerdiste);
        logo.getStyleClass().remove("logo-enlace"); // sin sede no hay cartelera a la que ir
        accionLogo = () -> { };
    }

    /** Cambia lo que hace el logo (p. ej. en el comprobante pregunta si cerrar sesión o seguir comprando). */
    public void alTocarLogo(Runnable accion) {
        accionLogo = accion;
    }

    /** Para login, registro y comprobante: solo un botón a la derecha («Volver», «Volver al inicio»). */
    public void soloVolver(String texto, Runnable accion) {
        ocultar(btnSede);
        ocultar(btnMenuAdmin);
        ocultar(btnPerdiste);
        ocultar(btnPromociones);
        ocultar(btnSesion);
        btnVolver.setText(texto);
        btnVolver.setVisible(true);
        btnVolver.setManaged(true);
        accionVolver = accion;
    }

    @FXML
    private void onCambiarSede() {
        nav.ir("elegir-sede");
    }

    /** Para los paneles de empleado: título, sede, «Ir a comprar entradas» y el menú del usuario. */
    public void modoPanel(String titulo, String subtitulo) {
        ocultar(btnSede);
        ocultar(btnMenuAdmin);
        ocultar(btnPerdiste);
        ocultar(btnPromociones);
        lblTituloPanel.setText(titulo);
        lblSubtituloPanel.setText(subtitulo);
        mostrarNodo(cajaTitulo);
        mostrarNodo(btnComprar);
        accionLogo = this::onComprar; // el empleado pasa a su cuenta personal, igual que «Ir a comprar entradas»
    }

    /** Solo para Administración: muestra el botón táctil (☰) que oculta o despliega el menú lateral. */
    public void configurarMenuLateral(boolean menuOculto, Runnable alAlternar) {
        this.accionMenuAdmin = alAlternar != null ? alAlternar : () -> { };
        actualizarBotonMenu(menuOculto);
        mostrarNodo(btnMenuAdmin);
    }

    public void actualizarBotonMenu(boolean menuOculto) {
        btnMenuAdmin.setGraphic(Iconos.crear(Iconos.MENU, 20));
        btnMenuAdmin.getStyleClass().remove("activo");
        if (!menuOculto) btnMenuAdmin.getStyleClass().add("activo");
    }

    @FXML
    private void onAlternarMenu() {
        accionMenuAdmin.run();
    }

    private static void mostrarNodo(Node nodo) {
        nodo.setVisible(true);
        nodo.setManaged(true);
    }

    @FXML
    private void onComprar() {
        nav.irACuentaPersonal();
    }

    /** Para «Promociones»: el botón queda marcado en rojo como pantalla actual. */
    public void marcarPromociones() {
        btnPromociones.getStyleClass().removeAll("btn-secundario", "btn-nav");
        btnPromociones.getStyleClass().add("btn-activo");
    }

    @FXML
    private void onPerdiste() {
        nav.ir("recuperar");
    }

    @FXML
    private void onPromociones() {
        nav.ir("promociones");
    }

    /**
     * 11 · Menú de usuario por rol: cliente, personal de dulcería o administrador. Un empleado que pasó a su
     * cuenta personal ve el menú de cliente y, además, el regreso a sus paneles (con PIN).
     */
    @FXML
    private void onSesion() {
        Sesion sesion = estado().getSesion();
        if (sesion == null) {
            nav.ir("iniciar-sesion");
            return;
        }
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("menu-usuario");
        if (sesion.esEmpleado() && sesion.enModoCliente()) {
            menu.getItems().addAll(
                    opcion("Mi cuenta", Iconos.USUARIO, false, () -> nav.ir("mi-cuenta")),
                    opcion("Mis puntos", Iconos.ESTRELLA, false, () -> nav.ir("mi-cuenta")),
                    opcion("Panel de dulcería", Iconos.PANEL, true, () -> nav.irAPanel("panel-dulceria", "Dulcería")));
            if (sesion.empleado().esAdministrador()) {
                menu.getItems().add(opcion("Administración", Iconos.ENGRANAJE, true,
                        () -> nav.irAPanel("admin-peliculas", "Administración")));
            }
        } else switch (sesion.tipo()) {
            case CLIENTE -> menu.getItems().addAll(
                    opcion("Mi cuenta", Iconos.USUARIO, false, () -> nav.ir("mi-cuenta")),
                    opcion("Mis puntos", Iconos.ESTRELLA, false, () -> nav.ir("mi-cuenta")));
            case DULCERIA -> menu.getItems().addAll(
                    opcion("Panel de dulcería", Iconos.PANEL, true, () -> nav.ir("panel-dulceria")),
                    opcion("Mi cuenta", Iconos.USUARIO, false, () -> nav.ir("mi-cuenta-empleado")));
            case ADMINISTRADOR -> menu.getItems().addAll(
                    opcion("Administración", Iconos.ENGRANAJE, true, () -> nav.ir("admin-peliculas")),
                    opcion("Panel de dulcería", Iconos.PANEL, false, () -> nav.ir("panel-dulceria")),
                    opcion("Mi cuenta", Iconos.USUARIO, false, () -> nav.ir("mi-cuenta-empleado")));
        }
        menu.getItems().add(opcion("Cerrar sesión", Iconos.SALIR, false, nav::cerrarSesion));
        menu.show(btnSesion, Side.BOTTOM, -60, 8);
    }

    private static MenuItem opcion(String texto, String icono, boolean destacada, Runnable accion) {
        MenuItem item = new MenuItem(texto, Iconos.crear(icono, 16));
        if (destacada) item.getStyleClass().add("menu-destacado");
        item.setOnAction(e -> accion.run());
        return item;
    }

    @FXML
    private void onVolver() {
        accionVolver.run();
    }

    private static void ocultar(Node nodo) {
        nodo.setVisible(false);
        nodo.setManaged(false);
    }
}
