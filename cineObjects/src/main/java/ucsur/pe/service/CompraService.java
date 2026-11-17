package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.MapaButacas;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.SeleccionAsientos;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.CuponRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso "Comprar entrada", "Personalizar compra" y "Cancelar compra"
 * (RF-05 a RF-11, RF-29, RF-46). Crea la compra, la guarda y le pide al modelo cada paso;
 * las reglas de la compra siguen dentro de Compra.
 */
public class CompraService {
    private final CompraRepository compras;
    private final CuponRepository cupones;
    // Colaboradores sin estado: uno solo sirve para todas las compras, por eso llegan desde App.
    private final CalculadoraImporte calculadora;
    private final GestorPuntos gestorPuntos;
    private final Reloj reloj;

    public CompraService(CompraRepository compras, CuponRepository cupones,
                         CalculadoraImporte calculadora, GestorPuntos gestorPuntos, Reloj reloj) {
        this.compras = compras;
        this.cupones = cupones;
        this.calculadora = calculadora;
        this.gestorPuntos = gestorPuntos;
        this.reloj = reloj;
    }

    /** Empieza la compra en la sede elegida; desde aquí corren los 4 minutos. */
    public Compra iniciarCompra(Cliente cliente, Sede sede) {
        // SeleccionAsientos sí se crea aquí: guarda las entradas de ESTA compra, así que cada compra tiene la suya.
        Compra compra = new Compra(compras.siguienteId(), null, cliente, sede,
                new SeleccionAsientos(), calculadora, gestorPuntos, reloj);
        cliente.agregarCompra(compra);
        compras.guardar(compra);
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
    public List<Butaca> recomendarAsientos(Compra compra, Funcion funcion, int cantidad, boolean incluirAccesibles) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        List<Butaca> elegidas = (compra != null) ? compra.butacasElegidas(funcion) : List.of();
        return funcion.recomendarMejoresAsientos(cantidad, incluirAccesibles, elegidas);
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
        compras.guardar(compra);
    }

    /** «Volver» desde Dulcería: libera las butacas bloqueadas para poder cambiarlas; el plazo sigue corriendo. */
    public void liberarAsientos(Compra compra) {
        compra.liberarSeleccionAsientos();
        compras.guardar(compra);
    }

    public void asignarPedido(Compra compra, Pedido pedido) {
        compra.asignarPedido(pedido);
    }

    /** Busca el cupón por el código que escribe el cliente y lo aplica (RF-11). */
    public void aplicarCupon(Compra compra, String codigo) {
        Cupon cupon = cupones.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaNegocioException("No existe el cupón " + codigo));
        compra.aplicarDescuento(cupon); // al ser Cupon, Java usa la versión que cuenta usos
    }

    public void aplicarPromocion(Compra compra, Promocion promocion) {
        compra.aplicarDescuento(promocion);
    }

    /** Avisa antes de pagar si una entrada o combo es solo para socios o faltan puntos. */
    public void validarBeneficiosSocio(Compra compra) {
        compra.validarBeneficiosSocio();
    }

    public void cancelar(Compra compra) {
        compra.cancelar();
        compras.guardar(compra);
    }

    public String generarComprobante(Compra compra) {
        return compra.generarComprobante();
    }
}
