package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: maneja las entradas que el cliente eligió en una compra:
 * agregarlas, bloquear sus butacas, venderlas o liberarlas.
 */
public class SeleccionAsientos {
    private final List<Entrada> entradas = new ArrayList<>();
    private boolean bloqueados; // true cuando las butacas ya se bloquearon en sus funciones

    /** Preselecciona una entrada; la butaca aún no se bloquea y no se repite la misma butaca y función. */
    public void agregar(Entrada entrada) {
        if (bloqueados) {
            throw new IllegalStateException("Los asientos ya fueron bloqueados; no se pueden agregar más");
        }
        if (entrada == null) {
            throw new IllegalArgumentException("La entrada no puede ser nula");
        }
        if (!entrada.getButaca().estaDisponible(entrada.getFuncion())) {
            throw new IllegalStateException("La butaca " + entrada.getButaca().getCodigoAsiento() + " no está disponible");
        }
        for (Entrada existente : entradas) {
            if (existente.getButaca() == entrada.getButaca() && existente.getFuncion() == entrada.getFuncion()) {
                return;
            }
        }
        entradas.add(entrada);
    }

    /** Quita una selección mientras los asientos sigan sin bloquearse. */
    public boolean quitar(Entrada entrada) {
        if (bloqueados) {
            throw new IllegalStateException("No se pueden modificar los asientos una vez bloqueados");
        }
        return entradas.remove(entrada);
    }

    /**
     * Valida que las butacas sigan libres y las bloquea hasta la hora límite indicada.
     * Si alguna ya no está disponible lanza un error con su código y no bloquea ninguna.
     * Si ya están bloqueadas, la operación es idempotente y devuelve true.
     */
    public boolean bloquear(DateTime hastaCuando) {
        if (bloqueados) {
            return true;
        }
        if (entradas.isEmpty()) {
            return false;
        }

        // 1. Se revisan todas las butacas antes de bloquear alguna.
        List<String> noDisponibles = new ArrayList<>();
        for (Entrada e : entradas) {
            if (!e.getFuncion().estaDisponible()) {
                throw new IllegalStateException("La función ya no está disponible para la venta (" + e.getFuncion().getEstado() + ")");
            }
            if (!e.getFuncion().esButacaDisponible(e.getButaca())) {
                noDisponibles.add(e.getButaca().getCodigoAsiento());
            }
        }
        if (!noDisponibles.isEmpty()) {
            throw new IllegalStateException("Butacas no disponibles: " + String.join(", ", noDisponibles)
                    + ". Quita esas entradas y elige otras butacas.");
        }

        // 2. Se bloquean todas; si algo falla, se revierte lo ya bloqueado.
        List<Entrada> bloqueadas = new ArrayList<>();
        try {
            for (Entrada e : entradas) {
                if (!e.bloquearTemporalmente(hastaCuando)) {
                    throw new IllegalStateException("No se pudo bloquear la butaca " + e.getButaca().getCodigoAsiento());
                }
                bloqueadas.add(e);
            }
        } catch (RuntimeException ex) {
            for (Entrada e : bloqueadas) {
                e.liberarBloqueo();
            }
            throw ex;
        }

        this.bloqueados = true;
        return true;
    }

    /** Revisa los bloqueos (los vencidos pasan a EXPIRADA) y devuelve true si alguno venció. */
    public boolean algunaExpirada() {
        boolean expirada = false;
        for (Entrada e : entradas) {
            if (e.haExpirado()) {
                expirada = true;
            }
        }
        return expirada;
    }

    /** Indica si todas las entradas tienen su tipo asignado, requisito para venderlas. */
    public boolean todasConTipo() {
        for (Entrada e : entradas) {
            if (e.getTipoEntrada() == null) return false;
        }
        return true;
    }

    /** Pasa a VENDIDA cada entrada bloqueada; devuelve cuántas se vendieron. */
    public int venderBloqueadas() {
        int vendidas = 0;
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                e.confirmarVenta();
                vendidas++;
            }
        }
        return vendidas;
    }

    /** Cuenta las entradas bloqueadas, es decir, las que se venderían al confirmar. */
    public int contarBloqueadas() {
        int cantidad = 0;
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) cantidad++;
        }
        return cantidad;
    }

    /** Cancelación por el cliente: anula las entradas bloqueadas y libera sus butacas. */
    public void anularBloqueadas() {
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                e.anular();
            }
        }
        this.bloqueados = false;
    }

    /**
     * Fin del plazo: si las butacas estaban bloqueadas se expiran (se liberan en la función);
     * si nunca se bloquearon, solo se anula la preselección.
     */
    public void liberarPorTimeout() {
        for (Entrada e : entradas) {
            if (Entrada.BLOQUEADA.equals(e.getEstado())) {
                if (bloqueados) {
                    e.expirar();
                } else {
                    e.anular();
                }
            }
        }
        this.bloqueados = false;
    }

    /** Indica si todas las entradas son de funciones canceladas (condición para reembolsar). */
    public boolean todasConFuncionCancelada() {
        if (entradas.isEmpty()) return false;
        for (Entrada e : entradas) {
            if (!Funcion.CANCELADA.equals(e.getFuncion().getEstado())) {
                return false;
            }
        }
        return true;
    }

    /** Anula las entradas vendidas (reembolso). */
    public void anularVendidas() {
        for (Entrada e : entradas) {
            if (Entrada.VENDIDA.equals(e.getEstado())) {
                e.anular();
            }
        }
    }

    /** Entradas que cuentan para el cobro y los puntos: ni anuladas ni expiradas. */
    public List<Entrada> getEntradasValidas() {
        List<Entrada> validas = new ArrayList<>();
        for (Entrada e : entradas) {
            if (!Entrada.ANULADA.equals(e.getEstado()) && !Entrada.EXPIRADA.equals(e.getEstado())) {
                validas.add(e);
            }
        }
        return validas;
    }

    public boolean isBloqueados() { return bloqueados; }
    public boolean estaVacia() { return entradas.isEmpty(); }
    public List<Entrada> getEntradas() { return new ArrayList<>(entradas); }
}
