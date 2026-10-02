package com.upv.sensoresbeacon;

import java.util.ArrayList;
import java.util.List;

/**
 * Doble de prueba de TransporteHttp: registra la peticion realizada y devuelve
 * una respuesta preparada, sin abrir ningun socket.
 */
public class TransporteHttpFalso implements TransporteHttp {

    public static class Llamada {
        final String metodo;
        final String urlDestino;
        final String cuerpo;

        Llamada(String metodo, String urlDestino, String cuerpo) {
            this.metodo = metodo;
            this.urlDestino = urlDestino;
            this.cuerpo = cuerpo;
        }
    }

    private final List<Llamada> llamadas = new ArrayList<>();
    private RespuestaREST respuesta;

    public TransporteHttpFalso() {
        this(new RespuestaREST(200, "{\"ok\":true}"));
    }

    public TransporteHttpFalso(RespuestaREST respuesta) {
        this.respuesta = respuesta;
    }

    @Override
    public RespuestaREST enviar(String metodo, String urlDestino, String cuerpo) {
        llamadas.add(new Llamada(metodo, urlDestino, cuerpo));
        return respuesta;
    }

    public List<Llamada> getLlamadas() {
        return llamadas;
    }

    public int getNumeroLlamadas() {
        return llamadas.size();
    }

    public Llamada getUltimaLlamada() {
        return llamadas.isEmpty() ? null : llamadas.get(llamadas.size() - 1);
    }

    public void setRespuesta(RespuestaREST respuesta) {
        this.respuesta = respuesta;
    }
}
