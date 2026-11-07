package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.ContextoDescuento;
import ucsur.pe.cineInterfaz.Descuento;
import ucsur.pe.cineInterfaz.Reloj;
import ucsur.pe.cineInterfaz.Totalizable;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: representa la compra de un cliente en una sede y controla su proceso de principio a fin
 * (4 minutos para completarla). Coordina y delega: asientos a SeleccionAsientos, dinero a CalculadoraImporte,
 * puntos a GestorPuntos y comprobante a Comprobante.
 */
public class Compra implements Totalizable{
    // Estados de una compra (constantes: se usan como Compra.NOMBRE y su valor no cambia).
    public static final String EN_PROCESO = "EN_PROCESO"; // el cliente aún está eligiendo y pagando
    public static final String CONFIRMADA = "CONFIRMADA"; // pagada; las entradas quedan vendidas
    public static final String CANCELADA = "CANCELADA"; // el cliente la canceló antes de pagar
    public static final String EXPIRADA = "EXPIRADA"; // se acabaron los 4 minutos sin pagar
    public static final String REEMBOLSADA = "REEMBOLSADA"; // se devolvió el dinero (función cancelada)

    public static final int TIEMPO_MAXIMO_COMPRA_MIN = 4; // minutos para completar toda la compra

    private int idCompra;
    private DateTime fechaHora;
    private DateTime expiraCompra; // Hora límite de todo el proceso (butacas, dulcería y pago); en la BD es compra.expira_compra.
    private String serieNumero;
    private ResumenImporte importe; // subtotal, descuento, IGV y total del último cálculo
    private int puntosCanjeados; // Puntos que cuestan los tipos de entrada elegidos; se descuentan solo al confirmar.
    private int puntosOtorgados; // Puntos ganados al confirmar; se revierten si se reembolsa.
    private String estado;
    private Cliente cliente;
    private Sede sede; // Sede donde se hace la compra; en la BD es compra.id_sede NOT NULL.
    /** Cupones y promociones aplicados; Compra los trata a todos como Descuento (la interfaz). */
    private final List<Descuento> descuentos = new ArrayList<>();
    private Pedido pedido;

    // Colaboradores: cada uno se encarga de una parte de la compra. Llegan por el constructor
    // (inyección de dependencias), así Compra no decide cómo se crean.
    private final SeleccionAsientos asientos;
    private final CalculadoraImporte calculadora;
    private final GestorPuntos gestorPuntos;
    private final Reloj reloj; // de dónde sale la hora actual (el real o uno fijo en pruebas)

    /** Constructor de uso normal: crea los colaboradores por defecto. */
    public Compra(int idCompra, DateTime fechaHora, Cliente cliente, Sede sede) {
        this(idCompra, fechaHora, cliente, sede,
                new SeleccionAsientos(), new CalculadoraImporte(), new GestorPuntos(), new RelojSistema());
    }

    /** Recibe los colaboradores ya creados; sirve para cambiarlos (por ejemplo, en pruebas) sin tocar Compra. */
    public Compra(int idCompra, DateTime fechaHora, Cliente cliente, Sede sede,
                  SeleccionAsientos asientos, CalculadoraImporte calculadora, GestorPuntos gestorPuntos,
                  Reloj reloj) {
        if (asientos == null || calculadora == null || gestorPuntos == null || reloj == null) {
            throw new IllegalArgumentException("Los colaboradores de la compra son obligatorios");
        }
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente es obligatorio");
        }
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        this.idCompra = idCompra;
        this.reloj = reloj;
        this.fechaHora = fechaHora != null ? fechaHora : reloj.ahora();
        this.cliente = cliente;
        this.sede = sede;
        this.estado = EN_PROCESO;
        this.importe = ResumenImporte.VACIO;
        this.asientos = asientos;
        this.calculadora = calculadora;
        this.gestorPuntos = gestorPuntos;

