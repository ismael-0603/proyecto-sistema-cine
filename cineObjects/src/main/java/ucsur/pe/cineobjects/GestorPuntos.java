package ucsur.pe.cineobjects;

import java.util.List;

/**
 * Por qué existe: junta las reglas de puntos de una compra: cuántos cuesta, cuántos gana el cliente
 * y si puede usar entradas o combos exclusivos de socio.
 */
public class GestorPuntos {

    /** Suma los puntos que cuestan los tipos de entrada elegidos (entradas canjeadas con puntos). */
    public int calcularPuntosRequeridos(List<Entrada> entradasValidas) {
        int requeridos = 0;
        for (Entrada e : entradasValidas) {
            if (e.getTipoEntrada() != null) {
                requeridos += e.getTipoEntrada().getPuntosCanje();
            }
        }
        return requeridos;
    }

    /** Suma los puntos base de las entradas y de los combos del pedido, sin multiplicador de nivel. */
    public int calcularPuntosBase(List<Entrada> entradasValidas, Pedido pedido) {
        int puntos = 0;
        for (Entrada e : entradasValidas) {
            if (e.getTipoEntrada() != null) {
                puntos += e.getTipoEntrada().obtenerPuntos();
            }
        }
        if (pedido != null) {
            for (DetallePedido d : pedido.getDetalles()) {
                if (d.getProducto().isEsCombo()) {
                    puntos += d.getCantidad() * d.getProducto().obtenerPuntos();
                }
            }
        }
        return puntos;
    }

    /** Puntos que gana el cliente: los puntos base con el multiplicador de su nivel de socio. */
    public int calcularPuntosGanados(Cliente cliente, List<Entrada> entradasValidas, Pedido pedido) {
        return cliente.aplicarMultiplicador(calcularPuntosBase(entradasValidas, pedido));
    }

    /**
     * Comprueba que las entradas de socio y los combos de oferta los use un cliente con el nivel requerido,
     * y que el saldo de puntos alcance para los tipos de entrada que cuestan puntos.
     */
    public void validarBeneficiosSocio(Cliente cliente, List<Entrada> entradasValidas, Pedido pedido) {
        for (Entrada e : entradasValidas) {
            TipoEntrada t = e.getTipoEntrada();
            if (t != null && !t.validarElegibilidad(cliente)) {
                throw new IllegalStateException("El cliente no puede usar la entrada '" + t.getNombre()
                        + "' (es exclusiva para socios o está inactiva)");
            }
        }
        int requeridos = calcularPuntosRequeridos(entradasValidas);
        if (requeridos > cliente.consultarPuntos()) {
            throw new IllegalStateException("Puntos insuficientes: la compra requiere " + requeridos
                    + " y el saldo es " + cliente.consultarPuntos());
        }
        validarProductosSocio(cliente, pedido);
    }

    /** Rechaza productos de oferta si el cliente no tiene el nivel de socio requerido. */
    public void validarProductosSocio(Cliente cliente, Pedido pedido) {
        if (pedido == null) return;
        for (DetallePedido d : pedido.getDetalles()) {
            Producto prod = d.getProducto();
            if (!prod.validarElegibilidad(cliente)) {
                throw new IllegalStateException("El producto '" + prod.getNombre()
                        + "' no está disponible para este cliente (nivel de socio requerido: "
                        + prod.getNivelSocioMinimo() + ", o producto inactivo)");
            }
        }
    }
}
