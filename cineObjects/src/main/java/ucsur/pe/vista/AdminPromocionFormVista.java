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
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Promocion;
import java.util.ArrayList;
import java.util.List;

/** 19.1 · Formulario de promoción: nueva o editar, con días de la semana, nivel de socio y estado. */
public class AdminPromocionFormVista extends PantallaAdmin {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private TextField txtValor;
    @FXML private TextField txtDesde;
    @FXML private TextField txtHasta;
    @FXML private FlowPane dias;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private Label lblMensaje;
    @FXML private RadioButton rbActiva;
    @FXML private RadioButton rbInactiva;

    private final List<CheckBox> casillasDias = new ArrayList<>();
    private List<String> tipos = List.of();
    private Promocion editando;

    public AdminPromocionFormVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("promociones")) return;
        editando = estado().getEnEdicion(Promocion.class);
        ToggleGroup estadoGrupo = new ToggleGroup();
        rbActiva.setToggleGroup(estadoGrupo);
        rbInactiva.setToggleGroup(estadoGrupo);
        Respuesta<List<String>> t = app().adminDescuentos().tiposDescuento();
        if (t.ok()) tipos = t.dato();
        cmbTipo.setItems(FXCollections.observableArrayList(tipos.stream().map(Formato::tipoDescuento).toList()));
        cmbTipo.getSelectionModel().selectFirst();
        cmbNivel.setItems(FXCollections.observableArrayList(AdminPromocionesVista.NIVELES));
        cmbNivel.getSelectionModel().selectFirst();
        for (String d : DIAS) {
            CheckBox c = new CheckBox(d);
            casillasDias.add(c);
            dias.getChildren().add(c);
        }
        if (editando != null) cargarDatos();
    }

    private void cargarDatos() {
        lblTitulo.setText("Editar promoción");
        txtNombre.setText(editando.getNombre());
        txtDescripcion.setText(editando.getDescripcion());
        cmbTipo.getSelectionModel().select(Math.max(0, tipos.indexOf(editando.getTipoDescuento())));
        txtValor.setText(AdminPromocionesVista.PORCENTAJE.equals(editando.getTipoDescuento())
                ? String.valueOf(editando.getValor()) : Formato.dinero(editando.getValor()).replace("S/ ", ""));
        if (editando.getFechaInicio() != null) txtDesde.setText(Formato.fecha(editando.getFechaInicio()));
        if (editando.getFechaFin() != null) txtHasta.setText(Formato.fecha(editando.getFechaFin()));
        for (int d : editando.getDiasAplicables()) casillasDias.get(d - 1).setSelected(true);
        cmbNivel.getSelectionModel().select(Math.max(0, Math.min(editando.getNivelSocioMinimo(), 4)));
        (editando.isEstado() ? rbActiva : rbInactiva).setSelected(true);
    }

    @FXML
    private void onGuardar() {
        String tipo = tipos.isEmpty() ? null : tipos.get(cmbTipo.getSelectionModel().getSelectedIndex());
        long valor;
        DateTime desde;
        DateTime hasta;
        try {
            valor = AdminPromocionesVista.leerValor(tipo, txtValor.getText());
            desde = Formato.leerFecha(txtDesde.getText(), "Vigencia desde");
            hasta = Formato.leerFecha(txtHasta.getText(), "Vigencia hasta");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensaje, e.getMessage());
            return;
        }
        List<Integer> diasElegidos = new ArrayList<>();
        for (int i = 0; i < casillasDias.size(); i++) if (casillasDias.get(i).isSelected()) diasElegidos.add(i + 1);
        int nivel = cmbNivel.getSelectionModel().getSelectedIndex();

        Promocion promo;
        if (editando == null) {
            Respuesta<Promocion> r = app().adminDescuentos().registrarPromocion(admin, txtNombre.getText(), txtDescripcion.getText(),
                    tipo, valor, desde, hasta, nivel, diasElegidos);
            if (!mostrar(lblMensaje, r)) return;
            promo = r.dato();
        } else {
            Respuesta<Void> r = app().adminDescuentos().actualizarPromocion(admin, editando, txtNombre.getText(),
                    txtDescripcion.getText(), tipo, valor, desde, hasta, nivel, diasElegidos);
            if (!mostrar(lblMensaje, r)) return;
            promo = editando;
        }
        // El estado se cambia aparte (activar o desactivar), solo si es distinto del actual
        if (promo.isEstado() != rbActiva.isSelected()) {
            Respuesta<Void> e = app().adminDescuentos().cambiarEstadoPromocion(admin, promo, rbActiva.isSelected());
            if (!mostrar(lblMensaje, e)) return;
        }
        nav.ir("admin-promociones");
    }

    @FXML
    private void onVolver() {
        nav.ir("admin-promociones");
    }
}
