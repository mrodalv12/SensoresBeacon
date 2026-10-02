package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de la clase Medicion y de la enumeracion MedicionesID.
 */
public class MedicionTest {

    @Test
    public void asignarTipoReconoceLosTresTiposDeMedicion() {
        Medicion medicion = new Medicion();
        assertEquals("co2", medicion.asignarTipo(11));
        assertEquals("temperatura", medicion.asignarTipo(12));
        assertEquals("ruido", medicion.asignarTipo(13));
    }

    @Test
    public void asignarTipoGuardaElResultadoEnLaVariable() {
        Medicion medicion = new Medicion(2450);
        medicion.asignarTipo(11);
        assertEquals("co2", medicion.getTipoDeMedicion());
        assertEquals(2450.0, medicion.getValor(), 0.0001);
    }

    @Test
    public void asignarTipoDevuelveDesconocidoParaCodigosNoRegistrados() {
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(99));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(0));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(-1));
    }

    @Test
    public void laEnumeracionMantieneLosCodigosDelEnunciado() {
        assertEquals(11, MedicionesID.co2.getCodigo());
        assertEquals(12, MedicionesID.temperatura.getCodigo());
        assertEquals(13, MedicionesID.ruido.getCodigo());
        assertEquals(3, MedicionesID.values().length);
    }

    @Test
    public void existeIndicaSiElCodigoPerteneceALaEnumeracion() {
        assertTrue(MedicionesID.existe(11));
        assertTrue(MedicionesID.existe(13));
        assertTrue(!MedicionesID.existe(14));
    }

    @Test
    public void desdeTramaTomaElTipoDelByteAltoDelMajorYElValorDelMinor() {
        // major = (TEMPERATURA << 8) + 3, segun codifica el emisor
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((12 << 8) + 3, 23, -59));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("temperatura", medicion.getTipoDeMedicion());
        assertEquals(23.0, medicion.getValor(), 0.0001);
    }

    @Test
    public void desdeTramaConCo2YCO2DaElValorEnPpm() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((11 << 8) + 1, 812, -70));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("co2", medicion.getTipoDeMedicion());
        assertEquals(812.0, medicion.getValor(), 0.0001);
    }

    @Test
    public void desdeTramaConRuidoDaElValor() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((13 << 8) + 7, 68, -55));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("ruido", medicion.getTipoDeMedicion());
        assertEquals(68.0, medicion.getValor(), 0.0001);
    }

    @Test
    public void desdeTramaConUnTipoDesconocidoQuedaComoDesconocido() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((77 << 8) + 2, 5, -40));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, Medicion.desdeTrama(trama).getTipoDeMedicion());
    }

    @Test
    public void elContadorDelMajorNoAfectaAlTipo() {
        for (int contador = 0; contador < 256; contador++) {
            TramaIBeacon trama = new TramaIBeacon(
                    TramaIBeaconTest.tramaCompleta((11 << 8) + contador, 500, -60));
            assertEquals("co2", Medicion.desdeTrama(trama).getTipoDeMedicion());
        }
    }

    @Test
    public void elConstructorVacioDejaLaMedicionSinAsignar() {
        Medicion medicion = new Medicion();
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, medicion.getTipoDeMedicion());
        assertEquals(0.0, medicion.getValor(), 0.0001);
    }

    @Test
    public void setValorActualizaElValorDeLaMedicion() {
        Medicion medicion = new Medicion();
        medicion.setValor(68.5);
        assertEquals(68.5, medicion.getValor(), 0.0001);
    }

    @Test
    public void toStringIncluyeElTipoYElValor() {
        Medicion medicion = new Medicion(55);
        medicion.asignarTipo(13);
        assertTrue(medicion.toString().contains("ruido"));
        assertTrue(medicion.toString().contains("55.0"));
    }

    @Test
    public void cadaTipoProduceUnaMedicionDistinta() {
        assertNotEquals(new Medicion().asignarTipo(11), new Medicion().asignarTipo(12));
    }
}
