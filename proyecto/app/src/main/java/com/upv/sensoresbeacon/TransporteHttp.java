package com.upv.sensoresbeacon;


//==============================================================================================
//Nombre: TransporteHttp.java
//Descripcion: Interfaz de transporte HTTP: permite cambiar la comunicacion real por un
//              doble de prueba.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================


/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 4 - Firmas):
 *
 * metodo: Text, urlDestino: Text, cuerpo: Text
 *      --> enviar() --> RespuestaREST
 *
 * Permite sustituir la comunicacion HTTP real por un doble de prueba, de modo
 * que la logica fake pueda verificarse sin necesidad de un servidor real.
 */
public interface TransporteHttp {

    /**
     * metodo: Text, urlDestino: Text, cuerpo: Text --> enviar() --> laRespuesta: RespuestaREST
     */
    RespuestaREST enviar(String metodo, String urlDestino, String cuerpo);
}
