package ucsur.pe.vista;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import java.util.List;

/**
 * Encabezado del flujo de compra: película y función elegidas, la cuenta regresiva de los 4 minutos,
 * el botón ✕ (con la confirmación 8.3) y la barra de pasos Asientos · Dulcería · Pago.
 */
public class EncabezadoCompraVista extends Pantalla {

    @FXML private Label lblTitulo;
    @FXML private Label lblDetalle;
    @FXML private VBox cajaTiempo;
    @FXML private Label lblTiempo;
    @FXML private HBox pasos;
    @FXML private Label num1, num2, num3, txt1, txt2, txt3;
    @FXML private HBox logo;

    private BooleanBinding urgente; // se guarda para que el enlace viva mientras viva la pantalla

    public EncabezadoCompraVista(Navegador nav) {
        super(nav);
    }

    @FXML
    private void initialize() {
        logo.setOnMouseClicked(e -> onLogo());
        Funcion f = estado().getFuncion();
        if (f != null) {
            lblTitulo.setText(f.getPelicula().getTitulo());
            lblDetalle.setText(Formato.detalleFuncion(f));
        }
        if (estado().cuentaActiva()) {
            var segundos = estado().segundosRestantesProperty();
            lblTiempo.textProperty().bind(Bindings.createStringBinding(estado()::tiempoRestante, segundos));
            urgente = segundos.lessThanOrEqualTo(60);
            urgente.addListener((obs, antes, ahora) -> marcarUrgente(ahora));
            marcarUrgente(urgente.get());
        } else {
            cajaTiempo.setVisible(false);
            cajaTiempo.setManaged(false);
        }
        pasos.setVisible(false);
        pasos.setManaged(false);
    }

    /** Muestra la barra de pasos con el paso actual (1 Asientos, 2 Dulcería, 3 Pago). */
    public void setPaso(int actual) {
        pasos.setVisible(true);
        pasos.setManaged(true);
        Label[] numeros = {num1, num2, num3};
        Label[] textos = {txt1, txt2, txt3};
        for (int i = 0; i < 3; i++) {
            int paso = i + 1;
            numeros[i].getStyleClass().removeAll("activo", "hecho");
            textos[i].getStyleClass().remove("activo");
            if (paso < actual) {
                numeros[i].setText("✓");
                numeros[i].getStyleClass().add("hecho");
            } else {
                numeros[i].setText(String.valueOf(paso));
                if (paso == actual) {
                    numeros[i].getStyleClass().add("activo");
                    textos[i].getStyleClass().add("activo");
                }
            }
        }
    }

    private void marcarUrgente(boolean si) {
        cajaTiempo.getStyleClass().remove("urgente");
        if (si) cajaTiempo.getStyleClass().add("urgente");
    }

    /** ✕: con una compra en curso pide confirmación (8.3); antes de empezarla solo vuelve a los horarios. */
    @FXML
    private void onCerrar() {
        if (!estado().cuentaActiva()) {
            nav.ir("horario");
            return;
        }
        confirmarCancelacion();
    }

    /** Logo: lleva a la cartelera; con una compra en curso primero pide la misma confirmación que ✕. */
    private void onLogo() {
        if (!estado().cuentaActiva()) {
            nav.cancelarCompra(); // aún no empezó la cuenta regresiva: no hay butacas que liberar
            return;
        }
        confirmarCancelacion();
    }

    private void confirmarCancelacion() {
        List<Entrada> entradas = estado().getCompra().getEntradas();
        List<Butaca> butacas = entradas.isEmpty() ? estado().getButacas()
                : entradas.stream().map(Entrada::getButaca).toList();
        String texto;
        if (butacas.isEmpty()) {
            texto = "Se cancelará la compra y no se te cobrará nada.";
        } else {
            String codigos = String.join(", ", butacas.stream().map(Butaca::getCodigoAsiento).toList());
            texto = "Se liberarán tus " + butacas.size() + " butaca" + (butacas.size() == 1 ? "" : "s")
                    + " (" + codigos + ") y se quitará lo que elegiste en dulcería. No se te cobrará nada.";
        }
        DialogoVista d = nav.dialogo();
        d.configurar("¿Cancelar la compra?", texto, false);
        d.botonSecundario("Seguir comprando", nav::cerrarModal);
        d.botonPrincipal("Sí, cancelar", nav::cancelarCompra);
    }
}
