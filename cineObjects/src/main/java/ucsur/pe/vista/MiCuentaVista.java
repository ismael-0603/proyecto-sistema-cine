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
import ucsur.pe.model.Persona;
import ucsur.pe.model.Cliente;

/** 12 · Mi cuenta del cliente: editar correo y teléfono, ver puntos, nivel de socio y beneficios. */
public class MiCuentaVista extends Pantalla {

    @FXML private Button btnVolver;
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
    private ValidacionEnVivo validacion;

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
        validacion = validacion(lblMensaje)
                .campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()))
                .campo(txtTelefono, () -> Persona.errorTelefono(txtTelefono.getText()));
        if (estado().getSede() == null && btnVolver != null) {
            btnVolver.setText("← Volver");
        }
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
            int visitas = cliente.contarVisitasAnioActual();
            int meta = nivel * Cliente.VISITAS_POR_NIVEL;
            int progreso = Math.min(visitas, meta);
            lblVisitas.setText(progreso + " de " + meta
                    + " visitas del año para subir a nivel " + (nivel + 1));
            barraVisitas.setProgress((double) progreso / meta);
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
        if (!validacion.validarTodos()) return;
        Respuesta<Void> r = app().login().actualizarDatos(cliente, txtTelefono.getText(), txtCorreo.getText());
        if (!mostrar(lblMensaje, r) && r.mensaje() != null && r.mensaje().toLowerCase().contains("correo")) {
            validacion.marcar(txtCorreo, r.mensaje()); // p. ej. el correo ya lo usa otra cuenta
        }
    }

    @FXML
    private void onCerrarSesion() {
        nav.cerrarSesion();
    }

    @FXML
    private void onVolver() {
        nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
    }

    @FXML
    private void onMisCompras() {
        nav.ir("mis-compras");
    }
}
