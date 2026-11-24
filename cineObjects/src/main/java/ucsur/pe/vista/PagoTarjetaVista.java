package ucsur.pe.vista;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Persona;
import ucsur.pe.model.TransaccionPago;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

/**
 * 8.1 · Datos de tarjeta. La pantalla solo revisa que el formulario esté completo y bien escrito;
 * los datos no se guardan ni se envían a ningún lado (la pasarela es simulada y solo recibe el medio y el monto).
 */
public class PagoTarjetaVista extends PantallaPago {

    private static final String CREDITO = "Crédito";
    private static final String DEBITO = "Débito";

    @FXML private EncabezadoCompraVista encabezadoController;
    @FXML private TextField txtTitular;
    @FXML private TextField txtNumero;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<String> cmbMes;
    @FXML private ComboBox<String> cmbAnio;
    @FXML private PasswordField txtCvv;
    @FXML private ComboBox<String> cmbDocumento;
    @FXML private TextField txtDocumento;
    @FXML private CheckBox chkTerminos;
    @FXML private Label lblMensaje;
    @FXML private VBox resumen;
    @FXML private Label lblPieTotal;
    @FXML private Button btnPagar;

    public PagoTarjetaVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        encabezadoController.setPaso(3);
        if (compra() == null) {
            Platform.runLater(() -> nav.ir("cartelera"));
            return;
        }
        cmbTipo.setItems(FXCollections.observableArrayList(CREDITO, DEBITO));
        List<String> meses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) meses.add(String.format("%02d", m));
        cmbMes.setItems(FXCollections.observableArrayList(meses));
        List<String> anios = new ArrayList<>();
        int actual = DateTime.ahora().getAnio();
        for (int a = actual; a <= actual + 12; a++) anios.add(String.valueOf(a));
        cmbAnio.setItems(FXCollections.observableArrayList(anios));
        cmbDocumento.setItems(FXCollections.observableArrayList("DNI", "Carné de extranjería", "Pasaporte"));

        resumen.getChildren().add(ResumenCompra.tarjeta(estado().getFuncion(), compra(),
                "Conexión cifrada · Pasarela certificada"));
        String total = Formato.dinero(compra().getTotalCentavos());
        lblPieTotal.setText(total);
        btnPagar.setText("Pagar " + total);
    }

    @FXML
    private void onPagar() {
        String error = revisarFormulario();
        if (error != null) {
            mostrarError(lblMensaje, error);
            return;
        }
        limpiar(lblMensaje);
        pagar(CREDITO.equals(cmbTipo.getValue()) ? TransaccionPago.TARJETA_CREDITO : TransaccionPago.TARJETA_DEBITO);
    }

    /** Revisión de formato del formulario (trabajo de la pantalla); devuelve el primer problema o null. */
    private String revisarFormulario() {
        String numero = soloDigitos(txtNumero.getText());
        String titular = texto(txtTitular);
        if (titular.isEmpty()) return "Ingresa el nombre del titular.";
        // Misma regla que los nombres de Persona: rechaza "123456".
        if (!titular.matches(Persona.REGEX_NOMBRE)) {
            return "El nombre del titular solo puede tener letras.";
        }
        if (!numero.matches("\\d*")) return "El número de tarjeta solo puede tener dígitos.";
        if (numero.length() < 13 || numero.length() > 19) return "El número de tarjeta debe tener entre 13 y 19 dígitos.";
        if (cmbTipo.getValue() == null) return "Elige si la tarjeta es de crédito o débito.";
        if (cmbMes.getValue() == null || cmbAnio.getValue() == null) return "Elige el mes y el año de vencimiento.";
        DateTime hoy = DateTime.ahora();
        int mes = Integer.parseInt(cmbMes.getValue());
        int anio = Integer.parseInt(cmbAnio.getValue());
        if (anio == hoy.getAnio() && mes < hoy.getMes()) return "La tarjeta está vencida.";
        // El CVV se revisa tal cual se escribió ("12-3" no vale) y su largo depende de la marca:
        // American Express (empieza con 34 o 37) usa 4 dígitos; Visa, Mastercard y las demás, 3.
        boolean amex = numero.startsWith("34") || numero.startsWith("37");
        if (amex && !texto(txtCvv).matches("\\d{4}")) return "El CVV de American Express tiene 4 dígitos.";
        if (!amex && !texto(txtCvv).matches("\\d{3}")) return "El CVV tiene 3 dígitos.";
        if (cmbDocumento.getValue() == null) return "Elige el tipo de documento.";
        String documento = texto(txtDocumento);
        if (documento.isEmpty()) return "Ingresa tu número de documento.";
        if ("DNI".equals(cmbDocumento.getValue())) {
            if (!Persona.esDniValido(documento)) return "El DNI debe tener exactamente 8 dígitos.";
        } else if (!documento.matches("[A-Za-z0-9]{6,12}")) {
            return "El documento debe tener entre 6 y 12 letras o números.";
        }
        if (!chkTerminos.isSelected()) return "Debes aceptar los términos y condiciones.";
        return null;
    }

    private static String texto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("[\\s-]", "");
    }

    @FXML
    private void onVolver() {
        nav.ir("resumen-pago");
    }
}
