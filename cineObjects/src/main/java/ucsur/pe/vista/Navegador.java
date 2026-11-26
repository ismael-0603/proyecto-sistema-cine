package ucsur.pe.vista;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.HostServices;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;

/**
 * Por qué existe: el kiosco es una sola ventana que va cambiando de pantalla. El navegador carga cada FXML
 * (como App.start en MVCone, con setControllerFactory para pasarle a cada controller lo que necesita),
 * muestra las ventanas que aparecen encima (sinopsis, avisos) y coordina los pasos que cruzan pantallas:
 * empezar, cancelar o vencer una compra, el aviso de inactividad y el paso del empleado entre su panel
 * y su cuenta personal.
 */
public class Navegador {

    private static final int SEGUNDOS_INACTIVIDAD = 150; // sin actividad en compra o sesión, pregunta «¿Sigues ahí?»
    private static final int SEGUNDOS_AVISO = 30;        // margen antes de cancelar compra o cerrar sesión (3 min total: RNF-04)

    private final Stage stage;
    private final App app;
    private final HostServices host;
    private final EstadoKiosco estado = new EstadoKiosco();

    /** Correos simulados visibles a la vez; al llegar otro, se quita el más antiguo. */
    private static final int MAX_CORREOS_VISIBLES = 3;
    /** Lo que dura la pantalla «Cambiando a tu cuenta personal…» / «Accediendo a…». */
    private static final double SEGUNDOS_TRANSICION = 1.2;

    private final StackPane raiz = new StackPane();
    private final StackPane capaModal = new StackPane();
    private final VBox capaCorreos = new VBox(12);
    private final VBox capaTransicion = new VBox(18);
    private final Label lblTransicion = new Label();
    private final CambioDeCuenta cambioDeCuenta = new CambioDeCuenta(this);
    private final PauseTransition inactividad = new PauseTransition(Duration.seconds(SEGUNDOS_INACTIVIDAD));
    private Timeline cuentaAviso;
    private String pantallaActual;
    private String pantallaAnterior;

    public Navegador(Stage stage, App app, HostServices host) {
        this.stage = stage;
        this.app = app;
        this.host = host;
    }

    /** Arma la escena y muestra la primera pantalla: elegir la sede del kiosco. */
    public void empezar() {
        capaModal.getStyleClass().add("capa-modal");
        capaModal.setVisible(false);
        // Los correos simulados van encima de todo, abajo a la derecha; los clics pasan donde no hay tarjeta
        capaCorreos.setAlignment(Pos.BOTTOM_RIGHT);
        capaCorreos.setPadding(new Insets(24));
        capaCorreos.setPickOnBounds(false);
        // La transición entre cuentas va encima de todo y bloquea los clics mientras dura
        ProgressIndicator carga = new ProgressIndicator();
        lblTransicion.getStyleClass().add("transicion-texto");
        capaTransicion.getChildren().addAll(carga, lblTransicion);
        capaTransicion.setAlignment(Pos.CENTER);
        capaTransicion.getStyleClass().add("capa-transicion");
        capaTransicion.setVisible(false);
        raiz.getChildren().addAll(new Region(), capaModal, capaCorreos, capaTransicion);
        raiz.getStyleClass().add("fondo");

        Scene escena = new Scene(raiz, 1440, 900);
        escena.getStylesheets().add(App.class.getResource("css/estilos.css").toExternalForm());
        // Cualquier toque, tecla o desplazamiento reinicia el contador de inactividad
        for (var tipo : List.of(MouseEvent.MOUSE_PRESSED, KeyEvent.KEY_PRESSED,
                ScrollEvent.SCROLL, TouchEvent.TOUCH_PRESSED)) {
            escena.addEventFilter(tipo, this::reiniciarInactividad);
        }
        inactividad.setOnFinished(e -> avisarInactividad());
        stage.setScene(escena);
        ir("elegir-sede");
    }

    // ---------- Pantallas ----------

    /** Cambia a la pantalla del archivo view/{nombre}.fxml al instante (sin opacidad intermedia que difumine texto). */
    public void ir(String nombre) {
        cerrarModal();
        if ("recuperar".equals(pantallaActual) && !"recuperar".equals(nombre) && !"detalle-compra".equals(nombre)) {
            estado.limpiarRecuperacion();
        } else if ("detalle-compra".equals(pantallaActual) && !"recuperar".equals(nombre)
                && !"detalle-compra".equals(nombre) && !estado.getComprasRecuperadas().isEmpty()) {
            estado.limpiarRecuperacion();
        }
        if (pantallaActual != null && !pantallaActual.equals(nombre)
                && !esCambioPestanaCuenta(pantallaActual, nombre)) {
            pantallaAnterior = pantallaActual;
        }
        pantallaActual = nombre;
        raiz.getChildren().set(0, cargar(nombre).raiz);
    }

