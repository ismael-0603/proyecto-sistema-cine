package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Butaca {
    private int idButaca;
    private String fila;
    private int numero;
    private String tipo = "ESTANDAR"; // Estandar, preferencial/discapacitados
    
    public Butaca() { 
    }

    public Butaca(int idButaca, String fila, int numero, String tipo, boolean estado) {
        this.idButaca = idButaca;
        this.fila = fila;
        this.numero = numero;
        this.tipo = tipo;
    }

    public int getIdButaca() {
        return idButaca;
    }

    public void setIdButaca(int idButaca) {
        this.idButaca = idButaca;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    
}
