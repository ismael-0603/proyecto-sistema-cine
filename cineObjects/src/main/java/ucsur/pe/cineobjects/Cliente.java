package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author user
 */
public class Cliente extends Persona {
    private static final int VISITAS_POR_NIVEL = 10;
    private static final int NIVEL_MAXIMO = 4;

    private int idCliente;
    private DateTime fechaRegistro;
    private int puntos = 0;
    private int nivelSocio = 0;
    private boolean esInvitado;
    private List<Compra> historialCompras;

    public Cliente(int idCliente, String dni, String nombres, String apellidos, DateTime fechaNac,
                   String correo, String telefono, String contra) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra);
        this.idCliente = idCliente;
        this.fechaRegistro = DateTime.ahora();
        this.puntos = 0;
        this.nivelSocio = 1; // todo cliente registrado inicia en nivel 1
        this.esInvitado = false;
        this.historialCompras = new ArrayList<>();
    }

    private Cliente(String correo) {
        super(null, null, null, null, correo, null, null);
        this.fechaRegistro = DateTime.ahora();
        this.puntos = 0;
        this.nivelSocio = 0;
        this.esInvitado = true;
        this.historialCompras = new ArrayList<>();
    }

    // Cumple ck_client_registrado de la BD
    public boolean registrarse() {
        if (this.dni != null && this.dni.length() == 8 && this.nombres != null &&
            this.apellidos != null && this.contra != null && this.correo != null) {
            this.esInvitado = false;
            this.estado = true;
            this.fechaRegistro = DateTime.ahora();
            if (this.nivelSocio == 0) this.nivelSocio = 1;
            return true;
        }
        return false;
    }

    public static Cliente ingresarComoInvitado(String correo) {
        return new Cliente(correo);
    }

    public boolean esSocio() {
        return !this.esInvitado && this.nivelSocio > 0 && Boolean.TRUE.equals(this.estado);
    }

    // PUNTOS (solo saldo canjeable; no influyen en el nivel) ----------
    public boolean acumularPuntos(int cant) {
        if (cant <= 0 || this.esInvitado) return false;
        this.puntos += cant;
        return true;
    }

    public boolean canjearPuntos(int cant) {
        if (cant <= 0 || this.esInvitado || this.puntos < cant) return false;
        this.puntos -= cant;
        return true;
    }

    public int consultarPuntos() {
        return this.puntos;
    }

    // NIVEL DE SOCIO(por visitas del año) 
    // Visita = compra CONFIRMADA dentro del año calendario actual
    public int contarVisitasAnioActual() {
        int anioActual = DateTime.ahora().getAnio();
        int visitas = 0;
        for (Compra c : historialCompras) {
            if (Compra.CONFIRMADA.equals(c.getEstado()) && c.getFechaHora().getAnio() == anioActual) {
                visitas++;
            }
        }
        return visitas;
    }

    // El nivel solo sube, Cada 10 visitas en el año equivalen a +1 nivel.
    public void actualizarNivel() {
        if (this.esInvitado) return;
        int nivelPorVisitas = Math.min(1 + contarVisitasAnioActual() / VISITAS_POR_NIVEL, NIVEL_MAXIMO);
        if (nivelPorVisitas > this.nivelSocio) {
            this.nivelSocio = nivelPorVisitas;
        }
    }

    // COMPRAS
    public List<Compra> consultarHistorial() {
        return new ArrayList<>(this.historialCompras);
    }

    public Compra realizarCompra() {
        Compra nuevaCompra = new Compra(0, DateTime.ahora(), this); // el id real lo genera la BD
        this.historialCompras.add(nuevaCompra);
        return nuevaCompra;
    }

    public int getIdCliente() { return idCliente; }
    public DateTime getFechaRegistro() { return fechaRegistro; }
    public int getPuntos() { return puntos; }
    public int getNivelSocio() { return nivelSocio; }
    public boolean isEsInvitado() { return esInvitado; }

    @Override
    public String toString() {
        return esInvitado ? "Invitado (" + correo + ")" : getNombreCompleto() + " - nivel " + nivelSocio;
    }
}
