package com.upv.sensoresbeacon;

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

    /**
     * --> RespuestaREST() -->
     */
    public RespuestaREST() {
        this(SIN_CODIGO, "");
    }

    /**
     * codigoRespuesta: Z, cuerpoRespuesta: Text --> RespuestaREST() -->
     */
    public RespuestaREST(int codigoRespuesta, String cuerpoRespuesta) {
        this.codigoRespuesta = codigoRespuesta;
        this.cuerpoRespuesta = cuerpoRespuesta == null ? "" : cuerpoRespuesta;
    }

    /**
     * --> getCodigoRespuesta() <--
     */
    public int getCodigoRespuesta() {
        return codigoRespuesta;
    }

    /**
     * --> getCuerpoRespuesta() <--
     */
    public String getCuerpoRespuesta() {
        return cuerpoRespuesta;
    }

    /**
     * --> comoFue() <--
     *
     * Indica si el servidor respondio con un codigo de la familia 2xx/3xx.
     */
    public boolean comoFue() {
        return codigoRespuesta >= 200 && codigoRespuesta < 400;
    }

    /**
     * --> toString() <--
     */
    @Override
    public String toString() {
        return "RespuestaREST{codigoRespuesta=" + codigoRespuesta
                + ", cuerpoRespuesta='" + cuerpoRespuesta + "'}";
    }
}
