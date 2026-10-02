package com.upv.sensoresbeacon;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

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
 * Logica "fake": en lugar de persistir en una base de datos, performs una
 * peticion POST contra el servidor REST (POST /mediciones), que sera quien
 * escriba los datos en la base de datos en el futuro.
 */
public class LogicaFake {

    public static final String ETIQUETA = "LogicaFake";
    public static final String RUTA_MEDICIONES = "/mediciones";
    public static final String URL_POR_DEFECTO = "http://10.0.2.2:8080" + RUTA_MEDICIONES;

    private static final String C_TIPO = "tipo";
    private static final String C_VALOR = "valor";
    private static final String C_UUID = "uuid";
    private static final String C_TX_POWER = "txPower";
    private static final String C_MAJOR = "major";
    private static final String C_MINOR = "minor";
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
     * medicion: Medicion, uuid: Byte, txPower: Byte, major: Byte, minor: Byte,
     * nombre_emisora: Text --> guardarMedicion() -->
     *
     * Realiza el POST /mediciones con los datos de la medicion y del beacon.
     *
     * "txPower" se mantiene con signo porque es una potencia en dBm y puede
     * ser negativa; "major" y "minor" son naturales de 16 bits sin signo.
     */
    public void guardarMedicion(Medicion medicion, String uuid, int txPower, int major, int minor,
                                String nombre_emisora) {
        this.medicion = medicion == null ? new Medicion() : medicion;
        String cuerpo = construirCuerpo(uuid, txPower, major, minor, nombre_emisora);

        peticionario = new PeticionarioREST(
                PeticionarioREST.METODO_POST, urlServidor, cuerpo, transporte);
        RespuestaREST respuesta = peticionario.hacerPeticionarioREST();

        Log.i(ETIQUETA, "guardarMedicion: " + cuerpo + " -> " + respuesta);
    }

    /**
     * uuid: Text, txPower: Z, major: N, minor: N, nombre_emisora: Text
     *   --> construirCuerpo() --> cuerpo: Text
     *
     * Serializa en JSON la medicion junto con los datos del beacon.
     */
    public String construirCuerpo(String uuid, int txPower, int major, int minor, String nombre_emisora) {
        JSONObject cuerpo = new JSONObject();
        try {
            cuerpo.put(C_TIPO, medicion.getTipoDeMedicion());
            cuerpo.put(C_VALOR, medicion.getValor());
            cuerpo.put(C_UUID, uuid == null ? "" : uuid);
            cuerpo.put(C_TX_POWER, txPower);
            cuerpo.put(C_MAJOR, major);
            cuerpo.put(C_MINOR, minor);
            cuerpo.put(C_NOMBRE_EMISORA, nombre_emisora == null ? "" : nombre_emisora);
        } catch (JSONException e) {
            Log.e(ETIQUETA, "No se pudo construir el cuerpo JSON: " + e.getMessage());
            return "{}";
        }
        return cuerpo.toString();
    }

    /**
     * --> getMedicion() <--
     */
    public Medicion getMedicion() {
        return medicion;
    }

    /**
     * --> getUrlServidor() <--
     */
    public String getUrlServidor() {
        return urlServidor;
    }

    /**
     * --> getUltimaPeticion() <--
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
