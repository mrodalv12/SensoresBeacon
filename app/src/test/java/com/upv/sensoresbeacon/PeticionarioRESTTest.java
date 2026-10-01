package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Pruebas de RespuestaREST y de PeticionarioREST.
 */
public class PeticionarioRESTTest {

    private TransporteHttpFalso transporte;

    @Before
    public void setUp() {
        transporte = new TransporteHttpFalso();
    }

    @Test
    public void unaRespuestaSinCodigoNoEsExitosa() {
        RespuestaREST respuesta = new RespuestaREST();
        assertEquals(RespuestaREST.SIN_CODIGO, respuesta.getCodigoRespuesta());
        assertEquals("", respuesta.getCuerpoRespuesta());
        assertFalse(respuesta.comoFue());
    }

    @Test
    public void comoFueAceptaLaFamilia2xxY3xx() {
        assertTrue(new RespuestaREST(200, "").comoFue());
        assertTrue(new RespuestaREST(201, "").comoFue());
        assertTrue(new RespuestaREST(399, "").comoFue());
    }

    @Test
    public void comoFueRechazaLosErrores() {
        assertFalse(new RespuestaREST(400, "").comoFue());
        assertFalse(new RespuestaREST(500, "").comoFue());
    }

    @Test
    public void unCuerpoNuloSeNormalizaACadenaVacia() {
        assertEquals("", new RespuestaREST(200, null).getCuerpoRespuesta());
    }

    @Test
    public void hacerPeticionarioRESTDelegaEnElTransporte() {
        PeticionarioREST peticion = new PeticionarioREST("POST", "http://x/mediciones", "{}", transporte);

        RespuestaREST respuesta = peticion.hacerPeticionarioREST();

        assertEquals(1, transporte.getNumeroLlamadas());
        assertEquals("POST", transporte.getUltimaLlamada().metodo);
        assertEquals("http://x/mediciones", transporte.getUltimaLlamada().urlDestino);
        assertEquals("{}", transporte.getUltimaLlamada().cuerpo);
        assertEquals(200, respuesta.getCodigoRespuesta());
    }

    @Test
    public void elPeticionarioExponeElCodigoYElCuerpoRecibidos() {
        transporte.setRespuesta(new RespuestaREST(201, "{\"id\":7}"));
        PeticionarioREST peticion = new PeticionarioREST("POST", "http://x/mediciones", "{}", transporte);

        peticion.hacerPeticionarioREST();

        assertEquals(201, peticion.getCodigo());
        assertEquals("{\"id\":7}", peticion.getCuerpo());
        assertTrue(peticion.comoFue());
    }

    @Test
    public void comoFueReflejaElCodigoRecibido() {
        transporte.setRespuesta(new RespuestaREST(500, ""));
        PeticionarioREST peticion = new PeticionarioREST("GET", "http://x/mediciones", "", transporte);

        peticion.hacerPeticionarioREST();

        assertFalse(peticion.comoFue());
    }

    @Test
    public void unCuerpoNuloEnElPeticionarioSeNormalizaACadenaVacia() {
        PeticionarioREST peticion = new PeticionarioREST("POST", "http://x/mediciones", null, transporte);
        assertEquals("", peticion.getElCuerpo());
    }

    @Test
    public void crearPOSTConstruyeUnaPeticionConElMetodoYLaRutaCorrectos() {
        PeticionarioREST peticion = PeticionarioREST.crearPOST("http://x/mediciones", "{\"a\":1}");

        assertEquals("POST", peticion.getMetodo());
        assertEquals("http://x/mediciones", peticion.getUrlDestino());
        assertEquals("{\"a\":1}", peticion.getElCuerpo());
    }

    @Test
    public void unaPeticionSinTransporteDevuelveLaRespuestaVacia() {
        PeticionarioREST peticion = PeticionarioREST.crearPOST("http://localhost:1/mediciones", "{}");
        peticion.setTransporte(transporte);
        peticion.hacerPeticionarioREST();
        assertEquals(1, transporte.getNumeroLlamadas());
    }

    @Test
    public void elConstructorPorDefectoUsaElTransporteReal() {
        PeticionarioREST peticion = new PeticionarioREST();
        assertEquals("GET", peticion.getMetodo());
        assertEquals("", peticion.getUrlDestino());
    }
}
