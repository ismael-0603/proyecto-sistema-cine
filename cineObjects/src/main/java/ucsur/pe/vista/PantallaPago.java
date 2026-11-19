package ucsur.pe.vista;

import javafx.scene.control.Label;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;

/**
 * Lo común a «Datos de tarjeta» (8.1) y «Pago con billetera» (8.2): pedir el pago al controller y,
 * si se rechaza, mostrar el aviso 8.5 con el motivo; las butacas siguen reservadas mientras quede tiempo.
 */
public abstract class PantallaPago extends Pantalla {

    protected PantallaPago(Navegador nav) {
        super(nav);
    }

    protected Compra compra() {
        return estado().getCompra();
    }

    /** El monto es el total que calculó la compra; la pasarela (simulada) decide si aprueba. */
    protected void pagar(String medioPago) {
        Respuesta<TransaccionPago> r = app().pago().pagar(compra(), compra().getTotalCentavos(), medioPago);
        if (r.ok()) {
            estado().setMedioPago(medioPago);
            nav.compraPagada();
            return;
        }
        DialogoVista d = nav.dialogo();
        d.configurar("No pudimos procesar tu pago", "El medio de pago fue rechazado. No se hizo ningún cobro.", true);
        Label motivo = new Label("Motivo: " + (r.mensaje() == null ? "sin detalle" : r.mensaje()));
        motivo.getStyleClass().addAll("aviso", "nota");
        motivo.setWrapText(true);
        motivo.setMaxWidth(Double.MAX_VALUE);
        Label quedan = new Label("Tus butacas siguen reservadas: quedan " + estado().tiempoRestante());
        quedan.getStyleClass().add("etiqueta-fuerte");
        quedan.setMaxWidth(Double.MAX_VALUE);
        d.agregarContenido(motivo, quedan);
        d.botonSecundario("Cambiar método", () -> nav.ir("resumen-pago"));
        d.botonPrincipal("Reintentar pago", nav::cerrarModal);
        d.enlace("Cancelar compra", nav::cancelarCompra);
    }
}
