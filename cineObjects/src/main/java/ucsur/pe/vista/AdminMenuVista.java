package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

/** Menú lateral de administración: marca la sección actual y lleva a las demás. */
public class AdminMenuVista extends Pantalla {

    @FXML private Button btnPeliculas;
    @FXML private Button btnFunciones;
    @FXML private Button btnSalas;
    @FXML private Button btnPromociones;
    @FXML private Button btnProductos;
    @FXML private Button btnSedes;
    @FXML private Button btnReclamos;

    public AdminMenuVista(Navegador nav) {
        super(nav);
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
}
