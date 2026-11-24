package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import ucsur.pe.service.Sesion;

/**
 * Encabezado blanco de las pantallas del cliente. Por defecto muestra la sede elegida, «¿Perdiste tus
 * entradas?», «Promociones» e «Iniciar sesión» (o el nombre de quien inició sesión). Las pantallas que lo
 * incluyen lo ajustan con {@link #sinSede()} o {@link #soloVolver(String, Runnable)}.
 */
public class EncabezadoVista extends Pantalla {

    @FXML private Button btnSede;
    @FXML private Button btnPerdiste;
    @FXML private Button btnPromociones;
    @FXML private Button btnSesion;
    @FXML private Button btnVolver;
    @FXML private javafx.scene.layout.VBox cajaTitulo;
    @FXML private javafx.scene.control.Label lblTituloPanel;
    @FXML private javafx.scene.control.Label lblSubtituloPanel;
    @FXML private Button btnComprar;

    private Runnable accionVolver = () -> { };

    public EncabezadoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        ocultar(btnVolver);
        if (estado().getSede() != null) {
            btnSede.setText("Sede: " + estado().getSede().getNombre() + " · Cambiar");
        } else {
            ocultar(btnSede);
        }
        if (estado().getSesion() != null) {
            btnSesion.setText("Hola, " + estado().getSesion().cuenta().getNombres().split(" ")[0] + "  ▾");
        } else {
            btnSesion.setText("Iniciar sesión");
        }
    }

    /** Para «Elegir sede»: todavía no hay sede ni entradas que recuperar. */
    public void sinSede() {
        ocultar(btnSede);
        ocultar(btnPerdiste);
    }

    /** Para login, registro y comprobante: solo un botón a la derecha («Volver», «Volver al inicio»). */
    public void soloVolver(String texto, Runnable accion) {
        ocultar(btnSede);
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
        ocultar(btnPerdiste);
        ocultar(btnPromociones);
        lblTituloPanel.setText(titulo);
        lblSubtituloPanel.setText(subtitulo);
        mostrarNodo(cajaTitulo);
        mostrarNodo(btnComprar);
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
        btnPromociones.getStyleClass().removeAll("btn-secundario");
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
                    opcion("👤   Mi cuenta", false, () -> nav.ir("mi-cuenta")),
                    opcion("☆   Mis puntos", false, () -> nav.ir("mi-cuenta")),
                    opcion("▦   Panel de dulcería", true, () -> nav.irAPanel("panel-dulceria", "Dulcería")));
            if (sesion.empleado().esAdministrador()) {
                menu.getItems().add(opcion("⚙   Administración", true,
                        () -> nav.irAPanel("admin-peliculas", "Administración")));
            }
        } else switch (sesion.tipo()) {
            case CLIENTE -> menu.getItems().addAll(
                    opcion("👤   Mi cuenta", false, () -> nav.ir("mi-cuenta")),
                    opcion("☆   Mis puntos", false, () -> nav.ir("mi-cuenta")));
            case DULCERIA -> menu.getItems().addAll(
                    opcion("▦   Panel de dulcería", true, () -> nav.ir("panel-dulceria")),
                    opcion("👤   Mi cuenta", false, () -> nav.ir("mi-cuenta-empleado")));
            case ADMINISTRADOR -> menu.getItems().addAll(
                    opcion("⚙   Administración", true, () -> nav.ir("admin-peliculas")),
                    opcion("▦   Panel de dulcería", false, () -> nav.ir("panel-dulceria")),
                    opcion("👤   Mi cuenta", false, () -> nav.ir("mi-cuenta-empleado")));
        }
        menu.getItems().add(opcion("⎋   Cerrar sesión", false, () -> {
            estado().cerrarSesion();
            nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
        }));
        menu.show(btnSesion, Side.BOTTOM, -60, 8);
    }

    private static MenuItem opcion(String texto, boolean destacada, Runnable accion) {
        MenuItem item = new MenuItem(texto);
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
