package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class DateTime {
    private int dia;
    private int mes;
    private int anio;
    private int hora;
    private int minuto;

    public DateTime(int dia, int mes, int anio, int hora, int minuto) {
        validar(dia, mes, anio, hora, minuto);
        this.dia = dia;
        this.mes = mes;
        this.anio = anio;
        this.hora = hora;
        this.minuto = minuto;
    }
    
    // Constructor solo FECHA
    public DateTime(int dia, int mes, int anio){
        this(dia, mes, anio, 0, 0);
    }
    
    //Constructor solo HORA (fecha base 01/01/1970)
    public DateTime(int hora, int minuto){
        this(1,1,1970, hora, minuto);
    }
    
    // VALIDACIONES Y UTILIDADES DE CALENDARIO
    public static boolean esBisiesto(int anio) { 
        return (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0;
    } // calcula si un año es bisiesto o no
    
    public static int diasDelMes(int mes, int anio) {
        return switch (mes) {
            case 2 -> esBisiesto(anio) ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    } // calcula cuantos dias tienen los meses
    
    private static void validar(int dia, int mes, int anio, int hora, int minuto) {
        if (anio < 1) throw new IllegalArgumentException("Año inválido");
        if (mes < 1 || mes > 12) throw new IllegalArgumentException("Mes inválido");
        if (dia < 1 || dia > diasDelMes(mes, anio)) throw new IllegalArgumentException("Día inválido");
        if (hora < 0 || hora > 23) throw new IllegalArgumentException("Hora inválida");
        if (minuto < 0 || minuto > 59) throw new IllegalArgumentException("Minuto inválido");
    }
    
    // FECHA Y HORA ACTUAL (Perú, UTC-5)
    public static DateTime ahora() {
        long minutosTotales = System.currentTimeMillis() / 60000L - 5 * 60;
        long dias = minutosTotales / 1440;
        int minutosDelDia = (int) (minutosTotales % 1440);

        int anio = 1970;
        while (dias >= (esBisiesto(anio) ? 366 : 365)) {
            dias -= esBisiesto(anio) ? 366 : 365;
            anio++;
        }
        int mes = 1;
        while (dias >= diasDelMes(mes, anio)) {
            dias -= diasDelMes(mes, anio);
            mes++;
        }
        return new DateTime((int) dias + 1, mes, anio, minutosDelDia / 60, minutosDelDia % 60);
    }

    // COMPARACIONES
    // Valor numérico comparable (AAAAMMDDHHMM)
    public long toValorComparable() {
        return (long) anio * 100000000L + mes * 1000000L + dia * 10000L + hora * 100L + minuto;
    }

    public long toValorSoloFecha() {
        return (long) anio * 10000L + mes * 100L + dia;
    }

    public boolean esAntesDe(DateTime otra) {
        return this.toValorComparable() < otra.toValorComparable();
    }

    public boolean esDespuesDe(DateTime otra) {
        return this.toValorComparable() > otra.toValorComparable();
    }

    public boolean estaEnRangoFecha(DateTime inicio, DateTime fin) {
        long actual = this.toValorSoloFecha();
        return actual >= inicio.toValorSoloFecha() && actual <= fin.toValorSoloFecha();
    }
    
    // Día de la semana: 1 = lunes ... 7 = domingo (algoritmo de Sakamoto)
    public int diaDeLaSemana() {
        int[] t = {0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4};
        int y = (mes < 3) ? anio - 1 : anio;
        int dow = (y + y / 4 - y / 100 + y / 400 + t[mes - 1] + dia) % 7; // 0 = domingo
        return (dow == 0) ? 7 : dow;
    }

    // OPERACIONES
    // Suma días (acepta negativos, permite retroceder) respetando meses y años bisiestos
    public DateTime sumarDias(int dias) {
        int d = this.dia, m = this.mes, a = this.anio;
        while (dias > 0) {
            d++;
            if (d > diasDelMes(m, a)) { d = 1; m++; if (m > 12) { m = 1; a++; } }
            dias--;
        }
        while (dias < 0) {
            d--;
            if (d < 1) { m--; if (m < 1) { m = 12; a--; } d = diasDelMes(m, a); }
            dias++;
        }
        return new DateTime(d, m, a, this.hora, this.minuto);
    }

    // Suma minutos (acepta negativos,permite retroceder); si cruza medianoche, cambia el día
    public DateTime sumarMinutos(int minutosExtra) {
        int total = this.hora * 60 + this.minuto + minutosExtra;
        int diasExtra = Math.floorDiv(total, 1440);
        int resto = Math.floorMod(total, 1440);
        DateTime base = new DateTime(this.dia, this.mes, this.anio, resto / 60, resto % 60);
        return base.sumarDias(diasExtra);
    }

    // Para objetos "solo hora": suma minutos y da la vuelta a las 24 h (23:30 + 150 = 02:00)
    public DateTime sumarMinutosAHora(int minutosExtra) {
        int total = Math.floorMod(this.hora * 60 + this.minuto + minutosExtra, 1440);
        return new DateTime(total / 60, total % 60);
    }

    public int calcularAniosHasta(DateTime fechaActual) {
        int edad = fechaActual.anio - this.anio;
        if (fechaActual.mes < this.mes || (fechaActual.mes == this.mes && fechaActual.dia < this.dia)) {
            edad--;
        }
        return Math.max(0, edad);
    }

    // TEXTO 
    public String soloFecha() {
        return String.format("%02d/%02d/%04d", dia, mes, anio);
    }

    public String soloHora() {
        return String.format("%02d:%02d", hora, minuto);
    }

    @Override
    public String toString() {
        return soloFecha() + " " + soloHora();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateTime)) return false;
        return this.toValorComparable() == ((DateTime) o).toValorComparable();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toValorComparable());
    }

    // ---------- Getters y Setters (con validación) ----------
    public int getDia() { return dia; }
    public void setDia(int dia) { validar(dia, mes, anio, hora, minuto); this.dia = dia; }
    public int getMes() { return mes; }
    public void setMes(int mes) { validar(dia, mes, anio, hora, minuto); this.mes = mes; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { validar(dia, mes, anio, hora, minuto); this.anio = anio; }
    public int getHora() { return hora; }
    public void setHora(int hora) { validar(dia, mes, anio, hora, minuto); this.hora = hora; }
    public int getMinuto() { return minuto; }
    public void setMinuto(int minuto) { validar(dia, mes, anio, hora, minuto); this.minuto = minuto; }

    void calcularAniosHata(DateTime ahora) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
    
}
