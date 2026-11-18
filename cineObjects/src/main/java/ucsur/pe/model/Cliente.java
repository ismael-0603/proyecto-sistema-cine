package ucsur.pe.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: es quien compra. Puede ser invitado (solo correo) o socio registrado,
 * que gana puntos y sube de nivel (1 a 4) según sus visitas del año.
 */
public class Cliente extends Persona {
    private static final int VISITAS_POR_NIVEL = 10; // visitas confirmadas del año que hacen subir un nivel
    public static final int NIVEL_MAXIMO = 4; // nivel de socio más alto; Producto también lo usa
    /** Multiplicador de puntos por nivel (posición = nivel): 0 = invitado, 1 = x1, 2 = x1.25, 3 = x1.5, 4 = x2. */
    private static final double[] MULTIPLICADOR_POR_NIVEL = {0.0, 1.0, 1.25, 1.5, 2.0};

    private int idCliente;
    private DateTime fechaRegistro;
    private int puntos = 0; // saldo canjeable; no depende del nivel
    private int nivelSocio = 0; // 0 = invitado; 1 a 4 = socio, sube con las visitas del año
    private boolean esInvitado; // compra solo con correo, sin puntos ni nivel
    /** Compras realizadas por el cliente; se usa para consultar visitas y su historial. */
    private List<Compra> historialCompras;

    public Cliente(int idCliente, String dni, String nombres, String apellidos, DateTime fechaNac,
                   String correo, String telefono, String contra, Reloj reloj) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra, reloj);
        exigirDatosCompletos(); // igual que ck_client_registrado en la BD: un socio necesita DNI, nombres, apellidos, correo y clave
        this.idCliente = idCliente;
        this.fechaRegistro = getReloj().ahora();
        this.puntos = 0;
        this.nivelSocio = 1; // Todo cliente registrado comienza en el primer nivel.
        this.esInvitado = false;
        this.historialCompras = new ArrayList<>();
    }

    private Cliente(String correo, Reloj reloj) {
        super(null, null, null, null, correo, null, null, reloj);
        exigirCorreo(); // RF-01: el invitado compra sin registrarse, pero necesita su correo (en la BD, correo NOT NULL)
        this.fechaRegistro = getReloj().ahora();
        this.puntos = 0;
        this.nivelSocio = 0;
        this.esInvitado = true;
        this.historialCompras = new ArrayList<>();
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un cliente (socio o invitado) con
     * su saldo de puntos, su nivel, su fecha de registro y si la cuenta está activa.
     * Su historial se llena después, al restaurar cada una de sus compras (Compra.restaurar).
     */
    public static Cliente restaurar(int idCliente, String dni, String nombres, String apellidos, DateTime fechaNac,
                                    String correo, String telefono, String contra, boolean estado,
                                    DateTime fechaRegistro, int puntos, int nivelSocio, boolean esInvitado,
                                    Reloj reloj) {
        if (fechaRegistro == null) {
            throw new IllegalArgumentException("La fecha de registro es obligatoria");
        }
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos"); // en la BD, ck_cliente_puntos
        }
        if (nivelSocio < 0 || nivelSocio > NIVEL_MAXIMO) {
            throw new IllegalArgumentException("Nivel de socio inválido: " + nivelSocio);
        }
        // Los constructores validan los datos personales (completos para un socio, solo el correo para un invitado)
        Cliente cliente = esInvitado
                ? new Cliente(correo, reloj)
                : new Cliente(idCliente, dni, nombres, apellidos, fechaNac, correo, telefono, contra, reloj);
        cliente.idCliente = idCliente;
        cliente.fechaRegistro = fechaRegistro;
        cliente.puntos = puntos;
        cliente.nivelSocio = nivelSocio;
        if (!estado) cliente.desactivar();
        return cliente;
    }

    /** Crea una cuenta temporal con correo, sin datos personales, puntos ni nivel de socio. */
    public static Cliente ingresarComoInvitado(String correo, Reloj reloj) {
        return new Cliente(correo, reloj);
    }

    /**
     * Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT. Todo cliente nuevo, socio o invitado,
     * nace con id 0; su compra necesita ese id en la BD (compra.id_cliente).
     */
    public void asignarId(int idCliente) {
        this.idCliente = Ids.asignar(this.idCliente, idCliente, "el cliente");
    }

    /**
     * Indica si el cliente alcanza un nivel de socio mínimo; 0 significa que no se exige nada.
     * Lo usan TipoEntrada y Producto para sus ofertas exclusivas, así la regla está en un solo lugar.
     */
    public boolean cumpleNivelSocio(int nivelMinimo) {
        return nivelMinimo <= 0 || (esSocio() && nivelSocio >= nivelMinimo);
    }

    /** Devuelve true solo para clientes registrados, activos y con nivel de socio. */
    public boolean esSocio() {
        return !this.esInvitado && this.nivelSocio > 0 && isEstado();
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
        int anioActual = getReloj().ahora().getAnio();
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

    /** Agrega al historial una compra de este cliente (la crea CompraService). */
    public void agregarCompra(Compra compra) {
        if (compra == null || compra.getCliente() != this) {
            throw new IllegalArgumentException("La compra no pertenece a este cliente");
        }
        if (!historialCompras.contains(compra)) {
            historialCompras.add(compra);
        }
    }

    /** Multiplicador de puntos según el nivel de socio: 1 = x1, 2 = x1.25, 3 = x1.5, 4 = x2. */
    public double obtenerMultiplicadorPuntos() {
        if (esInvitado) return 0.0;
        return MULTIPLICADOR_POR_NIVEL[nivelSocio];
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
    public boolean esInvitado() { return esInvitado; }

    @Override
    public String toString() {
        return esInvitado ? "Invitado (" + getCorreo() + ")" : getNombreCompleto() + " - nivel " + nivelSocio;
    }
}
