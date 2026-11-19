package ucsur.pe.vista;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetalleCompra;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.service.Sesion;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: guarda lo que el cliente va eligiendo mientras pasa de una pantalla a otra
 * (sede, cuenta o invitado, película, función y la compra en curso), y lleva la cuenta regresiva
 * de los 4 minutos que se muestra en el encabezado. Es solo estado de pantalla: las reglas
 * (precios, butacas libres, vencimiento real) siguen en el modelo y los servicios.
 */
public class EstadoKiosco {

    private Sede sede;
    private Sesion sesion;          // cuenta que inició sesión (cliente o empleado); null si nadie
    private Cliente invitado;       // cliente invitado, si eligió continuar sin cuenta
    private Pelicula pelicula;
    private DateTime diaElegido;    // día filtrado en la cartelera: «Horarios» abre en ese día (null = el primero)
    private Funcion funcion;
    private Compra compra;          // compra EN_PROCESO; null fuera del flujo de compra
    private String medioPago;       // medio elegido en «Resumen y pago»
    // Lo elegido en «Asientos», para volver a mostrarlo si el cliente regresa desde la dulcería
    private final Map<TipoEntrada, Integer> cantidades = new LinkedHashMap<>();
    private final List<Butaca> butacas = new ArrayList<>();

    // Postventa (fase 2): la compra elegida en «Mis compras» o la recuperada con su código
    private Compra compraElegida;
    private DetalleCompra detalleRecuperado;
    private String correoRecuperado;
    private int paginaCompras;
    // Administración: el registro que se abre en un formulario para editarlo (null = nuevo)
    private Object enEdicion;
    // Correo escrito en «Iniciar sesión» o «Recuperar contraseña», para no volver a escribirlo al pasar de una a otra
    private String correoLogin;

    private final IntegerProperty segundosRestantes = new SimpleIntegerProperty(0);
    private Timeline cuentaRegresiva;

    // ---------- Cuenta ----------

    /** El cliente que compra: el de la sesión iniciada o el invitado; null si aún no se identificó. */
    public Cliente cliente() {
        if (sesion != null && sesion.esCliente()) return sesion.cliente();
        return invitado;
    }

    public boolean hayCliente() { return cliente() != null; }

    public void iniciarSesion(Sesion sesion) {
        this.sesion = sesion;
        this.invitado = null;
    }

    public void entrarComoInvitado(Cliente invitado) {
        this.invitado = invitado;
    }

    public void cerrarSesion() {
        sesion = null;
        invitado = null;
        compraElegida = null;
        paginaCompras = 0;
        correoLogin = null;
    }

    /** El cliente con sesión iniciada (no el invitado); null si no hay. */
    public Cliente clienteConSesion() {
        return (sesion != null && sesion.esCliente()) ? sesion.cliente() : null;
    }

    // ---------- Compra ----------

    /** Empieza la cuenta regresiva de la compra; al llegar a cero llama a {@code alTerminar}. */
    public void empezarCompra(Compra compra, Runnable alTerminar) {
        this.compra = compra;
        cantidades.clear();
        butacas.clear();
        medioPago = null;
        detenerCuenta();
        segundosRestantes.set(Compra.TIEMPO_MAXIMO_COMPRA_MIN * 60);
        cuentaRegresiva = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundosRestantes.set(Math.max(0, segundosRestantes.get() - 1));
            if (segundosRestantes.get() == 0) {
                detenerCuenta();
                alTerminar.run();
            }
        }));
        cuentaRegresiva.setCycleCount(Timeline.INDEFINITE);
        cuentaRegresiva.play();
    }

    /** Sale del flujo de compra (pagada, cancelada o vencida): se olvida la compra y lo elegido. */
    public void terminarCompra() {
        detenerCuenta();
        compra = null;
        funcion = null;
        medioPago = null;
        cantidades.clear();
        butacas.clear();
        // El invitado se conserva para que pueda elegir otro horario; volverAlInicio lo olvida (cerrarSesion)
    }

    /** Detiene la cuenta regresiva (por ejemplo, cuando la compra ya se pagó). */
    public void detenerCuenta() {
        if (cuentaRegresiva != null) {
            cuentaRegresiva.stop();
            cuentaRegresiva = null;
        }
    }

    public boolean hayCompra() { return compra != null; }

    /** Hay una compra corriendo contra el reloj (aún sin pagar). */
    public boolean cuentaActiva() { return cuentaRegresiva != null; }

    public IntegerProperty segundosRestantesProperty() { return segundosRestantes; }

    /** Tiempo que queda en formato mm:ss. */
    public String tiempoRestante() {
        int s = segundosRestantes.get();
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    // ---------- Getters y setters ----------

    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }
    public Sesion getSesion() { return sesion; }
    public Pelicula getPelicula() { return pelicula; }
    public void setPelicula(Pelicula pelicula) { this.pelicula = pelicula; }
    public DateTime getDiaElegido() { return diaElegido; }
    public void setDiaElegido(DateTime dia) { this.diaElegido = dia; }
    public Funcion getFuncion() { return funcion; }
    public void setFuncion(Funcion funcion) { this.funcion = funcion; }
    public Compra getCompra() { return compra; }
    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }
    public Map<TipoEntrada, Integer> getCantidades() { return cantidades; }
    public Compra getCompraElegida() { return compraElegida; }
    public void setCompraElegida(Compra compra) { this.compraElegida = compra; this.detalleRecuperado = null; }
    public DetalleCompra getDetalleRecuperado() { return detalleRecuperado; }
    public String getCorreoRecuperado() { return correoRecuperado; }
    public String getCorreoLogin() { return correoLogin; }
    public void setCorreoLogin(String correo) { this.correoLogin = (correo == null || correo.isBlank()) ? null : correo.trim(); }
    public void setDetalleRecuperado(DetalleCompra detalle, String correo) {
        this.detalleRecuperado = detalle;
        this.correoRecuperado = correo;
        this.compraElegida = null;
    }
    public int getPaginaCompras() { return paginaCompras; }
    public void setPaginaCompras(int pagina) { this.paginaCompras = pagina; }

    /** Abre un formulario de administración: con el registro a editar o null para uno nuevo. */
    public void setEnEdicion(Object registro) { this.enEdicion = registro; }

    /** El registro en edición si es del tipo pedido; si no, null (formulario de alta). */
    public <T> T getEnEdicion(Class<T> tipo) {
        return tipo.isInstance(enEdicion) ? tipo.cast(enEdicion) : null;
    }
    public List<Butaca> getButacas() { return butacas; }
}
