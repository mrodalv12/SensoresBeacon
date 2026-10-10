package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

//==============================================================================================
//Nombre: LogicaFakeIntegracionTest.java
//Descripcion: Prueba de integracion de la logica fake contra un servidor HTTP real
//              levantado en un puerto local efimero.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Prueba de integracion: levanta un servidor HTTP real en un puerto local
 * efimero, comprueba que el POST de la logica fake llega con el metodo, la
 * ruta y el cuerpo JSON esperados, y que la respuesta se interpreta bien.
 */
public class LogicaFakeIntegracionTest {

    private static final String RESPUESTA = "{\"id\":1,\"mensaje\":\"guardada\"}";
    private static final int ESPERA_SEGUNDOS = 10;

    private ServerSocket servidor;
    private Thread hiloServidor;
    private String base;
    private final List<String> peticiones = new ArrayList<>();
    private final CountDownLatch peticionRecibida = new CountDownLatch(1);
    private final AtomicReference<String> cabeceraRecibida = new AtomicReference<>();

    // arrancarServidor(): levanta el servidor HTTP real en un puerto efimero.
    @Before
    public void arrancarServidor() throws Exception {
        servidor = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        base = "http://127.0.0.1:" + servidor.getLocalPort() + "/mediciones";
        hiloServidor = new Thread(this::atender, "servidorDePruebas");
        hiloServidor.setDaemon(true);
        hiloServidor.start();
    }

    // pararServidor(): cierra el servidor y espera al hilo que lo atiende.
    @After
    public void pararServidor() throws Exception {
        if (servidor != null) {
            servidor.close();
        }
        if (hiloServidor != null) {
            hiloServidor.join(ESPERA_SEGUNDOS * 1000L);
        }
    }

    /**
     * Acepta una conexion, guarda la peticion y responde 201 Created.
     */
    private void atender() {
        try (Socket cliente = servidor.accept()) {
            BufferedReader lector = new BufferedReader(
                    new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8));
            int longitudCuerpo = 0;
            String linea;
            while ((linea = lector.readLine()) != null && !linea.isEmpty()) {
                peticiones.add(linea);
                if (linea.toLowerCase().startsWith("content-length:")) {
                    longitudCuerpo = Integer.parseInt(linea.split(":")[1].trim());
                }
            }
            char[] cuerpo = new char[longitudCuerpo];
            if (longitudCuerpo > 0) {
                new String(cuerpo, 0, lector.read(cuerpo, 0, longitudCuerpo));
            }
            StringBuilder bruto = new StringBuilder();
            for (String peticion : peticiones) {
                bruto.append(peticion).append('\n');
            }
            bruto.append(cuerpo, 0, longitudCuerpo);
            cabeceraRecibida.set(bruto.toString());

            byte[] respuesta = RESPUESTA.getBytes(StandardCharsets.UTF_8);
            OutputStream salida = cliente.getOutputStream();
            salida.write(("HTTP/1.1 201 Created\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + respuesta.length + "\r\n"
                    + "Connection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            salida.write(respuesta);
            salida.flush();
        } catch (IOException e) {
            // el servidor de pruebas se cierra al terminar la prueba
        } finally {
            peticionRecibida.countDown();
        }
    }

    // el POST llega con el metodo, la ruta y el cuerpo JSON esperados.
    @Test
    public void elPostDeLaMedicionLlegaAlServidorConLosTresCampos() throws Exception {
        LogicaFake logica = new LogicaFake(base);
        Medicion medicion = new Medicion(2450);
        medicion.asignarTipo(12);

        logica.guardarMedicion(medicion, "GTI-3A-MJ");

        assertTrue("El servidor no recibio ninguna peticion",
                peticionRecibida.await(ESPERA_SEGUNDOS, TimeUnit.SECONDS));

        String bruto = cabeceraRecibida.get();
        assertTrue(bruto.startsWith("POST /mediciones HTTP/1.1"));
        assertTrue(bruto.contains("Content-Type: application/json; charset=utf-8"));

        String cuerpo = bruto.substring(bruto.indexOf("{\"tipo\""));
        JSONObject json = new JSONObject(cuerpo);
        // solo tipo, valor y nombre_emisora: ningun campo del beacon.
        assertEquals(3, json.length());
        assertEquals("temperatura", json.getString("tipo"));
        assertEquals(2450.0, json.getDouble("valor"), 0.0001);
        assertEquals("GTI-3A-MJ", json.getString("nombre_emisora"));
    }

    // el codigo 200 del servidor se interpreta como peticion aceptada.
    @Test
    public void laRespuestaDelServidorSeInterpretaComoExitosa() {
        LogicaFake logica = new LogicaFake(base);
        Medicion medicion = new Medicion(812);
        medicion.asignarTipo(11);

        logica.guardarMedicion(medicion, "GTI-3A-MJ");

        PeticionarioREST peticion = logica.getUltimaPeticion();
        assertEquals(201, peticion.getCodigo());
        assertTrue(peticion.comoFue());
        assertEquals(RESPUESTA, peticion.getCuerpo());
    }

    // si el servidor no responde, se devuelve -1 sin lanzar excepcion.
    @Test
    public void unServidorIlegibleDevuelveCodigoMenosUnoSinExcepcion() {
        LogicaFake logica = new LogicaFake("http://127.0.0.1:1/mediciones");

        logica.guardarMedicion(new Medicion(1), "GTI-3A-MJ");

        assertEquals(RespuestaREST.SIN_CODIGO, logica.getUltimaPeticion().getCodigo());
    }
}
