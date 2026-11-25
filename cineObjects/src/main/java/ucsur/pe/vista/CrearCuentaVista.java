package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Persona;
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
    @FXML private TextField txtContraVisible;
    @FXML private ToggleButton btnVerContra;
    @FXML private PasswordField txtConfirmar;
    @FXML private TextField txtConfirmarVisible;
    @FXML private ToggleButton btnVerConfirmar;
    @FXML private CheckBox chkTerminos;
    @FXML private Label lblMensaje;

    public CrearCuentaVista(Navegador nav) {
        super(nav);
    }

    private ValidacionEnVivo validacion;

    @FXML
    private void initialize() {
        encabezadoController.soloVolver("Volver", () -> nav.ir("iniciar-sesion"));
        configurarOjo(txtContra, txtContraVisible, btnVerContra);
        configurarOjo(txtConfirmar, txtConfirmarVisible, btnVerConfirmar);
        txtFechaNac.setTextFormatter(new TextFormatter<String>(CrearCuentaVista::mascaraFecha));
        // Cada campo se pone en rojo al salir de él si su dato no cumple la regla del modelo
        validacion = validacion(lblMensaje)
                .campo(txtDni, () -> Persona.errorDni(txtDni.getText()))
                .campo(txtFechaNac, this::errorFecha)
                .campo(txtNombres, () -> Persona.errorNombres(txtNombres.getText()))
                .campo(txtApellidos, () -> Persona.errorApellidos(txtApellidos.getText()))
                .campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()))
                .campo(txtTelefono, () -> Persona.errorTelefono(txtTelefono.getText()))
                .campo(txtContra, () -> Persona.errorContrasena(txtContra.getText()), txtContraVisible)
                .campo(txtConfirmar, this::errorConfirmar, txtConfirmarVisible)
                .relacionar(txtContra, txtConfirmar)
                .campo(chkTerminos, () -> chkTerminos.isSelected() ? null
                        : "Debes aceptar los términos y el tratamiento de tus datos para crear la cuenta.");
    }

    private static void configurarOjo(PasswordField oculto, TextField visible, ToggleButton boton) {
        visible.textProperty().bindBidirectional(oculto.textProperty());
        visible.visibleProperty().bind(boton.selectedProperty());
        oculto.visibleProperty().bind(boton.selectedProperty().not());
        boton.setText(null);
        boton.setGraphic(Iconos.crear(Iconos.OJO, 18, "icono-suave"));
        boton.selectedProperty().addListener((o, antes, mostrar) ->
                boton.setGraphic(Iconos.crear(mostrar ? Iconos.OJO_OCULTO : Iconos.OJO, 18, "icono-suave")));
    }

    /** dd/mm/aaaa completa, fecha que exista y edad entre 15 y 110 años. */
    private String errorFecha() {
        String texto = txtFechaNac.getText();
        if (texto == null || texto.isBlank()) return "La fecha de nacimiento es obligatoria";
        try {
            return Persona.errorFechaNacimiento(Formato.leerFecha(texto, "Fecha de nacimiento"), DateTime.ahora());
        } catch (IllegalArgumentException e) { // formato incompleto o día que no existe (31/02)
            return e.getMessage();
        }
    }

    private String errorConfirmar() {
        String confirmar = txtConfirmar.getText();
        if (confirmar == null || confirmar.isEmpty()) return "Confirma tu contraseña";
        return confirmar.equals(txtContra.getText()) ? null : "Las contraseñas no coinciden";
    }

    /**
     * Máscara dd/mm/aaaa: solo deja escribir números (máximo 8) y coloca las barras solas.
     * Al escribir, "15" pasa a "15/" y "1505" a "15/05/"; al borrar no se vuelve a poner la barra,
     * así se puede retroceder sin quedar atascado en ella.
     */
    private static TextFormatter.Change mascaraFecha(TextFormatter.Change c) {
        String digitos = c.getControlNewText().replaceAll("\\D", "");
        if (digitos.length() > 8) return null;
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i == 2 || i == 4) f.append('/');
            f.append(digitos.charAt(i));
        }
        boolean escribiendo = !c.getText().isEmpty();
        if (escribiendo && (digitos.length() == 2 || digitos.length() == 4)) f.append('/');
        String nuevo = f.toString();
        c.setRange(0, c.getControlText().length());
        c.setText(nuevo);
        c.setCaretPosition(nuevo.length());
        c.setAnchor(nuevo.length());
        return c;
    }

    @FXML
    private void onCrear() {
        if (!validacion.validarTodos()) return; // el mensaje y los campos en rojo ya los puso la validación

        DateTime fechaNac = Formato.leerFecha(txtFechaNac.getText(), "Fecha de nacimiento");
        Respuesta<Cliente> r = app().login().registrarCliente(txtDni.getText(), txtNombres.getText(),
                txtApellidos.getText(), fechaNac, txtCorreo.getText(), txtTelefono.getText(),
                txtContra.getText(), txtConfirmar.getText());
        if (!mostrar(lblMensaje, r)) {
            validacion.marcar(campoDelError(r.mensaje()), r.mensaje()); // p. ej. DNI o correo ya registrado
            return;
        }

        // Con la cuenta creada se inicia sesión con los mismos datos
        Respuesta<Sesion> sesion = app().login().iniciarSesion(txtCorreo.getText(), txtContra.getText());
        if (!mostrar(lblMensaje, sesion)) return;
        estado().iniciarSesion(sesion.dato());
        Respuesta<Compra> compra = nav.continuarTrasIdentificarse();
        if (compra != null && !compra.ok()) mostrarError(lblMensaje, compra.mensaje());
    }

    /**
     * Los errores que solo sabe el servicio (DNI o correo ya registrados) llegan como un mensaje;
     * según de qué habla se sabe qué campo marcar. Los de formato ya los atrapa la validación en vivo.
     */
    private Control campoDelError(String mensaje) {
        if (mensaje == null) return null;
        String m = mensaje.toLowerCase();
        if (m.contains("no coinciden")) return txtConfirmar;
        if (m.contains("contraseña")) return txtContra;
        if (m.contains("dni")) return txtDni;
        if (m.contains("nombres")) return txtNombres;
        if (m.contains("apellidos")) return txtApellidos;
        if (m.contains("nacimiento") || m.contains("años")) return txtFechaNac;
        if (m.contains("correo")) return txtCorreo;
        if (m.contains("celular") || m.contains("teléfono")) return txtTelefono;
        return null;
    }

    @FXML
    private void onIniciarSesion() {
        nav.ir("iniciar-sesion");
    }
}
