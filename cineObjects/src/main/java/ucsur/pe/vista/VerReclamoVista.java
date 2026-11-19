package ucsur.pe.vista;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Reclamo;

/** 13.1 · Ver reclamo: lo que escribió el cliente y, si ya hay, la respuesta del cine. */
public class VerReclamoVista extends Pantalla {

    @FXML private Label lblTitulo;
    @FXML private Label lblEstado;
    @FXML private Label lblCompra;
    @FXML private Label lblTipo;
    @FXML private Label lblFecha;
    @FXML private Label lblDescripcion;
    @FXML private Label lblTituloRespuesta;
    @FXML private Label lblRespuesta;

    public VerReclamoVista(Navegador nav) {
        super(nav);
    }

    public void mostrar(Reclamo reclamo) {
        lblTitulo.setText("Reclamo #R-" + String.format("%04d", reclamo.getIdReclamo()));
        lblEstado.setText(Formato.estadoReclamo(reclamo.getEstado()));
        if (Reclamo.RESUELTO.equals(reclamo.getEstado())) lblEstado.getStyleClass().add("respondido");
        Compra c = reclamo.getCompra();
        Funcion f = c.getEntradas().isEmpty() ? null : c.getEntradas().get(0).getFuncion();
        lblCompra.setText("Compra " + c.getSerieNumero()
                + (f == null ? "" : " · " + f.getPelicula().getTitulo() + " · " + Formato.fechaHoraCorta(f.obtenerFechaHoraInicio())));
        lblTipo.setText(Formato.tipoReclamo(reclamo.getTipo()));
        lblFecha.setText(reclamo.getFecha() == null ? "—" : Formato.fechaHora(reclamo.getFecha()));
        lblDescripcion.setText(reclamo.getDescripcion());
        String respuesta = reclamo.getRespuesta();
        boolean hayRespuesta = respuesta != null && !respuesta.isBlank();
        lblTituloRespuesta.setText(Reclamo.RECHAZADO.equals(reclamo.getEstado()) ? "Motivo del rechazo" : "Respuesta del cine");
        lblRespuesta.setText(hayRespuesta ? respuesta : "Aún estamos revisando tu reclamo. Te avisaremos por correo.");
    }

    @FXML
    private void onCerrar() {
        nav.cerrarModal();
    }
}
