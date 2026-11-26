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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Producto;
import java.io.File;
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
    @FXML private StackPane vistaImagen;
    @FXML private TextField txtImagen;

    private Producto editando;

    private static final double ANCHO_PREVIA = 250; // tamaño de la vista previa de imagen en el formulario
    private static final double ALTO_PREVIA = 157;

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
        Respuesta<List<Producto>> todos = app().adminProductos().listarProductos(admin);
        if (todos.ok()) todos.dato().forEach(p -> categorias.add(Formato.capitalizar(p.getCategoria())));
        cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
        cmbNivel.setItems(FXCollections.observableArrayList(AdminPromocionesVista.NIVELES));
        cmbNivel.getSelectionModel().selectFirst();
        txtImagen.textProperty().addListener((o, a, t) -> mostrarImagen());
        cmbCategoria.getEditor().textProperty().addListener((o, a, t) -> mostrarImagen());
        if (editando != null) cargarDatos();
        mostrarImagen();
    }

    /** Vista previa con la misma foto (o el recuadro con la categoría) que ve el cliente en la dulcería. */
    private void mostrarImagen() {
        String categoria = cmbCategoria.getEditor().getText();
        vistaImagen.getChildren().setAll(Formato.imagenProducto(vacioANulo(txtImagen.getText()),
                categoria == null || categoria.isBlank() ? "Foto" : categoria, ANCHO_PREVIA, ALTO_PREVIA));
    }

    @FXML
    private void onElegirImagen() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Elegir foto del producto");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"));
        File archivo = selector.showOpenDialog(txtImagen.getScene().getWindow());
        if (archivo != null) txtImagen.setText(archivo.toURI().toString());
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void cargarDatos() {
        lblTitulo.setText("Editar producto");
        txtNombre.setText(editando.getNombre());
        txtDescripcion.setText(editando.getDescripcion());
        cmbCategoria.setValue(Formato.capitalizar(editando.getCategoria()));
        txtPrecio.setText(Formato.dinero(editando.getPrecioCentavos()).replace("S/ ", ""));
        cmbNivel.getSelectionModel().select(Math.max(0, Math.min(editando.getNivelSocioMinimo(), 4)));
        chkCombo.setSelected(editando.isEsCombo());
        txtImagen.setText(editando.getImagen());
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
        String imagen = vacioANulo(txtImagen.getText());
        Producto producto;
        if (editando == null) {
            Respuesta<Producto> r = app().adminProductos().registrarProducto(admin, txtNombre.getText(), txtDescripcion.getText(),
                    categoria, precio, chkCombo.isSelected(), nivel, imagen, stock);
            if (!mostrar(lblMensaje, r)) return;
            producto = r.dato();
            editando = producto; // si falla cambiarEstado abajo, reintentar actualiza en lugar de duplicar
            lblTitulo.setText("Editar producto");
            cajaStock.setVisible(false);
            cajaStock.setManaged(false);
        } else {
            Respuesta<Void> r = app().adminProductos().actualizarProducto(admin, editando, txtNombre.getText(),
                    txtDescripcion.getText(), categoria, precio, chkCombo.isSelected(), nivel, imagen);
            if (!mostrar(lblMensaje, r)) return;
            producto = editando;
        }
        if (producto.isEstado() != rbActivo.isSelected()) {
            Respuesta<Void> e = app().adminProductos().cambiarEstadoProducto(admin, producto, rbActivo.isSelected());
            if (!mostrar(lblMensaje, e)) return;
        }
        nav.ir("admin-productos");
    }

    @FXML
    private void onVolver() {
        nav.ir("admin-productos");
    }
}
