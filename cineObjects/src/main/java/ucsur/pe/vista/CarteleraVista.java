package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 1 · Cartelera de la sede elegida. A la izquierda, «Filtrar por:» fecha, género, idioma y formato
 * (los aplica el servicio con filtrarCartelera; la búsqueda por título filtra lo ya mostrado).
 * Arriba de las películas, tres pestañas: «En cartelera» (en cartelera y reestrenos con funciones),
 * «Preventa» (preestrenos que ya venden funciones) y «Próximamente» (anunciadas, aún sin funciones).
 * Las películas van siempre en 4 columnas: las tarjetas toman el ancho de su columna.
 */
public class CarteleraVista extends Pantalla {

    private static final int COLUMNAS = 4;
    private static final double HUECO = 22;     // hgap del GridPane
    private static final double RELLENO = 20;   // padding izquierdo + derecho de .tarjeta-pelicula

    /** Una opción de los desplegables: lo que se ve y el valor que se filtra (null = sin filtro). */
    private record Opcion<T>(String texto, T valor) {
        @Override public String toString() { return texto; }
    }

    private enum Pestana { CARTELERA, PREVENTA, PROXIMAMENTE }

    @FXML private EncabezadoVista encabezadoController;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Opcion<DateTime>> cmbFecha;
    @FXML private ComboBox<Opcion<Genero>> cmbGenero;
    @FXML private ComboBox<Opcion<String>> cmbIdioma;
    @FXML private ComboBox<Opcion<String>> cmbFormato;
    @FXML private Button tabCartelera;
    @FXML private Button tabPreventa;
    @FXML private Button tabProximamente;
    @FXML private Label lblCantidad;
    @FXML private Label lblMensaje;
    @FXML private ScrollPane scroll;
    @FXML private GridPane peliculas;

