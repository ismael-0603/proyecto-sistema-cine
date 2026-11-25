package ucsur.pe.vista;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
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

    // ---------- Campos con dato incorrecto (borde rojo, ver .campo-error en estilos.css) ----------

    private static final String CAMPO_ERROR = ValidacionEnVivo.CAMPO_ERROR;

    /** Validación en vivo de un formulario cuyo mensaje va en 'mensaje' (ver ValidacionEnVivo). */
    protected ValidacionEnVivo validacion(Label mensaje) {
        return new ValidacionEnVivo(mensaje);
    }

    /** Pinta de rojo el borde del campo; el rojo se quita solo cuando el usuario lo vuelve a editar. */
    protected void marcarError(Control campo) {
        if (campo == null || campo.getStyleClass().contains(CAMPO_ERROR)) return;
        campo.getStyleClass().add(CAMPO_ERROR);
    }

    /** Quita la marca roja de los campos indicados. */
    protected void limpiarErrores(Control... campos) {
        for (Control c : campos) if (c != null) c.getStyleClass().remove(CAMPO_ERROR);
    }

    /** Hace que cada campo pierda su marca roja en cuanto el usuario cambia su contenido. */
    protected void quitarErrorAlEditar(Control... campos) {
        for (Control c : campos) {
            if (c instanceof TextInputControl t) t.textProperty().addListener((o, a, n) -> limpiarErrores(t));
            else if (c instanceof CheckBox cb) cb.selectedProperty().addListener((o, a, n) -> limpiarErrores(cb));
        }
    }

    /** Para ValidacionEnVivo y los diálogos: el mismo mensaje rojo (error) o verde que el resto de pantallas. */
    static void ponerMensaje(Label mensaje, String texto, boolean error) {
        cambiarMensaje(mensaje, texto, error ? ERROR : OK);
    }

    private static void cambiarMensaje(Label mensaje, String texto, String estilo) {
        if (mensaje == null) return;
        mensaje.getStyleClass().removeAll(OK, ERROR);
        mensaje.getStyleClass().add(estilo);
        mensaje.setText(texto == null ? "" : texto);
        mensaje.setVisible(texto != null && !texto.isEmpty());
        mensaje.setManaged(mensaje.isVisible());
    }
}
