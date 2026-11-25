package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 22 · Elegir sede: la cartelera, los horarios y las butacas dependen de la sede elegida. */
public class ElegirSedeVista extends Pantalla {

    private enum Filtro { TODAS, LIMA, PROVINCIAS, ABIERTAS }

    @FXML private EncabezadoVista encabezadoController;
    @FXML private HBox barraVolver;
    @FXML private TextField txtBuscar;
    @FXML private Button btnLimpiar;
    @FXML private HBox filtros;
    @FXML private FlowPane sedes;
    @FXML private Label lblMensaje;

    private List<Sede> todas = new ArrayList<>();
    private final List<Button> botonesFiltro = new ArrayList<>();
    private Filtro filtroActivo = Filtro.TODAS;
    private boolean navegando;

    public ElegirSedeVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.sinSede();
        boolean haySedeActual = estado().getSede() != null;
        barraVolver.setVisible(haySedeActual);
        barraVolver.setManaged(haySedeActual);

        Respuesta<List<Sede>> r = app().cartelera().listarSedes();
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        Sede actual = estado().getSede();
        todas = new ArrayList<>(r.dato());
        // Primero la sede actual (si se está cambiando), luego las abiertas y al final las cerradas/mantenimiento
        todas.sort(Comparator
                .comparing((Sede s) -> !s.equals(actual))
                .thenComparing(s -> !s.estaOperativa()));

        agregarFiltro("Todas (" + todas.size() + ")", Filtro.TODAS);
        agregarFiltro("Lima y Callao", Filtro.LIMA);
        agregarFiltro("Provincias", Filtro.PROVINCIAS);
        agregarFiltro("Solo abiertas", Filtro.ABIERTAS);
        seleccionarFiltro(Filtro.TODAS, botonesFiltro.get(0));

