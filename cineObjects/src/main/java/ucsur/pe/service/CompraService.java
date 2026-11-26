package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.MapaButacas;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.SeleccionAsientos;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.CuponRepository;
import ucsur.pe.repository.TipoEntradaRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso "Comprar entrada", "Personalizar compra" y "Cancelar compra"
 * (RF-05 a RF-11, RF-29, RF-46). Crea la compra, la guarda y le pide al modelo cada paso;
 * las reglas de la compra siguen dentro de Compra.
 */
public class CompraService {
    private final CompraRepository compras;
    private final CuponRepository cupones;
    private final TipoEntradaRepository tiposEntrada;
    // Colaboradores sin estado: uno solo sirve para todas las compras, por eso llegan desde App.
    private final CalculadoraImporte calculadora;
    private final GestorPuntos gestorPuntos;
    private final Reloj reloj;

    public CompraService(CompraRepository compras, CuponRepository cupones, TipoEntradaRepository tiposEntrada,
                         CalculadoraImporte calculadora, GestorPuntos gestorPuntos, Reloj reloj) {
        this.compras = compras;
        this.cupones = cupones;
        this.tiposEntrada = tiposEntrada;
        this.calculadora = calculadora;
        this.gestorPuntos = gestorPuntos;
        this.reloj = reloj;
    }

    /** Empieza la compra en la sede elegida; desde aquí corren los 4 minutos. */
    public Compra iniciarCompra(Cliente cliente, Sede sede) {
        // SeleccionAsientos sí se crea aquí: guarda las entradas de ESTA compra, así que cada compra tiene la suya.
        Compra compra = new Compra(0, null, cliente, sede,
                new SeleccionAsientos(), calculadora, gestorPuntos, reloj);
        cliente.agregarCompra(compra);
        compras.guardar(compra); // la base le genera el id (AUTO_INCREMENT)
        return compra;
    }

    /**
     * Mapa de butacas que ve el cliente al entrar a «1. Asientos», con la sugerencia ya marcada (RF-07, RF-29).
     * La sugerencia no tiene en cuenta las butacas que esta compra ya eligió. Si no hay suficientes butacas
     * libres, el mapa llega igual, sin sugerencia y con un aviso.
     */
    public MapaButacas consultarMapa(Compra compra, Funcion funcion, int cantidad, boolean incluirAccesibles) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        List<Butaca> sugeridas = List.of();
        String aviso = null;
        try {
            sugeridas = recomendarAsientos(compra, funcion, cantidad, incluirAccesibles);
        } catch (IllegalStateException e) {
            aviso = e.getMessage();
        }
        return new MapaButacas(funcion.getSala().obtenerMapaButacas(), funcion.obtenerButacasDisponibles(), sugeridas, aviso);
    }

    /** Mejores butacas para el grupo (RF-29); no repite las que la compra ya eligió. */
    private List<Butaca> recomendarAsientos(Compra compra, Funcion funcion, int cantidad, boolean incluirAccesibles) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        List<Butaca> elegidas = (compra != null) ? compra.butacasElegidas(funcion) : List.of();
        return funcion.recomendarMejoresAsientos(cantidad, incluirAccesibles, elegidas);
    }

    /** Tarifas que el cliente puede elegir para sus entradas: activas y, si son de socio, solo para socios. */
    public List<TipoEntrada> listarTiposEntrada(Cliente cliente) {
        return tiposEntrada.listarActivos().stream().filter(t -> t.validarElegibilidad(cliente)).toList();
    }

    public Entrada agregarEntrada(Compra compra, Funcion funcion, Butaca butaca, TipoEntrada tipo) {
        Entrada entrada = new Entrada(0, funcion, butaca, tipo); // el id real lo genera la BD
        compra.agregarEntrada(entrada);
        return entrada;
    }

    public void quitarEntrada(Compra compra, Entrada entrada) {
        if (!compra.quitarEntrada(entrada)) {
            throw new ReglaNegocioException("La entrada no forma parte de esta compra");
        }
    }

    /** Bloquea las butacas elegidas; con BD, aquí se insertan las entradas (ya con su tipo). */
    public void confirmarAsientos(Compra compra) {
        compra.confirmarSeleccionAsientos();
        try {
            compras.guardar(compra);
        } catch (RuntimeException ex) {
            compra.liberarSeleccionAsientos();
            throw ex;
        }
    }

    /** «Volver» desde Dulcería: libera las butacas bloqueadas para poder cambiarlas; el plazo sigue corriendo. */
    public void liberarAsientos(Compra compra) {
        compra.liberarSeleccionAsientos();
        compras.guardar(compra);
    }

    // ---------- Dulcería (el pedido lo arma el servicio, no la vista) ----------
    /**
     * Agrega un producto al pedido de la compra; si la compra aún no tiene pedido, lo crea (RF-12 a RF-14).
     * La compra vuelve a validar las ofertas de socio y recalcula el total con cada cambio.
     */
    public DetallePedido agregarProducto(Compra compra, Producto producto, int cantidad, String personalizacion) {
        exigirCompra(compra);
        DetallePedido detalle = new DetallePedido(0, producto, cantidad, personalizacion);
        compra.agregarProducto(detalle); // la compra valida su estado y las ofertas de socio antes de tocar el pedido
        return detalle;
    }

    /** Cambia la cantidad de una línea del pedido y recalcula el total de la compra. */
    public void cambiarCantidad(Compra compra, DetallePedido detalle, int cantidad) {
        exigirCompra(compra);
        compra.cambiarCantidad(detalle, cantidad);
    }

    /** Quita una línea del pedido; si el pedido queda vacío, la compra sigue sin dulcería. */
    public void quitarProducto(Compra compra, DetallePedido detalle) {
        exigirCompra(compra);
        compra.quitarProducto(detalle);
    }

    private static void exigirCompra(Compra compra) {
        if (compra == null) throw new IllegalArgumentException("La compra es obligatoria");
    }

    /** Busca el cupón por el código que escribe el cliente y lo aplica (RF-11). */
    public void aplicarCupon(Compra compra, String codigo) {
        Cupon cupon = cupones.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaNegocioException("No existe el cupón " + codigo));
        compra.aplicarDescuento(cupon); // el uso del cupón se cuenta al confirmar la compra
    }

    public void aplicarPromocion(Compra compra, Promocion promocion) {
        compra.aplicarDescuento(promocion);
    }

    public void cancelar(Compra compra) {
        compra.cancelar();
        compras.guardar(compra);
    }

}