        // El plazo comienza al crear la compra, cuando el cliente entra al mapa de asientos.
        this.expiraCompra = this.fechaHora.sumarMinutos(TIEMPO_MAXIMO_COMPRA_MIN);
    }

    /** Comprueba el plazo y expira la compra si venció; devuelve true si ya expiró. */
    public boolean verificarExpiracion() {
        if (!EN_PROCESO.equals(estado)) {
            return EXPIRADA.equals(estado);
        }
        DateTime ahora = reloj.ahora();
        boolean tiempoVencido = (expiraCompra != null && ahora.esDespuesDe(expiraCompra));
        if (asientos.isBloqueados() && asientos.algunaExpirada(ahora)) {
            tiempoVencido = true;
        }
        if (tiempoVencido) {
            cancelarPorTimeout();
            return true;
        }
        return false;
    }

    /** Expira la compra si venció su plazo y lanza error si ya no está EN_PROCESO. */
    private void validarEnProceso() {
        verificarExpiracion();
        if (!EN_PROCESO.equals(estado)) {
            throw new IllegalStateException("La compra ya no está EN_PROCESO (estado actual: " + estado + ")");
        }
    }

    // ---------- Asientos (delegado en SeleccionAsientos) ----------
    /** Preselecciona una entrada de esta sede; la butaca aún no se bloquea y no se repite la misma butaca. */
    public void agregarEntrada(Entrada entrada) {
        validarEnProceso();
        if (entrada != null && !entrada.getFuncion().getSala().getSede().equals(this.sede)) {
            throw new IllegalArgumentException("La función pertenece a otra sede; la compra es de " + sede.getNombre());
        }
        asientos.agregar(entrada);
        calcularTotal();
    }

    /** Quita una selección mientras los asientos sigan sin bloquearse. */
    public boolean quitarEntrada(Entrada entrada) {
        validarEnProceso();
        boolean removida = asientos.quitar(entrada);
        if (removida) {
            calcularTotal();
        }
        return removida;
    }

    /**
     * Valida que las butacas sigan libres y las bloquea hasta la hora límite de la compra.
     * Si alguna ya no está disponible lanza un error con su código y no bloquea ninguna:
     * el cliente debe quitar esa entrada, elegir otra butaca y volver a confirmar.
     */
    public boolean confirmarSeleccionAsientos() {
        validarEnProceso();
        return asientos.bloquear(reloj.ahora(), this.expiraCompra);
    }

    /** Asigna el pedido de dulcería; rechaza productos de oferta si el cliente no tiene el nivel requerido. */
    public void asignarPedido(Pedido pedido) {
        validarEnProceso();
        if (pedido != null && pedido.getSede() != null && !pedido.getSede().equals(this.sede)) {
            throw new IllegalArgumentException("El pedido es de otra sede; la compra es de " + sede.getNombre());
        }
        gestorPuntos.validarProductosSocio(cliente, pedido);
        this.pedido = pedido;
        calcularTotal();
    }

    // ---------- Totales (delegado en CalculadoraImporte) ----------
    /** Recalcula subtotal, descuentos, IGV y total, y los puntos que cuesta la compra. */
    @Override
    public double calcularTotal() {
        List<Entrada> validas = asientos.getEntradasValidas();
        if (EN_PROCESO.equals(estado)) {
            this.puntosCanjeados = gestorPuntos.calcularPuntosRequeridos(validas);
        }
        this.importe = calculadora.calcular(validas, pedido, descuentos);
        return importe.getTotal();
    }

    // ---------- Puntos (delegado en GestorPuntos) ----------
    /**
     * Comprueba que las entradas de socio y los combos de oferta los use un cliente con el nivel requerido,
     * y que el saldo de puntos alcance para los tipos de entrada que cuestan puntos.
     */
    public void validarBeneficiosSocio() {
        gestorPuntos.validarBeneficiosSocio(cliente, asientos.getEntradasValidas(), pedido);
    }

    // ---------- Descuentos (cupones y promociones vistos como Descuento) ----------
    /** Busca el cupón por código en el catálogo y lo aplica si sigue vigente. */
    public boolean aplicarCupon(String codigo, CatalogoCupones catalogo) {
        validarEnProceso();
        if (catalogo == null) return false;
        Cupon encontrado = catalogo.buscarPorCodigo(codigo);
        return encontrado != null && aplicarDescuento(encontrado);
    }

    /**
     * Aplica cualquier descuento (cupón, promoción...) si cumple sus condiciones para esta compra.
     * Como en la BD (columnas id_cupon e id_promocion), hay a lo más uno de cada clase:
     * uno nuevo de la misma clase reemplaza al anterior.
     */
    public boolean aplicarDescuento(Descuento descuento) {
        validarEnProceso();
        if (descuento == null || !descuento.esAplicable(contextoDescuento())) {
            return false;
        }
        descuentos.removeIf(d -> d.getClass() == descuento.getClass());
        descuentos.add(descuento);
        calcularTotal();
        return true;
    }

    /** Datos de esta compra que un descuento necesita para decidir si aplica. */
    private ContextoDescuento contextoDescuento() {
        return new ContextoDescuento(fechaHora, cliente.esSocio());
    }

    // ---------- Confirmación, cancelación y reembolso ----------
    /**
     * Revisa, SIN CAMBIAR NADA, si la compra se puede confirmar en el instante indicado.
     * Devuelve null si todo está bien, o el motivo por el que no se puede (para mostrárselo al cliente).
     * TransaccionPago la llama ANTES de cobrar: si hay un motivo, no se cobra.
     */
    public String motivoNoConfirmable(DateTime ahora) {
        if (!EN_PROCESO.equals(estado)) return "la compra está " + estado;
        if (ahora == null) return "falta la fecha y hora de la operación";
        if (ahora.esDespuesDe(expiraCompra)) {
            return "venció el plazo de " + TIEMPO_MAXIMO_COMPRA_MIN + " minutos";
        }
        // Las butacas deben haberse bloqueado antes con confirmarSeleccionAsientos()
        if (!asientos.estaVacia() && !asientos.isBloqueados()) return "falta confirmar la selección de asientos";
        if (asientos.algunBloqueoVencido(ahora)) return "venció el bloqueo de alguna butaca";
        if (!asientos.todasConTipo()) return "hay entradas sin tipo de entrada";
        if (asientos.contarBloqueadas() == 0 && pedido == null) return "no hay entradas ni pedido que vender";
        // Cada descuento debe seguir siendo aplicable (por ejemplo, un cupón que agotó sus usos ya no)
        for (Descuento d : descuentos) {
            if (!d.esAplicable(contextoDescuento())) return "el descuento " + d + " ya no es válido";
        }
        String motivoSocio = gestorPuntos.motivoBeneficiosSocio(cliente, asientos.getEntradasValidas(), pedido);
        if (motivoSocio != null) return motivoSocio;
        if (pedido != null && !pedido.isRegistrado() && !pedido.hayStock(sede)) {
            return "no hay stock suficiente en la sede para el pedido de dulcería";
        }
        return null;
    }

    /**
     * Confirma la venta con el cobro ya aprobado por la pasarela. No es pública: solo la llama TransaccionPago,
     * así nadie confirma una compra sin pagarla.
     * Todo o nada (RNF-10): primero vuelve a revisar con el MISMO instante con el que se validó antes de cobrar,
     * y recién después hace los cambios. Como ya se comprobó que todo procede, ningún cambio puede fallar
     * a mitad de camino (por ejemplo, dejar el stock descontado con entradas sin vender).
     */
    void confirmar(TransaccionPago pago, DateTime ahora) {
        if (pago == null || pago.getCompra() != this || pago.getReferenciaPasarela() == null) {
            throw new IllegalStateException("Se requiere un cobro aprobado de esta compra para confirmarla");
        }
        String motivo = motivoNoConfirmable(ahora);
        if (motivo != null) {
            throw new IllegalStateException("No se puede confirmar la compra: " + motivo);
        }
        calcularTotal();

        // Desde aquí solo se aplican cambios que ya se sabe que proceden.
        // Los puntos van primero: si el canje fallara, se corta antes de tocar el stock o las entradas.
        if (puntosCanjeados > 0 && !cliente.canjearPuntos(puntosCanjeados)) {
            throw new IllegalStateException("No se pudieron canjear " + puntosCanjeados + " puntos");
        }
        if (pedido != null && !pedido.isRegistrado()) {
            pedido.registrar(sede); // descuenta el stock de la sede (ya se verificó que alcanza)
        }
        asientos.venderBloqueadas(ahora);
        for (Descuento d : descuentos) {
            d.registrarUso(); // cada descuento sabe qué hacer: el cupón suma un uso, la promoción nada
        }
        this.estado = CONFIRMADA; // la serie la asigna la base al confirmar (trigger trg_compra_serie)

        // Los puntos ganados usan el nivel que tenía el cliente antes de esta compra
        this.puntosOtorgados = gestorPuntos.calcularPuntosGanados(cliente, asientos.getEntradasValidas(), pedido);
        if (puntosOtorgados > 0) {
            cliente.acumularPuntos(puntosOtorgados);
        }
        cliente.actualizarNivel();
    }

    /** Cancela una compra en proceso y libera las butacas que estuvieran bloqueadas. */
    public void cancelar() {
        validarEnProceso();
        asientos.anularBloqueadas();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = CANCELADA;
    }

    /**
     * Expira la compra por tiempo y libera los bloqueos; no cambia compras ya finalizadas.
     * Es privada: solo se expira cuando la propia compra comprueba que el plazo venció.
     */
    private void cancelarPorTimeout() {
        if (!EN_PROCESO.equals(estado)) return;
        asientos.liberarPorTimeout();
        this.puntosCanjeados = 0; // los puntos nunca se descontaron del saldo
        this.estado = EXPIRADA;
    }

    /**
     * Reembolsa solo si todas las funciones fueron canceladas y el pedido no fue entregado.
     * Devuelve los puntos canjeados, quita los puntos ganados con esta compra, devuelve el uso del cupón
     * y anula el pedido de dulcería (su stock vuelve solo si nadie lo había empezado a preparar).
     */
    public boolean reembolsar() {
        if (!CONFIRMADA.equals(estado)) return false;
        if (!asientos.todasConFuncionCancelada()) return false;
        if (pedido != null && Pedido.ENTREGADO.equals(pedido.getEstado())) {
            return false;
        }
        asientos.anularVendidas();
        for (Descuento d : descuentos) {
            d.revertirUso(); // el cupón recupera el uso; la promoción no hace nada
        }
        if (pedido != null && pedido.isRegistrado()) {
            pedido.anular();
        }
        this.estado = REEMBOLSADA;
        cliente.devolverPuntos(puntosCanjeados);
        cliente.revertirPuntos(puntosOtorgados);
        return true;
    }

    // ---------- Comprobante (delegado en Comprobante) ----------
    /** Guarda la serie que la base asignó al confirmar la compra (por ejemplo B001-000123); solo se fija una vez. */
    public void asignarSerie(String serieNumero) {
        if (!CONFIRMADA.equals(estado) && !REEMBOLSADA.equals(estado)) {
            throw new IllegalStateException("Solo una compra confirmada puede tener serie");
        }
        if (this.serieNumero != null) {
            throw new IllegalStateException("La compra ya tiene serie asignada");
        }
        if (serieNumero == null || serieNumero.trim().isEmpty() || serieNumero.trim().length() > 20) {
            throw new IllegalArgumentException("La serie es obligatoria (máx. 20 caracteres)");
        }
        this.serieNumero = serieNumero.trim();
    }

    /** Texto del comprobante; exige que la compra ya tenga serie. */
    public String generarComprobante() {
        if (serieNumero == null) {
            throw new IllegalStateException("La compra aún no tiene comprobante (la base asigna la serie al confirmarla)");
        }
        return new Comprobante(serieNumero, fechaHora, importe).generarTexto();
    }

    /** Reloj de la compra; TransaccionPago lo usa para que el pago y la compra miren la misma hora. */
    Reloj getReloj() { return reloj; }

    public int getIdCompra() { return idCompra; }
    public DateTime getFechaHora() { return fechaHora; }
    public DateTime getExpiraCompra() { return expiraCompra; }
    public String getSerieNumero() { return serieNumero; }
    public ResumenImporte getImporte() { return importe; }
    public double getTotal() { return importe.getTotal(); }
    public int getPuntosCanjeados() { return puntosCanjeados; }
    public int getPuntosOtorgados() { return puntosOtorgados; }
    public String getEstado() { return estado; }
    public Cliente getCliente() { return cliente; }
    public Sede getSede() { return sede; }
    public List<Descuento> getDescuentos() { return new ArrayList<>(descuentos); }
    public Pedido getPedido() { return pedido; }
    public List<Entrada> getEntradas() { return asientos.getEntradas(); }

    @Override
    public String toString() {
        return "Compra #" + idCompra + " [" + estado + "] S/ " + String.format("%.2f", getTotal());
    }
}
