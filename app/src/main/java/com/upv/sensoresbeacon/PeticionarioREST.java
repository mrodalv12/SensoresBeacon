package com.upv.sensoresbeacon;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 5 - Clases):
 *
 *        metodo          : Text
 *        urlDestino      : Text
 *        elCuerpo        : Text
 *        laRespuesta     : RespuestaREST
 *        codigo          : Z
 *  ------------------ PeticionarioREST -----------------
 *      |
 *      |
 *  --> PeticionarioREST() -->
 *      |
 *      |
 *  --> hacerPeticionarioREST() -->
 *      |
 *      |
 *  --> doInBackground() <--
 *      |
 *      |
 *  --> comoFue() <--
 *      |
 *      |
 *  --> getCodigo() <--
 *      |
 *      |
 *  --> getCuerpo() <--
 *  -------------------------------------------------
 *
 * Envoltorio asincrono sobre el cliente HTTP. El trabajo real se delega en un
 * "TransporteHttp" intercambiable para poder probarlo sin servidor.
 */
@SuppressWarnings("deprecation")
public class PeticionarioREST extends AsyncTask<Void, Void, RespuestaREST> {

    public static final String ETIQUETA = "PeticionarioREST";
    public static final String METODO_POST = "POST";
    public static final String METODO_GET = "GET";
    public static final String TIPO_CONTENIDO = "application/json; charset=utf-8";

    private String metodo;
    private String urlDestino;
    private String elCuerpo;
    private RespuestaREST laRespuesta;
    private int codigo;
    private String cuerpo;
    private TransporteHttp transporte;

    /**
     * --> PeticionarioREST() -->
     */
    public PeticionarioREST() {
        this(METODO_GET, "", "", new TransporteHttpReal());
    }

    /**
     * metodo: Text, urlDestino: Text, elCuerpo: Text --> PeticionarioREST() -->
     */
    public PeticionarioREST(String metodo, String urlDestino, String elCuerpo) {
        this(metodo, urlDestino, elCuerpo, new TransporteHttpReal());
    }

    /**
     * metodo: Text, urlDestino: Text, elCuerpo: Text --> PeticionarioREST() -->
     */
    public PeticionarioREST(String metodo, String urlDestino, String elCuerpo, TransporteHttp transporte) {
        this.metodo = metodo;
        this.urlDestino = urlDestino;
        this.elCuerpo = elCuerpo == null ? "" : elCuerpo;
        this.transporte = transporte;
        this.laRespuesta = new RespuestaREST();
    }

    /**
     * urlDestino: Text, elCuerpo: Text --> crearPOST() --> peticionario: PeticionarioREST --x
     */
    public static PeticionarioREST crearPOST(String urlDestino, String elCuerpo) {
        return new PeticionarioREST(METODO_POST, urlDestino, elCuerpo);
    }

    /**
     * --> hacerPeticionarioREST() -->
     *
     * Permite ejecutar la peticion de forma sincrona y comprobable en pruebas.
     */
    public RespuestaREST hacerPeticionarioREST() {
        laRespuesta = transporte.enviar(metodo, urlDestino, elCuerpo);
        codigo = laRespuesta.getCodigoRespuesta();
        cuerpo = laRespuesta.getCuerpoRespuesta();
        Log.i(ETIQUETA, metodo + " " + urlDestino + " cuerpo=" + elCuerpo
                + " -> codigo=" + codigo + " respuesta=" + cuerpo);
        return laRespuesta;
    }

    /**
     * --> doInBackground() <--
     *
     * Metodo ejecutado por AsyncTask en el hilo de segundo plano.
     */
    @Override
    protected RespuestaREST doInBackground(Void... parametros) {
        return hacerPeticionarioREST();
    }

    /**
     * --> onPostExecute() -->
     */
    @Override
    protected void onPostExecute(RespuestaREST resultado) {
        Log.i(ETIQUETA, "Peticion finalizada, respuesta=" + resultado);
    }

    /**
     * --> comoFue() <--
     */
    public boolean comoFue() {
        return laRespuesta.comoFue();
    }

    /**
     * --> getCodigo() <--
     */
    public int getCodigo() {
        return codigo;
    }

    /**
     * --> getCuerpo() <--
     */
    public String getCuerpo() {
        return cuerpo;
    }

    /**
     * --> getLaRespuesta() <--
     */
    public RespuestaREST getLaRespuesta() {
        return laRespuesta;
    }

    /**
     * transporte: TransporteHttp --> setTransporte() -->
     *
     * Inyecta un doble de prueba en lugar del cliente HTTP real.
     */
    public void setTransporte(TransporteHttp transporte) {
        this.transporte = transporte;
    }

    /**
     * --> getMetodo() <--
     */
    public String getMetodo() {
        return metodo;
    }

    /**
     * --> getUrlDestino() <--
     */
    public String getUrlDestino() {
        return urlDestino;
    }

    /**
     * --> getElCuerpo() <--
     */
    public String getElCuerpo() {
        return elCuerpo;
    }
}
