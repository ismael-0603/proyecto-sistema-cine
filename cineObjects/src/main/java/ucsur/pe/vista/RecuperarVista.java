package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Persona;
import java.util.List;

/**
 * 24 · Recuperar entradas en dos pasos: 1) pedir un código de 6 dígitos al correo con el que se compró;
 * 2) validar el código para ver las compras confirmadas y abrir su detalle con QR.
 * Si el cliente ya inició sesión, ofrece ir directo a «Mis compras».
 */
public class RecuperarVista extends Pantalla {

    @FXML private VBox cajaConSesion;
    @FXML private VBox cajaFormulario;
    @FXML private Label lblInstruccion;
    @FXML private TextField txtCorreo;
    @FXML private Button btnEnviar;
    @FXML private VBox pasoCodigo;
    @FXML private TextField txtCodigo;
    @FXML private Label lblMensaje;
    @FXML private VBox cajaResultados;
    @FXML private Label lblResumenCorreo;
    @FXML private VBox listaCompras;

    private ValidacionEnVivo validacionCorreo;
    private ValidacionEnVivo validacionCodigo;

    public RecuperarVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        // Solo cifras y como máximo 6 en el código de verificación
        txtCodigo.textProperty().addListener((o, antes, ahora) -> {
            String limpio = ahora.replaceAll("\\D", "");
            if (limpio.length() > 6) limpio = limpio.substring(0, 6);
            if (!limpio.equals(ahora)) txtCodigo.setText(limpio);
        });
        validacionCorreo = validacion(lblMensaje)
                .campo(txtCorreo, () -> Persona.errorCorreo(txtCorreo.getText()));
        validacionCodigo = validacion(lblMensaje)
                .campo(txtCodigo, () -> txtCodigo.getText().trim().length() == 6
                        ? null : "Escribe el código de 6 dígitos que enviamos a tu correo");

        if (estado().clienteConSesion() != null) {
            mostrarSolo(cajaConSesion);
            return;
        }
        // Si vuelve desde el detalle de una de varias compras ya verificadas, sigue viendo su lista
        if (estado().getComprasRecuperadas().size() > 1) {
            mostrarResultados(estado().getComprasRecuperadas(), estado().getCorreoRecuperado());
            return;
        }
        estado().limpiarRecuperacion();
        mostrarSolo(cajaFormulario);
    }

    @FXML
    private void onEnviarCodigo() {
        if (!validacionCorreo.validarTodos()) return;
        Respuesta<Void> r = app().emision().solicitarCodigoRecuperacion(txtCorreo.getText());
        if (!mostrar(lblMensaje, r)) return;
        mostrarPasoCodigo(true);
        txtCodigo.requestFocus();
    }

    @FXML
    private void onVerificar() {
        if (!validacionCodigo.validarTodos()) return;
        String correo = txtCorreo.getText().trim();
        Respuesta<List<Compra>> r = app().emision().verificarCodigoRecuperacion(correo, txtCodigo.getText());
        if (!mostrar(lblMensaje, r)) {
            if (r.mensaje() != null && r.mensaje().toLowerCase().contains("código")) {
                validacionCodigo.marcar(txtCodigo, r.mensaje());
            }
            return;
        }
        List<Compra> compras = r.dato();
        estado().setComprasRecuperadas(compras, correo);
        if (compras.size() == 1) {
            estado().setCompraRecuperada(compras.get(0));
            nav.ir("detalle-compra");
            return;
        }
        mostrarResultados(compras, correo);
    }

    @FXML
    private void onOtroCorreo() {
        estado().limpiarRecuperacion();
        mostrarPasoCodigo(false);
        limpiar(lblMensaje);
        mostrarSolo(cajaFormulario);
        txtCorreo.requestFocus();
    }

    @FXML
    private void onMisCompras() {
        nav.ir("mis-compras");
    }

    @FXML
    private void onVolver() {
        estado().limpiarRecuperacion();
        nav.ir(estado().getSede() == null ? "elegir-sede" : "cartelera");
    }

    private void mostrarPasoCodigo(boolean visible) {
        pasoCodigo.setVisible(visible);
        pasoCodigo.setManaged(visible);
        btnEnviar.setVisible(!visible);
        btnEnviar.setManaged(!visible);
        txtCorreo.setDisable(visible);
        lblInstruccion.setText(visible
                ? "Revisa tu correo y escribe el código de 6 dígitos que te enviamos."
                : "Escribe el correo con el que compraste y te enviaremos un código de 6 dígitos.");
        if (!visible) {
            txtCodigo.clear();
            validacionCodigo.limpiar();
        }
    }

    private void mostrarResultados(List<Compra> compras, String correo) {
        lblResumenCorreo.setText("Compras asociadas a " + Formato.correoOculto(correo)
                + ". Elige una para ver su código QR o imprimirla.");
        listaCompras.getChildren().clear();
        for (Compra c : compras) {
            listaCompras.getChildren().add(filaCompra(c));
        }
        mostrarSolo(cajaResultados);
    }

    private HBox filaCompra(Compra c) {
        Funcion f = c.getEntradas().isEmpty() ? null : c.getEntradas().get(0).getFuncion();
        String titulo = f != null ? f.getPelicula().getTitulo() : "Solo dulcería";
        String fecha = Formato.fechaHoraCorta(f != null ? f.obtenerFechaHoraInicio() : c.getFechaHora());
        String detalle = c.getSede().getNombre()
                + (f != null ? " · Sala " + f.getSala().getNumero() : "")
                + " · " + c.getSerieNumero()
                + " · " + Formato.dinero(c.getTotalCentavos());

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("etiqueta-fuerte");
        Label lblSub = new Label(fecha + " · " + detalle);
        lblSub.getStyleClass().add("nota");
        lblSub.setWrapText(true);
        VBox info = new VBox(2, lblTitulo, lblSub);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnVer = new Button("Ver entradas");
        btnVer.getStyleClass().addAll("btn-primario", "btn-chico");
        btnVer.setMinWidth(Region.USE_PREF_SIZE);
        btnVer.setOnAction(e -> {
            estado().setCompraRecuperada(c);
            nav.ir("detalle-compra");
        });

        HBox fila = new HBox(16, info, btnVer);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("tabla-fila");
        return fila;
    }

    private void mostrarSolo(Node activa) {
        for (Node n : List.of(cajaConSesion, cajaFormulario, cajaResultados)) {
            boolean ver = n == activa;
            n.setVisible(ver);
            n.setManaged(ver);
        }
    }
}