        txtBuscar.textProperty().addListener((obs, antes, texto) -> {
            boolean conTexto = texto != null && !texto.isBlank();
            btnLimpiar.setVisible(conTexto);
            btnLimpiar.setManaged(conTexto);
            mostrarSedes();
        });
    }

    private void agregarFiltro(String texto, Filtro filtro) {
        Button b = new Button(texto);
        b.getStyleClass().add("sede-filtro");
        b.setOnAction(e -> seleccionarFiltro(filtro, b));
        botonesFiltro.add(b);
        filtros.getChildren().add(b);
    }

    private void seleccionarFiltro(Filtro filtro, Button boton) {
        filtroActivo = filtro;
        botonesFiltro.forEach(b -> b.getStyleClass().remove("activo"));
        boton.getStyleClass().add("activo");
        mostrarSedes();
    }

    /** Filtra por búsqueda (nombre, distrito o dirección) y por zona/estado. */
    private void mostrarSedes() {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        sedes.getChildren().clear();
        for (Sede s : todas) {
            if (!cumpleFiltro(s)) continue;
            boolean coincideTexto = buscado.isEmpty()
                    || s.getNombre().toLowerCase().contains(buscado)
                    || s.getDistrito().toLowerCase().contains(buscado)
                    || s.getDireccion().toLowerCase().contains(buscado);
            if (coincideTexto) {
                sedes.getChildren().add(tarjeta(s));
            }
        }
        if (sedes.getChildren().isEmpty()) {
            mostrarError(lblMensaje, todas.isEmpty() ? "No hay sedes registradas."
                    : buscado.isEmpty() ? "No hay sedes disponibles en este filtro."
                    : "Ninguna sede coincide con «" + buscado + "».");
        } else {
            limpiar(lblMensaje);
        }
    }

    private boolean cumpleFiltro(Sede s) {
        return switch (filtroActivo) {
            case TODAS -> true;
            case LIMA -> esLima(s);
            case PROVINCIAS -> !esLima(s);
            case ABIERTAS -> s.estaOperativa();
        };
    }

    private static boolean esLima(Sede s) {
        String tel = s.getTelefono();
        return tel != null && tel.trim().startsWith("01");
    }

    private VBox tarjeta(Sede sede) {
        boolean operativa = sede.estaOperativa();
        boolean esActual = sede.equals(estado().getSede());
        boolean enMantenimiento = Sede.EN_MANTENIMIENTO.equals(sede.getEstado());

        StackPane cajaIcono = new StackPane(Iconos.crear(Iconos.UBICACION, 18,
                esActual ? "icono-svg" : (operativa ? "icono-rojo" : "icono-suave")));
        cajaIcono.getStyleClass().add("sede-icono");

        Label nombre = new Label(sede.getNombre());
        nombre.getStyleClass().add("tarjeta-titulo");
        nombre.setWrapText(true);
        Label distrito = new Label(sede.getDistrito());
        distrito.getStyleClass().add("nota-suave");
        VBox tituloCaja = new VBox(1, nombre, distrito);
        HBox.setHgrow(tituloCaja, Priority.ALWAYS);

        Label estadoChip = new Label(esActual ? "Sede actual"
                : operativa ? "Abierta"
                : enMantenimiento ? "Mantenimiento" : "Cerrada");
        if (esActual) {
            estadoChip.getStyleClass().addAll("recargo", "premium");
        } else {
            estadoChip.getStyleClass().addAll("estado-chip",
                    operativa ? "pagada" : enMantenimiento ? "reembolsada" : "anulada");
        }
        estadoChip.setMinWidth(Region.USE_PREF_SIZE);

        HBox cabecera = new HBox(12, cajaIcono, tituloCaja, estadoChip);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("sede-card-cabecera");

        Label direccion = new Label(sede.getDireccion());
        direccion.getStyleClass().add("nota");
        direccion.setWrapText(true);

        HBox horario = new HBox(7, Iconos.crear(Iconos.RELOJ, 14, "icono-suave"),
                etiquetaHorario(sede));
        horario.setAlignment(Pos.CENTER_LEFT);

        FlowPane chips = chipsSalas(sede);

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        Button elegir = new Button(esActual ? "Seguir en esta sede"
                : operativa ? "Elegir esta sede"
                : enMantenimiento ? "En mantenimiento" : "Sede cerrada");
        elegir.getStyleClass().add(operativa ? "btn-primario" : "btn-secundario");
        elegir.setMaxWidth(Double.MAX_VALUE);
        elegir.setDisable(!operativa);
        elegir.setOnAction(e -> seleccionar(sede));

        VBox cuerpo = new VBox(10, direccion, horario, chips, espacio, elegir);
        cuerpo.getStyleClass().add("sede-card-cuerpo");
        VBox.setVgrow(cuerpo, Priority.ALWAYS);

        VBox tarjeta = new VBox(cabecera, cuerpo);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-sede");
        if (esActual) tarjeta.getStyleClass().add("actual");
        if (operativa) {
            tarjeta.setOnMouseClicked(e -> seleccionar(sede));
        } else {
            tarjeta.getStyleClass().add("cerrada");
        }
        tarjeta.setPrefSize(438, 304);
        tarjeta.setMinHeight(304);
        return tarjeta;
    }

    private static Label etiquetaHorario(Sede sede) {
        String h = sede.getHorarioAtencion();
        Label l = new Label(h == null || h.isBlank() ? "Horario por confirmar" : h);
        l.getStyleClass().add("nota-suave");
        l.setWrapText(true);
        return l;
    }

    private void seleccionar(Sede sede) {
        if (navegando) return;
        navegando = true;
        estado().setSede(sede);
        nav.ir("cartelera");
    }

    @FXML
    private void onLimpiar() {
        txtBuscar.clear();
        txtBuscar.requestFocus();
    }

    @FXML
    private void onVolver() {
        nav.ir("cartelera");
    }

    /** Píldoras con la cantidad de salas operativas y sus formatos («6 salas», «2D Tradicional», «IMAX 2D»...). */
    private static FlowPane chipsSalas(Sede sede) {
        int cantidad = 0;
        Set<String> formatos = new LinkedHashSet<>();
        Set<String> premium = new LinkedHashSet<>();
        for (Sala sala : sede.getSalas()) {
            if (sala.estaOperativa()) {
                cantidad++;
                String f = sala.getTipoSala().getFormato();
                formatos.add(f);
                if (sala.getTipoSala().obtenerRecargoFormatoCentavos() >= 1400) {
                    premium.add(f);
                }
            }
        }
        FlowPane fp = new FlowPane(6, 6);
        if (cantidad == 0) {
            Label sin = new Label("Sin salas disponibles");
            sin.getStyleClass().add("nota-suave");
            fp.getChildren().add(sin);
            return fp;
        }
        Label total = new Label(cantidad + (cantidad == 1 ? " sala" : " salas"),
                Iconos.crear(Iconos.SALA, 13, "icono-svg"));
        total.setGraphicTextGap(5);
        total.getStyleClass().add("recargo");
        fp.getChildren().add(total);
        for (String f : formatos) {
            Label tag = new Label(f);
            tag.getStyleClass().add(premium.contains(f) ? "sede-formato-premium" : "sede-formato");
            fp.getChildren().add(tag);
        }
        return fp;
    }
}
