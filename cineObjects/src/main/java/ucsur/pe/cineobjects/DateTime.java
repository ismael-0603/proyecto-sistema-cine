package ucsur.pe.cineobjects;

/**
 * Guarda una fecha y hora, valida días reales del calendario y ofrece operaciones de comparación y suma.
 * La fecha actual se calcula con UTC-5, que corresponde a la hora de Perú.
 */
public class DateTime {
    private int dia; // Día válido para el mes y año indicados.
    private int mes; // Mes del año, de 1 a 12.
    private int anio; // Año positivo.
    private int hora; // Hora en formato de 24 horas, de 0 a 23.
    private int minuto; // Minutos, de 0 a 59.

    public DateTime(int dia, int mes, int anio, int hora, int minuto) {
        validar(dia, mes, anio, hora, minuto);
        this.dia = dia;
        this.mes = mes;
        this.anio = anio;
        this.hora = hora;
        this.minuto = minuto;
    }
    
    /** Crea una fecha y asigna medianoche como hora. */
    public DateTime(int dia, int mes, int anio){
        this(dia, mes, anio, 0, 0);
    }
    
    /** Crea una hora usando 01/01/1970 como fecha de referencia. */
    public DateTime(int hora, int minuto){
        this(1,1,1970, hora, minuto);
    }
    
    /** Un año es bisiesto según la regla del calendario gregoriano. */
    public static boolean esBisiesto(int anio) { 
        return (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0;
    }
    
    /** Devuelve 28 o 29, 30 o 31 días según el mes y el año. */
    public static int diasDelMes(int mes, int anio) {
        return switch (mes) {
            case 2 -> esBisiesto(anio) ? 29 : 28;
            case 4, 6, 9, 11 -> 30;
            default -> 31;
        };
    }
    
    /** Lanza error si algún componente está fuera de rango o el día no existe en ese mes. */
    private static void validar(int dia, int mes, int anio, int hora, int minuto) {
        if (anio < 1) throw new IllegalArgumentException("Año inválido");
        if (mes < 1 || mes > 12) throw new IllegalArgumentException("Mes inválido");
        if (dia < 1 || dia > diasDelMes(mes, anio)) throw new IllegalArgumentException("Día inválido");
        if (hora < 0 || hora > 23) throw new IllegalArgumentException("Hora inválida");
        if (minuto < 0 || minuto > 59) throw new IllegalArgumentException("Minuto inválido");
    }
    
    /** Devuelve la fecha y hora actual usando el huso horario fijo UTC-5 (Perú). */
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

    // Las claves numéricas mantienen el orden cronológico al compararse.
    /** Convierte fecha y hora en un número ordenable; se usa para comparar y crear referencias. */
    public long toValorComparable() {
        return (long) anio * 100000000L + mes * 1000000L + dia * 10000L + hora * 100L + minuto;
    }

    /** Devuelve una clave numérica de año, mes y día, sin considerar la hora. */
    public long toValorSoloFecha() {
        return (long) anio * 10000L + mes * 100L + dia;
    }

    /** Compara fecha y hora: true si esta es anterior a la otra. */
    public boolean esAntesDe(DateTime otra) {
        return this.toValorComparable() < otra.toValorComparable();
    }

    /** Compara fecha y hora: true si esta es posterior a la otra. */
    public boolean esDespuesDe(DateTime otra) {
        return this.toValorComparable() > otra.toValorComparable();
    }

    /** Comprueba si la fecha está entre inicio y fin, incluyendo ambos extremos. */
    public boolean estaEnRangoFecha(DateTime inicio, DateTime fin) {
        long actual = this.toValorSoloFecha();
        return actual >= inicio.toValorSoloFecha() && actual <= fin.toValorSoloFecha();
    }
    
    /** Devuelve el día de la semana: 1 corresponde al lunes y 7 al domingo. */
    public int diaDeLaSemana() {
        int[] t = {0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4};
        int y = (mes < 3) ? anio - 1 : anio;
        int dow = (y + y / 4 - y / 100 + y / 400 + t[mes - 1] + dia) % 7; // 0 = domingo
        return (dow == 0) ? 7 : dow;
    }

    /** Devuelve una fecha nueva con los días sumados (o restados si son negativos), ajustando mes y año. */
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

    /** Devuelve una fecha y hora nuevas con los minutos sumados; al cruzar medianoche cambia el día. */
    public DateTime sumarMinutos(int minutosExtra) {
        int total = this.hora * 60 + this.minuto + minutosExtra;
        int diasExtra = Math.floorDiv(total, 1440);
        int resto = Math.floorMod(total, 1440);
        DateTime base = new DateTime(this.dia, this.mes, this.anio, resto / 60, resto % 60);
        return base.sumarDias(diasExtra);
    }

    /** Suma minutos solo a la hora, sin cambiar la fecha; al pasar medianoche vuelve a las 00:00. */
    public DateTime sumarMinutosAHora(int minutosExtra) {
        int total = Math.floorMod(this.hora * 60 + this.minuto + minutosExtra, 1440);
        return new DateTime(total / 60, total % 60);
    }

    /** Calcula los años cumplidos desde esta fecha hasta la indicada; nunca devuelve negativos. */
    public int calcularAniosHasta(DateTime fechaActual) {
        int edad = fechaActual.anio - this.anio;
        if (fechaActual.mes < this.mes || (fechaActual.mes == this.mes && fechaActual.dia < this.dia)) {
            edad--;
        }
        return Math.max(0, edad);
    }

    /** Fecha con formato dd/MM/yyyy. */
    public String soloFecha() {
        return String.format("%02d/%02d/%04d", dia, mes, anio);
    }

    /** Hora con formato HH:mm. */
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

    // Cada setter valida el nuevo valor junto con los demás componentes antes de guardarlo.
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
    
}
