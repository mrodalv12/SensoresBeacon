package com.upv.sensoresbeacon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

//==============================================================================================
//Nombre: MedicionTest.java
//Descripcion: Pruebas de la clase Medicion y de la enumeracion MedicionesID.
//Copyright:
//Fecha de creacion: 2026-10-01
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Pruebas de la clase Medicion y de la enumeracion MedicionesID.
 */
public class MedicionTest {

    // asignarTipo(): los codigos 11, 12 y 13 se traducen a co2, temperatura y ruido.
    @Test
    public void asignarTipoReconoceLosTresTiposDeMedicion() {
        Medicion medicion = new Medicion();
        assertEquals("co2", medicion.asignarTipo(11));
        assertEquals("temperatura", medicion.asignarTipo(12));
        assertEquals("ruido", medicion.asignarTipo(13));
    }

    // asignarTipo(): ademas de devolver el tipo, lo deja guardado en la medicion.
    @Test
    public void asignarTipoGuardaElResultadoEnLaVariable() {
        Medicion medicion = new Medicion(2450);
        medicion.asignarTipo(11);
        assertEquals("co2", medicion.getTipoDeMedicion());
        assertEquals(2450.0, medicion.getValor(), 0.0001);
    }

    // asignarTipo(): un codigo fuera de la enumeracion devuelve tipo desconocido.
    @Test
    public void asignarTipoDevuelveDesconocidoParaCodigosNoRegistrados() {
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(99));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(0));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, new Medicion().asignarTipo(-1));
    }

    // MedicionesID: conserva los codigos 11, 12 y 13, y son los tres unicos.
    @Test
    public void laEnumeracionMantieneLosCodigosDelEnunciado() {
        assertEquals(11, MedicionesID.co2.getCodigo());
        assertEquals(12, MedicionesID.temperatura.getCodigo());
        assertEquals(13, MedicionesID.ruido.getCodigo());
        assertEquals(3, MedicionesID.values().length);
    }

    // existe(): dice si un codigo corresponde a un tipo de medicion registrado.
    @Test
    public void existeIndicaSiElCodigoPerteneceALaEnumeracion() {
        assertTrue(MedicionesID.existe(11));
        assertTrue(MedicionesID.existe(13));
        assertTrue(!MedicionesID.existe(14));
    }

    // desdeTrama(): el byte alto del major da el tipo y el minor da el valor.
    @Test
    public void desdeTramaTomaElTipoDelByteAltoDelMajorYElValorDelMinor() {
        // major = (TEMPERATURA << 8) + 3, segun codifica el emisor.
        // El minor viaja escalado x10 (230 = 23.0 grados).
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((12 << 8) + 3, 230, -59));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("temperatura", medicion.getTipoDeMedicion());
        assertEquals(23.0, medicion.getValor(), 0.0001);
    }

    // desdeTrama(): con major de CO2 el valor devuelto es el minor, en ppm.
    @Test
    public void desdeTramaConCo2YCO2DaElValorEnPpm() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((11 << 8) + 1, 8120, -70));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("co2", medicion.getTipoDeMedicion());
        assertEquals(812.0, medicion.getValor(), 0.0001);
    }

    // desdeTrama(): con major de ruido tambien toma el valor del minor.
    @Test
    public void desdeTramaConRuidoDaElValor() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((13 << 8) + 7, 680, -55));
        Medicion medicion = Medicion.desdeTrama(trama);
        assertEquals("ruido", medicion.getTipoDeMedicion());
        assertEquals(68.0, medicion.getValor(), 0.0001);
    }

    // desdeTrama(): el minor viene escalado x10 y se recupera con 1 decimal.
    @Test
    public void desdeTramaRecuperaUnDecimalDelMinorEscalado() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((12 << 8) + 3, 234, -59));
        assertEquals(23.4, Medicion.desdeTrama(trama).getValor(), 0.0001);
    }

    // desdeTrama(): el minor se interpreta con signo (temperaturas negativas).
    @Test
    public void desdeTramaAdmiteValoresNegativos() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((12 << 8) + 3, -25, -59));
        assertEquals(-2.5, Medicion.desdeTrama(trama).getValor(), 0.0001);
    }

    // desdeTrama(): un major con tipo no registrado deja la medicion como desconocida.
    @Test
    public void desdeTramaConUnTipoDesconocidoQuedaComoDesconocido() {
        TramaIBeacon trama = new TramaIBeacon(
                TramaIBeaconTest.tramaCompleta((77 << 8) + 2, 5, -40));
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, Medicion.desdeTrama(trama).getTipoDeMedicion());
    }

    // desdeTrama(): el contador del major no cambia el tipo detectado.
    @Test
    public void elContadorDelMajorNoAfectaAlTipo() {
        for (int contador = 0; contador < 256; contador++) {
            TramaIBeacon trama = new TramaIBeacon(
                    TramaIBeaconTest.tramaCompleta((11 << 8) + contador, 500, -60));
            assertEquals("co2", Medicion.desdeTrama(trama).getTipoDeMedicion());
        }
    }

    // Medicion(): el constructor vacio deja tipo y valor sin asignar.
    @Test
    public void elConstructorVacioDejaLaMedicionSinAsignar() {
        Medicion medicion = new Medicion();
        assertEquals(MedicionesID.TIPO_DESCONOCIDO, medicion.getTipoDeMedicion());
        assertEquals(0.0, medicion.getValor(), 0.0001);
    }

    // setValor(): actualiza el valor guardado en la medicion.
    @Test
    public void setValorActualizaElValorDeLaMedicion() {
        Medicion medicion = new Medicion();
        medicion.setValor(68.5);
        assertEquals(68.5, medicion.getValor(), 0.0001);
    }

    // toString(): la cadena incluye el tipo de medicion y su valor.
    @Test
    public void toStringIncluyeElTipoYElValor() {
        Medicion medicion = new Medicion(55);
        medicion.asignarTipo(13);
        assertTrue(medicion.toString().contains("ruido"));
        assertTrue(medicion.toString().contains("55.0"));
    }

    // Medicion(): los tres tipos producen mediciones distintas entre si.
    @Test
    public void cadaTipoProduceUnaMedicionDistinta() {
        assertNotEquals(new Medicion().asignarTipo(11), new Medicion().asignarTipo(12));
    }
}
