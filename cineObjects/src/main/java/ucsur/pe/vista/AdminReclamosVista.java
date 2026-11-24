package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Reclamo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 21 · Reclamos: el administrador los revisa y los cierra respondiendo o rechazando con un motivo.
 * Registrado → (Iniciar revisión) → En revisión → Respondido o Rechazado.
 */
public class AdminReclamosVista extends PantallaAdmin {

    private enum Filtro { TODOS, RECIBIDOS, EN_REVISION, CERRADOS }

    @FXML private HBox filtros;
    @FXML private Label lblMensaje;
    @FXML private VBox lista;
    @FXML private Label lblTitulo;
    @FXML private Label lblEstado;
    @FXML private Label lblCliente;
    @FXML private Label lblCompra;
    @FXML private Label lblFecha;
    @FXML private Label lblDescripcion;
    @FXML private TextArea txtRespuesta;
    @FXML private Label lblContadorRespuesta;
    @FXML private Label lblMensajeDetalle;
    @FXML private Button btnRevision;
    @FXML private Button btnRechazar;
    @FXML private Button btnResponder;

    private Filtro filtro = Filtro.TODOS;
    private final List<Button> botonesFiltro = new ArrayList<>();
    private Reclamo elegido;

    public AdminReclamosVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("reclamos")) return;
        txtRespuesta.textProperty().addListener((o, a, texto) -> lblContadorRespuesta.setText(
                (texto == null ? 0 : texto.length()) + " de " + Reclamo.MAX_RESPUESTA + " caracteres"));
        agregarFiltro("Todos", Filtro.TODOS);
        agregarFiltro("Recibidos", Filtro.RECIBIDOS);
        agregarFiltro("En revisión", Filtro.EN_REVISION);
        agregarFiltro("Cerrados", Filtro.CERRADOS);
        botonesFiltro.get(0).getStyleClass().add("activo");
        mostrarDetalle(null);
        cargar();
    }

    private void agregarFiltro(String texto, Filtro f) {
        Button b = new Button(texto);
        b.getStyleClass().add("chip");
        b.setOnAction(e -> {
            filtro = f;
            botonesFiltro.forEach(x -> x.getStyleClass().remove("activo"));
            b.getStyleClass().add("activo");
            cargar();
        });
        botonesFiltro.add(b);
        filtros.getChildren().add(b);
    }

    private void cargar() {
        Respuesta<List<Reclamo>> r = app().postventa().listarReclamos(admin, null);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        List<Reclamo> visibles = new ArrayList<>(r.dato().stream().filter(this::pasaFiltro).toList());
        visibles.sort(Comparator.comparingInt(Reclamo::getIdReclamo).reversed()); // el más reciente arriba
        lista.getChildren().clear();
        for (Reclamo rec : visibles) lista.getChildren().add(item(rec));
        if (visibles.isEmpty()) {
            Label vacio = new Label("No hay reclamos en este grupo.");
            vacio.getStyleClass().add("nota-clara");
            lista.getChildren().add(vacio);
        }
        if (elegido != null && visibles.contains(elegido)) mostrarDetalle(elegido);
        else mostrarDetalle(visibles.isEmpty() ? null : visibles.get(0));
    }

    private boolean pasaFiltro(Reclamo r) {
        return switch (filtro) {
            case TODOS -> true;
            case RECIBIDOS -> Reclamo.REGISTRADO.equals(r.getEstado());
            case EN_REVISION -> Reclamo.EN_REVISION.equals(r.getEstado());
            case CERRADOS -> Reclamo.RESUELTO.equals(r.getEstado()) || Reclamo.RECHAZADO.equals(r.getEstado());
        };
    }

    private VBox item(Reclamo r) {
        Label titulo = new Label(numero(r) + " · " + Formato.tipoReclamo(r.getTipo()));
        titulo.getStyleClass().add("etiqueta-fuerte");
        Label cliente = new Label("Cliente: " + nombreCliente(r) + " · "
                + (r.getFecha() == null ? "" : Formato.fechaHoraCorta(r.getFecha())));
        cliente.getStyleClass().add("nota");
        Label est = Tabla.chip(estadoTexto(r), estiloEstado(r));
        VBox v = new VBox(6, titulo, cliente, new HBox(est));
        v.getStyleClass().add("reclamo-item");
        if (r == elegido) v.getStyleClass().add("activo");
        v.setOnMouseClicked(e -> {
            mostrarDetalle(r);
            cargar();
        });
        return v;
    }

    private void mostrarDetalle(Reclamo r) {
        elegido = r;
        limpiar(lblMensajeDetalle);
        boolean hay = r != null;
        lblEstado.setVisible(hay);
        if (!hay) {
            lblTitulo.setText("Elige un reclamo");
            for (Label l : new Label[] {lblCliente, lblCompra, lblFecha, lblDescripcion}) l.setText("—");
            txtRespuesta.clear();
            for (Button b : new Button[] {btnRevision, btnRechazar, btnResponder}) b.setDisable(true);
            txtRespuesta.setDisable(true);
            return;
        }
        lblTitulo.setText(numero(r) + " · " + Formato.tipoReclamo(r.getTipo()));
        lblEstado.setText(estadoTexto(r));
        lblEstado.getStyleClass().setAll("label", "estado-chip");
        if (estiloEstado(r) != null) lblEstado.getStyleClass().add(estiloEstado(r));
        Cliente c = r.getCompra().getCliente();
        lblCliente.setText(nombreCliente(r) + (c == null || c.getDni() == null ? "" : " · DNI " + c.getDni()));
        Compra compra = r.getCompra();
        Funcion f = compra.getEntradas().isEmpty() ? null : compra.getEntradas().get(0).getFuncion();
        lblCompra.setText(compra.getSerieNumero() + (f == null ? "" : " · " + f.getPelicula().getTitulo()
                + " · " + Formato.fechaHoraCorta(f.obtenerFechaHoraInicio())));
        lblFecha.setText(r.getFecha() == null ? "—" : Formato.fechaHora(r.getFecha()));
        lblDescripcion.setText(r.getDescripcion());
        boolean registrado = Reclamo.REGISTRADO.equals(r.getEstado());
        boolean enRevision = Reclamo.EN_REVISION.equals(r.getEstado());
        boolean cerrado = !registrado && !enRevision;
        txtRespuesta.setText(cerrado && r.getRespuesta() != null ? r.getRespuesta() : "");
        txtRespuesta.setDisable(!enRevision);
        btnRevision.setDisable(!registrado);
        btnRechazar.setDisable(!enRevision);
        btnResponder.setDisable(!enRevision);
    }

    @FXML
    private void onIniciarRevision() {
        despues(app().postventa().iniciarRevision(admin, elegido));
    }

    @FXML
    private void onResponder() {
        despues(app().postventa().responderReclamo(admin, elegido, txtRespuesta.getText()));
    }

    @FXML
    private void onRechazar() {
        despues(app().postventa().rechazarReclamo(admin, elegido, txtRespuesta.getText()));
    }

    private void despues(Respuesta<Void> r) {
        if (!r.ok()) {
            mostrarError(lblMensajeDetalle, r.mensaje());
            return;
        }
        mostrarOk(lblMensaje, numero(elegido) + ": " + r.mensaje().toLowerCase());
        cargar();
    }

    private static String numero(Reclamo r) {
        return "#R-" + String.format("%04d", r.getIdReclamo());
    }

    private static String nombreCliente(Reclamo r) {
        Cliente c = r.getCompra().getCliente();
        if (c == null) return "—";
        return c.esInvitado() ? "Invitado (" + c.getCorreo() + ")" : c.getNombreCompleto();
    }

    /** El administrador ve «Recibido» donde el cliente ve «Registrado». */
    private static String estadoTexto(Reclamo r) {
        return Reclamo.REGISTRADO.equals(r.getEstado()) ? "Recibido" : Formato.estadoReclamo(r.getEstado());
    }

    private static String estiloEstado(Reclamo r) {
        return switch (r.getEstado()) {
            case Reclamo.EN_REVISION -> "respondido";
            case Reclamo.RESUELTO -> "pagada";
            case Reclamo.RECHAZADO -> "agotado";
            default -> null;
        };
    }
}
