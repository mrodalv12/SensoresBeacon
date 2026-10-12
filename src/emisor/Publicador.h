//=============================================================================================
//Nombre: Publicador.h
//Descripcion: Publica las mediciones de CO2 y temperatura como anuncios iBeacon.
//              El tipo de medicion va codificado en el campo major y el valor
//              medido en el campo minor, con un UUID fijo en todos los anuncios.
//Copyright:
//Fecha de creacion: 2019-07-06
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

// -*- mode: c++ -*-

#ifndef PUBLICADOR_H_INCLUIDO
#define PUBLICADOR_H_INCLUIDO

// --------------------------------------------------------------
// --------------------------------------------------------------
//Clase Publicador: gestiona la emision de mediciones por Bluetooth
// --------------------------------------------------------------
class Publicador {

  // ............................................................
  // ............................................................
private:

  //UUID fijo que se usa en todos los anuncios iBeacon: "EPSG-GTI-PROY-MJ"
  // Son 16 bytes ASCII (los guiones incluidos como byte 0x2D), NO es una UUID
  // hexadecimal. Su equivalente hexadecimal, que es el valor que viaja por
  // el aire y el que debe filtrar la app Android, es:
  //     45505347-2D47-5449-2D50-524F592D4D4A
  uint8_t beaconUUID[16] = { 
	'E', 'P', 'S', 'G', '-', 'G', 'T', 'I', 
	'-', 'P', 'R', 'O', 'Y', '-', 'M', 'J'
	};

  // Factor fijo de escala del valor medido. El campo minor de un iBeacon son
  // 2 bytes enteros (no admite decimales), asi que el valor viaja multiplicado
  // por este factor; la app Android lo divide por el mismo numero para
  // recuperarlo. Con 10 se conserva 1 decimal (56.7 -> minor 567 -> 56.7).
  static const int FACTOR_ESCALA = 10;

  // ------------------------------------------------------------------
  // DISENO de codificarValor( valor ):
  //   Convierte el valor real medido al entero que cabe en el campo minor,
  //   multiplicandolo por FACTOR_ESCALA y redondeando al entero mas cercano.
  // ------------------------------------------------------------------
  // valor: R --> codificarValor() --> minor: Z
  int16_t codificarValor( float valor ) {
	float escalado = valor * FACTOR_ESCALA;
	return (int16_t) (escalado >= 0 ? escalado + 0.5f : escalado - 0.5f);
  } // ()

  // ............................................................
  // ............................................................
public:
  //laEmisora: objeto que controla el hardware Bluetooth para emitir anuncios
  EmisoraBLE laEmisora {
	"GTI-3A-MJ", //  nombre emisora
	  0x004c, // fabricanteID (Apple)
	  4 // txPower
	  };
  
  const int RSSI = -53; // valor de referencia de potencia de senal (por ahora no se usa)

  // ............................................................
  // ............................................................
public:

  // ............................................................
  //MedicionesID: enum que asigna un numero a cada tipo de medicion
  //          para codificarla en el campo major del iBeacon.
  // ............................................................
  enum MedicionesID  {
	CO2 = 11,
	TEMPERATURA = 12,
	RUIDO = 13
  };

  // ............................................................
  //Constructor: crea el Publicador sin encender la emisora aun.
  //          La emisora se tiene que encender aparte, desde el setup().
  // ............................................................
// ------------------------------------------------------------------
// DISENO de Publicador() [constructor]:
//   No recibe datos ni devuelve datos: crea el publicador sin encender
//   la emisora, que se enciende aparte con encenderEmisora().
// ------------------------------------------------------------------
  // --> Publicador() -->
  Publicador( ) {
	// ATENCION: no hacerlo aqui. (*this).laEmisora.encenderEmisora();
	// Pondremos un metodo para llamarlo desde el setup() mas tarde
  } // ()

  // ............................................................
  //encenderEmisora(): llama a EmisoraBLE para activar el 
  //          hardware Bluetooth de la placa.
  // ............................................................
// ------------------------------------------------------------------
// DISENO de encenderEmisora():
//   No recibe datos ni devuelve datos: enciende el hardware Bluetooth.
// ------------------------------------------------------------------
  // --> encenderEmisora() -->
  void encenderEmisora() {
	(*this).laEmisora.encenderEmisora();
  } // ()

  // ............................................................
  //publicarCO2(): publica el valor de CO2 recibido como un 
  //          iBeacon durante el tiempo indicado (en ms).
  //          El campo major lleva el ID del CO2 y el contador,
  //          el campo minor lleva el valor medido.
  // ............................................................
// ------------------------------------------------------------------
// DISENO de publicarCO2( valorCO2, contador, tiempoEspera ):
//   El valor medido y el numero de iteracion viajan codificados en el
//   anuncio (minor y major); el intervalo solo controla la duracion.
//   No devuelve nada.
// ------------------------------------------------------------------
  // valorCO2: R, contador: N, tiempoEspera: N --> publicarCO2() -->
  void publicarCO2( float valorCO2, uint8_t contador,
					long tiempoEspera ) {

	//
	// 1. empezamos anuncio
	//
	uint16_t major = (MedicionesID::CO2 << 8) + contador;
	(*this).laEmisora.emitirAnuncioIBeacon( (*this).beaconUUID, 
											major,
											(*this).codificarValor( valorCO2 ), // minor
											(*this).RSSI // rssi
									);

	/*
	Globales::elPuerto.escribir( "   publicarCO2(): valor=" );
	Globales::elPuerto.escribir( valorCO2 );
	Globales::elPuerto.escribir( "   contador=" );
	Globales::elPuerto.escribir( contador );
	Globales::elPuerto.escribir( "   todo="  );
	Globales::elPuerto.escribir( major );
	Globales::elPuerto.escribir( "\n" );
	*/

	//
	// 2. esperamos el tiempo que nos digan
	//
	delay( tiempoEspera );

	//
	// 3. paramos anuncio
	//
	(*this).laEmisora.detenerAnuncio();
  } // ()

  // ............................................................
  //publicarTemperatura(): publica el valor de temperatura como 
  //          un iBeacon durante el tiempo indicado (en ms).
  //          Funciona igual que publicarCO2() pero con el ID
  //          de TEMPERATURA.
  // ............................................................
// ------------------------------------------------------------------
// DISENO de publicarTemperatura( valorTemperatura, contador,
//                                tiempoEspera ):
//   Igual que publicarCO2(), pero con el ID de medicion de temperatura.
//   No devuelve nada.
// ------------------------------------------------------------------
  // valorTemperatura: R, contador: N, tiempoEspera: N
  //     --> publicarTemperatura() -->
  void publicarTemperatura( float valorTemperatura,
							uint8_t contador, long tiempoEspera ) {

	uint16_t major = (MedicionesID::TEMPERATURA << 8) + contador;
	(*this).laEmisora.emitirAnuncioIBeacon( (*this).beaconUUID, 
											major,
											(*this).codificarValor( valorTemperatura ), // minor
											(*this).RSSI // rssi
									);
	delay( tiempoEspera );

	(*this).laEmisora.detenerAnuncio();
  } // ()
	
}; // class

// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
#endif
