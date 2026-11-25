package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;

/** 12.1 · Mi cuenta del empleado: sus datos, rol, sede, turno, los accesos que le da su rol y su PIN. */
public class MiCuentaEmpleadoVista extends Pantalla {

    @FXML private Button btnVolver;
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
    private String destinoVolver;
    // Dos formularios: datos de contacto (lblMensaje) y PIN (lblMensajePin)
    private ValidacionEnVivo validacionDatos;
    private ValidacionEnVivo validacionPin;

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
        configurarVolver(admin);
        for (PasswordField campo : new PasswordField[] {txtPinActual, txtPinNuevo, txtPinConfirmar}) {
            campo.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,4}") ? c : null));
        }
        validacionDatos = validacion(lblMensaje)
                .campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()))
                .campo(txtTelefono, () -> Persona.errorTelefono(txtTelefono.getText()));
        // Que el PIN actual sea el correcto solo lo sabe el servicio: se revisa al pulsar el botón
        validacionPin = validacion(lblMensajePin)
                .campo(txtPinActual, () -> Empleado.errorPin(txtPinActual.getText()))
                .campo(txtPinNuevo, () -> Empleado.errorPin(txtPinNuevo.getText()))
                .campo(txtPinConfirmar, () -> txtPinConfirmar.getText().equals(txtPinNuevo.getText()) ? null
                        : "Los PIN nuevos no coinciden")
                .relacionar(txtPinNuevo, txtPinConfirmar);
    }

    @FXML
    private void onCambiarPin() {
        if (!validacionPin.validarTodos()) return;
        Respuesta<Void> r = app().login().cambiarPin(empleado, txtPinActual.getText(),
                txtPinNuevo.getText(), txtPinConfirmar.getText());
        if (mostrar(lblMensajePin, r)) {
            txtPinActual.clear();
            txtPinNuevo.clear();
            txtPinConfirmar.clear();
            validacionPin.limpiar(); // sin rojo tras vaciar los campos
        } else if (r.mensaje() != null && r.mensaje().contains("PIN") && !r.mensaje().contains("nuevos")) {
            validacionPin.marcar(txtPinActual, r.mensaje()); // el PIN actual no es el correcto
        }
    }

    @FXML
    private void onGuardar() {
        if (!validacionDatos.validarTodos()) return;
        Respuesta<Void> r = app().login().actualizarDatos(empleado, txtTelefono.getText(), txtCorreo.getText());
        if (!mostrar(lblMensaje, r) && r.mensaje() != null && r.mensaje().toLowerCase().contains("correo")) {
            validacionDatos.marcar(txtCorreo, r.mensaje());
        }
    }

    @FXML
    private void onCerrarSesion() {
        estado().cerrarSesion();
        nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
    }

    private void configurarVolver(boolean admin) {
        String anterior = nav.pantallaAnterior();
        if (anterior != null && anterior.startsWith("admin-") && admin) {
            destinoVolver = anterior;
            btnVolver.setText("← Volver a Administración");
        } else if ("panel-dulceria".equals(anterior)) {
            destinoVolver = "panel-dulceria";
            btnVolver.setText("← Volver al Panel de dulcería");
        } else if ("cartelera".equals(anterior) || "horario".equals(anterior)) {
            destinoVolver = "cartelera";
            btnVolver.setText("← Volver a la cartelera");
        } else {
            destinoVolver = admin ? "admin-peliculas" : "panel-dulceria";
            btnVolver.setText(admin ? "← Volver a Administración" : "← Volver al Panel de dulcería");
        }
    }

    @FXML
    private void onVolver() {
        nav.ir(destinoVolver != null ? destinoVolver
                : (empleado != null && empleado.esAdministrador() ? "admin-peliculas" : "panel-dulceria"));
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
