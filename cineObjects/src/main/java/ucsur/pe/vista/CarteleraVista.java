package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 1 · Cartelera: las películas con funciones en la sede elegida. Los filtros de idioma, clasificación
 * y género los aplica el servicio (filtrarCartelera); la búsqueda por título filtra lo ya mostrado.
 */
public class CarteleraVista extends Pantalla {

    private static final String TODOS_IDIOMAS = "Idioma";
    private static final String TODAS_CLASIF = "Clasificación";

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbIdioma;
    @FXML private ComboBox<String> cmbClasificacion;
    @FXML private HBox generos;
    @FXML private Label lblCantidad;
    @FXML private Label lblMensaje;
    @FXML private FlowPane peliculas;

    private Genero generoElegido; // null = «Todos»
    private final List<Button> chips = new ArrayList<>();

    public CarteleraVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (estado().getSede() == null) { // se entró sin sede: primero hay que elegirla
            javafx.application.Platform.runLater(() -> nav.ir("elegir-sede"));
            return;
        }
        estado().setFuncion(null);
        Respuesta<List<Funcion>> r = app().cartelera().consultarCartelera(estado().getSede());
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        cargarOpciones(r.dato());
        cargarGeneros();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbIdioma.valueProperty().addListener((o, a, b) -> filtrar());
        cmbClasificacion.valueProperty().addListener((o, a, b) -> filtrar());
        filtrar();
    }

    /** Idiomas y clasificaciones que realmente hay en la cartelera de esta sede. */
    private void cargarOpciones(List<Funcion> funciones) {
        TreeSet<String> idiomas = new TreeSet<>();
        TreeSet<String> clasificaciones = new TreeSet<>();
        for (Funcion f : funciones) {
            idiomas.add(Formato.capitalizar(f.getIdioma()));
            clasificaciones.add(f.getPelicula().getClasificacionEdad());
        }
        List<String> listaIdiomas = new ArrayList<>(List.of(TODOS_IDIOMAS));
        listaIdiomas.addAll(idiomas);
        List<String> listaClasif = new ArrayList<>(List.of(TODAS_CLASIF));
        listaClasif.addAll(clasificaciones);
        cmbIdioma.setItems(FXCollections.observableArrayList(listaIdiomas));
        cmbClasificacion.setItems(FXCollections.observableArrayList(listaClasif));
        cmbIdioma.getSelectionModel().selectFirst();
        cmbClasificacion.getSelectionModel().selectFirst();
    }

    private void cargarGeneros() {
        Button todos = chip("Todos", null);
        generos.getChildren().add(todos);
        Respuesta<List<Genero>> r = app().cartelera().listarGeneros();
        if (r.ok()) {
            for (Genero g : r.dato()) generos.getChildren().add(chip(g.getNombre(), g));
        }
        marcarChip(todos);
    }

    private Button chip(String texto, Genero genero) {
        Button b = new Button(texto);
        b.getStyleClass().add("chip");
        b.setMinWidth(Region.USE_PREF_SIZE); // el nombre del género nunca se corta con «…»
        b.setOnAction(e -> {
            generoElegido = genero;
            marcarChip(b);
            filtrar();
        });
        chips.add(b);
        return b;
    }

    private void marcarChip(Button elegido) {
        for (Button b : chips) b.getStyleClass().remove("activo");
        elegido.getStyleClass().add("activo");
    }

    @FXML
    private void onLimpiar() {
        txtBuscar.clear();
        cmbIdioma.getSelectionModel().selectFirst();
        cmbClasificacion.getSelectionModel().selectFirst();
        generoElegido = null;
        if (!chips.isEmpty()) marcarChip(chips.get(0));
        filtrar();
    }

    private void filtrar() {
        String idioma = TODOS_IDIOMAS.equals(cmbIdioma.getValue()) ? null : cmbIdioma.getValue();
        String clasif = TODAS_CLASIF.equals(cmbClasificacion.getValue()) ? null : cmbClasificacion.getValue();
        Respuesta<List<Funcion>> r = app().cartelera().filtrarCartelera(estado().getSede(), idioma, clasif, generoElegido);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        // Una tarjeta por película, en el orden en que llegan sus funciones
        Map<Integer, Pelicula> unicas = new LinkedHashMap<>();
        for (Funcion f : r.dato()) unicas.putIfAbsent(f.getPelicula().getIdPelicula(), f.getPelicula());

        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        peliculas.getChildren().clear();
        for (Pelicula p : unicas.values()) {
            if (buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado)) {
                peliculas.getChildren().add(tarjeta(p));
            }
        }
        int n = peliculas.getChildren().size();
        lblCantidad.setText(n + (n == 1 ? " película" : " películas"));
        if (n == 0) mostrarError(lblMensaje, "No hay funciones con esos filtros. Prueba con «Limpiar filtros».");
        else limpiar(lblMensaje);
    }

    private VBox tarjeta(Pelicula p) {
        // Póster a todo el ancho de la tarjeta, en proporción 2:3
        VBox poster = new VBox(Formato.poster(p, 248, 372));
        poster.setAlignment(Pos.CENTER);

        Label titulo = new Label(p.getTitulo());
        titulo.getStyleClass().add("pelicula-titulo");
        titulo.setWrapText(true);
        titulo.setMinHeight(44);
        titulo.setAlignment(Pos.TOP_LEFT);

        HBox datos = new HBox(10);
        datos.setAlignment(Pos.CENTER_LEFT);
        if (!p.getGeneros().isEmpty()) {
            Label genero = new Label(p.getGeneros().get(0).getNombre());
            genero.getStyleClass().add("tag-genero");
            datos.getChildren().add(genero);
        }
        Label duracion = new Label(Formato.duracion(p.getDuracion()));
        duracion.getStyleClass().add("nota");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label clasif = new Label(p.getClasificacionEdad());
        clasif.getStyleClass().add("tag-clasificacion");
        if (p.getClasificacionEdad().toUpperCase().startsWith("APT")) clasif.getStyleClass().add("apta");
        datos.getChildren().addAll(duracion, espacio, clasif);

        Button ver = new Button("Ver funciones");
        ver.getStyleClass().add("btn-primario");
        ver.setMaxWidth(Double.MAX_VALUE);
        ver.setOnAction(e -> {
            estado().setPelicula(p);
            nav.ir("horario");
        });

        VBox tarjeta = new VBox(8, poster, titulo, datos, ver);
        tarjeta.getStyleClass().add("tarjeta-pelicula");
        tarjeta.setPrefWidth(268);
        tarjeta.setMaxWidth(268);
        return tarjeta;
    }
}
