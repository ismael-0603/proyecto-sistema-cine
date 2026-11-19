package ucsur.pe.vista;

import javafx.scene.control.Label;
import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;

/**
 * Por qué existe: lo común a todos los controllers de pantalla (los de los archivos FXML).
 * Como el controller del ejemplo MVCone, solo leen lo que el usuario escribió, piden la acción
 * y muestran el resultado; aquí la acción se pide a los controllers de App, que responden con
 * Respuesta (ok, mensaje, dato), así que la pantalla nunca toca servicios ni repositorios.
 */
public abstract class Pantalla {

    private static final String OK = "ok";
    private static final String ERROR = "error";

    protected final Navegador nav;

    protected Pantalla(Navegador nav) {
        this.nav = nav;
    }

    protected App app() { return nav.app(); }

    protected EstadoKiosco estado() { return nav.estado(); }

    /** Muestra el mensaje de la respuesta (verde si salió bien, rojo si no) y devuelve si salió bien. */
    protected boolean mostrar(Label mensaje, Respuesta<?> r) {
        if (r.ok()) mostrarOk(mensaje, r.mensaje());
        else mostrarError(mensaje, r.mensaje());
        return r.ok();
    }

    protected void mostrarOk(Label mensaje, String texto) {
        cambiarMensaje(mensaje, texto, OK);
    }

    protected void mostrarError(Label mensaje, String texto) {
        cambiarMensaje(mensaje, texto, ERROR);
    }

    protected void limpiar(Label mensaje) {
        cambiarMensaje(mensaje, "", OK);
    }

    private void cambiarMensaje(Label mensaje, String texto, String estilo) {
        if (mensaje == null) return;
        mensaje.getStyleClass().removeAll(OK, ERROR);
        mensaje.getStyleClass().add(estilo);
        mensaje.setText(texto == null ? "" : texto);
        mensaje.setVisible(texto != null && !texto.isEmpty());
        mensaje.setManaged(mensaje.isVisible());
    }
}
