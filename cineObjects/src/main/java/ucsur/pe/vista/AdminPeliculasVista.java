package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.util.ArrayList;
import java.util.List;

/** 17 · Películas: buscar y filtrar, crear, editar y retirar de cartelera (con los avisos 17.2 y 17.3). */
public class AdminPeliculasVista extends PantallaAdmin {

    private static final String TODOS_GENEROS = "Género";
    private static final String TODOS_ESTADOS = "Estado";
    static final List<String> ESTADOS = List.of(Pelicula.PROXIMAMENTE, Pelicula.PREESTRENO, Pelicula.EN_CARTELERA,
            Pelicula.REESTRENO, Pelicula.FUERA_DE_CARTELERA);

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbGenero;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;

    private final Tabla<Pelicula> tabla = new Tabla<>(
            new String[] {"Título", "Género", "Duración", "Clasificación", "Estado", "Acciones"},
            new double[] {300, 170, 120, 130, 170}, 8);
    private List<Pelicula> peliculas = new ArrayList<>();

    public AdminPeliculasVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("peliculas")) return;
        contenedorTabla.getChildren().add(tabla.vista());
        List<String> generos = new ArrayList<>(List.of(TODOS_GENEROS));
        Respuesta<List<Genero>> g = app().admin().listarGeneros(admin);
        if (g.ok()) g.dato().forEach(x -> generos.add(x.getNombre()));
        cmbGenero.setItems(FXCollections.observableArrayList(generos));
        List<String> estados = new ArrayList<>(List.of(TODOS_ESTADOS));
        ESTADOS.forEach(e -> estados.add(Formato.capitalizar(e)));
        cmbEstado.setItems(FXCollections.observableArrayList(estados));
        cmbGenero.getSelectionModel().selectFirst();
        cmbEstado.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar(false));
        cmbGenero.valueProperty().addListener((o, a, b) -> filtrar(false));
        cmbEstado.valueProperty().addListener((o, a, b) -> filtrar(false));
        cargar(false);
    }

    private void cargar(boolean mantenerPagina) {
        Respuesta<List<Pelicula>> r = app().admin().listarPeliculas(admin);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        peliculas = r.dato();
        filtrar(mantenerPagina);
    }

    /** Buscar y filtrar lo ya cargado es trabajo de la pantalla. */
    private void filtrar(boolean mantenerPagina) {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String genero = TODOS_GENEROS.equals(cmbGenero.getValue()) ? null : cmbGenero.getValue();
        String estado = TODOS_ESTADOS.equals(cmbEstado.getValue()) ? null : cmbEstado.getValue();
        List<Pelicula> visibles = peliculas.stream()
                .filter(p -> buscado.isEmpty() || p.getTitulo().toLowerCase().contains(buscado))
                .filter(p -> genero == null || p.getGeneros().stream().anyMatch(x -> x.getNombre().equals(genero)))
                .filter(p -> estado == null || Formato.capitalizar(p.getEstado()).equals(estado))
                .toList();
        tabla.mostrar(visibles, this::fila, mantenerPagina);
    }

    private List<Node> fila(Pelicula p) {
        String generos = p.getGeneros().isEmpty() ? "—"
                : String.join(", ", p.getGeneros().stream().map(Genero::getNombre).toList());
        boolean fuera = Pelicula.FUERA_DE_CARTELERA.equals(p.getEstado());
        var retirar = Tabla.boton("Retirar", false, () -> confirmarRetiro(p));
        retirar.setDisable(fuera);
        return List.of(Tabla.texto(p.getTitulo()), Tabla.texto(generos), Tabla.texto(p.getDuracion() + " min"),
                Tabla.texto(p.getClasificacionEdad()),
                Tabla.chip(Formato.capitalizar(p.getEstado()), fuera ? "inactiva" : "pagada"),
                Tabla.acciones(Tabla.boton("Editar", false, () -> abrirFormulario(p)), retirar));
    }

    @FXML
    private void onNueva() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Pelicula p) {
        estado().setEnEdicion(p);
        nav.ir("admin-pelicula-form");
    }

    /** 17.2 · confirmación; si el servicio la rechaza por funciones pendientes, 17.3. */
    private void confirmarRetiro(Pelicula p) {
        DialogoVista d = nav.dialogo();
        d.configurar("¿Retirar «" + p.getTitulo() + "»?", "Pasará a «Fuera de cartelera» y dejará de verse en la "
                + "cartelera. No se borra: puedes volver a publicarla desde Editar.", false);
        d.botonSecundario("Volver", nav::cerrarModal);
        d.botonPrincipal("Retirar película", () -> {
            Respuesta<Void> r = app().admin().retirarPelicula(admin, p);
            if (r.ok()) {
                nav.cerrarModal();
                mostrarOk(lblMensaje, "«" + p.getTitulo() + "» " + r.mensaje().toLowerCase());
                cargar(true);
                return;
            }
            DialogoVista aviso = nav.dialogo();
            aviso.configurar("No se pudo retirar", r.mensaje(), true);
            aviso.botonSecundario("Entendido", nav::cerrarModal);
            aviso.botonPrincipal("Ir a Funciones", () -> nav.ir("admin-funciones"));
        });
    }
}
