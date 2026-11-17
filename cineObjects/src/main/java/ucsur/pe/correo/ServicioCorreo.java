package ucsur.pe.correo;

/**
 * Por qué existe: el envío de correos es un sistema externo, igual que la pasarela de pagos.
 * Los servicios solo conocen esta interfaz; hoy la cumple CorreoSimulado y mañana un proveedor real
 * (SMTP, SendGrid...) sin tocar los servicios.
 */
public interface ServicioCorreo {
    /** Envía el mensaje; devuelve false si el proveedor no lo aceptó. */
    boolean enviar(String destinatario, String asunto, String cuerpo);
}
