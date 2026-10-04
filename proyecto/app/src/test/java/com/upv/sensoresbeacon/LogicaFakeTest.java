package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
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
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la logica fake: debe lanzar una peticion POST a /mediciones con
 * los datos de la medicion y los del beacon.
 */
public class LogicaFakeTest {

    private static final String URL = "http://10.0.2.2:8080/mediciones";
    private static final String UUID_HEX = "F7826DA64FA24E988024BC5B71E0893E";
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

        logica.guardarMedicion(medicion, UUID_HEX, -59, 11, 812, EMISORA);

        assertEquals(1, transporte.getNumeroLlamadas());
        assertEquals("POST", transporte.getUltimaLlamada().metodo);
    }

    // guardarMedicion(): la peticion va contra la ruta /mediciones.
    @Test
    public void guardarMedicionApuntaALaRutaDeMediciones() {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);
        assertEquals(URL, transporte.getUltimaLlamada().urlDestino);
        assertTrue(transporte.getUltimaLlamada().urlDestino.endsWith(LogicaFake.RUTA_MEDICIONES));
    }

    // el JSON enviado lleva el tipo y el valor de la medicion.
    @Test
    public void elCuerpoJsonContieneElTipoYElValorDeLaMedicion() throws Exception {
        Medicion medicion = new Medicion(23.5);
        medicion.asignarTipo(12);

        logica.guardarMedicion(medicion, UUID_HEX, -59, 12, 23, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("temperatura", cuerpo.getString("tipo"));
        assertEquals(23.5, cuerpo.getDouble("valor"), 0.0001);
    }

    // el JSON enviado lleva uuid, txPower, major, minor y nombre.
    @Test
    public void elCuerpoJsonContieneTodosLosDatosDelBeacon() throws Exception {
        logica.guardarMedicion(new Medicion(68), UUID_HEX, -70, 13, 68, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(UUID_HEX, cuerpo.getString("uuid"));
        assertEquals(-70, cuerpo.getInt("txPower"));
        assertEquals(13, cuerpo.getInt("major"));
        assertEquals(68, cuerpo.getInt("minor"));
        assertEquals(EMISORA, cuerpo.getString("nombre_emisora"));
    }

    // los campos numericos del beacon van como numero JSON, no como texto.
    @Test
    public void losNumerosDelBeaconSeEnvianComoEnterosYNoComoText() throws Exception {
        logica.guardarMedicion(new Medicion(2450), UUID_HEX, -59, 12, 2450, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertTrue(cuerpo.get("valor") instanceof Number);
        assertTrue(cuerpo.get("major") instanceof Number);
        assertTrue(cuerpo.get("minor") instanceof Number);
        assertTrue(cuerpo.get("txPower") instanceof Number);
        assertTrue(cuerpo.get("tipo") instanceof String);
        assertTrue(cuerpo.get("uuid") instanceof String);
    }

    // el txPower negativo se envia con su signo.
    @Test
    public void txPowerNegativoSeEnviaConSigno() throws Exception {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -65, 11, 812, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(-65, cuerpo.getInt("txPower"));
    }

    // major y minor conservan su valor completo, sin truncar a 16 bits.
    @Test
    public void losCamposDe16BitsNoSeTruncan() throws Exception {
        logica.guardarMedicion(new Medicion(65535), UUID_HEX, -128, 65535, 65535, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(65535, cuerpo.getInt("major"));
        assertEquals(65535, cuerpo.getInt("minor"));
        assertEquals(65535.0, cuerpo.getDouble("valor"), 0.0001);
        assertEquals(-128, cuerpo.getInt("txPower"));
    }

    // guardarMedicion(): la medicion se guarda tambien en la logica.
    @Test
    public void guardarMedicionConservaLaMedicionEnLaLogica() {
        Medicion medicion = new Medicion(15);
        medicion.asignarTipo(13);

        logica.guardarMedicion(medicion, UUID_HEX, -50, 13, 15, EMISORA);

        assertEquals(medicion, logica.getMedicion());
        assertEquals("ruido", logica.getMedicion().getTipoDeMedicion());
    }

    // un tipo no registrado se envia como desconocido.
    @Test
    public void unMedidorDesconocidoSeEnviaComoDesconocido() throws Exception {
        Medicion medicion = new Medicion(7);
        medicion.asignarTipo(42);

        logica.guardarMedicion(medicion, UUID_HEX, -50, 42, 7, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
        assertEquals(42, cuerpo.getInt("major"));
    }

    // una medicion nula no revienta: no hace ninguna peticion.
    @Test
    public void unaMedicionNulaNoProvocaUnaExcepcion() throws Exception {
        logica.guardarMedicion(null, UUID_HEX, -50, 11, 500, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
    }

    // un nombre nulo se convierte en cadena vacia.
    @Test
    public void nombreEmisoraYNulosSeNormalizanACadenaVacia() throws Exception {
        logica.guardarMedicion(new Medicion(1), null, -50, 11, 1, null);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("", cuerpo.getString("uuid"));
        assertEquals("", cuerpo.getString("nombre_emisora"));
    }

    // la logica guarda la ultima peticion realizada, para poder inspeccionarla.
    @Test
    public void seGuardaLaUltimaPeticionRealizada() {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);

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

        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);

        assertEquals(500, logica.getUltimaPeticion().getCodigo());
    }

    // desde la trama hasta el POST: el flujo completo envia la medicion.
    @Test
    public void elFlujoCompletoDesdeLaTramaEnviaLaMedicion() throws Exception {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((11 << 8) + 1, 612, -65));
        Medicion medicion = Medicion.desdeTrama(trama);

        logica.guardarMedicion(medicion,
                Utilidades.uuidToHexString(trama.getUUIDComoUUID()),
                trama.getTxPower(),
                trama.getMajorComoInt(),
                trama.getMinorComoInt(),
                EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("co2", cuerpo.getString("tipo"));
        assertEquals(612.0, cuerpo.getDouble("valor"), 0.0001);
        assertEquals("F7826DA6-4FA2-4E98-8024-BC5B71E0893E".replace("-", ""), cuerpo.getString("uuid"));
        assertEquals(-65, cuerpo.getInt("txPower"));
        assertEquals((11 << 8) + 1, cuerpo.getInt("major"));
        assertEquals(612, cuerpo.getInt("minor"));
    }
}
