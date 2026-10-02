package com.upv.sensoresbeacon;

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
