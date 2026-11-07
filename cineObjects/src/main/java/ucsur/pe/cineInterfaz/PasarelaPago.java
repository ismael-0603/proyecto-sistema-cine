package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: es el actor externo "pasarela de pagos" del diagrama de contexto (RF-17).
 * TransaccionPago depende de esta interfaz y no de una pasarela concreta: se puede cambiar la
 * simulación por una real (Niubiz, Izipay, Culqi...) o por una falsa en pruebas sin tocar el modelo.
 */
public interface PasarelaPago {
    /** Intenta cobrar el monto con el medio indicado; devuelve si se aprobó, con su referencia o el motivo. */
    ResultadoPago cobrar(String medioPago, double monto, String referenciaCompra);
    /** Devuelve al cliente el dinero de un cobro aprobado; true si la pasarela aceptó la devolución. */
    boolean reembolsar(String referenciaPasarela, double monto);
}
