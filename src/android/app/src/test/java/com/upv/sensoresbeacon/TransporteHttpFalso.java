package com.upv.sensoresbeacon;

import java.util.ArrayList;
import java.util.List;

//==============================================================================================
//Nombre: TransporteHttpFalso.java
//Descripcion: Doble de prueba de TransporteHttp: registra la peticion realizada y devuelve
//              una respuesta preparada, sin abrir ningun socket.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Doble de prueba de TransporteHttp: registra la peticion realizada y devuelve
 * una respuesta preparada, sin abrir ningun socket.
 */
public class TransporteHttpFalso implements TransporteHttp {

    public static class Llamada {
        final String metodo;
        final String urlDestino;
        final String cuerpo;

        // Llamada(): guarda el metodo, la URL y el cuerpo de una peticion.
        Llamada(String metodo, String urlDestino, String cuerpo) {
            this.metodo = metodo;
            this.urlDestino = urlDestino;
            this.cuerpo = cuerpo;
        }
    }

    private final List<Llamada> llamadas = new ArrayList<>();
    private RespuestaREST respuesta;

    // TransporteHttpFalso(): doble que responde siempre 200 con ok.
    public TransporteHttpFalso() {
        this(new RespuestaREST(200, "{\"ok\":true}"));
    }

    // TransporteHttpFalso( respuesta ): doble que responde lo que se le indique.
    public TransporteHttpFalso(RespuestaREST respuesta) {
        this.respuesta = respuesta;
    }

    // enviar(): registra la peticion y devuelve la respuesta preparada.
    @Override
    public RespuestaREST enviar(String metodo, String urlDestino, String cuerpo) {
        llamadas.add(new Llamada(metodo, urlDestino, cuerpo));
        return respuesta;
    }

    // getLlamadas(): devuelve todas las peticiones registradas.
    public List<Llamada> getLlamadas() {
        return llamadas;
    }

    // getNumeroLlamadas(): cuenta las peticiones registradas.
    public int getNumeroLlamadas() {
        return llamadas.size();
    }

    // getUltimaLlamada(): devuelve la ultima peticion, o null si no hay ninguna.
    public Llamada getUltimaLlamada() {
        return llamadas.isEmpty() ? null : llamadas.get(llamadas.size() - 1);
    }

    // setRespuesta(): cambia la respuesta de la siguiente peticion.
    public void setRespuesta(RespuestaREST respuesta) {
        this.respuesta = respuesta;
    }
}
