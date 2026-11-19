package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.TipoSala;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 2 · Selección de horario: las funciones de la película en la sede, por día y por formato de sala.
 * Al tocar un horario empieza la compra (si ya hay cliente) o se pide iniciar sesión o seguir como invitado.
 */
public class HorarioVista extends Pantalla {

    @FXML private StackPane poster;
    @FXML private Label lblTitulo;
    @FXML private FlowPane generos;
    @FXML private Label lblDuracion;
    @FXML private Label lblClasificacion;
    @FXML private Button btnTrailer;
    @FXML private Label lblSedeFecha;
    @FXML private HBox dias;
    @FXML private Label lblMensaje;
    @FXML private VBox formatos;

    private final List<Funcion> funciones = new ArrayList<>();
    private final List<Button> botonesDia = new ArrayList<>();

    public HorarioVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        Pelicula p = estado().getPelicula();
        if (p == null || estado().getSede() == null) {
            Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        estado().setFuncion(null);
        poster.getChildren().add(Formato.poster(p, 320, 480));
        lblTitulo.setText(p.getTitulo());
        for (Genero g : p.getGeneros()) {
            Label tag = new Label(g.getNombre());
            tag.getStyleClass().addAll("tag-genero", "tag-genero-claro");
            generos.getChildren().add(tag);
        }
        lblDuracion.setText(p.getDuracion() + " min");
        lblClasificacion.setText(p.getClasificacionEdad());
        btnTrailer.setDisable(p.getTrailer() == null || p.getTrailer().isBlank());

        Respuesta<List<Funcion>> r = app().cartelera().consultarCartelera(estado().getSede());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        for (Funcion f : r.dato()) {
            if (f.getPelicula().getIdPelicula() == p.getIdPelicula()) funciones.add(f);
        }
        funciones.sort(Comparator.comparingLong(f -> f.obtenerFechaHoraInicio().toValorComparable()));
        cargarDias();
    }

    /** Un botón por día con funciones; se elige el día filtrado en la cartelera o, si no hay, el primero. */
    private void cargarDias() {
        Map<Long, DateTime> unicos = new LinkedHashMap<>();
        for (Funcion f : funciones) unicos.putIfAbsent(f.getFecha().toValorComparable(), f.getFecha());
        if (unicos.isEmpty()) {
            lblSedeFecha.setText(estado().getSede().getNombre());
            mostrarError(lblMensaje, "Esta película ya no tiene funciones disponibles en esta sede.");
            return;
        }
        for (DateTime dia : unicos.values()) {
            Button b = new Button(Formato.diaCorto(dia));
            b.getStyleClass().addAll("chip-dia", "btn-pastilla");
            b.setOnAction(e -> elegirDia(dia, b));
            botonesDia.add(b);
            dias.getChildren().add(b);
        }
        List<DateTime> lista = new ArrayList<>(unicos.values());
        DateTime filtrado = estado().getDiaElegido();
        int elegido = 0;
        for (int i = 0; filtrado != null && i < lista.size(); i++) {
            if (lista.get(i).toValorSoloFecha() == filtrado.toValorSoloFecha()) elegido = i;
        }
        elegirDia(lista.get(elegido), botonesDia.get(elegido));
    }

    private void elegirDia(DateTime dia, Button boton) {
        for (Button b : botonesDia) b.getStyleClass().remove("activo");
        boton.getStyleClass().add("activo");
        lblSedeFecha.setText(estado().getSede().getNombre() + " · " + Formato.fechaLarga(dia));

        // Agrupa las funciones del día por formato de sala (2D, 3D, Prime...)
        Map<Integer, List<Funcion>> porFormato = new LinkedHashMap<>();
        Map<Integer, TipoSala> tipos = new LinkedHashMap<>();
        for (Funcion f : funciones) {
            if (!f.getFecha().equals(dia)) continue;
            TipoSala tipo = f.getSala().getTipoSala();
            porFormato.computeIfAbsent(tipo.getIdTipoSala(), k -> new ArrayList<>()).add(f);
            tipos.putIfAbsent(tipo.getIdTipoSala(), tipo);
        }
        formatos.getChildren().clear();
        for (var entrada : porFormato.entrySet()) {
            formatos.getChildren().add(tarjetaFormato(tipos.get(entrada.getKey()), entrada.getValue()));
        }
    }

    private HBox tarjetaFormato(TipoSala tipo, List<Funcion> delFormato) {
        Label nombre = new Label(tipo.getFormato());
        nombre.getStyleClass().add("titulo-seccion");
        long recargo = tipo.obtenerRecargoFormatoCentavos();
        Label etiqueta = new Label(recargo == 0 ? "Precio base" : "+" + Formato.dinero(recargo));
        etiqueta.getStyleClass().add("recargo");
        HBox cabecera = new HBox(10, nombre, etiqueta);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        Label descripcion = new Label(tipo.getDescripcion());
        descripcion.getStyleClass().add("nota-suave");
        descripcion.setWrapText(true);
        VBox info = new VBox(6, cabecera, descripcion);
        info.setMinWidth(300);
        info.setPrefWidth(300);
        info.setMaxWidth(300);

        FlowPane horas = new FlowPane(12, 12);
        horas.setAlignment(Pos.CENTER_LEFT);
        for (Funcion f : delFormato) horas.getChildren().add(botonHorario(f));

        HBox tarjeta = new HBox(30, info, horas);
        tarjeta.getStyleClass().add("tarjeta-plana");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setMinHeight(112);
        return tarjeta;
    }

    private Button botonHorario(Funcion f) {
        Label hora = new Label(f.getHoraInicio().soloHora());
        hora.getStyleClass().add("horario-hora");
        Label idioma = new Label(f.estaDisponible() ? Formato.capitalizar(f.getIdioma()) : "Agotado");
        idioma.getStyleClass().add("horario-idioma");
        VBox contenido = new VBox(2, hora, idioma);
        contenido.setAlignment(Pos.CENTER);
        Button b = new Button();
        b.setGraphic(contenido);
        b.getStyleClass().add("horario");
        b.setDisable(!f.estaDisponible());
        b.setOnAction(e -> elegirFuncion(f));
        return b;
    }

    /** Con cliente ya identificado empieza la compra; si no, pregunta si inicia sesión o sigue como invitado. */
    private void elegirFuncion(Funcion f) {
        estado().setFuncion(f);
        if (!estado().hayCliente()) {
            nav.ir("login-invitado");
            return;
        }
        Respuesta<Compra> r = nav.iniciarCompra();
        if (!r.ok()) mostrarError(lblMensaje, r.mensaje());
    }

    @FXML
    private void onVolver() {
        nav.ir("cartelera");
    }

    @FXML
    private void onSinopsis() {
        SinopsisVista sinopsis = nav.modal("sinopsis");
        sinopsis.mostrar(estado().getPelicula(), funciones);
    }

    @FXML
    private void onTrailer() {
        nav.abrirEnlace(estado().getPelicula().getTrailer());
    }
}
