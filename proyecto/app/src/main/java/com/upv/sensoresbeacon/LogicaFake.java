package com.upv.sensoresbeacon;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

//==============================================================================================
//Nombre: LogicaFake.java
//Descripcion: Logica "fake": en vez de persistir en local, envia cada medicion con un
//              POST /mediciones al servidor REST.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 5 - Clases):
 *
 *        medicion     : Medicion
 *        urlServidor  : Text
 *  -------------------- LogicaFake ------------------
 *      |
 *      |
 *  --> LogicaFake() -->
 *      |
 *      |
 *  --> guardarMedicion() -->
 *      |
 *      |
 *  --> construirCuerpo() <--
 *      |
 *      |
 *  --> getMedicion() <--
 *  -------------------------------------------------
 *
 * Logica "fake": en lugar de persistir en una base de datos, realiza una
 * peticion POST contra el servidor REST (POST /mediciones), que sera quien
 * escriba los datos en la base de datos en el futuro.
 *
 * Por el POST solo viajan tipo, valor y nombre_emisora. Los datos del beacon
 * (uuid, txPower, major, minor) se quedan en la app: la uuid se usa unicamente
 * para buscar el iBeacon y los demas solo se muestran en pantalla.
 */
public class LogicaFake {

    public static final String ETIQUETA = "LogicaFake";
    public static final String RUTA_MEDICIONES = "/mediciones";
    public static final String URL_POR_DEFECTO = "http://10.0.2.2:8080" + RUTA_MEDICIONES;

    private static final String C_TIPO = "tipo";
    private static final String C_VALOR = "valor";
    private static final String C_NOMBRE_EMISORA = "nombre_emisora";

    private Medicion medicion;
    private String urlServidor;
    private TransporteHttp transporte;
    private PeticionarioREST peticionario;

    /**
     * --> LogicaFake() -->
     *
     * Logica fake apuntando al servidor REST local del emulador.
     */
    public LogicaFake() {
        this(URL_POR_DEFECTO);
    }

    /**
     * urlServidor: Text --> LogicaFake() -->
     */
    public LogicaFake(String urlServidor) {
        this(urlServidor, new TransporteHttpReal());
    }

    /**
     * urlServidor: Text --> LogicaFake() -->
     */
    public LogicaFake(String urlServidor, TransporteHttp transporte) {
        this.urlServidor = urlServidor;
        this.transporte = transporte;
        this.medicion = new Medicion();
    }

    /**
     * medicion: Medicion, nombre_emisora: Text --> guardarMedicion() -->
     *
     * Realiza el POST /mediciones con los tres campos del contrato: tipo, valor
     * y nombre_emisora. Los datos del beacon no viajan en la peticion.
     */
    public void guardarMedicion(Medicion medicion, String nombre_emisora) {
        this.medicion = medicion == null ? new Medicion() : medicion;
        String cuerpo = construirCuerpo(nombre_emisora);

        peticionario = new PeticionarioREST(
                PeticionarioREST.METODO_POST, urlServidor, cuerpo, transporte);
        RespuestaREST respuesta = peticionario.hacerPeticionarioREST();

        Log.i(ETIQUETA, "guardarMedicion: " + cuerpo + " -> " + respuesta);
    }

    /**
     * nombre_emisora: Text --> construirCuerpo() --> cuerpo: Text
     *
     * Serializa en JSON la medicion (tipo y valor) con el nombre de la emisora.
     */
    public String construirCuerpo(String nombre_emisora) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put(C_TIPO, medicion.getTipoDeMedicion());
            cuerpo.put(C_VALOR, medicion.getValor());
            cuerpo.put(C_NOMBRE_EMISORA, nombre_emisora == null ? "" : nombre_emisora);
        } catch (JSONException e) {
            Log.e(ETIQUETA, "No se pudo construir el cuerpo JSON: " + e.getMessage());
            return "{}";
        }
        return cuerpo.toString();
    }

    /**
     * --> getMedicion() <--
      *
      * devuelve la ultima medicion que se ha guardado en la logica.
     */
    public Medicion getMedicion() {
        return medicion;
    }

    /**
     * --> getUrlServidor() <--
      *
      * devuelve la URL del servidor REST configurada.
     */
    public String getUrlServidor() {
        return urlServidor;
    }

    /**
     * --> getUltimaPeticion() <--
      *
      * devuelve la ultima peticion HTTP realizada.
     */
    public PeticionarioREST getUltimaPeticion() {
        return peticionario;
    }

    /**
     * urlServidor: Text --> setUrlServidor() -->
     */
    public void setUrlServidor(String urlServidor) {
        this.urlServidor = urlServidor;
    }

    /**
     * peticionario: PeticionarioREST --> setTransporte() -->
     *
     * Permite inyectar un doble de prueba en lugar del cliente HTTP real.
     */
    public void setTransporte(TransporteHttp transporte) {
        this.transporte = transporte;
    }
}
