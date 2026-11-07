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
     * Devuelve null si todo está bien, o el motivo si algo no se cumple (no lanza errores).
     */
    public String motivoBeneficiosSocio(Cliente cliente, List<Entrada> entradasValidas, Pedido pedido) {
        for (Entrada e : entradasValidas) {
            TipoEntrada t = e.getTipoEntrada();
            if (t != null && !t.validarElegibilidad(cliente)) {
                return "El cliente no puede usar la entrada '" + t.getNombre()
                        + "' (es exclusiva para socios o está inactiva)";
            }
        }
        int requeridos = calcularPuntosRequeridos(entradasValidas);
        if (requeridos > cliente.consultarPuntos()) {
            return "Puntos insuficientes: la compra requiere " + requeridos
                    + " y el saldo es " + cliente.consultarPuntos();
        }
        return motivoProductosSocio(cliente, pedido);
    }

    /** Igual que motivoBeneficiosSocio, pero lanza un error con el motivo (para avisar al cliente). */
    public void validarBeneficiosSocio(Cliente cliente, List<Entrada> entradasValidas, Pedido pedido) {
        lanzarSiHayMotivo(motivoBeneficiosSocio(cliente, entradasValidas, pedido));
    }

    /** Rechaza productos de oferta si el cliente no tiene el nivel de socio requerido. */
    public void validarProductosSocio(Cliente cliente, Pedido pedido) {
        lanzarSiHayMotivo(motivoProductosSocio(cliente, pedido));
    }

    /** Devuelve null si el cliente puede llevar todos los productos del pedido, o el motivo si no. */
    private String motivoProductosSocio(Cliente cliente, Pedido pedido) {
        if (pedido == null) return null;
        for (DetallePedido d : pedido.getDetalles()) {
            Producto prod = d.getProducto();
            if (!prod.validarElegibilidad(cliente)) {
                return "El producto '" + prod.getNombre()
                        + "' no está disponible para este cliente (nivel de socio requerido: "
                        + prod.getNivelSocioMinimo() + ", o producto inactivo)";
            }
        }
        return null;
    }

    private static void lanzarSiHayMotivo(String motivo) {
        if (motivo != null) {
            throw new IllegalStateException(motivo);
        }
    }
}
