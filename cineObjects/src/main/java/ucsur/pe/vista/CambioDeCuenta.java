package ucsur.pe.vista;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.LoginController;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Sede;
import ucsur.pe.service.CuentaPersonal;
import ucsur.pe.service.Sesion;

/**
 * Por qué existe: el paso del empleado entre su panel y su cuenta personal se dispara desde varias pantallas
 * (encabezado, horarios y menú admin). Aquí van sus diálogos: vincular/crear cuenta personal y pedir PIN al volver.
 */
final class CambioDeCuenta {

    private final Navegador nav;

    CambioDeCuenta(Navegador nav) {
        this.nav = nav;
    }

    private LoginController login() { return nav.app().login(); }

    private Sesion sesion() { return nav.estado().getSesion(); }

    // ---------- Del panel a la cuenta personal ----------

    /** «Ir a comprar entradas»: entra con la cuenta personal, o la vincula si es la primera vez. */
    void aCuentaPersonal() {
        Sesion sesion = sesion();
        if (sesion == null || !sesion.esEmpleado()) return;
        if (sesion.enModoCliente()) {
            nav.ir("cartelera");
            return;
        }
        Respuesta<CuentaPersonal> r = login().consultarCuentaPersonal(sesion.empleado());
        if (!r.ok()) {
            avisar("No se pudo abrir tu cuenta personal", r.mensaje());
            return;
        }
        switch (r.dato().estado()) {
            case VINCULADA -> {
                Respuesta<Void> cambio = login().pasarAModoCliente(sesion);
                if (cambio.ok()) entrarComoCliente();
                else avisar("No se pudo abrir tu cuenta personal", cambio.mensaje());
            }
            case POR_CONFIRMAR -> pedirContrasenaExistente(r.dato().correoEnmascarado());
            case NUEVA -> pedirCuentaNueva();
        }
    }

    /** Ya hay un cliente con su DNI: se vincula con la contraseña de esa cuenta. */
    private void pedirContrasenaExistente(String correoEnmascarado) {
        PasswordField contra = new PasswordField();
        Label mensaje = mensaje();
        DialogoVista d = nav.dialogo();
        d.configurar("Vincular tu cuenta personal",
                "Ya tienes una cuenta de cliente con tu DNI (" + correoEnmascarado + "). Escribe su contraseña "
                        + "para usarla aquí; solo se pide esta vez.", false);
        d.agregarContenido(campo("Contraseña de tu cuenta personal", contra), mensaje);
        Runnable vincular = () -> {
            Respuesta<Void> r = login().vincularCuentaExistente(sesion(), contra.getText());
            if (r.ok()) entrarComoCliente();
            else error(mensaje, r.mensaje());
        };
        contra.setOnAction(e -> vincular.run());
        d.botonSecundario("Cancelar", nav::cerrarModal);
        d.botonPrincipal("Vincular", vincular);
        contra.requestFocus();
    }

    /** Primera vez: crea su cuenta personal con un correo y una contraseña propios (no los de trabajo). */
    private void pedirCuentaNueva() {
        TextField correo = new TextField();
        correo.setPromptText("tucorreo@gmail.com");
        PasswordField contra = new PasswordField();
        PasswordField confirmar = new PasswordField();
        Label mensaje = mensaje();
        DialogoVista d = nav.dialogo();
        d.configurar("Crea tu cuenta personal",
                "Para comprar entradas y acumular puntos usa una cuenta personal, con tu correo propio. "
                        + "Tus nombres y DNI se toman de tu registro de empleado.", false);
        d.agregarContenido(campo("Correo personal", correo), campo("Contraseña (mínimo 8 caracteres)", contra),
                campo("Confirmar contraseña", confirmar), mensaje);
        // Validación en vivo con las reglas de Persona, como en «Crear cuenta»
        ValidacionEnVivo validacion = new ValidacionEnVivo(mensaje)
                .campo(correo, () -> Persona.errorCorreo(correo.getText()))
                .campo(contra, () -> Persona.errorContrasena(contra.getText()))
                .campo(confirmar, () -> confirmar.getText().isEmpty() ? "Confirma tu contraseña"
                        : confirmar.getText().equals(contra.getText()) ? null : "Las contraseñas no coinciden")
                .relacionar(contra, confirmar);
        Runnable crear = () -> {
            if (!validacion.validarTodos()) return;
            Respuesta<Void> r = login().crearCuentaPersonal(sesion(), correo.getText(), contra.getText(),
                    confirmar.getText());
            if (r.ok()) {
                entrarComoCliente();
                return;
            }
            error(mensaje, r.mensaje());
            if (r.mensaje() != null && r.mensaje().toLowerCase().contains("correo")) validacion.marcar(correo, r.mensaje());
        };
        confirmar.setOnAction(e -> crear.run());
        d.botonSecundario("Cancelar", nav::cerrarModal);
        d.botonPrincipal("Crear y continuar", crear);
        correo.requestFocus();
    }

