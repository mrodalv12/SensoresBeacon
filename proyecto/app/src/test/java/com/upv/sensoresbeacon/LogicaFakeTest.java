package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setUp() {
        transporte = new TransporteHttpFalso();
        logica = new LogicaFake(URL, transporte);
    }

    @Test
    public void guardarMedicionHaceUnaUnicaPeticionPost() {
        Medicion medicion = new Medicion(812);
        medicion.asignarTipo(11);

        logica.guardarMedicion(medicion, UUID_HEX, -59, 11, 812, EMISORA);

        assertEquals(1, transporte.getNumeroLlamadas());
        assertEquals("POST", transporte.getUltimaLlamada().metodo);
    }

    @Test
    public void guardarMedicionApuntaALaRutaDeMediciones() {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);
        assertEquals(URL, transporte.getUltimaLlamada().urlDestino);
        assertTrue(transporte.getUltimaLlamada().urlDestino.endsWith(LogicaFake.RUTA_MEDICIONES));
    }

    @Test
    public void elCuerpoJsonContieneElTipoYElValorDeLaMedicion() throws Exception {
        Medicion medicion = new Medicion(23.5);
        medicion.asignarTipo(12);

        logica.guardarMedicion(medicion, UUID_HEX, -59, 12, 23, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("temperatura", cuerpo.getString("tipo"));
        assertEquals(23.5, cuerpo.getDouble("valor"), 0.0001);
    }

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

    @Test
    public void txPowerNegativoSeEnviaConSigno() throws Exception {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -65, 11, 812, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(-65, cuerpo.getInt("txPower"));
    }

    @Test
    public void losCamposDe16BitsNoSeTruncan() throws Exception {
        logica.guardarMedicion(new Medicion(65535), UUID_HEX, -128, 65535, 65535, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(65535, cuerpo.getInt("major"));
        assertEquals(65535, cuerpo.getInt("minor"));
        assertEquals(65535.0, cuerpo.getDouble("valor"), 0.0001);
        assertEquals(-128, cuerpo.getInt("txPower"));
    }

    @Test
    public void guardarMedicionConservaLaMedicionEnLaLogica() {
        Medicion medicion = new Medicion(15);
        medicion.asignarTipo(13);

        logica.guardarMedicion(medicion, UUID_HEX, -50, 13, 15, EMISORA);

        assertEquals(medicion, logica.getMedicion());
        assertEquals("ruido", logica.getMedicion().getTipoDeMedicion());
    }

    @Test
    public void unMedidorDesconocidoSeEnviaComoDesconocido() throws Exception {
        Medicion medicion = new Medicion(7);
        medicion.asignarTipo(42);

        logica.guardarMedicion(medicion, UUID_HEX, -50, 42, 7, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
        assertEquals(42, cuerpo.getInt("major"));
    }

    @Test
    public void unaMedicionNulaNoProvocaUnaExcepcion() throws Exception {
        logica.guardarMedicion(null, UUID_HEX, -50, 11, 500, EMISORA);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, cuerpo.getString("tipo"));
    }

    @Test
    public void nombreEmisoraYNulosSeNormalizanACadenaVacia() throws Exception {
        logica.guardarMedicion(new Medicion(1), null, -50, 11, 1, null);

        JSONObject cuerpo = new JSONObject(transporte.getUltimaLlamada().cuerpo);
        assertEquals("", cuerpo.getString("uuid"));
        assertEquals("", cuerpo.getString("nombre_emisora"));
    }

    @Test
    public void seGuardaLaUltimaPeticionRealizada() {
        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);

        PeticionarioREST peticion = logica.getUltimaPeticion();
        assertEquals(PeticionarioREST.METODO_POST, peticion.getMetodo());
        assertEquals(URL, peticion.getUrlDestino());
        assertEquals(200, peticion.getCodigo());
        assertTrue(peticion.comoFue());
    }

    @Test
    public void unErrorDelServidorNoPropagaUnaExcepcion() {
        transporte.setRespuesta(new RespuestaREST(500, "{\"error\":\"fallo\"}"));

        logica.guardarMedicion(new Medicion(812), UUID_HEX, -59, 11, 812, EMISORA);

        assertEquals(500, logica.getUltimaPeticion().getCodigo());
    }

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
