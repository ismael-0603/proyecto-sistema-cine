package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: es quien compra. Puede ser invitado (solo correo) o socio registrado,
 * que gana puntos y sube de nivel (1 a 4) según sus visitas del año.
 */
public class Cliente extends Persona {
    private static final int VISITAS_POR_NIVEL = 10; // visitas confirmadas del año que hacen subir un nivel
    private static final int NIVEL_MAXIMO = 4; // nivel de socio más alto

    private int idCliente;
    private DateTime fechaRegistro;
    private int puntos = 0; // saldo canjeable; no depende del nivel
    private int nivelSocio = 0; // 0 = invitado; 1 a 4 = socio, sube con las visitas del año
    private boolean esInvitado; // compra solo con correo, sin puntos ni nivel
    /** Compras realizadas por el cliente; se usa para consultar visitas y su historial. */
    private List<Compra> historialCompras;

    public Cliente(int idCliente, String dni, String nombres, String apellidos, DateTime fechaNac,
                   String correo, String telefono, String contra) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra);
        this.idCliente = idCliente;
        this.fechaRegistro = DateTime.ahora();
        this.puntos = 0;
        this.nivelSocio = 1; // Todo cliente registrado comienza en el primer nivel.
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

    /** Completa el registro si están presentes los datos obligatorios y activa la cuenta.
     * Un invitado que se registra pasa al nivel 1 y deja de tratarse como invitado.
     * @return true si el registro se completó; false si falta algún dato obligatorio.
     */
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

    /** Crea una cuenta temporal con correo, sin datos personales, puntos ni nivel de socio. */
    public static Cliente ingresarComoInvitado(String correo) {
        return new Cliente(correo);
    }

    /** Devuelve true solo para clientes registrados, activos y con nivel de socio. */
    public boolean esSocio() {
        return !this.esInvitado && this.nivelSocio > 0 && this.estado;
    }

    // Los puntos son un saldo canjeable independiente del nivel de socio.
    public boolean acumularPuntos(int cant) {
        if (cant <= 0 || this.esInvitado) return false;
        this.puntos += cant;
        return true;
    }

    /** Resta puntos únicamente si la cantidad es positiva y el saldo alcanza. */
    public boolean canjearPuntos(int cant) {
        if (cant <= 0 || this.esInvitado || this.puntos < cant) return false;
        this.puntos -= cant;
        return true;
    }

    /** Devuelve el saldo actual de puntos. */
    public int consultarPuntos() {
        return this.puntos;
    }

    // Una visita cuenta cuando la compra está confirmada y pertenece al año calendario actual.
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

    // Cada diez visitas del año suben un nivel; el nivel no baja y tiene un máximo de cuatro.
    /** Sube el nivel según las visitas confirmadas; nunca lo reduce ni supera el nivel 4. */
    public void actualizarNivel() {
        if (this.esInvitado) return;
        int nivelPorVisitas = Math.min(1 + contarVisitasAnioActual() / VISITAS_POR_NIVEL, NIVEL_MAXIMO);
        if (nivelPorVisitas > this.nivelSocio) {
            this.nivelSocio = nivelPorVisitas;
        }
    }

    /** Devuelve una copia del historial para que la lista interna no se modifique desde fuera. */
    public List<Compra> consultarHistorial() {
        return new ArrayList<>(this.historialCompras);
    }

    /** Crea una compra en proceso en la sede indicada, la agrega al historial y la devuelve. */
    public Compra realizarCompra(Sede sede) {
        Compra nuevaCompra = new Compra(0, DateTime.ahora(), this, sede); // el id real lo genera la BD
        this.historialCompras.add(nuevaCompra);
        return nuevaCompra;
    }

    /** Multiplicador de puntos según el nivel de socio: 1 = x1, 2 = x1.25, 3 = x1.5, 4 = x2. */
    public double obtenerMultiplicadorPuntos() {
        if (esInvitado) return 0.0;
        return switch (nivelSocio) {
            case 2 -> 1.25;
            case 3 -> 1.5;
            case 4 -> 2.0;
            default -> 1.0;
        };
    }

    /**
     * Aplica el multiplicador de su nivel a unos puntos base y redondea hacia abajo.
     * Solo un socio activo gana puntos; para invitados o cuentas inactivas devuelve cero.
     */
    public int aplicarMultiplicador(int puntosBase) {
        if (puntosBase <= 0 || !esSocio()) return 0;
        return (int) Math.floor(puntosBase * obtenerMultiplicadorPuntos());
    }

    /** Devuelve puntos canjeados (por ejemplo, tras un reembolso). */
    public void devolverPuntos(int cant) {
        if (cant > 0 && !esInvitado) this.puntos += cant;
    }

    /** Quita puntos ganados por una compra reembolsada; el saldo nunca queda negativo. */
    public void revertirPuntos(int cant) {
        if (cant > 0) this.puntos = Math.max(0, this.puntos - cant);
    }

    public int getIdCliente() { return idCliente; }
    public DateTime getFechaRegistro() { return fechaRegistro; }
    public int getNivelSocio() { return nivelSocio; }
    public boolean isEsInvitado() { return esInvitado; }

    @Override
    public String toString() {
        return esInvitado ? "Invitado (" + correo + ")" : getNombreCompleto() + " - nivel " + nivelSocio;
    }
}
