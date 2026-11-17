package ucsur.pe.correo;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: simula el proveedor de correo para la demo y las pruebas: no envía nada,
 * solo guarda los mensajes para poder revisarlos.
 */
public class CorreoSimulado implements ServicioCorreo {
    /** Un correo "enviado". */
    public record Mensaje(String destinatario, String asunto, String cuerpo) { }

    private final List<Mensaje> enviados = new ArrayList<>();

    @Override
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if (destinatario == null || !destinatario.contains("@")) {
            return false; // un proveedor real rechazaría una dirección inválida
        }
        enviados.add(new Mensaje(destinatario, asunto, cuerpo));
        return true;
    }

    public List<Mensaje> getEnviados() { return List.copyOf(enviados); }
}
