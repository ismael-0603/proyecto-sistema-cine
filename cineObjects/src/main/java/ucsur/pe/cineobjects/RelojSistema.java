package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.Reloj;

/**
 * Por qué existe: es el reloj real del sistema (hora de Perú, UTC-5). Es el que se usa
 * fuera de las pruebas; sigue usando nuestra clase DateTime.
 */
public class RelojSistema implements Reloj {
    @Override
    public DateTime ahora() {
        return DateTime.ahora();
    }
}
