package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import ucsur.pe.model.Empleado;

/** 12.1 · Mi cuenta del empleado: sus datos, rol, sede, turno, los accesos que le da su rol y su PIN. */
public class MiCuentaEmpleadoVista extends Pantalla {

    @FXML private Label lblSubtitulo;
    @FXML private Label lblNombres;
    @FXML private Label lblApellidos;
    @FXML private Label lblDni;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Label lblMensaje;
    @FXML private Label lblRol;
    @FXML private Label lblSede;
    @FXML private Label lblTurno;
    @FXML private Button btnAdministracion;
    @FXML private PasswordField txtPinActual;
    @FXML private PasswordField txtPinNuevo;
    @FXML private PasswordField txtPinConfirmar;
    @FXML private Label lblMensajePin;

    private Empleado empleado;

    public MiCuentaEmpleadoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        empleado = estado().getSesion() == null ? null : estado().getSesion().empleadoEnPanel();
        if (empleado == null) {
            Platform.runLater(() -> nav.ir("iniciar-sesion"));
            return;
        }
        String rol = empleado.getRol() == null ? "—" : empleado.getRol().getNombre();
        lblSubtitulo.setText("Cuenta de empleado · " + rol);
        lblNombres.setText(empleado.getNombres());
        lblApellidos.setText(empleado.getApellidos());
        lblDni.setText(empleado.getDni());
        txtCorreo.setText(empleado.getCorreo());
        txtTelefono.setText(empleado.getTelefono());
        lblRol.setText(rol);
        lblSede.setText(empleado.getSede() == null ? "—" : empleado.getSede().getNombre());
        lblTurno.setText(empleado.getTurno() == null ? "—" : Formato.capitalizar(empleado.getTurno()));
        // «Ir a Administración» solo aparece para el administrador
        boolean admin = empleado.esAdministrador();
        btnAdministracion.setVisible(admin);
        btnAdministracion.setManaged(admin);
        for (PasswordField campo : new PasswordField[] {txtPinActual, txtPinNuevo, txtPinConfirmar}) {
            campo.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,4}") ? c : null));
        }
    }

    @FXML
    private void onCambiarPin() {
        boolean ok = mostrar(lblMensajePin, app().login().cambiarPin(empleado, txtPinActual.getText(),
                txtPinNuevo.getText(), txtPinConfirmar.getText()));
        if (ok) {
            txtPinActual.clear();
            txtPinNuevo.clear();
            txtPinConfirmar.clear();
        }
    }

    @FXML
    private void onGuardar() {
        mostrar(lblMensaje, app().login().actualizarDatos(empleado, txtTelefono.getText(), txtCorreo.getText()));
    }

    @FXML
    private void onCerrarSesion() {
        estado().cerrarSesion();
        nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
    }

    @FXML
    private void onPanelDulceria() {
        nav.ir("panel-dulceria");
    }

    @FXML
    private void onAdministracion() {
        nav.ir("admin-peliculas");
    }
}
