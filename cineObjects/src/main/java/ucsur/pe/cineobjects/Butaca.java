package ucsur.pe.cineobjects;

/**
 * Por qué existe: es un asiento físico de una sala (fila + número, por ejemplo A5).
 * Sabe su tipo y si está libre en una función.
 */
public class Butaca {
    // Tipos de butaca (constantes: se usan como Butaca.NOMBRE y su valor no cambia).
    public static final String ESTANDAR = "ESTANDAR"; // asiento normal
    public static final String PREFERENCIAL = "PREFERENCIAL"; // asiento de mejor ubicación o comodidad
    public static final String DISCAPACIDAD = "DISCAPACIDAD"; // asiento accesible; no se recomienda automáticamente

    private int idButaca;
    private String fila; // de 1 a 3 letras, se guarda en mayúsculas
    private int numero;
    private String tipo;
    private Sala sala;

    public Butaca(int idButaca, String fila, int numero, String tipo, Sala sala) {
        if (fila == null || fila.trim().isEmpty() || fila.trim().length() > 3) {
            throw new IllegalArgumentException("La fila es obligatoria (máx. 3 caracteres)");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de butaca debe ser mayor a 0");
        }
        if (!ESTANDAR.equals(tipo) && !PREFERENCIAL.equals(tipo) && !DISCAPACIDAD.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de butaca inválido: " + tipo);
        }
        if (sala == null) {
            throw new IllegalArgumentException("La sala es obligatoria");
        }
        this.idButaca = idButaca;
        this.fila = fila.trim().toUpperCase();
        this.numero = numero;
        this.tipo = tipo;
        this.sala = sala;
    }

    /** Código visible del asiento: fila y número, como A5. */
    public String getCodigoAsiento() {
        return fila + numero;
    }

    /** Indica si es una butaca para personas con discapacidad. */
    public boolean esAccesible() {
        return DISCAPACIDAD.equals(tipo);
    }

    /** Consulta la disponibilidad de esta butaca para una función; si no hay función, devuelve false. */
    public boolean estaDisponible(Funcion funcion) {
        if (funcion == null) return false;
        return funcion.esButacaDisponible(this);
    }

    public int getIdButaca() { return idButaca; }
    public String getFila() { return fila; }
    public int getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public Sala getSala() { return sala; }

    @Override
    public String toString() {
        return getCodigoAsiento() + " (" + tipo + ")";
    }
}
