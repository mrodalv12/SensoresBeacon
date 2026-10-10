package com.upv.sensoresbeacon;


//==============================================================================================
//Nombre: RespuestaREST.java
//Descripcion: Codigo y cuerpo de la respuesta del servidor, e indica si la peticion
//              fue aceptada.
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
 * RespuestaREST = ( codigoRespuesta: Z, cuerpoRespuesta: Text )
 *
 * Diseno logico (seccion 5 - Clases):
 *
 *        codigoRespuesta : Z
 *        cuerpoRespuesta : Text
 *  ------------------- RespuestaREST ----------------
 *      |
 *      |
 *  --> RespuestaREST() -->
 *      |
 *      |
 *  --> getCodigoRespuesta() <--
 *      |
 *      |
 *  --> getCuerpoRespuesta() <--
 *      |
 *      |
 *  --> comoFue() <--
 *  -------------------------------------------------
 */
public class RespuestaREST {

    public static final int SIN_CODIGO = -1;

    private int codigoRespuesta;
    private String cuerpoRespuesta;

    //--------------------
    // --> RespuestaREST() -->
    //
    // Crea una respuesta vacia, sin codigo ni cuerpo.
    //--------------------
    public RespuestaREST() {
        this(SIN_CODIGO, "");
    }

    //--------------------
    // codigoRespuesta: Z, cuerpoRespuesta: Text --> RespuestaREST() -->
    //
    // Guarda el codigo y el cuerpo recibidos; un cuerpo nulo se deja vacio.
    //--------------------
    public RespuestaREST(int codigoRespuesta, String cuerpoRespuesta) {
        this.codigoRespuesta = codigoRespuesta;
        this.cuerpoRespuesta = cuerpoRespuesta == null ? "" : cuerpoRespuesta;
    }

    //--------------------
    // --> getCodigoRespuesta() <--
    //
    // devuelve el codigo de respuesta.
    //--------------------
    public int getCodigoRespuesta() {
        return codigoRespuesta;
    }

    //--------------------
    // --> getCuerpoRespuesta() <--
    //
    // devuelve el cuerpo de la respuesta.
    //--------------------
    public String getCuerpoRespuesta() {
        return cuerpoRespuesta;
    }

    //--------------------
    // --> comoFue() <--
    //
    // Indica si el servidor respondio con un codigo de la familia 2xx/3xx.
    //--------------------
    public boolean comoFue() {
        return codigoRespuesta >= 200 && codigoRespuesta < 400;
    }

    //--------------------
    // --> toString() <--
    //--------------------
    // describe la respuesta con su codigo y su cuerpo.
    @Override
    public String toString() {
        return "RespuestaREST{codigoRespuesta=" + codigoRespuesta
                + ", cuerpoRespuesta='" + cuerpoRespuesta + "'}";
    }
}
