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
    private final Reloj reloj;

    public CompraService(CompraRepository compras, CuponRepository cupones, Reloj reloj) {
        this.compras = compras;
        this.cupones = cupones;
        this.reloj = reloj;
    }

    /** Empieza la compra en la sede elegida; desde aquí corren los 4 minutos. */
    public Compra iniciarCompra(Cliente cliente, Sede sede) {
        Compra compra = new Compra(compras.siguienteId(), null, cliente, sede,
                new SeleccionAsientos(), new CalculadoraImporte(), new GestorPuntos(), reloj);
        cliente.agregarCompra(compra);
        compras.guardar(compra);
        return compra;
    }

    /** Butacas contiguas sugeridas cerca del centro (RF-29). */
    public List<Butaca> recomendarAsientos(Funcion funcion, int cantidad) {
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        return funcion.recomendarMejoresAsientos(cantidad);
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

    public void asignarPedido(Compra compra, Pedido pedido) {
        compra.asignarPedido(pedido);
    }

    /** Busca el cupón por el código que escribe el cliente y lo aplica (RF-11). */
    public void aplicarCupon(Compra compra, String codigo) {
        Cupon cupon = cupones.buscarPorCodigo(codigo)
                .orElseThrow(() -> new ReglaNegocioException("No existe el cupón " + codigo));
        compra.aplicarDescuento(cupon);
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
