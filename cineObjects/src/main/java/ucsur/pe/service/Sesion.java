package ucsur.pe.service;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;

/**
 * Por qué existe: es lo que devuelve el inicio de sesión. Guarda la cuenta ya con su tipo (Cliente o Empleado)
 * y dice a qué zona del kiosco entra (cliente, dulcería o administración). Así la vista no necesita
 * preguntar con instanceof qué clase de Persona llegó ni hacer casts.
 * Un empleado tiene además un modo: EMPLEADO (su panel) o CLIENTE (compra con su cuenta personal).
 * El modo solo lo cambia AutenticacionService, que pide el PIN para volver al panel.
 */
public final class Sesion {

    /** Zona del kiosco a la que entra la cuenta. */
    public enum TipoCuenta { CLIENTE, DULCERIA, ADMINISTRADOR }

    /** Qué está usando el empleado en este momento. */
    public enum Modo { EMPLEADO, CLIENTE }

    private final Cliente cliente;    // cliente que inició sesión; null si es un empleado
    private final Empleado empleado;  // empleado que inició sesión; null si es un cliente
    private Modo modo;
    private int intentosPinFallidos;
    private boolean cerradaPorSeguridad;

    private Sesion(Cliente cliente, Empleado empleado) {
        if ((cliente == null) == (empleado == null)) {
            throw new IllegalArgumentException("La sesión es de un cliente o de un empleado");
        }
        this.cliente = cliente;
        this.empleado = empleado;
        this.modo = cliente != null ? Modo.CLIENTE : Modo.EMPLEADO;
    }

    public static Sesion deCliente(Cliente cliente) {
        return new Sesion(cliente, null);
    }

    public static Sesion deEmpleado(Empleado empleado) {
        return new Sesion(null, empleado);
    }

    /** El rol decide la zona del empleado: con permiso ADMINISTRAR va al panel; si no, a la cola de la dulcería. */
    public TipoCuenta tipo() {
        if (cliente != null) return TipoCuenta.CLIENTE;
        return empleado.esAdministrador() ? TipoCuenta.ADMINISTRADOR : TipoCuenta.DULCERIA;
    }

    public boolean esEmpleado() { return empleado != null; }

    public boolean enModoCliente() { return modo == Modo.CLIENTE; }

    /** La cuenta de cliente que compra ahora: el cliente, o la cuenta personal del empleado en modo cliente. */
    public Cliente cliente() {
        if (cliente != null) return cliente;
        return enModoCliente() ? empleado.getCuentaPersonal() : null;
    }

    /** El empleado de la sesión, esté en su panel o en modo cliente; null si es un cliente. */
    public Empleado empleado() { return empleado; }

    /** El empleado solo mientras está en su panel: lo que exigen las pantallas de dulcería y administración. */
    public Empleado empleadoEnPanel() {
        return modo == Modo.EMPLEADO ? empleado : null;
    }

    /** La cuenta sin importar su tipo (para mostrar el nombre o editar teléfono y correo). */
    public Persona cuenta() {
        return cliente != null ? cliente : empleado;
    }

    /** true si se cerró por escribir mal el PIN demasiadas veces: la vista debe cerrar la sesión. */
    public boolean cerradaPorSeguridad() { return cerradaPorSeguridad; }

    // ---------- Cambios de modo (solo AutenticacionService) ----------

    void pasarAModoCliente() {
        modo = Modo.CLIENTE;
    }

    void pasarAModoEmpleado() {
        modo = Modo.EMPLEADO;
        intentosPinFallidos = 0;
    }

    /** Suma un PIN equivocado y devuelve cuántos van seguidos. */
    int registrarPinFallido() {
        return ++intentosPinFallidos;
    }

    void cerrarPorSeguridad() {
        cerradaPorSeguridad = true;
    }

    @Override
    public String toString() {
        return cuenta().getNombreCompleto() + " (" + tipo() + ", modo " + modo + ")";
    }
}
