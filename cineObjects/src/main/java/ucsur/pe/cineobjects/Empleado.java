package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: es el personal de una sede. Según su rol puede preparar y entregar pedidos de dulcería.
 */
public class Empleado extends Persona {
    private int idEmpleado;
    private String turno; // texto libre, máx. 20 caracteres
    private Rol rol;
    private Sede sede;
    /** Cola de pedidos que el empleado puede atender. */
    private List<Pedido> pedidosAsignados;

    public Empleado(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                    String correoElectronico, String telefono, String contra,
                    String turno, Rol rol, Sede sede) {
        super(dni, nombres, apellidos, fechaNac, correoElectronico, telefono, contra);
        if (rol == null) throw new IllegalArgumentException("El rol es obligatorio");
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (turno != null && turno.length() > 20) {
            throw new IllegalArgumentException("El turno no puede superar 20 caracteres");
        }
        this.idEmpleado = idEmpleado;
        this.turno = turno;
        this.rol = rol;
        this.sede = sede;
        this.pedidosAsignados = new ArrayList<>();
    }

    /** Devuelve una copia de la cola con pedidos pendientes o que ya se están preparando. */
    public List<Pedido> consultarPedidosPendientes() {
        List<Pedido> pendientes = new ArrayList<>();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.PENDIENTE.equals(p.getEstado()) || Pedido.EN_PREPARACION.equals(p.getEstado())) {
                pendientes.add(p);
            }
        }
        return pendientes;
    }

    /** Inicia la preparación del primer pedido pendiente, si existe. */
    public void prepararPedido() {
        validarPermisoPedidos();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.PENDIENTE.equals(p.getEstado())) {
                p.iniciarPreparacion(this);
                return;
            }
        }
    }

    /** Asigna el pedido indicado a la cola y comienza su preparación si está pendiente. */
    public void prepararPedido(Pedido pedido) {
        validarPermisoPedidos();
        if (pedido != null && Pedido.PENDIENTE.equals(pedido.getEstado())) {
            if (!pedidosAsignados.contains(pedido)) {
                pedidosAsignados.add(pedido);
            }
            pedido.iniciarPreparacion(this);
        }
    }

    /** Entrega el primer pedido listo que encuentre en su cola. */
    public void entregarPedido() {
        validarPermisoPedidos();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.LISTO.equals(p.getEstado())) {
                p.entregar();
                return;
            }
        }
    }

    /** Entrega el pedido indicado si ya está listo; ignora pedidos nulos o en otro estado. */
    public void entregarPedido(Pedido pedido) {
        validarPermisoPedidos();
        if (pedido != null && Pedido.LISTO.equals(pedido.getEstado())) {
            pedido.entregar();
        }
    }

    /** Impide preparar o entregar pedidos cuando el rol no tiene ese permiso. */
    private void validarPermisoPedidos() {
        if (!rol.tienePermiso(Rol.ATENDER_PEDIDO)) {
            throw new IllegalStateException("El rol " + rol.getNombre() + " no puede atender pedidos");
        }
    }

    public int getIdEmpleado() { return idEmpleado; }
    public String getTurno() { return turno; }
    public Rol getRol() { return rol; }
    public Sede getSede() { return sede; }
    public List<Pedido> getPedidosAsignados() { return new ArrayList<>(pedidosAsignados); }

    @Override
    public String toString() {
        return getNombreCompleto() + " - " + rol.getNombre();
    }
}
