package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DescuentoBase;
import ucsur.pe.model.Promocion;
import java.util.List;

/**
 * 19 · Promociones y cupones. Pestaña «Cupones»: tabla y formulario al costado (tocar «Editar» lo llena;
 * tocar el estado lo activa o desactiva). Pestaña «Promociones»: tabla y formulario aparte (19.1).
 */
public class AdminPromocionesVista extends PantallaAdmin {

    static final String PORCENTAJE = "PORCENTAJE";
    static final String MONTO_FIJO = "MONTO_FIJO";
    static final List<String> NIVELES = List.of("Todos los clientes", "Cualquier socio", "Socio nivel 2+",
            "Socio nivel 3+", "Socio nivel 4");

    @FXML private Button btnNuevaPromo;
    @FXML private Button btnCupones;
    @FXML private Button btnPromos;
    @FXML private TextField txtBuscar;
    @FXML private Label lblMensaje;
    @FXML private VBox contenedorTabla;
    @FXML private Label lblAyuda;
    @FXML private VBox formCupon;
    @FXML private Label lblFormTitulo;
    @FXML private TextField txtCodigo;
    @FXML private RadioButton rbPorcentaje;
    @FXML private RadioButton rbMonto;
    @FXML private TextField txtValor;
    @FXML private TextField txtDesde;
    @FXML private TextField txtHasta;
    @FXML private TextField txtUsos;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private Label lblMensajeForm;

    private final Tabla<Cupon> tablaCupones = new Tabla<>(
            new String[] {"Código", "Tipo", "Valor", "Vigencia", "Usos", "Estado", "Acciones"},
            new double[] {115, 105, 80, 110, 80, 110}, 8); // más angosta: comparte el ancho con el formulario
    private final Tabla<Promocion> tablaPromos = new Tabla<>(
            new String[] {"Nombre", "Valor", "Días", "Vigencia", "Para", "Estado", "Acciones"},
            new double[] {200, 90, 170, 120, 150, 110}, 8);
    private boolean verCupones = true;
    private Cupon cuponEditando;

