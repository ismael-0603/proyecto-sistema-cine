package ucsur.pe.vista;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import ucsur.pe.App;
import ucsur.pe.Principal;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Por qué existe: el kiosco es una sola ventana que va cambiando de pantalla. El navegador carga cada FXML
 * (como App.start en MVCone, con setControllerFactory para pasarle a cada controller lo que necesita),
 * muestra las ventanas que aparecen encima (sinopsis, avisos) y coordina los pasos que cruzan pantallas:
 * empezar, cancelar o vencer una compra y el aviso de inactividad.
 */
public class Navegador {

    /** Sin tocar la pantalla este tiempo durante una compra, se pregunta «¿Sigues ahí?». */
    private static final int SEGUNDOS_INACTIVIDAD = 90;
    /** Tiempo para responder al aviso antes de cancelar la compra. */
    private static final int SEGUNDOS_AVISO = 30;

    private final Stage stage;
    private final App app;
    private final HostServices host;
    private final EstadoKiosco estado = new EstadoKiosco();

    private final StackPane raiz = new StackPane();
    private final StackPane capaModal = new StackPane();
    private final PauseTransition inactividad = new PauseTransition(Duration.seconds(SEGUNDOS_INACTIVIDAD));
    private Timeline cuentaAviso;

    public Navegador(Stage stage, App app, HostServices host) {
        this.stage = stage;
        this.app = app;
        this.host = host;
    }

    /** Arma la escena y muestra la primera pantalla: elegir la sede del kiosco. */
    public void empezar() {
        capaModal.getStyleClass().add("capa-modal");
        capaModal.setVisible(false);
        raiz.getChildren().addAll(new Region(), capaModal);
        raiz.getStyleClass().add("fondo");

        Scene escena = new Scene(raiz, 1440, 900);
        escena.getStylesheets().add(Principal.class.getResource("css/estilos.css").toExternalForm());
        // Cualquier toque, tecla o desplazamiento reinicia el contador de inactividad
        for (var tipo : java.util.List.of(MouseEvent.MOUSE_PRESSED, KeyEvent.KEY_PRESSED,
                ScrollEvent.SCROLL, TouchEvent.TOUCH_PRESSED)) {
            escena.addEventFilter(tipo, this::reiniciarInactividad);
        }
        inactividad.setOnFinished(e -> avisarInactividad());
        stage.setScene(escena);
        ir("elegir-sede");
    }

    // ---------- Pantallas ----------

    /** Cambia a la pantalla del archivo view/{nombre}.fxml. */
    public void ir(String nombre) {
        cerrarModal();
        raiz.getChildren().set(0, cargar(nombre).raiz);
    }

    /** Muestra view/{nombre}.fxml encima de la pantalla actual y devuelve su controller para configurarlo. */
    public <T> T modal(String nombre) {
        Cargado cargado = cargar(nombre);
        mostrarModal(cargado.raiz);
        @SuppressWarnings("unchecked")
        T controller = (T) cargado.controller;
        return controller;
    }

    /** Muestra un aviso genérico (título, texto y hasta dos botones) encima de la pantalla actual. */
    public DialogoVista dialogo() {
        return modal("dialogo");
    }

    public void cerrarModal() {
        if (cuentaAviso != null) {
            cuentaAviso.stop();
            cuentaAviso = null;
        }
        capaModal.getChildren().clear();
        capaModal.setVisible(false);
    }

    private void mostrarModal(Node contenido) {
        capaModal.getChildren().setAll(contenido);
        capaModal.setVisible(true);
    }

    /** Aviso para las secciones de las fases siguientes (promociones, recuperar entradas...). */
    public void proximamente(String seccion) {
        DialogoVista d = dialogo();
        d.configurar(seccion, "Esta sección se agrega en la siguiente fase de la interfaz.", false);
        d.botonPrincipal("Entendido", this::cerrarModal);
    }

    /** La impresora del kiosco se simula: muestra el ticket que se imprimiría. */
    public void mostrarTicket(String texto) {
        javafx.scene.control.TextArea ticket = new javafx.scene.control.TextArea(texto);
        ticket.setEditable(false);
        ticket.setPrefRowCount(18);
        ticket.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        DialogoVista d = dialogo();
        d.configurar("Comprobante para imprimir", "Así sale el ticket en la impresora del kiosco.", false);
        d.agregarContenido(ticket);
        d.botonPrincipal("Listo", this::cerrarModal);
    }

    /** Abre un enlace (por ejemplo, el tráiler) en el navegador web de la PC. */
    public void abrirEnlace(String url) {
        if (url != null && !url.isBlank()) host.showDocument(url);
    }

    private record Cargado(Parent raiz, Object controller) { }

