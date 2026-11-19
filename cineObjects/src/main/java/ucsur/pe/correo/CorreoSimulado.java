package ucsur.pe.correo;

import java.util.function.Consumer;

/**
 * Por qué existe: simula el proveedor de correo del kiosco: no envía nada. Cada mensaje "enviado" se entrega
 * a quien se registró con alEnviar; App lo conecta con el Navegador, que lo muestra como un aviso en la
 * pantalla (así se ven el código para cambiar la contraseña, el comprobante reenviado, etc.).
 */
public class CorreoSimulado implements ServicioCorreo {
    /** Un correo "enviado". */
    public record Mensaje(String destinatario, String asunto, String cuerpo) { }

    private Consumer<Mensaje> alEnviar = mensaje -> { }; // sin nadie registrado, el correo no se muestra

    /** Registra quién recibe cada correo enviado (en el kiosco, el aviso del Navegador). */
    public void alEnviar(Consumer<Mensaje> receptor) {
        this.alEnviar = receptor == null ? mensaje -> { } : receptor;
    }

    @Override
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if (destinatario == null || !destinatario.contains("@")) {
            return false; // un proveedor real rechazaría una dirección inválida
        }
        alEnviar.accept(new Mensaje(destinatario, asunto, cuerpo));
        return true;
    }
}
