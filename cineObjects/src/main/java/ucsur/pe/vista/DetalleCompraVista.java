package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetalleCompra;

/**
 * 23 · Detalle de compra. Llega de dos lados: «Mis compras» (con la compra del cliente) o «Recuperar entradas»
 * (solo con el detalle que devolvió el código). En el segundo caso no hay sesión, así que reenviar usa el correo
 * del comprador y la impresión usa el texto del propio detalle.
 */
public class DetalleCompraVista extends Pantalla {

    @FXML private Label lblTitulo;
    @FXML private HBox pestanas;
    @FXML private javafx.scene.control.Button btnVolver;
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

    private Compra compra;          // si viene de «Mis compras»
    private DetalleCompra detalle;
    private String correo;

    public DetalleCompraVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        compra = estado().getCompraElegida();
        if (compra != null) {
            Respuesta<DetalleCompra> r = app().emision().verDetalle(compra);
            if (!r.ok()) {
                mostrarError(lblMensaje, r.mensaje());
                return;
            }
            detalle = r.dato();
            correo = compra.getCliente().getCorreo();
        } else if (estado().getDetalleRecuperado() != null) {
            detalle = estado().getDetalleRecuperado();
            correo = estado().getCorreoRecuperado();
            lblTitulo.setText("Tus entradas");
            pestanas.setVisible(false);
            btnVolver.setText("← Recuperar entradas");
        } else {
            Platform.runLater(() -> nav.ir("cartelera"));
            return;
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
        lblCorreo.setText("Se enviará a " + (compra != null ? correo : Formato.correoOculto(correo)));
    }

    @FXML
    private void onEnviar() {
        if (compra != null) {
            Respuesta<Void> r = app().emision().enviarPorCorreo(compra, correo);
            mostrar(lblMensaje, r);
        } else {
            mostrar(lblMensaje, app().emision().recuperarPorCorreo(correo));
        }
    }

    /** La impresora del kiosco se simula: se muestra el ticket en pantalla. */
    @FXML
    private void onImprimir() {
        String texto;
        if (compra != null) {
            Respuesta<String> r = app().emision().imprimir(compra);
            if (!r.ok()) {
                mostrarError(lblMensaje, r.mensaje());
                return;
            }
            texto = r.dato();
        } else {
            texto = detalle.generarTexto();
        }
        nav.mostrarTicket(texto);
    }

    @FXML
    private void onVolver() {
        nav.ir(compra != null ? "mis-compras" : "recuperar");
    }

    @FXML
    private void onMisDatos() {
        nav.ir("mi-cuenta");
    }
}