    public AdminPromocionesVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        if (!prepararAdmin("promociones")) return;
        ToggleGroup tipo = new ToggleGroup();
        rbPorcentaje.setToggleGroup(tipo);
        rbMonto.setToggleGroup(tipo);
        cmbNivel.setItems(FXCollections.observableArrayList(NIVELES));
        cmbNivel.getSelectionModel().selectFirst();
        txtBuscar.textProperty().addListener((o, a, b) -> cargar(false));
        mostrarPestana(true);
    }

    @FXML private void onCupones() { mostrarPestana(true); }
    @FXML private void onPromociones() { mostrarPestana(false); }

    private void mostrarPestana(boolean cupones) {
        verCupones = cupones;
        btnCupones.getStyleClass().remove("activo");
        btnPromos.getStyleClass().remove("activo");
        (cupones ? btnCupones : btnPromos).getStyleClass().add("activo");
        formCupon.setVisible(cupones);
        formCupon.setManaged(cupones);
        btnNuevaPromo.setVisible(!cupones);
        contenedorTabla.getChildren().setAll(cupones ? tablaCupones.vista() : tablaPromos.vista());
        lblAyuda.setText(cupones ? "Tocar el estado activa o desactiva el cupón. «Editar» llena el formulario con sus datos."
                : "Tocar el estado activa o desactiva la promoción. La promoción se aplica antes que el cupón.");
        cargar(false);
    }

    private void cargar(boolean mantenerPagina) {
        String buscado = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        if (verCupones) {
            Respuesta<List<Cupon>> r = app().admin().listarCupones(admin);
            if (!r.ok()) { mostrarError(lblMensaje, r.mensaje()); return; }
            tablaCupones.mostrar(r.dato().stream()
                    .filter(c -> buscado.isEmpty() || c.getCodigo().toLowerCase().contains(buscado)).toList(),
                    this::filaCupon, mantenerPagina);
        } else {
            Respuesta<List<Promocion>> r = app().admin().listarPromociones(admin);
            if (!r.ok()) { mostrarError(lblMensaje, r.mensaje()); return; }
            tablaPromos.mostrar(r.dato().stream()
                    .filter(p -> buscado.isEmpty() || p.getNombre().toLowerCase().contains(buscado)).toList(),
                    this::filaPromocion, mantenerPagina);
        }
    }

    // ---------- Cupones ----------

    private List<Node> filaCupon(Cupon c) {
        Button estadoBtn = botonEstado(c.isEstado(),
                () -> despues(app().admin().cambiarEstadoCupon(admin, c, !c.isEstado())));
        return List.of(Tabla.texto(c.getCodigo()), Tabla.texto(Formato.tipoDescuento(c.getTipoDescuento())),
                Tabla.texto(Formato.valorDescuento(c.getTipoDescuento(), c.getValor())),
                Tabla.texto(vigencia(c)), Tabla.texto(c.getUsosRealizados() + "/" + c.getLimiteUso()),
                estadoBtn, Tabla.acciones(Tabla.boton("Editar", false, () -> editarCupon(c))));
    }

    private void editarCupon(Cupon c) {
        cuponEditando = c;
        lblFormTitulo.setText("Editar cupón " + c.getCodigo());
        txtCodigo.setText(c.getCodigo());
        (PORCENTAJE.equals(c.getTipoDescuento()) ? rbPorcentaje : rbMonto).setSelected(true);
        txtValor.setText(PORCENTAJE.equals(c.getTipoDescuento()) ? String.valueOf(c.getValor())
                : Formato.dinero(c.getValor()).replace("S/ ", ""));
        txtDesde.setText(c.getFechaInicio() == null ? "" : Formato.fecha(c.getFechaInicio()));
        txtHasta.setText(c.getFechaFin() == null ? "" : Formato.fecha(c.getFechaFin()));
        txtUsos.setText(String.valueOf(c.getLimiteUso()));
        cmbNivel.getSelectionModel().select(Math.max(0, Math.min(c.getNivelSocioMinimo(), NIVELES.size() - 1)));
        limpiar(lblMensajeForm);
    }

    @FXML
    private void onLimpiar() {
        cuponEditando = null;
        lblFormTitulo.setText("Nuevo cupón");
        txtCodigo.clear();
        rbPorcentaje.setSelected(true);
        txtValor.clear();
        txtDesde.clear();
        txtHasta.clear();
        txtUsos.setText("100");
        cmbNivel.getSelectionModel().selectFirst();
        limpiar(lblMensajeForm);
    }

    @FXML
    private void onGuardarCupon() {
        String tipo = rbPorcentaje.isSelected() ? PORCENTAJE : MONTO_FIJO;
        long valor;
        DateTime desde;
        DateTime hasta;
        int usos;
        try {
            valor = leerValor(tipo, txtValor.getText());
            desde = Formato.leerFecha(txtDesde.getText(), "Vigencia desde");
            hasta = Formato.leerFecha(txtHasta.getText(), "Vigencia hasta");
            usos = Formato.leerEntero(txtUsos.getText(), "Usos máx.");
        } catch (IllegalArgumentException e) {
            mostrarError(lblMensajeForm, e.getMessage());
            return;
        }
        int nivel = cmbNivel.getSelectionModel().getSelectedIndex();
        Respuesta<?> r = cuponEditando == null
                ? app().admin().registrarCupon(admin, txtCodigo.getText(), tipo, valor, desde, hasta, usos, nivel)
                : app().admin().actualizarCupon(admin, cuponEditando, txtCodigo.getText(), tipo, valor, desde, hasta, usos, nivel);
        if (!mostrar(lblMensajeForm, r)) return;
        String mensaje = r.mensaje();
        onLimpiar();
        mostrarOk(lblMensajeForm, mensaje);
        cargar(true);
    }

    /** Porcentaje: número entero (20 = 20 %). Monto fijo: soles con decimales (5.50), que se guarda en céntimos. */
    static long leerValor(String tipo, String texto) {
        return PORCENTAJE.equals(tipo) ? Formato.leerEntero(texto, "El valor") : Formato.leerDinero(texto, "El valor");
    }

    // ---------- Promociones ----------

    private List<Node> filaPromocion(Promocion p) {
        Button estadoBtn = botonEstado(p.isEstado(),
                () -> despues(app().admin().cambiarEstadoPromocion(admin, p, !p.isEstado())));
        return List.of(Tabla.texto(p.getNombre()), Tabla.texto(Formato.valorDescuento(p.getTipoDescuento(), p.getValor())),
                Tabla.texto(Formato.diasAplicables(p.getDiasAplicables()).replace("Válida ", "")),
                Tabla.texto(vigencia(p)), Tabla.texto(Formato.nivelSocio(p.getNivelSocioMinimo())),
                estadoBtn, Tabla.acciones(Tabla.boton("Editar", false, () -> {
                    estado().setEnEdicion(p);
                    nav.ir("admin-promocion-form");
                })));
    }

    @FXML
    private void onNuevaPromocion() {
        estado().setEnEdicion(null);
        nav.ir("admin-promocion-form");
    }

    // ---------- Apoyo ----------

    private static Button botonEstado(boolean activo, Runnable cambiar) {
        Button b = Tabla.boton(activo ? "Activo" : "Inactivo", activo, cambiar);
        b.getStyleClass().add("btn-estado");
        return b;
    }

    /** «hasta 30/11», «desde 01/12» o «vencido», según la fecha de hoy. */
    private static String vigencia(DescuentoBase d) {
        DateTime hoy = DateTime.ahora();
        if (d.getFechaFin() != null && d.getFechaFin().toValorSoloFecha() < hoy.toValorSoloFecha()) return "vencido";
        if (d.getFechaInicio() != null && d.getFechaInicio().toValorSoloFecha() > hoy.toValorSoloFecha()) {
            return "desde " + String.format("%02d/%02d", d.getFechaInicio().getDia(), d.getFechaInicio().getMes());
        }
        return d.getFechaFin() == null ? "sin fin" : "hasta " + String.format("%02d/%02d", d.getFechaFin().getDia(), d.getFechaFin().getMes());
    }

    private void despues(Respuesta<?> r) {
        mostrar(lblMensaje, r);
        cargar(true);
    }
}
