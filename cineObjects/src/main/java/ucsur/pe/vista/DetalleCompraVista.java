package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;

/**
 * 23 · Detalle de compra. Llega de dos lados: «Mis compras» (con sesión iniciada) o «Recuperar entradas»
 * (tras verificar el código enviado al correo). Permite ver el QR, reenviar ese comprobante e imprimirlo.
 */
public class DetalleCompraVista extends Pantalla {

    @FXML private Label lblTitulo;
    @FXML private HBox pestanas;
    @FXML private Button btnVolver;
    @FXML private Label lblSerie;
    @FXML private Label lblEstado;
    @FXML private Label lblFecha;
    @FXML private VBox filas;
    @FXML private HBox filaTotal;
    @FXML private Label lblMedio;
    @FXML private Label lblReembolso;
    @FXML private StackPane marcoQr;
    @FXML private Label lblCorreo;
    @FXML private Label lblMensaje;

    private Compra compra;
    private DetalleCompra detalle;
    private String correo;
    private boolean esRecuperada;

    public DetalleCompraVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        compra = estado().getCompraElegida();
        if (compra == null && estado().getCompraRecuperada() != null) {
            compra = estado().getCompraRecuperada();
            esRecuperada = true;
        }
        if (compra == null) {
            Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        Respuesta<DetalleCompra> r = app().emision().verDetalle(compra);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        detalle = r.dato();
        correo = compra.getCliente().getCorreo();
        if (esRecuperada) {
            lblTitulo.setText("Tus entradas");
            pestanas.setVisible(false);
            pestanas.setManaged(false);
            btnVolver.setText(estado().getComprasRecuperadas().size() > 1
                    ? "← Volver a mis compras" : "← Recuperar entradas");
        }
        lblSerie.setText("Compra " + detalle.getSerieNumero());
        lblEstado.setText(Formato.estadoCompra(detalle.getEstado()));
        lblEstado.getStyleClass().add(Compra.CONFIRMADA.equals(detalle.getEstado()) ? "pagada" : "reembolsada");
        lblFecha.setText("Comprado el " + Formato.fechaHora(detalle.getFechaCompra()));
        filas.getChildren().setAll(ResumenCompra.filasDetalle(detalle));
        filaTotal.getChildren().setAll(ResumenCompra.filaTotalPagado(detalle).getChildren());
        lblMedio.setText(detalle.getMedioPago() == null ? "" : "Pagado con " + detalle.getMedioPago());
        if (detalle.getMontoReembolsadoCentavos() > 0) {
            lblReembolso.setText("Función cancelada por el cine: se devolvieron "
                    + Formato.dinero(detalle.getMontoReembolsadoCentavos()) + " automáticamente.");
            lblReembolso.setVisible(true);
            lblReembolso.setManaged(true);
        }
        String codigo = detalle.getCodigosQR().isEmpty() ? detalle.getSerieNumero() : detalle.getCodigosQR().get(0);
        StackPane fondo = new StackPane(CodigoVisual.dibujar(codigo, 160));
        fondo.getStyleClass().add("qr-fondo");
        marcoQr.getChildren().setAll(fondo);
        lblCorreo.setText("Se enviará a " + (esRecuperada ? Formato.correoOculto(correo) : correo));
    }

    @FXML
    private void onEnviar() {
        Respuesta<Void> r = app().emision().enviarPorCorreo(compra, correo);
        mostrar(lblMensaje, r);
    }

    /** La impresora del kiosco se simula: se muestra el ticket con su QR en pantalla. */
    @FXML
    private void onImprimir() {
        Respuesta<String> r = app().emision().imprimir(compra);
        if (!r.ok()) {
            mostrarError(lblMensaje, r.mensaje());
            return;
        }
        nav.mostrarTicket(r.dato(), detalle);
    }

    @FXML
    private void onVolver() {
        if (esRecuperada) {
            if (estado().getComprasRecuperadas().size() <= 1) {
                estado().limpiarRecuperacion();
            }
            nav.ir("recuperar");
            return;
        }
        nav.ir("mis-compras");
    }

    @FXML
    private void onMisDatos() {
        nav.ir("mi-cuenta");
    }
}
