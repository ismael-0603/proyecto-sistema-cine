package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Compra;
import ucsur.pe.service.Sesion;

/** 4 · Iniciar sesión con correo y contraseña. */
public class IniciarSesionVista extends Pantalla {

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtCorreo;
    @FXML private PasswordField txtContra;
    @FXML private TextField txtContraVisible;
    @FXML private ToggleButton btnVer;
    @FXML private Label lblMensaje;

    private ValidacionEnVivo validacion;

    public IniciarSesionVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.soloVolver("Volver", this::volver);
        // El ojo muestra u oculta la contraseña: los dos campos comparten el mismo texto
        txtContraVisible.textProperty().bindBidirectional(txtContra.textProperty());
        txtContraVisible.visibleProperty().bind(btnVer.selectedProperty());
        txtContra.visibleProperty().bind(btnVer.selectedProperty().not());
        btnVer.setText(null);
        btnVer.setGraphic(Iconos.crear(Iconos.OJO, 18, "icono-suave"));
        btnVer.selectedProperty().addListener((o, antes, visible) ->
                btnVer.setGraphic(Iconos.crear(visible ? Iconos.OJO_OCULTO : Iconos.OJO, 18, "icono-suave")));
        // La contraseña solo se exige escrita: si es corta, el login responde «Correo o contraseña incorrectos»
        validacion = validacion(lblMensaje)
                .campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()))
                .campo(txtContra, () -> txtContra.getText().isEmpty() ? "Escribe tu contraseña" : null, txtContraVisible);
        // Al volver de «Recuperar contraseña» el correo ya viene escrito
        String correo = estado().getCorreoLogin();
        if (correo != null) {
            txtCorreo.setText(correo);
            txtContra.requestFocus();
        }
    }

    @FXML
    private void onIniciar() {
        if (!validacion.validarTodos()) return;
        Respuesta<Sesion> r = app().login().iniciarSesion(txtCorreo.getText(), txtContra.getText());
        if (!mostrar(lblMensaje, r)) return;
        Sesion sesion = r.dato();
        estado().iniciarSesion(sesion);
        if (sesion.tipo() == Sesion.TipoCuenta.DULCERIA) {
            nav.ir("panel-dulceria"); // el personal de dulcería entra directo a su cola de pedidos
            return;
        }
        if (sesion.tipo() == Sesion.TipoCuenta.ADMINISTRADOR) {
            nav.ir("admin-peliculas"); // el administrador entra a la gestión del cine
            return;
        }
        Respuesta<Compra> compra = nav.continuarTrasIdentificarse();
        if (compra != null && !compra.ok()) mostrarError(lblMensaje, compra.mensaje());
    }

    @FXML
    private void onOlvide() {
        estado().setCorreoLogin(txtCorreo.getText()); // si ya escribió su correo, la otra pantalla lo trae
        nav.ir("recuperar-contrasena");
    }

    @FXML
    private void onCrearCuenta() {
        nav.ir("crear-cuenta");
    }

    /** Vuelve a donde estaba: a «Login o invitado» si ya eligió horario, si no a la cartelera. */
    private void volver() {
        nav.ir(estado().getFuncion() != null ? "login-invitado" : "cartelera");
    }
}
