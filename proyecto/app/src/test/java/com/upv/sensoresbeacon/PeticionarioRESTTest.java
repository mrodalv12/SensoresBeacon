package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

//==============================================================================================
//Nombre: PeticionarioRESTTest.java
//Descripcion: Pruebas del envoltorio PeticionarioREST y de la clase RespuestaREST.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de RespuestaREST y de PeticionarioREST.
 */
public class PeticionarioRESTTest {

    private TransporteHttpFalso transporte;

    // setUp(): crea un peticionario con el transporte falso.
    @Before
    public void setUp() {
        transporte = new TransporteHttpFalso();
    }

    // RespuestaREST(): sin codigo, la respuesta no se considera exitosa.
    @Test
    public void unaRespuestaSinCodigoNoEsExitosa() {
        RespuestaREST respuesta = new RespuestaREST();
        assertEquals(RespuestaREST.SIN_CODIGO, respuesta.getCodigoRespuesta());
        assertEquals("", respuesta.getCuerpoRespuesta());
        assertFalse(respuesta.comoFue());
    }

    // comoFue(): da por buena cualquier respuesta 2xx o 3xx.
    @Test
    public void comoFueAceptaLaFamilia2xxY3xx() {
        assertTrue(new RespuestaREST(200, "").comoFue());
        assertTrue(new RespuestaREST(201, "").comoFue());
        assertTrue(new RespuestaREST(399, "").comoFue());
    }

    // comoFue(): los codigos de error y los ausentes no son exitosos.
    @Test
    public void comoFueRechazaLosErrores() {
        assertFalse(new RespuestaREST(400, "").comoFue());
        assertFalse(new RespuestaREST(500, "").comoFue());
    }

    // RespuestaREST(): un cuerpo nulo se guarda como cadena vacia.
    @Test
    public void unCuerpoNuloSeNormalizaACadenaVacia() {
        assertEquals("", new RespuestaREST(200, null).getCuerpoRespuesta());
    }

    // hacerPeticion(): la peticion se delega en el transporte.
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

    // el peticionario guarda el codigo y el cuerpo que devuelve el transporte.
    @Test
    public void elPeticionarioExponeElCodigoYElCuerpoRecibidos() {
        transporte.setRespuesta(new RespuestaREST(201, "{\"id\":7}"));
        PeticionarioREST peticion = new PeticionarioREST("POST", "http://x/mediciones", "{}", transporte);

        peticion.hacerPeticionarioREST();

        assertEquals(201, peticion.getCodigo());
        assertEquals("{\"id\":7}", peticion.getCuerpo());
        assertTrue(peticion.comoFue());
    }

    // comoFue(): refleja el codigo que dio el transporte.
    @Test
    public void comoFueReflejaElCodigoRecibido() {
        transporte.setRespuesta(new RespuestaREST(500, ""));
        PeticionarioREST peticion = new PeticionarioREST("GET", "http://x/mediciones", "", transporte);

        peticion.hacerPeticionarioREST();

        assertFalse(peticion.comoFue());
    }

    // PeticionarioREST(): un cuerpo nulo se expone como cadena vacia.
    @Test
    public void unCuerpoNuloEnElPeticionarioSeNormalizaACadenaVacia() {
        PeticionarioREST peticion = new PeticionarioREST("POST", "http://x/mediciones", null, transporte);
        assertEquals("", peticion.getElCuerpo());
    }

    // crearPOST(): monta la peticion con el metodo y la ruta indicados.
    @Test
    public void crearPOSTConstruyeUnaPeticionConElMetodoYLaRutaCorrectos() {
        PeticionarioREST peticion = PeticionarioREST.crearPOST("http://x/mediciones", "{\"a\":1}");

        assertEquals("POST", peticion.getMetodo());
        assertEquals("http://x/mediciones", peticion.getUrlDestino());
        assertEquals("{\"a\":1}", peticion.getElCuerpo());
    }

    // sin transporte asignado, la peticion devuelve una respuesta vacia.
    @Test
    public void unaPeticionSinTransporteDevuelveLaRespuestaVacia() {
        PeticionarioREST peticion = PeticionarioREST.crearPOST("http://localhost:1/mediciones", "{}");
        peticion.setTransporte(transporte);
        peticion.hacerPeticionarioREST();
        assertEquals(1, transporte.getNumeroLlamadas());
    }

    // el constructor por defecto trabaja con el transporte HTTP real.
    @Test
    public void elConstructorPorDefectoUsaElTransporteReal() {
        PeticionarioREST peticion = new PeticionarioREST();
        assertEquals("GET", peticion.getMetodo());
        assertEquals("", peticion.getUrlDestino());
    }
}