    /** Transición y luego la cartelera de su sede (o elegir sede si su sede no está operativa). */
    private void entrarComoCliente() {
        EstadoKiosco estado = nav.estado();
        Sede sedeEmpleado = sesion().empleado().getSede();
        if (estado.getSede() == null && sedeEmpleado != null && sedeEmpleado.estaOperativa()) {
            estado.setSede(sedeEmpleado);
        }
        nav.mostrarTransicion("Cambiando a tu cuenta personal…", () -> {
            if (estado.getSede() == null || !estado.getSede().estaOperativa()) {
                estado.setSede(null);
                estado.setFuncion(null);
                nav.ir("elegir-sede");
                return;
            }
            Respuesta<Compra> compra = nav.continuarTrasIdentificarse();
            if (compra != null && !compra.ok()) avisar("No se pudo empezar la compra", compra.mensaje());
        });
    }

    // ---------- De la cuenta personal al panel ----------

    /** «Panel de dulcería» o «Administración» en modo cliente: pide el PIN (y si hay compra, la cancela tras validar). */
    void aPanel(String pantalla, String zona) {
        EstadoKiosco estado = nav.estado();
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            DialogoVista d = nav.dialogo();
            d.configurar("Tienes una compra en curso",
                    "Si vuelves a " + zona + " se cancela la compra y se liberan tus butacas.", false);
            d.botonSecundario("Seguir comprando", nav::cerrarModal);
            d.botonPrincipal("Cancelar compra", () -> pedirPin(pantalla, zona, true));
            return;
        }
        pedirPin(pantalla, zona, false);
    }

    private void pedirPin(String pantalla, String zona, boolean cancelarCompraAlEntrar) {
        PasswordField pin = new PasswordField();
        pin.setPromptText("••••");
        pin.getStyleClass().add("campo-pin");
        pin.setMaxWidth(160);
        // Solo dígitos y como máximo 4
        pin.setTextFormatter(new TextFormatter<String>(c -> c.getControlNewText().matches("\\d{0,4}") ? c : null));
        Label mensaje = mensaje();
        DialogoVista d = nav.dialogo();
        d.configurar("Acceder a " + zona, "Escribe tu PIN de 4 dígitos para volver a tu panel.", false);
        d.agregarContenido(pin, mensaje);
        Runnable entrar = () -> {
            String errorFormato = Empleado.errorPin(pin.getText());
            if (errorFormato != null) {
                error(mensaje, errorFormato);
                pin.requestFocus();
                return;
            }
            Sesion sesion = sesion();
            Respuesta<Void> r = login().volverAModoEmpleado(sesion, pin.getText());
            if (r.ok()) {
                if (cancelarCompraAlEntrar) {
                    nav.abandonarCompra();
                }
                nav.mostrarTransicion("Accediendo a " + zona + "…", () -> nav.ir(pantalla));
            } else if (sesion.cerradaPorSeguridad()) {
                cerrarPorSeguridad(r.mensaje());
            } else {
                error(mensaje, r.mensaje());
                pin.clear();
                pin.requestFocus();
            }
        };
        pin.setOnAction(e -> entrar.run());
        d.botonSecundario("Cancelar", nav::cerrarModal); // cancelar no cuenta como intento fallido
        d.botonPrincipal("Entrar", entrar);
        pin.requestFocus();
    }

    private void cerrarPorSeguridad(String motivo) {
        nav.estado().cerrarSesion();
        DialogoVista d = nav.dialogo();
        d.configurar("Sesión cerrada", motivo + ". Vuelve a iniciar sesión con tu correo y contraseña.", true);
        d.botonPrincipal("Entendido", nav::volverAlInicio); // también limpia los correos simulados
    }

    // ---------- Piezas de los diálogos ----------

    private void avisar(String titulo, String texto) {
        DialogoVista d = nav.dialogo();
        d.configurar(titulo, texto, true);
        d.botonPrincipal("Entendido", nav::cerrarModal);
    }

    private static Node campo(String etiqueta, TextField campo) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("etiqueta-campo");
        campo.setMaxWidth(Double.MAX_VALUE);
        VBox caja = new VBox(4, lbl, campo);
        caja.setFillWidth(true);
        return caja;
    }

    private static Label mensaje() {
        Label mensaje = new Label();
        mensaje.getStyleClass().addAll("mensaje", "error");
        mensaje.setWrapText(true);
        mensaje.setVisible(false);
        mensaje.setManaged(false);
        return mensaje;
    }

    private static void error(Label mensaje, String texto) {
        mensaje.setText(texto);
        mensaje.setVisible(true);
        mensaje.setManaged(true);
    }
}
