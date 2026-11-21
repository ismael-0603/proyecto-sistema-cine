package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 17.1 · Formulario de película: nueva o editar (al editar, la duración no se cambia). */
public class AdminPeliculaFormVista extends PantallaAdmin {

    @FXML private Label lblTitulo;
    @FXML private TextField txtTitulo;
    @FXML private TextArea txtSinopsis;
    @FXML private Label lblContador;
    @FXML private Label lblDuracion;
    @FXML private TextField txtDuracion;
    @FXML private ComboBox<String> cmbClasificacion;
    @FXML private TextField txtEstreno;
    @FXML private FlowPane generos;
    @FXML private TextField txtTrailer;
    @FXML private Label lblMensaje;
    @FXML private StackPane vistaPoster;
    @FXML private TextField txtPoster;
    @FXML private VBox estados;

    private final Map<Genero, CheckBox> casillas = new LinkedHashMap<>();
    private final ToggleGroup grupoEstado = new ToggleGroup();
    private Pelicula editando;

    public AdminPeliculaFormVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("peliculas")) return;
        editando = estado().getEnEdicion(Pelicula.class);
        cmbClasificacion.setItems(FXCollections.observableArrayList("APT", "+14", "+18"));
        Respuesta<List<Genero>> g = app().adminPeliculas().listarGeneros(admin);
        if (g.ok()) {
            for (Genero genero : g.dato()) {
                CheckBox c = new CheckBox(genero.getNombre());
                casillas.put(genero, c);
                generos.getChildren().add(c);
            }
        }
        for (String e : AdminPeliculasVista.ESTADOS) {
            RadioButton r = new RadioButton(Formato.capitalizar(e));
            r.setUserData(e);
            r.setToggleGroup(grupoEstado);
            estados.getChildren().add(r);
        }
        txtSinopsis.textProperty().addListener((o, a, t) ->
                lblContador.setText(t.length() + " de " + Pelicula.MAX_SINOPSIS + " caracteres"));
        txtPoster.textProperty().addListener((o, a, t) -> mostrarPoster());
        if (editando != null) cargarDatos();
        else grupoEstado.getToggles().get(0).setSelected(true);
        mostrarPoster();
    }

    private void cargarDatos() {
        lblTitulo.setText("Editar película");
        txtTitulo.setText(editando.getTitulo());
        txtSinopsis.setText(editando.getSinopsis());
        txtDuracion.setText(String.valueOf(editando.getDuracion()));
        txtDuracion.setDisable(true);
        lblDuracion.setText("Duración (min) · no se cambia al editar");
        cmbClasificacion.setValue(editando.getClasificacionEdad());
        if (editando.getFechaEstreno() != null) txtEstreno.setText(Formato.fecha(editando.getFechaEstreno()));
        casillas.forEach((genero, c) -> c.setSelected(editando.tieneGenero(genero)));
        txtTrailer.setText(editando.getTrailer());
        txtPoster.setText(editando.getPoster());
        grupoEstado.getToggles().stream().filter(t -> editando.getEstado().equals(t.getUserData()))
                .findFirst().ifPresent(t -> t.setSelected(true));
    }

    /** Vista previa con el mismo póster (o reemplazo) que ve el cliente en la cartelera. */
    private void mostrarPoster() {
        String titulo = txtTitulo.getText() == null || txtTitulo.getText().isBlank() ? "Póster" : txtTitulo.getText();
        vistaPoster.getChildren().setAll(Formato.poster(titulo, vacioANulo(txtPoster.getText()), 170, 255));
    }

    @FXML
    private void onElegirImagen() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir póster");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
        File archivo = selector.showOpenDialog(txtPoster.getScene().getWindow());
        if (archivo != null) txtPoster.setText(archivo.toURI().toString());
    }

    @FXML
    private void onGuardar() {
        DateTime estreno;
        int duracion;
        try {
            estreno = txtEstreno.getText() == null || txtEstreno.getText().isBlank() ? null
                    : Formato.leerFecha(txtEstreno.getText(), "Fecha de estreno");
            duracion = Formato.leerEntero(txtDuracion.getText(), "La duración");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensaje, e.getMessage());
            return;
        }
        List<Genero> elegidos = new ArrayList<>();
        casillas.forEach((genero, c) -> { if (c.isSelected()) elegidos.add(genero); });
        String estadoElegido = grupoEstado.getSelectedToggle() == null ? null
                : (String) grupoEstado.getSelectedToggle().getUserData();
        String clasificacion = cmbClasificacion.getEditor().getText();

        Respuesta<?> r = editando == null
                ? app().adminPeliculas().registrarPelicula(admin, txtTitulo.getText(), txtSinopsis.getText(), duracion,
                        clasificacion, vacioANulo(txtTrailer.getText()), vacioANulo(txtPoster.getText()), estreno,
                        estadoElegido, elegidos)
                : app().adminPeliculas().actualizarPelicula(admin, editando, txtTitulo.getText(), txtSinopsis.getText(),
                        clasificacion, vacioANulo(txtTrailer.getText()), vacioANulo(txtPoster.getText()), estreno,
                        estadoElegido, elegidos);
        if (!mostrar(lblMensaje, r)) return;
        nav.ir("admin-peliculas");
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    @FXML
    private void onVolver() {
        nav.ir("admin-peliculas");
    }
}
