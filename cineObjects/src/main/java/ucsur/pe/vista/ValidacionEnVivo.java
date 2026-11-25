package ucsur.pe.vista;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Por qué existe: marca en rojo un campo apenas su dato no cumple la regla, sin esperar al botón.
 * Un objeto por formulario (cada uno con su Label de mensaje), así dos formularios de la misma
 * pantalla no se mezclan.
 *
 * Cuándo se revisa: la primera vez al salir del campo (o al pulsar el botón, con validarTodos); desde
 * entonces, en cada tecla, para quitar el rojo en cuanto el dato queda bien. Así no se pinta de rojo
 * con el primer carácter. El motivo queda en el tooltip del campo; en el mensaje se ve el motivo si hay
 * un solo error y «Datos inválidos» si hay varios (salvo el del campo que tiene el foco).
 *
 * Las reglas salen del modelo (Persona.errorXxx, Empleado.errorPin): el mismo texto que lanzaría el modelo.
 */
final class ValidacionEnVivo {

    static final String CAMPO_ERROR = "campo-error";
    static final String VARIOS = "Datos inválidos";

    private final Label mensaje;
    private final Map<Control, Supplier<String>> reglas = new LinkedHashMap<>();
    private final Map<Control, List<Control>> gemelos = new LinkedHashMap<>(); // «ojo» de contraseña
    private final Set<Control> tocados = new HashSet<>();
    private final Map<Control, String> errores = new LinkedHashMap<>();
    private boolean mensajePropio; // el mensaje lo puso esta validación (y puede borrarlo)

    ValidacionEnVivo(Label mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * Valida el campo con la regla (devuelve el motivo o null). Los gemelos son otros controles que
     * muestran el mismo dato (la contraseña visible del «ojo»): se pintan junto con el campo.
     */
    ValidacionEnVivo campo(Control campo, Supplier<String> regla, Control... gemelos) {
        List<Control> todos = new ArrayList<>();
        todos.add(campo);
        for (Control g : gemelos) if (g != null) todos.add(g);
        reglas.put(campo, regla);
        this.gemelos.put(campo, todos);
        for (Control c : todos) {
            c.focusedProperty().addListener((o, antes, ahora) -> {
                if (!ahora) {
                    tocados.add(campo);
                    revisar(campo);
                } else {
                    actualizarMensaje();
                }
            });
        }
        alCambiar(campo, () -> { if (tocados.contains(campo)) revisar(campo); });
        return this;
    }

    /** Cuando cambia 'origen', vuelve a revisar 'destino' si ya fue tocado (p. ej. contraseña y su confirmación). */
    ValidacionEnVivo relacionar(Control origen, Control destino) {
        alCambiar(origen, () -> { if (tocados.contains(destino)) revisar(destino); });
        return this;
    }

    /** Revisa todos los campos (al pulsar el botón) y muestra el mensaje; devuelve true si no hay errores. */
    boolean validarTodos() {
        for (Control c : reglas.keySet()) {
            tocados.add(c);
            revisar(c);
        }
        return errores.isEmpty();
    }

    /**
     * Marca un error que solo se sabe al pulsar el botón (correo ya registrado, PIN actual incorrecto).
     * El mensaje lo muestra la pantalla; el rojo se va cuando el usuario edita el campo y su regla se cumple.
     */
    void marcar(Control campo, String motivo) {
        if (campo == null) return;
        tocados.add(campo);
        errores.put(campo, motivo);
        pintar(campo, motivo);
        mensajePropio = false;
    }

    /** Quita el rojo, los tooltips y el seguimiento: llamarlo después de vaciar los campos con clear(). */
    void limpiar() {
        Set<Control> marcados = new HashSet<>(reglas.keySet());
        marcados.addAll(errores.keySet());
        tocados.clear();
        errores.clear();
        for (Control c : marcados) pintar(c, null);
        if (mensajePropio) Pantalla.ponerMensaje(mensaje, "", false);
        mensajePropio = false;
    }

    private void revisar(Control campo) {
        String motivo = reglas.get(campo).get();
        if (motivo == null) errores.remove(campo);
        else errores.put(campo, motivo);
        pintar(campo, motivo);
        actualizarMensaje();
    }

    private void pintar(Control campo, String motivo) {
        for (Control c : gemelos.getOrDefault(campo, List.of(campo))) {
            c.getStyleClass().remove(CAMPO_ERROR);
            if (motivo != null) c.getStyleClass().add(CAMPO_ERROR);
            c.setTooltip(motivo == null ? null : new Tooltip(motivo));
        }
    }

    /** 0 errores: borra el mensaje (si lo puso esta validación); 1: su motivo; 2 o más: «Datos inválidos». */
    private void actualizarMensaje() {
        if (mensaje == null) return;
        if (errores.isEmpty()) {
            if (mensajePropio) Pantalla.ponerMensaje(mensaje, "", false);
            mensajePropio = false;
            return;
        }
        String texto = errores.size() == 1 ? errores.values().iterator().next() : VARIOS;
        for (Map.Entry<Control, String> e : errores.entrySet()) {
            for (Control c : gemelos.getOrDefault(e.getKey(), List.of(e.getKey()))) {
                if (c.isFocused()) texto = e.getValue();
            }
        }
        Pantalla.ponerMensaje(mensaje, texto, true);
        mensajePropio = true;
    }

    private static void alCambiar(Control c, Runnable accion) {
        if (c instanceof TextInputControl t) t.textProperty().addListener((o, a, n) -> accion.run());
        else if (c instanceof CheckBox cb) cb.selectedProperty().addListener((o, a, n) -> accion.run());
    }
}
