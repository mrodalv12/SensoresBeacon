package com.upv.sensoresbeacon;


//==============================================================================================
//Nombre: Medicion.java
//Descripcion: Modelo de datos de una medicion: su tipo (CO2, temperatura o ruido) y su
//              valor, construida a partir de una trama iBeacon.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================


/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 2.2 - Aggregations):
 *
 * Medicion = ( tipoDeMedicion: Text, valor: R )
 *
 * Diseno logico (seccion 5 - Clases):
 *
 *        tipoDeMedicion : Text
 *        valor          : R
 *  -------------------- Medicion -------------------
 *      |
 *      |
 *  --> Medicion() -->
 *      |
 *      |
 *  --> asignarTipo() -->
 *      |
 *      |
 *  --> getTipoDeMedicion() <--
 *      |
 *      |
 *  --> getValor() <--
 *  -----------------------------------------------
 */
public class Medicion {

    private String tipoDeMedicion;
    private double valor;

    //--------------------
    // --> Medicion() -->
    //
    // Crea una medicion vacia, pendiente de asignar tipo y valor.
    //--------------------
    public Medicion() {
        this.tipoDeMedicion = MedicionesID.TIPO_DESCONOCIDO;
        this.valor = 0.0;
    }

    //--------------------
    // valor: R --> Medicion() -->
    //
    // Crea una medicion con el valor recibido y el tipo aun sin asignar.
    //--------------------
    public Medicion(double valor) {
        this();
        this.valor = valor;
    }

    //--------------------
    // valorMinor: N --> asignarTipo() --> tipo: Text
    //
    // El valor numérico recibido en el campo "minor" de la trama iBeacon
    // identifica el tipo de medicion segun la enumeracion MedicionesID.
    // Asigna el resultado a "tipoDeMedicion" y lo devuelve como cadena.
    //--------------------
    public String asignarTipo(int valorMinor) {
        this.tipoDeMedicion = MedicionesID.getTipo(valorMinor);
        return this.tipoDeMedicion;
    }

    //--------------------
    // --> getTipoDeMedicion() <--
    //
    // devuelve el tipo de la medicion: co2, temperatura o ruido.
    //--------------------
    public String getTipoDeMedicion() {
        return tipoDeMedicion;
    }

    //--------------------
    // --> getValor() <--
    //
    // devuelve el valor medido.
    //--------------------
    public double getValor() {
        return valor;
    }

    //--------------------
    // valor: R --> setValor() -->
    //--------------------
    public void setValor(double valor) {
        this.valor = valor;
    }

    //--------------------
    // trama: TramaIBeacon --> desdeTrama() --> medicion: Medicion --x
    //
    // El tipo de medicion va en el byte alto del campo "major" y el valor
    // medido en el "minor", segun codifica el emisor:
    // major = (tipo << 8) + contador.
    //--------------------
    public static Medicion desdeTrama(TramaIBeacon trama) {
        Medicion medicion = new Medicion(trama.getMinorComoInt());
        medicion.asignarTipo(trama.getTipoDeMedicionComoInt());
        return medicion;
    }

    //--------------------
    // --> toString() <--
    //--------------------
    // describe la medicion con su tipo y su valor.
    @Override
    public String toString() {
        return "Medicion{tipoDeMedicion='" + tipoDeMedicion + "', valor=" + valor + '}';
    }
}
