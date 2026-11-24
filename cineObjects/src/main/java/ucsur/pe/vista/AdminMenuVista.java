package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/** Menú lateral de administración: marca la sección actual y lleva a las demás. */
public class AdminMenuVista extends Pantalla {

    @FXML private VBox raizMenu;
    @FXML private Button btnColapsar;
    @FXML private Button btnPeliculas;
    @FXML private Button btnFunciones;
    @FXML private Button btnSalas;
    @FXML private Button btnPromociones;
    @FXML private Button btnProductos;
    @FXML private Button btnSedes;
    @FXML private Button btnReclamos;
    @FXML private Button btnComprar;

    private Runnable alColapsar = () -> { };

    public AdminMenuVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (btnColapsar != null) {
            btnColapsar.setGraphic(Iconos.crear(Iconos.CHEVRON_IZQ, 15));
            btnColapsar.setGraphicTextGap(2);
        }
        btnPeliculas.setGraphic(Iconos.crear(Iconos.PELICULA, 17));
        btnFunciones.setGraphic(Iconos.crear(Iconos.RELOJ, 17));
        btnSalas.setGraphic(Iconos.crear(Iconos.SALA, 17));
        btnPromociones.setGraphic(Iconos.crear(Iconos.ETIQUETA, 17));
        btnProductos.setGraphic(Iconos.crear(Iconos.DULCERIA, 17));
        btnSedes.setGraphic(Iconos.crear(Iconos.UBICACION, 17));
        btnReclamos.setGraphic(Iconos.crear(Iconos.CORREO, 17));
        btnComprar.setGraphic(Iconos.crear(Iconos.FLECHA_IZQ, 15));
        btnComprar.setGraphicTextGap(8);
    }

    /** Pinta en rojo la sección actual ("peliculas", "funciones", "salas"...). */
    public void marcar(String seccion) {
        Button elegido = switch (seccion) {
            case "funciones" -> btnFunciones;
            case "salas" -> btnSalas;
            case "promociones" -> btnPromociones;
            case "productos" -> btnProductos;
            case "sedes" -> btnSedes;
            case "reclamos" -> btnReclamos;
            default -> btnPeliculas;
        };
        elegido.getStyleClass().add("activo");
    }

    @FXML private void onPeliculas() { nav.ir("admin-peliculas"); }
    @FXML private void onFunciones() { nav.ir("admin-funciones"); }
    @FXML private void onSalas() { nav.ir("admin-salas"); }
    @FXML private void onPromociones() { nav.ir("admin-promociones"); }
    @FXML private void onProductos() { nav.ir("admin-productos"); }
    @FXML private void onSedes() { nav.ir("admin-sedes"); }
    @FXML private void onReclamos() { nav.ir("admin-reclamos"); }

    @FXML
    private void onComprar() {
        nav.irACuentaPersonal();
    }

    public void configurarColapso(Runnable accion) {
        this.alColapsar = accion != null ? accion : () -> { };
    }

    @FXML
    private void onColapsar() {
        alColapsar.run();
    }

    /** Oculta o muestra el menú lateral y su contenedor izquierdo en el BorderPane para ceder todo el ancho a la tabla. */
    public void aplicarVisibilidad(boolean visible) {
        VBox nodo = raizMenu != null ? raizMenu : (btnPeliculas != null && btnPeliculas.getParent() instanceof VBox v ? v : null);
        if (nodo == null) return;
        nodo.setVisible(visible);
        nodo.setManaged(visible);
        Parent contenedorIzq = nodo.getParent();
        if (contenedorIzq instanceof VBox) {
            contenedorIzq.setVisible(visible);
            contenedorIzq.setManaged(visible);
        }
    }
}
