package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;

/** 3 · Login o invitado: antes de elegir butacas, iniciar sesión o seguir como invitado con un correo. */
public class LoginOInvitadoVista extends Pantalla {

    @FXML private TextField txtCorreo;
    @FXML private Label lblMensaje;

    private ValidacionEnVivo validacion;

    public LoginOInvitadoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        validacion = validacion(lblMensaje).campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()));
    }

    @FXML
    private void onIniciarSesion() {
        nav.ir("iniciar-sesion");
    }

    @FXML
    private void onInvitado() {
        if (!validacion.validarTodos()) return;
        Respuesta<Cliente> r = app().login().ingresarComoInvitado(txtCorreo.getText());
        if (!mostrar(lblMensaje, r)) return;
        estado().entrarComoInvitado(r.dato());
        Respuesta<Compra> compra = nav.continuarTrasIdentificarse();
        if (compra != null && !compra.ok()) mostrarError(lblMensaje, compra.mensaje());
    }

    @FXML
    private void onCrearCuenta() {
        nav.ir("crear-cuenta");
    }
}
