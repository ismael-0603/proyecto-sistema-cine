package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cliente;

/** 12 · Mi cuenta del cliente: editar correo y teléfono, ver puntos, nivel de socio y beneficios. */
public class MiCuentaVista extends Pantalla {

    @FXML private Label lblNombres;
    @FXML private Label lblApellidos;
    @FXML private Label lblDni;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Label lblMensaje;
    @FXML private Label lblPuntos;
    @FXML private HBox niveles;
    @FXML private Label lblVisitas;
    @FXML private ProgressBar barraVisitas;
    @FXML private VBox beneficios;

    private Cliente cliente;

    public MiCuentaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        cliente = estado().clienteConSesion();
        if (cliente == null) {
            Platform.runLater(() -> nav.ir("iniciar-sesion"));
            return;
        }
        lblNombres.setText(cliente.getNombres());
        lblApellidos.setText(cliente.getApellidos());
        lblDni.setText(cliente.getDni());
        txtCorreo.setText(cliente.getCorreo());
        txtTelefono.setText(cliente.getTelefono());
        mostrarPuntos();
    }

    private void mostrarPuntos() {
        lblPuntos.setText(Formato.miles(cliente.consultarPuntos()) + " pts");
        int nivel = cliente.getNivelSocio();
        niveles.getChildren().clear();
        for (int n = 1; n <= Cliente.NIVEL_MAXIMO; n++) {
            Button b = new Button("Nivel " + n);
            b.getStyleClass().addAll("nivel", "btn-chico");
            if (n <= nivel) b.getStyleClass().add("activo");
            b.setFocusTraversable(false);
            b.setMouseTransparent(true);
            niveles.getChildren().add(b);
        }
        if (nivel >= Cliente.NIVEL_MAXIMO) {
            lblVisitas.setText("Ya tienes el nivel más alto. ¡Gracias por tu preferencia!");
            barraVisitas.setProgress(1);
        } else {
            int enEsteNivel = cliente.contarVisitasAnioActual() % Cliente.VISITAS_POR_NIVEL;
            lblVisitas.setText(enEsteNivel + " de " + Cliente.VISITAS_POR_NIVEL
                    + " visitas del año para subir a nivel " + (nivel + 1));
            barraVisitas.setProgress((double) enEsteNivel / Cliente.VISITAS_POR_NIVEL);
        }
        beneficios.getChildren().setAll(
                beneficio("Ganas x" + multiplicador() + " puntos por cada compra"),
                beneficio("Entrada tipo Socio (pagas con puntos)"),
                beneficio(nivel >= 2 ? "Combos exclusivos de tu nivel" : "Combos exclusivos desde nivel 2"),
                beneficio("Promociones solo para socios"));
    }

    /** 1.25 -> "1.25"; 2.0 -> "2" */
    private String multiplicador() {
        double m = cliente.obtenerMultiplicadorPuntos();
        return m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
    }

    private static Label beneficio(String texto) {
        Label l = new Label("•  " + texto);
        l.getStyleClass().add("nota");
        return l;
    }

    @FXML
    private void onGuardar() {
        Respuesta<Void> r = app().login().actualizarDatos(cliente, txtTelefono.getText(), txtCorreo.getText());
        mostrar(lblMensaje, r);
    }

    @FXML
    private void onCerrarSesion() {
        estado().cerrarSesion();
        nav.ir("cartelera");
    }

    @FXML
    private void onMisCompras() {
        nav.ir("mis-compras");
    }
}