    private Pestana pestana = Pestana.CARTELERA;
    private final List<Pelicula> mostradas = new ArrayList<>();
    private double anchoTarjeta = 268;

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
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar());
        cmbFecha.valueProperty().addListener((o, a, b) -> filtrar());
        cmbGenero.valueProperty().addListener((o, a, b) -> filtrar());
        cmbIdioma.valueProperty().addListener((o, a, b) -> filtrar());
        cmbFormato.valueProperty().addListener((o, a, b) -> filtrar());
        // Siempre 4 columnas: al cambiar el ancho, las tarjetas (y sus pósters) se rehacen a la medida
        scroll.viewportBoundsProperty().addListener((o, a, b) -> ajustarAncho(b.getWidth()));
        elegirPestana(Pestana.CARTELERA);
    }

    // ---------- Filtros ----------

    /** Fechas, idiomas y formatos que realmente hay en la cartelera de esta sede; los géneros, todos. */
    private void cargarOpciones(List<Funcion> funciones) {
        TreeMap<Long, DateTime> dias = new TreeMap<>();
        TreeSet<String> idiomas = new TreeSet<>();
        TreeSet<String> formatos = new TreeSet<>();
        for (Funcion f : funciones) {
            dias.putIfAbsent(f.getFecha().toValorSoloFecha(), f.getFecha());
            idiomas.add(Formato.capitalizar(f.getIdioma()));
            formatos.add(f.getSala().getTipoSala().getFormato());
        }
        List<Opcion<DateTime>> opcFechas = new ArrayList<>(List.of(new Opcion<>("Todas las fechas", null)));
        // Con el mes: la preventa puede tener funciones semanas después («Mié 16 dic»)
        for (DateTime d : dias.values()) opcFechas.add(new Opcion<>(Formato.diaCorto(d) + " " + Formato.mesCorto(d), d));

        List<Opcion<Genero>> opcGeneros = new ArrayList<>(List.of(new Opcion<>("Todos los géneros", null)));
        Respuesta<List<Genero>> generos = app().cartelera().listarGeneros();
        if (generos.ok()) for (Genero g : generos.dato()) opcGeneros.add(new Opcion<>(g.getNombre(), g));

        List<Opcion<String>> opcIdiomas = new ArrayList<>(List.of(new Opcion<>("Todos los idiomas", null)));
        for (String i : idiomas) opcIdiomas.add(new Opcion<>(i, i));
        List<Opcion<String>> opcFormatos = new ArrayList<>(List.of(new Opcion<>("Todos los formatos", null)));
        for (String f : formatos) opcFormatos.add(new Opcion<>(f, f));

        cmbFecha.setItems(FXCollections.observableArrayList(opcFechas));
        cmbGenero.setItems(FXCollections.observableArrayList(opcGeneros));
        cmbIdioma.setItems(FXCollections.observableArrayList(opcIdiomas));
        cmbFormato.setItems(FXCollections.observableArrayList(opcFormatos));
        seleccionarPrimeros();
    }

    private void seleccionarPrimeros() {
        cmbFecha.getSelectionModel().selectFirst();
        cmbGenero.getSelectionModel().selectFirst();
        cmbIdioma.getSelectionModel().selectFirst();
        cmbFormato.getSelectionModel().selectFirst();
    }

    @FXML
    private void onLimpiar() {
        txtBuscar.clear();
        seleccionarPrimeros();
        filtrar();
    }

    private static <T> T valor(ComboBox<Opcion<T>> combo) {
        return combo.getValue() == null ? null : combo.getValue().valor();
    }

    // ---------- Pestañas ----------

    @FXML private void onTabCartelera() { elegirPestana(Pestana.CARTELERA); }
    @FXML private void onTabPreventa() { elegirPestana(Pestana.PREVENTA); }
    @FXML private void onTabProximamente() { elegirPestana(Pestana.PROXIMAMENTE); }

    private void elegirPestana(Pestana nueva) {
        pestana = nueva;
        tabCartelera.getStyleClass().remove("activo");
        tabPreventa.getStyleClass().remove("activo");
        tabProximamente.getStyleClass().remove("activo");
        (nueva == Pestana.CARTELERA ? tabCartelera : nueva == Pestana.PREVENTA ? tabPreventa : tabProximamente)
                .getStyleClass().add("activo");
        // Las películas de «Próximamente» aún no tienen funciones: solo se filtran por título y género
        boolean conFunciones = nueva != Pestana.PROXIMAMENTE;
        cmbFecha.setDisable(!conFunciones);
        cmbIdioma.setDisable(!conFunciones);
        cmbFormato.setDisable(!conFunciones);
        filtrar();
    }

    // ---------- Películas ----------

    private void filtrar() {
        List<Pelicula> lista = pestana == Pestana.PROXIMAMENTE ? proximamente() : conFunciones();
        if (lista == null) return; // el error ya se mostró
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        mostradas.clear();
        for (Pelicula p : lista) {
            if (buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado)) mostradas.add(p);
        }
        dibujar();
        int n = mostradas.size();
        lblCantidad.setText(n + (n == 1 ? " película" : " películas"));
        if (n == 0) {
            mostrarError(lblMensaje, pestana == Pestana.PREVENTA && hayFiltros()
                    ? "No hay preventas con esos filtros. Prueba con «Limpiar filtros»."
                    : pestana == Pestana.PREVENTA ? "Por ahora no hay películas en preventa en esta sede."
                    : "No hay películas con esos filtros. Prueba con «Limpiar filtros».");
        } else {
            limpiar(lblMensaje);
        }
    }

    private boolean hayFiltros() {
        return valor(cmbFecha) != null || valor(cmbGenero) != null || valor(cmbIdioma) != null
                || valor(cmbFormato) != null || !txtBuscar.getText().isBlank();
    }

    /** «En cartelera» y «Preventa»: una película por cada una que tenga funciones con los filtros elegidos. */
    private List<Pelicula> conFunciones() {
        Respuesta<List<Funcion>> r = app().cartelera().filtrarCartelera(estado().getSede(),
                valor(cmbFecha), valor(cmbGenero), valor(cmbIdioma), valor(cmbFormato));
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return null;
        }
        boolean preventa = pestana == Pestana.PREVENTA;
        Map<Integer, Pelicula> unicas = new LinkedHashMap<>(); // en el orden en que llegan sus funciones
        for (Funcion f : r.dato()) {
            Pelicula p = f.getPelicula();
            if (Pelicula.PREESTRENO.equals(p.getEstado()) == preventa) unicas.putIfAbsent(p.getIdPelicula(), p);
        }
        return new ArrayList<>(unicas.values());
    }

    private List<Pelicula> proximamente() {
        Respuesta<List<Pelicula>> r = app().cartelera().listarProximamente();
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return null;
        }
        Genero genero = valor(cmbGenero);
        return r.dato().stream().filter(p -> p.tieneGenero(genero)).toList();
    }

    /** Con el ancho disponible, cada tarjeta mide un cuarto (menos los huecos); se rehace si cambió. */
    private void ajustarAncho(double anchoVisible) {
        if (anchoVisible <= 0) return;
        double nuevo = Math.floor((anchoVisible - HUECO * (COLUMNAS - 1)) / COLUMNAS);
        if (Math.abs(nuevo - anchoTarjeta) >= 2) {
            anchoTarjeta = nuevo;
            dibujar();
        }
    }

    private void dibujar() {
        peliculas.getChildren().clear();
        for (int i = 0; i < mostradas.size(); i++) {
            peliculas.add(tarjeta(mostradas.get(i)), i % COLUMNAS, i / COLUMNAS);
        }
    }

    private VBox tarjeta(Pelicula p) {
        double anchoPoster = Math.max(120, anchoTarjeta - RELLENO);
        VBox poster = new VBox(Formato.poster(p, anchoPoster, anchoPoster * 1.5)); // proporción 2:3
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
            genero.setMinWidth(Region.USE_PREF_SIZE); // si falta espacio, se acorta la duración, no el género
            datos.getChildren().add(genero);
        }
        Label detalle = new Label(pestana == Pestana.PROXIMAMENTE ? estreno(p) : Formato.duracion(p.getDuracion()));
        detalle.getStyleClass().add("nota");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label clasif = new Label(p.getClasificacionEdad());
        clasif.getStyleClass().add("tag-clasificacion");
        clasif.setMinWidth(Region.USE_PREF_SIZE);
        if (p.getClasificacionEdad().toUpperCase().startsWith("APT")) clasif.getStyleClass().add("apta");
        datos.getChildren().addAll(detalle, espacio, clasif);

        VBox tarjeta = new VBox(8, poster);
        if (pestana == Pestana.PREVENTA) {
            Label etiqueta = new Label("Preventa · " + estreno(p));
            etiqueta.getStyleClass().add("tag-dorada");
            tarjeta.getChildren().add(etiqueta);
        }
        tarjeta.getChildren().addAll(titulo, datos, boton(p));
        tarjeta.getStyleClass().add("tarjeta-pelicula");
        tarjeta.setMaxWidth(anchoTarjeta);
        return tarjeta;
    }

    /** «Ver funciones» abre los horarios (en el día filtrado, si eligió uno); en «Próximamente», la sinopsis. */
    private Button boton(Pelicula p) {
        if (pestana == Pestana.PROXIMAMENTE) {
            Button ver = new Button("Ver sinopsis");
            ver.getStyleClass().add("btn-secundario");
            ver.setMaxWidth(Double.MAX_VALUE);
            ver.setOnAction(e -> {
                SinopsisVista sinopsis = nav.modal("sinopsis");
                sinopsis.mostrar(p, List.of());
            });
            return ver;
        }
        Button ver = new Button("Ver funciones");
        ver.getStyleClass().add("btn-primario");
        ver.setMaxWidth(Double.MAX_VALUE);
        ver.setOnAction(e -> {
            estado().setPelicula(p);
            estado().setDiaElegido(valor(cmbFecha));
            nav.ir("horario");
        });
        return ver;
    }

    private static String estreno(Pelicula p) {
        DateTime f = p.getFechaEstreno();
        return f == null ? "Estreno por confirmar" : "Estreno " + f.getDia() + " " + Formato.mesCorto(f);
    }
}