    private Cargado cargar(String nombre) {
        FXMLLoader loader = new FXMLLoader(Principal.class.getResource("view/" + nombre + ".fxml"));
        // Cada controller de pantalla recibe el navegador por su constructor (inyección, como en MVCone)
        loader.setControllerFactory(this::crearController);
        try {
            Parent vista = loader.load();
            return new Cargado(vista, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar la pantalla " + nombre, e);
        }
    }

    private Object crearController(Class<?> clase) {
        try {
            return clase.getConstructor(Navegador.class).newInstance(this);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear el controller " + clase.getSimpleName(), e);
        }
    }

    // ---------- Flujo de compra ----------

    /**
     * Con sede, función y cliente ya elegidos, empieza la compra (4 minutos) y abre «Asientos».
     * Si el servicio la rechaza, devuelve la respuesta con el motivo para mostrarlo.
     */
    public Respuesta<Compra> iniciarCompra() {
        Respuesta<Compra> r = app.compra().iniciarCompra(estado.cliente(), estado.getSede());
        if (r.ok()) {
            estado.empezarCompra(r.dato(), this::tiempoAgotado);
            reiniciarInactividad(null);
            ir("asientos");
        }
        return r;
    }

    /**
     * Después de iniciar sesión, registrarse o entrar como invitado: si ya había elegido un horario, empieza
     * la compra; si no, vuelve a la cartelera. Devuelve la respuesta de iniciarCompra (o null si no aplica).
     */
    public Respuesta<Compra> continuarTrasIdentificarse() {
        if (estado.getFuncion() != null && !estado.hayCompra()) {
            return iniciarCompra();
        }
        ir("cartelera");
        return null;
    }

    /** Cancela la compra en curso (libera butacas y dulcería) y vuelve a la cartelera. */
    public void cancelarCompra() {
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            app.compra().cancelar(estado.getCompra());
        }
        estado.terminarCompra();
        ir("cartelera");
    }

    /** «← Volver» desde Asientos: se abandona la compra y se vuelve a elegir horario. */
    public void volverAHorarios() {
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            app.compra().cancelar(estado.getCompra());
        }
        estado.terminarCompra();
        inactividad.stop();
        ir("horario");
    }

    /** La compra pagada: se detiene el reloj y se muestra el comprobante. */
    public void compraPagada() {
        estado.detenerCuenta();
        inactividad.stop();
        ir("comprobante");
    }

    /** «Ir a comprar entradas» de los paneles: la cartelera de la sede del empleado, sin cerrar su sesión. */
    public void irACarteleraComoEmpleado() {
        var sesion = estado.getSesion();
        if (estado.getSede() == null && sesion != null && sesion.empleado() != null) {
            estado.setSede(sesion.empleado().getSede());
        }
        ir(estado.getSede() == null ? "elegir-sede" : "cartelera");
    }

    /** Fin del recorrido: la siguiente persona empieza en la cartelera de esta sede, sin sesión. */
    public void volverAlInicio() {
        estado.terminarCompra();
        estado.cerrarSesion();
        ir("cartelera");
    }

    /** Se acabaron los 4 minutos: la compra se cancela (si el modelo no la venció ya) y se avisa. */
    private void tiempoAgotado() {
        cerrarModal(); // si estaba abierto «¿Sigues ahí?», se detiene su cuenta para que no mande a la cartelera luego
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            app.compra().cancelar(estado.getCompra()); // si ya expiró en el modelo, el mensaje de error se ignora
        }
        estado.terminarCompra();
        inactividad.stop();
        DialogoVista d = dialogo();
        d.configurar("Se acabó el tiempo",
                "Tenías " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos para completar la compra. Liberamos tus "
                        + "butacas para otros clientes y no se hizo ningún cobro.", true);
        d.botonSecundario("Volver a la cartelera", () -> ir("cartelera"));
        d.botonPrincipal("Elegir otra vez", () -> ir(estado.getPelicula() != null ? "horario" : "cartelera"));
    }

    // ---------- Inactividad (wireframe 27) ----------

    private void reiniciarInactividad(Event e) {
        if (estado.cuentaActiva() && cuentaAviso == null) {
            inactividad.playFromStart();
        } else if (!estado.cuentaActiva()) {
            inactividad.stop();
        }
    }

    private void avisarInactividad() {
        if (!estado.cuentaActiva()) return;
        DialogoVista d = dialogo();
        d.configurar("¿Sigues ahí?",
                "No detectamos actividad en la pantalla. Por seguridad, cancelaremos tu compra en:", false);
        Label cuenta = new Label();
        cuenta.getStyleClass().add("cuenta-regresiva");
        Label nota = new Label("Si se cancela, se liberan tus butacas y no se cobra nada.");
        nota.getStyleClass().add("nota");
        d.agregarContenido(cuenta, nota);
        d.botonSecundario("Cancelar compra", this::cancelarCompra);
        d.botonPrincipal("Sí, continuar", () -> {
            cerrarModal();
            inactividad.playFromStart();
        });

        int[] quedan = {SEGUNDOS_AVISO};
        cuenta.setText(String.format("00:%02d", quedan[0]));
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            quedan[0]--;
            cuenta.setText(String.format("00:%02d", Math.max(0, quedan[0])));
            if (quedan[0] <= 0) cancelarCompra();
        }));
        t.setCycleCount(SEGUNDOS_AVISO);
        cuentaAviso = t; // se asigna después de dialogo(): cerrarModal() la detiene al responder
        t.play();
    }

    // ---------- Acceso para las pantallas ----------

    public App app() { return app; }

    public EstadoKiosco estado() { return estado; }
}
