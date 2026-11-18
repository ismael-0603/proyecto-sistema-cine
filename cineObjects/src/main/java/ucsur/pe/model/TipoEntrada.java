package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;

/**
 * Por qué existe: es una tarifa de entrada (General, Socio, Niño...) con su precio y los puntos que da.
 * Los puntos son un dato, así un tipo nuevo no necesita una clase nueva.
 */
public class TipoEntrada implements Activable {
    private int idTipoEntrada;
    private String nombre;
    private String descripcion;
    private long tarifaCentavos; // en céntimos: S/ 20.00 -> 2000 (ver ConversorDinero)
    /** Puntos base que gana el cliente por cada entrada de este tipo, antes del multiplicador de nivel. */
    private int puntosPorEntrada;
    private boolean requiereSocio; // si es true, solo la compra un cliente socio
    /** Puntos que cuesta canjear esta tarifa; cero significa que no se descuentan puntos. */
    private int puntosCanje;
    private boolean estado;

    public TipoEntrada(int idTipoEntrada, String nombre, String descripcion, long tarifaCentavos,
                       int puntosPorEntrada, boolean requiereSocio, boolean estado) {
        this(idTipoEntrada, nombre, descripcion, tarifaCentavos, puntosPorEntrada, requiereSocio, estado, 0);
    }

    public TipoEntrada(int idTipoEntrada, String nombre, String descripcion, long tarifaCentavos,
                       int puntosPorEntrada, boolean requiereSocio, boolean estado, int puntosCanje) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 40) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 40 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres");
        }
        if (tarifaCentavos < 0) {
            throw new IllegalArgumentException("La tarifa no puede ser negativa");
        }
        if (puntosPorEntrada < 0) {
            throw new IllegalArgumentException("Los puntos por entrada no pueden ser negativos");
        }
        if (puntosCanje < 0) {
            throw new IllegalArgumentException("Los puntos de canje no pueden ser negativos");
        }
        if (puntosCanje > 0 && !requiereSocio) {
            throw new IllegalArgumentException("Un tipo de entrada que cuesta puntos debe ser exclusivo para socios");
        }
        this.idTipoEntrada = idTipoEntrada;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.tarifaCentavos = tarifaCentavos;
        this.puntosPorEntrada = puntosPorEntrada;
        this.requiereSocio = requiereSocio;
        this.estado = estado;
        this.puntosCanje = puntosCanje;
    }

    /** Puntos que se descuentan del saldo del cliente al usar esta tarifa. */
    public int getPuntosCanje() { return puntosCanje; }

    /** Precio base de la entrada en céntimos, sin el recargo del formato de sala. */
    public long getTarifaCentavos() {
        return this.tarifaCentavos;
    }

    /** Rechaza tipos inactivos y exige que el cliente sea socio cuando la tarifa lo requiere. */
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        return !this.requiereSocio || (cliente != null && cliente.cumpleNivelSocio(1));
    }

    /** Habilita esta tarifa para nuevas compras. */
    @Override
    public void activar() {
        this.estado = true;
    }

    /** Deshabilita esta tarifa para nuevas compras. */
    @Override
    public void desactivar() {
        this.estado = false;
    }

    /** Puntos base que entrega esta tarifa, sin multiplicador; si está inactiva, devuelve cero. */
    public int obtenerPuntos() {
        return this.estado ? puntosPorEntrada : 0;
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una tarifa nueva nace con id 0. */
    public void asignarId(int idTipoEntrada) {
        this.idTipoEntrada = Ids.asignar(this.idTipoEntrada, idTipoEntrada, "el tipo de entrada");
    }

    public int getIdTipoEntrada() { return idTipoEntrada; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getPuntosPorEntrada() { return puntosPorEntrada; }
    public boolean isRequiereSocio() { return requiereSocio; }
    @Override
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (" + ConversorDinero.formatear(tarifaCentavos) + ")";
    }
}
