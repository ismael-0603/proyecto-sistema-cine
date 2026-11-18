package ucsur.pe.service;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;

/**
 * Por qué existe: es lo que devuelve el inicio de sesión. Guarda la cuenta ya con su tipo (Cliente o Empleado)
 * y dice a qué zona del kiosco entra (cliente, dulcería o administración). Así la vista no necesita
 * preguntar con instanceof qué clase de Persona llegó ni hacer casts: AutenticacionService sabe en qué
 * repositorio encontró la cuenta y arma la sesión con el tipo correcto.
 * Exactamente uno de los dos (cliente o empleado) tiene valor.
 */
public record Sesion(Cliente cliente, Empleado empleado) {

    /** Zona del kiosco a la que entra la cuenta. */
    public enum TipoCuenta { CLIENTE, DULCERIA, ADMINISTRADOR }

    public Sesion {
        if ((cliente == null) == (empleado == null)) {
            throw new IllegalArgumentException("La sesión es de un cliente o de un empleado");
        }
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

    public boolean esCliente() { return cliente != null; }

    /** La cuenta sin importar su tipo (para mostrar el nombre o editar teléfono y correo). */
    public Persona cuenta() {
        return cliente != null ? cliente : empleado;
    }

    @Override
    public String toString() {
        return cuenta().getNombreCompleto() + " (" + tipo() + ")";
    }
}
