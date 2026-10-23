package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author user
 */
public class Empleado extends Persona {
    private int idEmpleado;
    private String turno;
    private Rol rol;
    private Sede sede;
    private List<Pedido> pedidosAsignados;

    public Empleado(int idEmpleado, String dni, String nombres, String apellidos, DateTime fechaNac,
                    String correoElectronico, String telefono, String contra,
                    String turno, Rol rol, Sede sede) {
        super(dni, nombres, apellidos, fechaNac, correoElectronico, telefono, contra);
        if (rol == null) throw new IllegalArgumentException("El rol es obligatorio"); // esto se adapto por la BD
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria"); // esto se adapto por la BD
        if (turno != null && turno.length() > 20) {
            throw new IllegalArgumentException("El turno no puede superar 20 caracteres");
        }
        this.idEmpleado = idEmpleado;
        this.turno = turno;
        this.rol = rol;
        this.sede = sede;
        this.pedidosAsignados = new ArrayList<>();
    }

    public String consultarTurno() {
        return this.turno;
    }

    public List<Pedido> consultarPedidosPendientes() {
        List<Pedido> pendientes = new ArrayList<>();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.PENDIENTE.equals(p.getEstado()) || Pedido.EN_PREPARACION.equals(p.getEstado())) {
                pendientes.add(p);
            }
        }
        return pendientes;
    }

    // Toma el primer pedido PENDIENTE de su cola
    public void prepararPedido() {
        validarPermisoPedidos();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.PENDIENTE.equals(p.getEstado())) {
                p.iniciarPreparacion(this);
                return;
            }
        }
    }

    // Sobrecarga: asigna y prepara un pedido específico
    public void prepararPedido(Pedido pedido) {
        validarPermisoPedidos();
        if (pedido != null && Pedido.PENDIENTE.equals(pedido.getEstado())) {
            if (!pedidosAsignados.contains(pedido)) {
                pedidosAsignados.add(pedido);
            }
            pedido.iniciarPreparacion(this);
        }
    }

    // Entrega el primer pedido LISTO de su cola
    public void entregarPedido() {
        validarPermisoPedidos();
        for (Pedido p : pedidosAsignados) {
            if (Pedido.LISTO.equals(p.getEstado())) {
                p.entregar();
                return;
            }
        }
    }

    public void entregarPedido(Pedido pedido) {
        validarPermisoPedidos();
        if (pedido != null && Pedido.LISTO.equals(pedido.getEstado())) {
            pedido.entregar();
        }
    }

    // Solo quien tiene permiso de atender pedidos puede prepararlos o entregarlos
    private void validarPermisoPedidos() {
        if (!rol.tienePermiso(Rol.ATENDER_PEDIDO)) {
            throw new IllegalStateException("El rol " + rol.getNombre() + " no puede atender pedidos");
        }
    }

    public int getIdEmpleado() { return idEmpleado; }
    public String getTurno() { return turno; }
    public Rol getRol() { return rol; }
    public Sede getSede() { return sede; }
    public List<Pedido> getPedidosAsignados() { return pedidosAsignados; }

    @Override
    public String toString() {
        return getNombreCompleto() + " - " + rol.getNombre();
    }
}