    /** Última pantalla distinta antes de la actual (usada en «Volver» de Mi cuenta del empleado). */
    public String pantallaAnterior() {
        return pantallaAnterior;
    }

    private static boolean esCambioPestanaCuenta(String de, String a) {
        return ("mi-cuenta".equals(de) && "mis-compras".equals(a))
                || ("mis-compras".equals(de) && "mi-cuenta".equals(a));
    }

    /** Muestra view/{nombre}.fxml encima de la pantalla actual y devuelve su controller para configurarlo. */
    public <T> T modal(String nombre) {
        Cargado cargado = cargar(nombre);
        mostrarModal(cargado.raiz);
        @SuppressWarnings("unchecked")
        T controller = (T) cargado.controller;
        return controller;
    }

    /** Muestra un diálogo genérico (título, texto, contenido opcional y acciones) encima de la pantalla actual. */
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
        capaModal.setOpacity(0);
        capaModal.setVisible(true);
        contenido.setTranslateY(12);

        FadeTransition fondo = new FadeTransition(Duration.millis(130), capaModal);
        fondo.setFromValue(0);
        fondo.setToValue(1);
        fondo.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition deslizar = new TranslateTransition(Duration.millis(150), contenido);
        deslizar.setFromY(12);
        deslizar.setToY(0);
        deslizar.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fondo, deslizar).play();
    }

    /** Muestra un momento la capa de transición con el texto indicado y luego ejecuta {@code despues}. */
    public void mostrarTransicion(String texto, Runnable despues) {
        cerrarModal();
        lblTransicion.setText(texto);
        capaTransicion.setVisible(true);
        PauseTransition pausa = new PauseTransition(Duration.seconds(SEGUNDOS_TRANSICION));
        pausa.setOnFinished(e -> {
            try {
                despues.run(); // la pantalla nueva se arma debajo y recién entonces se quita la capa
            } finally {
                capaTransicion.setVisible(false); // aunque falle, el kiosco no queda bloqueado
            }
        });
        pausa.play();
    }

    /** La impresora del kiosco se simula: muestra el ticket con su texto y sus códigos QR visuales. */
    public void mostrarTicket(String texto, DetalleCompra detalle) {
        Label cuerpo = new Label(texto);
        cuerpo.getStyleClass().add("ticket-texto");
        cuerpo.setWrapText(true);
        cuerpo.setMaxWidth(Double.MAX_VALUE);

        VBox papel = new VBox(12, cuerpo);
        papel.getStyleClass().add("qr-fondo");
        papel.setMaxWidth(Double.MAX_VALUE);

        List<String> codigos = (detalle != null && !detalle.getCodigosQR().isEmpty())
                ? detalle.getCodigosQR()
                : (detalle != null && detalle.getSerieNumero() != null ? List.of(detalle.getSerieNumero()) : List.of());

        if (!codigos.isEmpty()) {
            Region separador = new Region();
            separador.getStyleClass().add("separador-ticket");
            separador.setMaxWidth(Double.MAX_VALUE);

            FlowPane zonaQrs = new FlowPane(12, 12);
            zonaQrs.setAlignment(Pos.CENTER);
            double tamano = codigos.size() > 1 ? 116 : 150;
            List<String> butacas = detalle != null ? detalle.getButacas() : List.of();
            int indiceEntrada = 0;

            for (String codigo : codigos) {
                String etiqueta;
                if (codigo.startsWith("QR-PED-")) {
                    etiqueta = "Dulcería";
                } else if (codigo.startsWith("QR-ENT-")) {
                    etiqueta = indiceEntrada < butacas.size()
                            ? "Entrada · Butaca " + butacas.get(indiceEntrada)
                            : "Entrada";
                    indiceEntrada++;
                } else {
                    etiqueta = "Comprobante";
                }
                Label lblTipo = new Label(etiqueta);
                lblTipo.getStyleClass().add("etiqueta-fuerte");
                Label lblCod = new Label(codigo);
                lblCod.getStyleClass().add("nota");
                lblCod.setWrapText(true);
                lblCod.setMaxWidth(tamano + 40);
                lblCod.setAlignment(Pos.CENTER);

                VBox tarjetaQr = new VBox(6, lblTipo, CodigoVisual.dibujar(codigo, tamano), lblCod);
                tarjetaQr.setAlignment(Pos.CENTER);
                tarjetaQr.getStyleClass().add("qr-fondo");
                zonaQrs.getChildren().add(tarjetaQr);
            }
            papel.getChildren().addAll(separador, zonaQrs);
        }

        ScrollPane scroll = new ScrollPane(papel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMaxHeight(500);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        DialogoVista d = dialogo();
        d.configurar("Comprobante para imprimir", "Así sale el ticket en la impresora del kiosco.", false);
        d.agregarContenido(scroll);
        d.botonPrincipal("Listo", this::cerrarModal);
    }

    /**
     * El correo del kiosco se simula: cada correo "enviado" aparece como una tarjeta en la esquina, con el
     * destinatario, el asunto y el texto (por ejemplo, el código para cambiar la contraseña). Se cierra con ✕.
     */
    public void mostrarCorreo(String destinatario, String asunto, String cuerpo) {
        Label titulo = new Label("Correo simulado", Iconos.crear(Iconos.CORREO, 15, "icono-rojo"));
        titulo.setGraphicTextGap(8);
        titulo.getStyleClass().add("correo-titulo");
        Button cerrar = new Button("✕");
        cerrar.getStyleClass().add("correo-cerrar");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox cabecera = new HBox(8, titulo, espacio, cerrar);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label para = new Label("Para: " + destinatario);
        para.getStyleClass().add("nota");
        Label lblAsunto = new Label(asunto);
        lblAsunto.getStyleClass().add("etiqueta-fuerte");
        lblAsunto.setWrapText(true);
        Label lblCuerpo = new Label(cuerpo);
        lblCuerpo.getStyleClass().add("nota");
        lblCuerpo.setWrapText(true);

        VBox tarjeta = new VBox(6, cabecera, para, lblAsunto, lblCuerpo);
        tarjeta.getStyleClass().add("correo-simulado");
        tarjeta.setMaxWidth(400);
        tarjeta.setMaxHeight(Region.USE_PREF_SIZE);
        cerrar.setOnAction(e -> capaCorreos.getChildren().remove(tarjeta));

        if (capaCorreos.getChildren().size() >= MAX_CORREOS_VISIBLES) {
            capaCorreos.getChildren().remove(0);
        }
        tarjeta.setOpacity(0);
        tarjeta.setTranslateY(18);
        capaCorreos.getChildren().add(tarjeta);

        FadeTransition fade = new FadeTransition(Duration.millis(190), tarjeta);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);
        TranslateTransition slide = new TranslateTransition(Duration.millis(190), tarjeta);
        slide.setFromY(18);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();
    }

    /** Abre un enlace (por ejemplo, el tráiler) en el navegador web de la PC. */
    public void abrirEnlace(String url) {
        if (url != null && !url.isBlank()) host.showDocument(url);
    }

    private record Cargado(Parent raiz, Object controller) { }

    private Cargado cargar(String nombre) {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("view/" + nombre + ".fxml"));
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
        abandonarCompra();
        if (estado.getSesion() == null) {
            estado.limpiarInvitado();
        }
        ir("cartelera");
        if (estado.getSesion() != null) {
            reiniciarInactividad(null);
        }
    }

    /** Cancela la compra en curso, si la hay, sin cambiar de pantalla. */
    void abandonarCompra() {
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            app.compra().cancelar(estado.getCompra());
        }
        estado.terminarCompra();
        inactividad.stop();
    }

    /** Marca la compra como expirada en servicio/BD sin cambiar de pantalla. */
    private void expirarCompraEnCurso() {
        if (estado.hayCompra() && Compra.EN_PROCESO.equals(estado.getCompra().getEstado())) {
            app.compra().expirar(estado.getCompra());
        }
        estado.terminarCompra();
        inactividad.stop();
    }

    /** «← Volver» desde Asientos: se abandona la compra y se vuelve a elegir horario. */
    public void volverAHorarios() {
        abandonarCompra();
        ir("horario");
    }

    /** La compra pagada: se detiene el reloj de compra y se muestra el comprobante. */
    public void compraPagada() {
        estado.detenerCuenta();
        ir("comprobante");
        reiniciarInactividad(null); // si hay sesión abierta, sigue vigilando inactividad en el comprobante
    }

    /**
     * «Ir a comprar entradas» de los paneles: pasa a la cuenta personal del empleado, sin cerrar su sesión,
     * y lleva a la cartelera (se olvida un horario elegido antes).
     */
    public void irACuentaPersonal() {
        estado.setFuncion(null);
        cambioDeCuenta.aCuentaPersonal();
    }

    /** Desde «Horarios»: pasa a la cuenta personal y sigue con el horario ya elegido. */
    public void irACuentaPersonalConHorario() {
        cambioDeCuenta.aCuentaPersonal();
    }

    /** Desde la cuenta personal, vuelve al panel del empleado (pide su PIN). */
    public void irAPanel(String pantalla, String zona) {
        cambioDeCuenta.aPanel(pantalla, zona);
    }

    /** Arranca o reinicia el contador de inactividad cuando hay una sesión activa. */
    public void iniciarVigilanciaSesion() {
        reiniciarInactividad(null);
    }

    /** Cierra la sesión actual, limpia correos simulados/invitado/recuperación y vuelve al inicio seguro. */
    public void cerrarSesion() {
        inactividad.stop();
        abandonarCompra();
        estado.cerrarSesion();
        capaCorreos.getChildren().clear(); // la siguiente persona no ve los correos de la anterior
        ir(estado.getSede() == null ? "elegir-sede" : "cartelera");
    }

    /** Fin del recorrido: la siguiente persona empieza en el inicio de esta sede (o elegir sede), sin sesión. */
    public void volverAlInicio() {
        cerrarSesion();
    }

    /** Tras pagar, el usuario elige seguir comprando: termina la compra pero conserva su sesión abierta. */
    public void seguirComprando() {
        estado.terminarCompra();
        capaCorreos.getChildren().clear();
        ir("cartelera");
        reiniciarInactividad(null);
    }

    /** Se acabaron los 4 minutos: la compra expira y se avisa. */
    private void tiempoAgotado() {
        cerrarModal(); // si estaba abierto «¿Sigues ahí?», se detiene su cuenta para que no mande a la cartelera luego
        expirarCompraEnCurso();
        if (estado.getSesion() == null) {
            estado.limpiarInvitado();
        } else {
            reiniciarInactividad(null);
        }
        DialogoVista d = dialogo();
        d.configurar("Se acabó el tiempo",
                "Tenías " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos para completar la compra. Liberamos tus "
                        + "butacas para otros clientes y no se hizo ningún cobro.", true);
        d.botonSecundario("Volver a la cartelera", () -> ir("cartelera"));
        d.botonPrincipal("Elegir otra vez", () -> ir(estado.getPelicula() != null ? "horario" : "cartelera"));
    }

    // ---------- Inactividad (wireframe 27) ----------

    private boolean debeVigilarInactividad() {
        return estado.cuentaActiva() || estado.getSesion() != null
                || estado.hayCliente() || !estado.getComprasRecuperadas().isEmpty();
    }

    private void reiniciarInactividad(Event e) {
        if (debeVigilarInactividad() && cuentaAviso == null) {
            inactividad.playFromStart();
        } else if (!debeVigilarInactividad()) {
            inactividad.stop();
        }
    }

    private void avisarInactividad() {
        boolean enCompra = estado.cuentaActiva();
        if (!debeVigilarInactividad()) return;
        DialogoVista d = dialogo();
        d.configurar("¿Sigues ahí?",
                enCompra
                        ? "No detectamos actividad en la pantalla. Por seguridad, cancelaremos tu compra en:"
                        : estado.getSesion() != null
                                ? "No detectamos actividad en la pantalla. Por seguridad, cerraremos tu sesión en:"
                                : "No detectamos actividad en la pantalla. Por seguridad, limpiaremos tu sesión en:",
                false);
        Label cuenta = new Label();
        cuenta.getStyleClass().add("cuenta-regresiva");
        Label nota = new Label(enCompra
                ? "Si se cancela, se liberan tus butacas y no se cobra nada."
                : "Si no respondes, el kiosco volverá al inicio para proteger tu cuenta.");
        nota.getStyleClass().add("nota");
        d.agregarContenido(cuenta, nota);
        if (enCompra) {
            d.botonSecundario("Cancelar compra", this::cancelarCompra);
        } else {
            d.botonSecundario(estado.getSesion() != null ? "Cerrar sesión" : "Salir", this::volverAlInicio);
        }
        d.botonPrincipal("Sí, continuar", () -> {
            cerrarModal();
            inactividad.playFromStart();
        });

        int[] quedan = {SEGUNDOS_AVISO};
        cuenta.setText(String.format("00:%02d", quedan[0]));
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            quedan[0]--;
            cuenta.setText(String.format("00:%02d", Math.max(0, quedan[0])));
            if (quedan[0] <= 0) {
                if (enCompra) cancelarCompra();
                else volverAlInicio();
            }
        }));
        t.setCycleCount(SEGUNDOS_AVISO);
        cuentaAviso = t; // se asigna después de dialogo(): cerrarModal() la detiene al responder
        t.play();
    }

    // ---------- Acceso para las pantallas ----------

    public App app() { return app; }

    public EstadoKiosco estado() { return estado; }
}
