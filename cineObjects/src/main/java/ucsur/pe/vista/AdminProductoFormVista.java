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
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Producto;
import java.util.List;
import java.util.TreeSet;

/** 20.1 · Formulario de producto: nuevo (con stock inicial en la sede del administrador) o editar. */
public class AdminProductoFormVista extends PantallaAdmin {

    @FXML private Label lblTitulo;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtPrecio;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private CheckBox chkCombo;
    @FXML private VBox cajaStock;
    @FXML private Label lblSede;
    @FXML private TextField txtStock;
    @FXML private Label lblMensaje;
    @FXML private RadioButton rbActivo;
    @FXML private RadioButton rbInactivo;

    private Producto editando;

    public AdminProductoFormVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("productos")) return;
        editando = estado().getEnEdicion(Producto.class);
        ToggleGroup estadoGrupo = new ToggleGroup();
        rbActivo.setToggleGroup(estadoGrupo);
        rbInactivo.setToggleGroup(estadoGrupo);
        lblSede.setText(admin.getSede().getNombre() + " (tu sede)");
        TreeSet<String> categorias = new TreeSet<>();
        Respuesta<List<Producto>> todos = app().admin().listarProductos(admin);
        if (todos.ok()) todos.dato().forEach(p -> categorias.add(Formato.capitalizar(p.getCategoria())));
        cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
        cmbNivel.setItems(FXCollections.observableArrayList(AdminPromocionesVista.NIVELES));
        cmbNivel.getSelectionModel().selectFirst();
        if (editando != null) cargarDatos();
    }

    private void cargarDatos() {
        lblTitulo.setText("Editar producto");
        txtNombre.setText(editando.getNombre());
        txtDescripcion.setText(editando.getDescripcion());
        cmbCategoria.setValue(Formato.capitalizar(editando.getCategoria()));
        txtPrecio.setText(Formato.dinero(editando.getPrecioCentavos()).replace("S/ ", ""));
        cmbNivel.getSelectionModel().select(Math.max(0, Math.min(editando.getNivelSocioMinimo(), 4)));
        chkCombo.setSelected(editando.isEsCombo());
        (editando.isEstado() ? rbActivo : rbInactivo).setSelected(true);
        // El stock se repone desde la lista («Reponer»), no aquí
        cajaStock.setVisible(false);
        cajaStock.setManaged(false);
    }

    @FXML
    private void onGuardar() {
        long precio;
        int stock = 0;
        try {
            precio = Formato.leerDinero(txtPrecio.getText(), "Precio");
            if (editando == null) stock = Formato.leerEntero(txtStock.getText(), "El stock inicial");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensaje, e.getMessage());
            return;
        }
        String categoria = cmbCategoria.getEditor().getText();
        int nivel = cmbNivel.getSelectionModel().getSelectedIndex();
        Producto producto;
        if (editando == null) {
            Respuesta<Producto> r = app().admin().registrarProducto(admin, txtNombre.getText(), txtDescripcion.getText(),
                    categoria, precio, chkCombo.isSelected(), nivel, stock);
            if (!mostrar(lblMensaje, r)) return;
            producto = r.dato();
        } else {
            Respuesta<Void> r = app().admin().actualizarProducto(admin, editando, txtNombre.getText(),
                    txtDescripcion.getText(), categoria, precio, chkCombo.isSelected(), nivel);
            if (!mostrar(lblMensaje, r)) return;
            producto = editando;
        }
        if (producto.isEstado() != rbActivo.isSelected()) {
            Respuesta<Void> e = app().admin().cambiarEstadoProducto(admin, producto, rbActivo.isSelected());
            if (!mostrar(lblMensaje, e)) return;
        }
        nav.ir("admin-productos");
    }

    @FXML
    private void onVolver() {
        nav.ir("admin-productos");
    }
}
