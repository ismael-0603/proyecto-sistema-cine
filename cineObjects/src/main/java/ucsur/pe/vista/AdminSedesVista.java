package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Sede;
import java.util.ArrayList;
import java.util.List;

/** 25 · Sedes: listar, crear, editar, poner en mantenimiento, dar de baja y volver a activar. */
public class AdminSedesVista extends PantallaAdmin {

    private static final String TODOS = "Estado";

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;
    @FXML private VBox panelForm;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDistrito;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtHorario;
    @FXML private Label lblMensajeForm;

    private final Tabla<Sede> tabla = new Tabla<>(
            new String[] {"Sede", "Distrito", "Dirección", "Salas", "Estado", "Acciones"},
            new double[] {170, 140, 225, 65, 125}, 295, 8);
    private List<Sede> sedes = new ArrayList<>();
    private Sede editando;

    public AdminSedesVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("sedes")) return;
        contenedorTabla.getChildren().add(tabla.vista());
        cmbEstado.setItems(FXCollections.observableArrayList(TODOS, "Activa", "En mantenimiento", "Inactiva"));
        cmbEstado.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((o, a, b) -> filtrar(false));
        cmbEstado.valueProperty().addListener((o, a, b) -> filtrar(false));
        cargar(false);
    }

    private void cargar(boolean mantenerPagina) {
        Respuesta<List<Sede>> r = app().adminInfraestructura().listarSedes(admin);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        sedes = r.dato();
        filtrar(mantenerPagina);
    }

    private void filtrar(boolean mantenerPagina) {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String est = TODOS.equals(cmbEstado.getValue()) ? null : cmbEstado.getValue();
        tabla.mostrar(sedes.stream()
                .filter(s -> buscado.isEmpty() || s.getNombre().toLowerCase().contains(buscado)
                        || s.getDistrito().toLowerCase().contains(buscado))
                .filter(s -> est == null || Formato.capitalizar(s.getEstado()).equals(est))
                .toList(), this::fila, mantenerPagina);
    }

    private List<Node> fila(Sede s) {
        boolean activa = Sede.ACTIVA.equals(s.getEstado());
        boolean inactiva = Sede.INACTIVA.equals(s.getEstado());
        Button principal = inactiva
                ? Tabla.boton("Activar", true, () -> despues(app().adminInfraestructura().cambiarEstadoSede(admin, s, Sede.ACTIVA)))
                : Tabla.boton("Dar de baja", false, () -> confirmarBaja(s));
        Button mantenimiento = activa
                ? Tabla.boton("Mantenimiento", false, () -> despues(app().adminInfraestructura().cambiarEstadoSede(admin, s, Sede.EN_MANTENIMIENTO)))
                : Tabla.boton("Reabrir", false, () -> despues(app().adminInfraestructura().cambiarEstadoSede(admin, s, Sede.ACTIVA)));
        mantenimiento.setVisible(!inactiva);
        mantenimiento.setManaged(!inactiva);
        return List.of(Tabla.texto(s.getNombre()), Tabla.texto(s.getDistrito()), Tabla.texto(s.getDireccion()),
                Tabla.texto(String.valueOf(s.getSalas().size())),
                Tabla.chip(Formato.capitalizar(s.getEstado()), activa ? "pagada" : "inactiva"),
                Tabla.acciones(Tabla.boton("Editar", false, () -> abrirForm(s)), principal, mantenimiento));
    }

    private void confirmarBaja(Sede s) {
        DialogoVista d = nav.dialogo();
        d.configurar("¿Dar de baja «" + s.getNombre() + "»?",
                "La sede queda inactiva: deja de aparecer al elegir sede y no se venden entradas en ella. "
                        + "Puedes volver a activarla después.", false);
        d.botonSecundario("Volver", nav::cerrarModal);
        d.botonPrincipal("Dar de baja", () -> {
            nav.cerrarModal();
            despues(app().adminInfraestructura().darDeBajaSede(admin, s));
        });
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar(true);
    }

    @FXML
    private void onNueva() {
        abrirForm(null);
    }

    private void abrirForm(Sede s) {
        editando = s;
        lblFormTitulo.setText(s == null ? "Nueva sede" : "Editar sede «" + s.getNombre() + "»");
        txtNombre.setText(s == null ? "" : s.getNombre());
        txtDistrito.setText(s == null ? "" : s.getDistrito());
        txtDireccion.setText(s == null ? "" : s.getDireccion());
        txtTelefono.setText(s == null ? "" : s.getTelefono());
        txtHorario.setText(s == null ? "" : s.getHorarioAtencion());
        limpiar(lblMensajeForm);
        panelForm.setVisible(true);
        panelForm.setManaged(true);
        txtNombre.requestFocus();
    }

    @FXML
    private void onCancelar() {
        editando = null;
        panelForm.setVisible(false);
        panelForm.setManaged(false);
    }

    @FXML
    private void onGuardar() {
        Respuesta<?> r = editando == null
                ? app().adminInfraestructura().registrarSede(admin, txtNombre.getText(), txtDireccion.getText(), txtDistrito.getText(),
                        txtTelefono.getText(), txtHorario.getText())
                : app().adminInfraestructura().actualizarSede(admin, editando, txtNombre.getText(), txtDireccion.getText(),
                        txtDistrito.getText(), txtTelefono.getText(), txtHorario.getText());
        if (!r.ok()) {
            mostrarError(lblMensajeForm, r.mensaje());
            return;
        }
        onCancelar();
        mostrarOk(lblMensaje, r.mensaje());
        cargar(true);
    }
}
