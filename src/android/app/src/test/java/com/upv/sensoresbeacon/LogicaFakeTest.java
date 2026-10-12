package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

//==============================================================================================
//Nombre: LogicaFakeTest.java
//Descripcion: Pruebas de la logica fake con un transporte HTTP falso: comprueba el POST
//              realizado y su cuerpo JSON.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-07
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la logica fake: debe lanzar una peticion POST a /mediciones con
 * tipo, valor y nombre_emisora. Los datos del beacon (uuid, txPower, major,
 * minor y rssi) no se envian: se quedan en la app.
 */
public class LogicaFakeTest {

    private static final String URL = "http://10.0.2.2:8080/mediciones";
    private static final String EMISORA = "GTI-3A-MJ";

    private TransporteHttpFalso transporte;
    private LogicaFake logica;

    // setUp(): crea la logica fake con un transporte falso en cada prueba.
    @Before
    public void setUp() {
        transporte = new TransporteHttpFalso();
        logica = new LogicaFake(URL, transporte);
    }

    // guardarMedicion(): lanza exactamente una peticion HTTP.
    @Test
    public void guardarMedicionHaceUnaUnicaPeticionPost() {
        Medicion medicion = new Medicion(812);
        medicion.asignarTipo(11);

        logica.guardarMedicion(medicion, EMISORA);

        assertEquals(1, transporte.getNumeroLlamadas());
        assertEquals("POST", transporte.getUltimaLlamada().metodo);
    }

    // guardarMedicion(): la peticion va contra la ruta /mediciones.
    @Test
    public void guardarMedicionApuntaALaRutaDeMediciones() {
        logica.guardarMedicion(new Medicion(812), EMISORA);
        assertEquals(URL, transporte.getUltimaLlamada().urlDestino);
        assertTrue(transporte.getUltimaLlamada().urlDestino.endsWith(LogicaFake.RUTA_MEDICIONES));
    }

    // el JSON enviado lleva el tipo y el valor de la medicion.
    @Test
    public void elCuerpoJsonContieneElTipoYElValorDeLaMedicion() throws Exception {
        Medicion medicion = new Medicion(23.5);
        medicion.asignarTipo(12);

        logica.guardarMedicion(medicion, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("temperatura", cuerpo.getString("tipo"));
        assertEquals(23.5, cuerpo.getDouble("valor"), 0.0001);
    }

    // el JSON no lleva ningun dato del beacon: uuid, txPower, major, minor y
    // rssi se quedan en la app y no viajan en el POST.
    @Test
    public void elCuerpoJsonNoLlevaLosDatosDelBeacon() throws Exception {
        logica.guardarMedicion(new Medicion(68), EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertFalse(cuerpo.has("uuid"));
        assertFalse(cuerpo.has("txPower"));
        assertFalse(cuerpo.has("major"));
        assertFalse(cuerpo.has("minor"));
        assertFalse(cuerpo.has("rssi"));
        assertEquals(EMISORA, cuerpo.getString("nombre_emisora"));
    }

    // el cuerpo del POST contiene exactamente los tres campos del contrato.
    @Test
    public void elCuerpoJsonSoloLlevaLosTresCamposDelContrato() throws Exception {
        logica.guardarMedicion(new Medicion(2450), EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(3, cuerpo.length());
        assertTrue(cuerpo.has("tipo"));
        assertTrue(cuerpo.has("valor"));
        assertTrue(cuerpo.has("nombre_emisora"));
        assertTrue(cuerpo.get("valor") instanceof Number);
        assertTrue(cuerpo.get("tipo") instanceof String);
    }

    // un valor negativo (por ejemplo ruido en dB) se envia con su signo.
    @Test
    public void valorNegativoSeEnviaConSigno() throws Exception {
        Medicion medicion = new Medicion(-72.5);
        medicion.asignarTipo(13);

        logica.guardarMedicion(medicion, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("ruido", cuerpo.getString("tipo"));
        assertEquals(-72.5, cuerpo.getDouble("valor"), 0.0001);
    }

    // el valor conserva su magnitud completa, sin truncar a 16 bits.
    @Test
    public void elValorDe16BitsNoSeTrunca() throws Exception {
        logica.guardarMedicion(new Medicion(65535), EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(65535.0, cuerpo.getDouble("valor"), 0.0001);
    }

    // guardarMedicion(): la medicion se guarda tambien en la logica.
    @Test
    public void guardarMedicionConservaLaMedicionEnLaLogica() {
        Medicion medicion = new Medicion(15);
        medicion.asignarTipo(13);

        logica.guardarMedicion(medicion, EMISORA);

        assertEquals(medicion, logica.getMedicion());
        assertEquals("ruido", logica.getMedicion().getTipoDeMedicion());
    }

    // un tipo no registrado se envia como desconocido.
    @Test
    public void unMedidorDesconocidoSeEnviaComoDesconocido() throws Exception {
        Medicion medicion = new Medicion(7);
        medicion.asignarTipo(42);

        logica.guardarMedicion(medicion, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
        assertEquals(7, cuerpo.getDouble("valor"), 0.0001);
    }

    // una medicion nula no revienta: no hace ninguna peticion.
    @Test
    public void unaMedicionNulaNoProvocaUnaExcepcion() throws Exception {
        logica.guardarMedicion(null, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
    }

    // un nombre nulo se convierte en cadena vacia.
    @Test
    public void nombreEmisoraYNulosSeNormalizanACadenaVacia() throws Exception {
        logica.guardarMedicion(new Medicion(1), null);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("", cuerpo.getString("nombre_emisora"));
    }

    // la logica guarda la ultima peticion realizada, para poder inspeccionarla.
    @Test
    public void seGuardaLaUltimaPeticionRealizada() {
        logica.guardarMedicion(new Medicion(812), EMISORA);

        PeticionarioREST peticion = logica.getUltimaPeticion();
        assertEquals(PeticionarioREST.METODO_POST, peticion.getMetodo());
        assertEquals(URL, peticion.getUrlDestino());
        assertEquals(200, peticion.getCodigo());
        assertTrue(peticion.comoFue());
    }

    // si el servidor falla, la excepcion no sale de la logica.
    @Test
    public void unErrorDelServidorNoPropagaUnaExcepcion() {
        transporte.setRespuesta(new RespuestaREST(500, "{\"error\":\"fallo\"}"));

        logica.guardarMedicion(new Medicion(812), EMISORA);

        assertEquals(500, logica.getUltimaPeticion().getCodigo());
    }

    // desde la trama hasta el POST: el flujo completo envia la medicion.
    @Test
    public void elFlujoCompletoDesdeLaTramaEnviaLaMedicion() throws Exception {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((11 << 8) + 1, 6120, -65));
        Medicion medicion = Medicion.desdeTrama(trama);

        logica.guardarMedicion(medicion, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("co2", cuerpo.getString("tipo"));
        assertEquals(612.0, cuerpo.getDouble("valor"), 0.0001);
        assertEquals(EMISORA, cuerpo.getString("nombre_emisora"));
        assertFalse(cuerpo.has("uuid"));
        assertFalse(cuerpo.has("major"));
        assertFalse(cuerpo.has("minor"));
        assertFalse(cuerpo.has("txPower"));
    }
}
