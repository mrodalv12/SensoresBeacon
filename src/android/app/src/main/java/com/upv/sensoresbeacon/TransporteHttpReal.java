package com.upv.sensoresbeacon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

//==============================================================================================
//Nombre: TransporteHttpReal.java
//Descripcion: Implementacion real de TransporteHttp sobre HttpURLConnection.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Diseno logico (seccion 5 - Clases):
 *
 *  ------------------ TransporteHttpReal ----------------
 *      |
 *      |
 *  --> enviar() -->                        --x
 *  -----------------------------------------------------
 *
 * Implementacion real de TransporteHttp sobre HttpURLConnection.
 */
public class TransporteHttpReal implements TransporteHttp {

    public static final int TIEMPO_LECTURA_MS = 10000;

    private final int tiempoLecturaMs;

    //--------------------
    // --> TransporteHttpReal() -->
    //
    // Usa el tiempo de lectura por defecto.
    //--------------------
    public TransporteHttpReal() {
        this(TIEMPO_LECTURA_MS);
    }

    //--------------------
    // tiempoLecturaMs: Z --> TransporteHttpReal() -->
    //
    // Fija cuantos milisegundos espera como maximo a la respuesta del servidor.
    //--------------------
    public TransporteHttpReal(int tiempoLecturaMs) {
        this.tiempoLecturaMs = tiempoLecturaMs;
    }

    //--------------------
    // metodo: Text, urlDestino: Text, cuerpo: Text --> enviar() --> laRespuesta: RespuestaREST
    //--------------------
    @Override
    public RespuestaREST enviar(String metodo, String urlDestino, String cuerpo) {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(urlDestino).openConnection();
            conexion.setRequestMethod(metodo);
            conexion.setConnectTimeout(tiempoLecturaMs);
            conexion.setReadTimeout(tiempoLecturaMs);
            conexion.setRequestProperty("Accept", "application/json");

            if (cuerpo != null && !cuerpo.isEmpty()) {
                conexion.setDoOutput(true);
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(cuerpo.getBytes(StandardCharsets.UTF_8));
                }
            }

            int codigo = conexion.getResponseCode();
            return new RespuestaREST(codigo, leerFlujo(codigo < 400
                    ? conexion.getInputStream()
                    : conexion.getErrorStream()));
        } catch (IOException e) {
            return new RespuestaREST(RespuestaREST.SIN_CODIGO,
                    "{\"error\":\"" + e.getClass().getSimpleName() + "\"}");
        } finally {
            if (conexion != null) {
                conexion.disconnect();
            }
        }
    }

    //--------------------
    // flujo: InputStream --> leerFlujo() --> texto: Text
    //
    // Convierte el flujo de bytes de la respuesta en una cadena, o devuelve
    // cadena vacia si es null.
    //--------------------
    private String leerFlujo(InputStream flujo) throws IOException {
        if (flujo == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(flujo, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                sb.append(linea);
            }
        }
        return sb.toString();
    }
}
