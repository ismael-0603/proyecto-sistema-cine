package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.service.Sesion;

/** 5 · Crear cuenta: registra al cliente y entra con esa cuenta. */
public class CrearCuentaVista extends Pantalla {

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtDni;
    @FXML private TextField txtFechaNac;
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField txtContra;
    @FXML private PasswordField txtConfirmar;
    @FXML private CheckBox chkTerminos;
    @FXML private Label lblMensaje;

    public CrearCuentaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.soloVolver("Volver", () -> nav.ir("iniciar-sesion"));
    }

    @FXML
    private void onCrear() {
        if (!chkTerminos.isSelected()) {
            mostrarError(lblMensaje, "Debes aceptar los términos y el tratamiento de tus datos para crear la cuenta.");
            return;
        }
        DateTime fechaNac;
        try {
            fechaNac = Formato.leerFecha(txtFechaNac.getText(), "Fecha de nacimiento");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensaje, e.getMessage());
            return;
        }
        Respuesta<Cliente> r = app().login().registrarCliente(txtDni.getText(), txtNombres.getText(),
                txtApellidos.getText(), fechaNac, txtCorreo.getText(), txtTelefono.getText(),
                txtContra.getText(), txtConfirmar.getText());
        if (!mostrar(lblMensaje, r)) return;

        // Con la cuenta creada se inicia sesión con los mismos datos
        Respuesta<Sesion> sesion = app().login().iniciarSesion(txtCorreo.getText(), txtContra.getText());
        if (!mostrar(lblMensaje, sesion)) return;
        estado().iniciarSesion(sesion.dato());
        Respuesta<Compra> compra = nav.continuarTrasIdentificarse();
        if (compra != null && !compra.ok()) mostrarError(lblMensaje, compra.mensaje());
    }

    @FXML
    private void onIniciarSesion() {
        nav.ir("iniciar-sesion");
    }
}
