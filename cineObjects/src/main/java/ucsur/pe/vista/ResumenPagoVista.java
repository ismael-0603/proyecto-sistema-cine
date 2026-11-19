package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Promocion;
import java.util.List;

/** 8 · Resumen y pago: importes, cupón, promoción y elección del método de pago. */
public class ResumenPagoVista extends Pantalla {

    private enum Metodo { TARJETA, BILLETERA }

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private VBox filas;
    @FXML private HBox filaTotal;
    @FXML private TextField txtCupon;
    @FXML private ComboBox<Promocion> cmbPromocion;
    @FXML private Label lblMensaje;
    @FXML private Label lblSocio;
    @FXML private Button btnTarjeta;
    @FXML private Button btnBilletera;
    @FXML private Label lblPieTotal;
    @FXML private Button btnPagar;

    private Compra compra;
    private Metodo metodo;

    public ResumenPagoVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(3);
        compra = estado().getCompra();
        if (compra == null) {
            javafx.application.Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        Respuesta<List<Promocion>> promos = app().cartelera().consultarPromociones();
        if (promos.ok()) cmbPromocion.setItems(FXCollections.observableArrayList(promos.dato()));
        cmbPromocion.setConverter(new StringConverter<>() {
            @Override public String toString(Promocion p) { return p == null ? "" : p.getNombre(); }
            @Override public Promocion fromString(String s) { return null; }
        });
        boolean socio = estado().cliente() != null && estado().cliente().esSocio();
        lblSocio.setText(socio ? "Como socio, tus beneficios se validan al pagar."
                : "Beneficios de socio: inicia sesión como socio para usar tus puntos y las promociones exclusivas.");
        refrescar();
    }

    private void refrescar() {
        filas.getChildren().setAll(ResumenCompra.filasImporte(compra));
        filaTotal.getChildren().setAll(ResumenCompra.filaTotal(compra).getChildren());
        String total = Formato.dinero(compra.getTotalCentavos());
        lblPieTotal.setText(total);
        btnPagar.setText("Pagar " + total);
    }

    @FXML
    private void onAplicarCupon() {
        mostrar(lblMensaje, app().compra().aplicarCupon(compra, txtCupon.getText()));
        refrescar();
    }

    @FXML
    private void onAplicarPromocion() {
        if (cmbPromocion.getValue() == null) {
            mostrarError(lblMensaje, "Elige una promoción de la lista.");
            return;
        }
        mostrar(lblMensaje, app().compra().aplicarPromocion(compra, cmbPromocion.getValue()));
        refrescar();
    }

    @FXML
    private void onTarjeta() {
        elegir(Metodo.TARJETA);
    }

    @FXML
    private void onBilletera() {
        elegir(Metodo.BILLETERA);
    }

    private void elegir(Metodo m) {
        metodo = m;
        btnTarjeta.getStyleClass().remove("activo");
        btnBilletera.getStyleClass().remove("activo");
        btnTarjeta.setText((m == Metodo.TARJETA ? "◉" : "◯") + "   💳  Tarjeta de crédito o débito");
        btnBilletera.setText((m == Metodo.BILLETERA ? "◉" : "◯") + "   📱  Billetera digital");
        (m == Metodo.TARJETA ? btnTarjeta : btnBilletera).getStyleClass().add("activo");
        limpiar(lblMensaje);
    }

    @FXML
    private void onPagar() {
        if (metodo == null) {
            mostrarError(lblMensaje, "Elige un método de pago.");
            return;
        }
        nav.ir(metodo == Metodo.TARJETA ? "pago-tarjeta" : "pago-billetera");
    }

    @FXML
    private void onVolver() {
        nav.ir("dulceria");
    }
}
