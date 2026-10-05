//=============================================================================================
//Nombre: Medidor.h
//Descripcion: Clase que lee los sensores analogos de la placa y convierte las
//              lecturas en CO2 (ppm) y temperatura (°C).
//Copyright:
//Fecha de creacion: 2019-07-07
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

// -*- mode: c++ -*-

#ifndef MEDIDOR_H_INCLUIDO
#define MEDIDOR_H_INCLUIDO
#include <Adafruit_TinyUSB.h>
#include <Arduino.h>

// ------------------------------------------------------
//Clase Medidor: lee el sensor de gas (CO2) y el de temperatura
// ------------------------------------------------------
class Medidor {

  // .....................................................
  // .....................................................
private:

  // Asignación de pines analógicos
  const int PIN_VGAS  = A4; // GPIO 5
  const int PIN_VREF  = A5; // GPIO 28
  const int PIN_VTEMP = A3; // Pin de temperatura

  // Parámetros de conversión ADC
  const float VREF_BOARD    = 3.3;      // Voltaje de referencia de la placa (3.3V)
  const int ADC_RES_BITS    = 10;       // 10 bits de resolución
  const float ADC_MAX_VALUE = 1023.0;   // (2^10) - 1

  // Parámetros del sensor (específicos del datasheet)
  const float SENSITIVITY_V_PER_PPM = -44.26 * 499 * 10e-6; 
  const float VREF_OFFSET          = 0.0;  

public:

  // .....................................................
  // Medidor( ) [constructor]
  //   Crea el medidor. No configura nada: la configuracion del ADC
  //   se hace despues, en iniciarMedidor().
  // .....................................................
  Medidor(  ) {
  } // ()

  // .....................................................
  // iniciarMedidor( )
  //   Prepara el conversor analogico-digital: fija la resolucion
  //   del ADC a 10 bits, que es la que espera el calculo de medirCO2().
  // .....................................................
  void iniciarMedidor() {
	// las cosas que no se puedan hacer en el constructor, if any
   analogReadResolution(ADC_RES_BITS);
  } // ()

  // .....................................................
  // medirCO2( ) --> ppm: float
  //   Lee el sensor de gas y el de referencia, convierte ambos a voltios,
  //   les resta la referencia y aplica la sensibilidad del sensor para
  //   obtener la concentracion de CO2 en ppm. Los negativos se recortan a 0.
  // .....................................................
  float medirCO2() {

  int rawVgas  = analogRead(PIN_VGAS); //medicion de gas del sensor directo
  int rawVref  = analogRead(PIN_VREF);  //medicion de gas del sensor directo
 

  // pasar los valores de vgas y vref a voltios
  float vGas  = (rawVgas  * VREF_BOARD) / ADC_MAX_VALUE;
  float vRef  = (rawVref  * VREF_BOARD) / ADC_MAX_VALUE;
  

  //  Calculo de Vgas - (Vref + Voffset) --> (para calcular los ppm)
  float vDifference = (vGas - vRef) - VREF_OFFSET;

  // conversion a ppm, vDifference * 1 / M ----> M = SENSITYVITY_V_PER_PPM (Declarado arriba)
  float ppm = 0.0;
  if (SENSITIVITY_V_PER_PPM != 0.0) {
    ppm = vDifference / SENSITIVITY_V_PER_PPM;
  }

  // quitar el ruido de valores negativos
  if (ppm < 0.0) {
    ppm = 0.0;
  }

  // 6. Impresión de datos
  /*Serial.print("Vgas: ");     Serial.print(vGas, 3);        Serial.print(" V | ");
  Serial.print("Vref: ");     Serial.print(vRef, 3);        Serial.print(" V | ");
  Serial.print("Vtemp: ");    Serial.print(vTemp, 3);       Serial.print(" V | ");
  Serial.print("Temp: ");     Serial.print(tempC, 1);       Serial.print(" °C | ");
  Serial.print("Gas: ");      Serial.print(ppm, 2);         Serial.println(" PPM");*/

	return ppm;
  } // ()

  // .....................................................
  // medirTemperatura( ) --> tempC: float
  //   Lee el sensor analogico de temperatura, lo convierte a voltios y
  //   aplica la formula del datasheet: (Vtemp / Vref) * 87 - 25.
  // .....................................................
  float medirTemperatura() {

    int rawVtemp = analogRead(PIN_VTEMP); //valor del sensor de temp
    float vTemp = (rawVtemp * VREF_BOARD) / ADC_MAX_VALUE; //conversion del valor del sensor a voltios
    
    // 5. Cálculo aproximado de Temperatura en °C (fórmula típica SPEC Sensors)
    // Temp (°C) = (Vtemp / Vref_board) * 87.0 - 25.0
    float tempC = (vTemp / VREF_BOARD) * 87.0 - 25.0;
	  return tempC;
  } // ()
	
}; // class

// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
// ------------------------------------------------------
#endif
