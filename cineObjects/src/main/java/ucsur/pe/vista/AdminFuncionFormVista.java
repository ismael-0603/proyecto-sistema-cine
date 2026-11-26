package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sala;
import java.util.List;
import java.util.function.Function;

/** 18.1 · Programar o editar una función en la sede del administrador. */
public class AdminFuncionFormVista extends PantallaAdmin {

    private static final String DOBLADA = "Doblada";
    private static final String SUBTITULADA = "Subtitulada";
    private static final String ESPANOL = "Español";

    @FXML private Label lblTitulo;
    @FXML private Label lblSede;
    @FXML private ComboBox<Pelicula> cmbPelicula;
    @FXML private ComboBox<Sala> cmbSala;
    @FXML private TextField txtFecha;
    @FXML private TextField txtHora;
    @FXML private RadioButton rbDoblada;
    @FXML private RadioButton rbSubtitulada;
    @FXML private RadioButton rbEspanol;
    @FXML private Label lblMensaje;
    @FXML private Label lblFormato;
    @FXML private Label lblCapacidad;
    @FXML private Label lblDuracion;
    @FXML private Label lblTermina;
    @FXML private Button btnGuardar;

    private Funcion editando;

    public AdminFuncionFormVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("funciones")) return;
        editando = estado().getEnEdicion(Funcion.class);
        ToggleGroup idioma = new ToggleGroup();
        rbDoblada.setToggleGroup(idioma);
        rbSubtitulada.setToggleGroup(idioma);
        rbEspanol.setToggleGroup(idioma);
        lblSede.setText("Sede: " + admin.getSede().getNombre() + " (tu sede)");
        cmbPelicula.setConverter(convertidor(Pelicula::getTitulo));
        cmbSala.setConverter(convertidor(s -> "Sala " + s.getNumero() + " · " + s.getTipoSala().getFormato()));

        Respuesta<List<Pelicula>> pelis = app().adminPeliculas().listarPeliculas(admin);
        if (pelis.ok()) {
            // Ayuda de la pantalla: solo las que se pueden programar (el servicio igual lo valida)
            cmbPelicula.setItems(FXCollections.observableArrayList(pelis.dato().stream()
                    .filter(p -> Pelicula.EN_CARTELERA.equals(p.getEstado()) || Pelicula.PREESTRENO.equals(p.getEstado())
                            || Pelicula.REESTRENO.equals(p.getEstado()))
                    .toList()));
        }
        Respuesta<List<Sala>> salas = app().adminInfraestructura().listarSalas(admin, admin.getSede());
        if (salas.ok()) cmbSala.setItems(FXCollections.observableArrayList(salas.dato().stream().filter(Sala::estaOperativa).toList()));

        cmbPelicula.valueProperty().addListener((o, a, b) -> actualizarDatosSala());
        cmbSala.valueProperty().addListener((o, a, b) -> actualizarDatosSala());
        txtFecha.textProperty().addListener((o, a, b) -> actualizarDatosSala());
        txtHora.textProperty().addListener((o, a, b) -> actualizarDatosSala());

        if (editando != null) {
            lblTitulo.setText("Editar función");
            btnGuardar.setText("Guardar cambios");
            cmbPelicula.setValue(editando.getPelicula());
            cmbSala.setValue(editando.getSala());
            cmbPelicula.setDisable(true);
            cmbSala.setDisable(true);
            txtFecha.setText(Formato.fecha(editando.getFecha()));
            txtHora.setText(editando.getHoraInicio().soloHora());
            String lang = editando.getIdioma();
            if (SUBTITULADA.equalsIgnoreCase(lang)) rbSubtitulada.setSelected(true);
            else if (ESPANOL.equalsIgnoreCase(lang) || "Espanol".equalsIgnoreCase(lang)) rbEspanol.setSelected(true);
            else rbDoblada.setSelected(true);
        }
        actualizarDatosSala();
    }

    private static <T> StringConverter<T> convertidor(Function<T, String> texto) {
        return new StringConverter<>() {
            @Override public String toString(T t) { return t == null ? "" : texto.apply(t); }
            @Override public T fromString(String s) { return null; }
        };
    }

    /** «Datos de la sala»: formato, capacidad, duración y hora aproximada de fin. */
    private void actualizarDatosSala() {
        Sala sala = cmbSala.getValue();
        Pelicula p = cmbPelicula.getValue();
        lblFormato.setText(sala == null ? "—" : sala.getTipoSala().getFormato());
        lblCapacidad.setText(sala == null ? "—" : sala.getCapacidad() + " butacas");
        lblDuracion.setText(p == null ? "—" : p.getDuracion() + " min");
        String termina = "—";
        if (p != null) {
            try {
                DateTime f = Formato.leerFecha(txtFecha.getText(), "Fecha");
                DateTime h = Formato.leerHora(txtHora.getText(), "Hora");
                termina = new DateTime(f.getDia(), f.getMes(), f.getAnio(), h.getHora(), h.getMinuto())
                        .sumarMinutos(p.getDuracion()).soloHora();
            } catch (IllegalArgumentException e) {
                termina = "—"; // fecha u hora aún incompletas
            }
        }
        lblTermina.setText(termina);
    }

    @FXML
    private void onGuardar() {
        DateTime fecha;
        DateTime hora;
        try {
            fecha = Formato.leerFecha(txtFecha.getText(), "Fecha");
            hora = Formato.leerHora(txtHora.getText(), "Hora de inicio");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensaje, e.getMessage());
            return;
        }
        String idioma = rbSubtitulada.isSelected() ? SUBTITULADA
                : rbEspanol.isSelected() ? ESPANOL : DOBLADA;
        Respuesta<?> r = editando == null
                ? app().adminFunciones().programarFuncion(admin, cmbPelicula.getValue(), cmbSala.getValue(), fecha, hora, idioma)
                : app().adminFunciones().editarFuncion(admin, editando, fecha, hora, idioma);
        if (!mostrar(lblMensaje, r)) return;
        nav.ir("admin-funciones");
    }

    @FXML
    private void onVolver() {
        nav.ir("admin-funciones");
    }
}
