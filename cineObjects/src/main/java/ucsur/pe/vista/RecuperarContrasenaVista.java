package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;

/**
 * 4.1 · Recuperar contraseña, desde «¿Olvidaste tu contraseña?» de la pantalla 4. En dos pasos:
 * 1) se pide el código al correo de la cuenta; 2) con el código se elige la contraseña nueva.
 * Las reglas (código de un solo uso, vencimiento, intentos, largo mínimo) las pone AutenticacionService;
 * el correo es simulado, así que el código aparece en el aviso de correo del Navegador.
 */
public class RecuperarContrasenaVista extends Pantalla {

    @FXML private EncabezadoVista encabezadoController;
    @FXML private Label lblInstruccion;
    @FXML private TextField txtCorreo;
    @FXML private Button btnEnviar;
    @FXML private VBox pasoCodigo;
    @FXML private TextField txtCodigo;
    @FXML private PasswordField txtNueva;
    @FXML private TextField txtNuevaVisible;
    @FXML private PasswordField txtConfirmar;
    @FXML private TextField txtConfirmarVisible;
    @FXML private ToggleButton btnVer;
    @FXML private Label lblMensaje;

    public RecuperarContrasenaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.soloVolver("Volver", this::volverAlLogin);
        // El ojo muestra u oculta las dos contraseñas a la vez
        txtNuevaVisible.textProperty().bindBidirectional(txtNueva.textProperty());
        txtConfirmarVisible.textProperty().bindBidirectional(txtConfirmar.textProperty());
        txtNuevaVisible.visibleProperty().bind(btnVer.selectedProperty());
        txtConfirmarVisible.visibleProperty().bind(btnVer.selectedProperty());
        txtNueva.visibleProperty().bind(btnVer.selectedProperty().not());
        txtConfirmar.visibleProperty().bind(btnVer.selectedProperty().not());
        btnVer.setText(null);
        btnVer.setGraphic(Iconos.crear(Iconos.OJO, 18, "icono-suave"));
        btnVer.selectedProperty().addListener((o, antes, visible) ->
                btnVer.setGraphic(Iconos.crear(visible ? Iconos.OJO_OCULTO : Iconos.OJO, 18, "icono-suave")));
        // Solo cifras y como máximo 6 en el código
        txtCodigo.textProperty().addListener((o, antes, ahora) -> {
            String limpio = ahora.replaceAll("\\D", "");
            if (limpio.length() > 6) limpio = limpio.substring(0, 6);
            if (!limpio.equals(ahora)) txtCodigo.setText(limpio);
        });
        // Si en el login ya había escrito su correo, se trae
        String correo = estado().getCorreoLogin();
        if (correo != null) txtCorreo.setText(correo);
    }

    /** «Enviar código» y «Reenviar código»: el mensaje es el mismo exista o no la cuenta. */
    @FXML
    private void onEnviar() {
        Respuesta<Void> r = app().login().solicitarRecuperacion(txtCorreo.getText());
        if (!mostrar(lblMensaje, r)) return;
        mostrarPasoCodigo(true);
        txtCodigo.requestFocus();
    }

    @FXML
    private void onCambiar() {
        Respuesta<Void> r = app().login().restablecerContrasena(txtCorreo.getText(), txtCodigo.getText(),
                txtNueva.getText(), txtConfirmar.getText());
        if (!mostrar(lblMensaje, r)) return;
        DialogoVista d = nav.dialogo();
        d.configurar("Contraseña cambiada", "Ya puedes iniciar sesión con tu nueva contraseña.", false);
        d.botonPrincipal("Iniciar sesión", this::volverAlLogin);
    }

    /** Vuelve al paso 1 para escribir otro correo; el código pedido para el anterior ya no se usa aquí. */
    @FXML
    private void onOtroCorreo() {
        mostrarPasoCodigo(false);
        limpiar(lblMensaje);
        txtCorreo.requestFocus();
    }

    private void mostrarPasoCodigo(boolean visible) {
        pasoCodigo.setVisible(visible);
        pasoCodigo.setManaged(visible);
        btnEnviar.setVisible(!visible);
        btnEnviar.setManaged(!visible);
        txtCorreo.setDisable(visible); // el código vale solo para el correo al que se envió
        lblInstruccion.setText(visible
                ? "Revisa tu correo: escribe el código que te enviamos y elige tu nueva contraseña."
                : "Escribe el correo de tu cuenta y te enviaremos un código de 6 dígitos.");
        if (!visible) {
            txtCodigo.clear();
            txtNueva.clear();
            txtConfirmar.clear();
        }
    }

    /** Al login, con el correo ya escrito. */
    private void volverAlLogin() {
        estado().setCorreoLogin(txtCorreo.getText());
        nav.ir("iniciar-sesion");
    }
}
