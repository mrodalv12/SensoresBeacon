//=============================================================================================
//Nombre: EmisoraBLE.h
//Descripcion: Clase que controla el hardware Bluetooth de la placa: enciende la
//              emision BLE, emite anuncios iBeacon (con UUID/major/minor fijo
//              o con 21 bytes libres) y gestiona un servicio BLE propio.
//Copyright:
//Fecha de creacion: 2019-07-05
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

#ifndef EMISORA_H_INCLUIDO
#define EMISORA_H_INCLUIDO
#include "ServicioEnEmisora.h"

class EmisoraBLE {

//variables de la clase...................................................................................
	private:
		const char * nombreEmisora; //nombre de la emisora
		const uint16_t fabricanteID; //id del fabricante
		const int8_t txPower; //potencia de transmision en un metro (en dBm)

	public:
		//son alias de funciones callbacks, estas se ejecutaran cuando un dispositivo se conecte o desconecte
		using CallbackConexionEstablecida = void ( uint16_t connHandle );
		using CallbackConexionTerminada = void ( uint16_t connHandle, uint8_t reason);

//........................................................................................................
// FUNCIONES DE LA CLASE =================================================================================
//	Constructor:
//	Inicializa las variables miembro privadas al crear una instancia de la clase.

	// ------------------------------------------------------------------
// DISENO de EmisoraBLE(...) [constructor]:
//   Identidad, fabricante y potencia son los datos de entrada. El
//   hardware no se enciende aqui.
// ------------------------------------------------------------------
	// nombreEmisora_: Text, fabricanteID_: N, txPower_: Z
	//     --> EmisoraBLE() -->
	EmisoraBLE( const char * nombreEmisora_, const uint16_t fabricanteID_,
				const uint16_t txPower_ ) 
	:
	nombreEmisora( nombreEmisora_ ) ,
	fabricanteID( fabricanteID_ ) ,
	txPower( txPower_ )
	{
		// Cuerpo del constructor
	}

//===========================================================================================================
//funcion encender emisora : sin parametros

  // ------------------------------------------------------------------
// DISENO de encenderEmisora():
//   No recibe datos ni devuelve datos: inicializa el stack BLE y
//   garantiza que no queda ninguna emision activa.
// ------------------------------------------------------------------
  // --> encenderEmisora() -->
  void encenderEmisora() {
		// Serial.println ( "Bluefruit.begin() " );
		Bluefruit.begin(); 
		(*this).detenerAnuncio(); // por si acaso, para asegurarse de que no haya ningun anuncio activo
  } 

//funcion encender emisora : con parametros callback
//	sobrecarga a la funcion anterior que ademas de encender la emisora registra los callback 
//		de conexion y desconexion

  // ------------------------------------------------------------------
// DISENO de encenderEmisora( cbce, cbct ):
//   Sobrecarga que ademas de encender la emisora registra los
//   callbacks de conexion establecida y de conexion terminada.
// ------------------------------------------------------------------
  // cbce: Callback, cbct: Callback --> encenderEmisora() -->
  void encenderEmisora( CallbackConexionEstablecida cbce,
						CallbackConexionTerminada cbct ) {

		encenderEmisora();
		instalarCallbackConexionEstablecida( cbce );
		instalarCallbackConexionTerminada( cbct );

  } 

//===========================================================================================================
//funcion detenerAnuncio: verifica si hay algun anuncio activo y, si hay, lo detiene

// void detenerAnuncio(){
//		si --> estaAnunciando()==true{
//			stop();
//	}
//}
//.............................................................
  // ------------------------------------------------------------------
// DISENO de detenerAnuncio():
//   No recibe datos ni devuelve datos: detiene la emision si habia
//   alguna en curso; si no, no hace nada.
// ------------------------------------------------------------------
  // --> detenerAnuncio() -->
  void detenerAnuncio() {
		if ( (*this).estaAnunciando() ) {
			// Serial.println ( "Bluefruit.Advertising.stop() " );
			Bluefruit.Advertising.stop(); 
		}
  } 
//===========================================================================================================
//funcion [estaAnunciando] --> bool
//	indica si hay algun anuncio en emitiendose 

  // ------------------------------------------------------------------
// DISENO de estaAnunciando():
//   Indica si hay alguna emision en curso.
// ------------------------------------------------------------------
  // --> estaAnunciando() --> B
  bool estaAnunciando() {
		return Bluefruit.Advertising.isRunning();
	}

//===========================================================================================================
//emitirAnuncioBeacon ()
// Configura y comienza a transmitir un iBeacon estándar usando
// 	 los datos recibidos (UUID, Major, Minor, RSSI).
// 		Establece parámetros como el nombre del dispositivo, 
// 			la potencia de emisión y un intervalo de transmisión de 100 ms.

//..........................................................................................................

  // ------------------------------------------------------------------
// DISENO de emitirAnuncioIBeacon( beaconUUID, major, minor, rssi ):
//   Los cuatro campos del anuncio iBeacon son las entradas, y se
//   emiten de forma indefinida hasta que se llame a detenerAnuncio().
// ------------------------------------------------------------------
  // beaconUUID: [ Byte ]_16, major: Z, minor: Z, rssi: Z
  //     --> emitirAnuncioIBeacon() -->
  void emitirAnuncioIBeacon( uint8_t * beaconUUID, int16_t major, int16_t minor, uint8_t rssi ) {

		(*this).detenerAnuncio();

		// creas el beacon
		BLEBeacon elBeacon( beaconUUID, major, minor, rssi );
		elBeacon.setManufacturer( (*this).fabricanteID );

		//le pones todos los parametros
		Bluefruit.setTxPower( (*this).txPower ); 
		Bluefruit.setName( (*this).nombreEmisora );
		Bluefruit.ScanResponse.addName(); 
	
		// pongo el beacon

	Bluefruit.Advertising.setBeacon( elBeacon );

	Bluefruit.Advertising.restartOnDisconnect(true); // no hace falta, pero lo pongo
	Bluefruit.Advertising.setInterval(100, 100);    // in unit of 0.625 ms

	// empieza el anuncio, 0 = tiempo indefinido (ya lo pararán)
	Bluefruit.Advertising.start( 0 ); 
  }

  // .........................................................
  //
  // Ejemplo de Beacon (31 bytes)
  //
  // https://os.mbed.com/blog/entry/BLE-Beacons-URIBeacon-AltBeacons-iBeacon/
  //
  // The iBeacon Prefix contains the hex data : 0x0201061AFF004C0215. This breaks down as follows:
  //
  // 0x020106 defines the advertising packet as BLE General Discoverable and BR/EDR high-speed incompatible.
  // Effectively it says this is only broadcasting, not connecting.
  //
  // 0x1AFF says the following data is 26 bytes long and is Manufacturer Specific Data.
  //
  // 0x004C is Apple’s Bluetooth Sig ID and is the part of this spec that makes it Apple-dependent.
  //
  // 0x02 is a secondary ID that denotes a proximity beacon, which is used by all iBeacons.
  //
  // 0x15 defines the remaining length to be 21 bytes (16+2+2+1).
  //
  // Por ejemmplo:
  //
  // 1. prefijo: 9bytes
  //       0x02, 0x01, 0x06,       // advFlags 3bytes
  //       0x1a, 0xff,             // advHeader 2 (0x1a = 26 = 25(lenght de 0x4c a 0xca)+1)   0xFF -> BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA
  //       0x4c, 0x00,             // companyID 2bytes
  //       0x02,                   // ibeacon type 1 byte
  //       0x15,                   // ibeacon length 1 byte (dec=21 lo que va a continuación: desde  la 'f' hasta 0x01)
  // 
  // 2. uuid: 16bytes
  // 'f', 'i', 's', 't', 'r', 'o', 'f', 'i', 's', 't', 'r', 'o', 0xa7, 0x10, 0x96, 0xe0
  // 
  // 2 major: 2bytes
  // 0x04, 0xd2,
  // 
  // minor: 2bytes
  // 0x10, 0xe1,
  // 
  // 0xca, // tx power : 1bytes
  //
  // 0x01, // este es el byte 31 = BLE_GAP_ADV_SET_DATA_SIZE_MAX, parece que sobra
  //
  // .........................................................
  // Para enviar como carga libre los ultimos 21 bytes de un iBeacon
  // (lo que normalmente seria uuid-16 major-2 minor-2 txPower-1)
  // .........................................................
  // NOTA: la variante de una sola firma, que calculara la longitud
  //       con strlen(), quedo comentada; la que se usa recibe la
  //       longitud ya calculada como parametro.
  /*
  void emitirAnuncioIBeaconLibre( const char * carga ) {

	const uint8_t tamanyoCarga = strlen( carga );
  */
//===========================================================================================================
//emitirAnuncioBeacoLibre()
//	 Es una variante de emitirAnuncioBeacon() que permite inyectar 21 bytes 
//		 de informacion personalizada (como lecturas de sensores)
// 			 en lugar de usar la estructura fija UUID/Major/Minor. Rellena los 4 bytes
//			 de cabecera obligatorios (Company ID, tipo de beacon y longitud)
//		 y copia los datos mediante memcpy.

//..........................................................................................................

  // ------------------------------------------------------------------
// DISENO de emitirAnuncioIBeaconLibre( carga, tamanyoCarga ):
//   En lugar de usar la estructura UUID/major/minor, mete la carga
//   recibida en los 21 bytes libres del anuncio. Si la carga es mas
//   larga de 21 bytes, se recorta.
// ------------------------------------------------------------------
  // carga: Text, tamanyoCarga: N --> emitirAnuncioIBeaconLibre() -->
  void emitirAnuncioIBeaconLibre( const char * carga, const uint8_t tamanyoCarga ) {

	(*this).detenerAnuncio(); 

	Bluefruit.Advertising.clearData();
	Bluefruit.ScanResponse.clearData(); 

	// Bluefruit.setTxPower( (*this).txPower ); creo que no lo pongo porque es uno de los bytes de la parte de carga que utilizo
	Bluefruit.setName( (*this).nombreEmisora );
	Bluefruit.ScanResponse.addName();

	Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAGS_LE_ONLY_GENERAL_DISC_MODE);

	// con este parece que no va  !
	// Bluefruit.Advertising.addFlags(BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE);

	//
	// hasta ahora habrá, supongo, ya puestos los 5 primeros bytes. Efectivamente.
	// Falta poner 4 bytes fijos (company ID, beacon type, longitud) y 21 de carga
	//
	uint8_t restoPrefijoYCarga[4+21] = {
	  0x4c, 0x00, // companyID 2
	  0x02, // ibeacon type 1byte
	  21, // ibeacon length 1byte (dec=21)  longitud del resto // 0x15 // ibeacon length 1byte (dec=21)  longitud del resto
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-', '-', '-', '-', 
	  '-'
	};

	//
	// addData() hay que usarlo sólo una vez. Por eso copio la carga
	// en el anterior array, donde he dejado 21 sitios libres
	//
	memcpy( &restoPrefijoYCarga[4], &carga[0], ( tamanyoCarga > 21 ? 21 : tamanyoCarga ) ); 

	//
	// copio la carga para emitir
	//
	Bluefruit.Advertising.addData( BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA,
								   &restoPrefijoYCarga[0],
								   4+21 );


	Bluefruit.Advertising.restartOnDisconnect(true);
	Bluefruit.Advertising.setInterval(100, 100);    // in unit of 0.625 ms

	Bluefruit.Advertising.setFastTimeout( 1 );      // number of seconds in fast mode
	//
	// empieza el anuncio, 0 = tiempo indefinido (ya lo pararán)
	//
	Bluefruit.Advertising.start( 0 ); 

	Serial.print( "emitiriBeacon libre  Bluefruit.Advertising.start( 0 );  \n");
  } // ()

//===========================================================================================================

  // ------------------------------------------------------------------
// DISENO de anyadirServicio( servicio ):
//   Registra el servicio BLE e indica si el registro ha tenido exito.
// ------------------------------------------------------------------
  // servicio: ServicioEnEmisora --> anyadirServicio() --> B
  bool anyadirServicio( ServicioEnEmisora & servicio ) {

	Serial.print( " Bluefruit.Advertising.addService( servicio ); \n");

	bool r = Bluefruit.Advertising.addService( servicio );

	if ( ! r ) {
	  Serial.println( " SERVICION NO AÑADIDO \n");
	}
	

	return r;
	 // nota: uso conversión de tipo de servicio (ServicioEnEmisora) a BLEService
	 // para addService()
  } 
//===========================================================================================================

  
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de anyadirServicioConSusCaracteristicas( servicio )  [caso base]:
//   Version sin caracteristicas: solo registra el servicio.
// ------------------------------------------------------------------
  // servicio: ServicioEnEmisora
  //     --> anyadirServicioConSusCaracteristicas() --> B
  bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio ) { 
	return (*this).anyadirServicio( servicio );
  } // 
//===========================================================================================================

  // .........................................................
  // ------------------------------------------------------------------
// DISENO de anyadirServicioConSusCaracteristicas( servicio,
//                              caracteristica, restoCaracteristicas... ):
//   Version recursiva: anade las caracteristicas de una en una hasta
//   llegar al caso base, que registra el servicio.
// ------------------------------------------------------------------
  // servicio: ServicioEnEmisora, caracteristica: Caracteristica,
  // restoCaracteristicas: [ Caracteristica ]
  //     --> anyadirServicioConSusCaracteristicas() --> B
  template <typename ... T>
  bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio,
											 ServicioEnEmisora::Caracteristica & caracteristica,
											 T& ... restoCaracteristicas) {

	servicio.anyadirCaracteristica( caracteristica );

	return anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );
	
  } // ()
//===========================================================================================================

  // .........................................................
  // ------------------------------------------------------------------
// DISENO de anyadirServicioConSusCaracteristicasYActivar( servicio,
//                                        restoCaracteristicas... ):
//   Registra el servicio con todas sus caracteristicas y lo activa en
//   el stack BLE. Devuelve el resultado del registro.
// ------------------------------------------------------------------
  // servicio: ServicioEnEmisora, restoCaracteristicas: [ Caracteristica ]
  //     --> anyadirServicioConSusCaracteristicasYActivar() --> B
  template <typename ... T>
  bool anyadirServicioConSusCaracteristicasYActivar( ServicioEnEmisora & servicio,
													 // ServicioEnEmisora::Caracteristica & caracteristica,
													 T& ... restoCaracteristicas) {

	bool r = anyadirServicioConSusCaracteristicas( servicio, restoCaracteristicas... );

	servicio.activarServicio();

	return r;
	
  } // ()
//===========================================================================================================

  // .........................................................
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de instalarCallbackConexionEstablecida( cb ):
//   Registra la funcion que se ejecutara al establecerse una conexion.
//   No devuelve nada.
// ------------------------------------------------------------------
  // cb: Callback --> instalarCallbackConexionEstablecida() -->
  void instalarCallbackConexionEstablecida( CallbackConexionEstablecida cb ) {
	Bluefruit.Periph.setConnectCallback( cb );
  } // ()
//===========================================================================================================

  // .........................................................
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de instalarCallbackConexionTerminada( cb ):
//   Registra la funcion que se ejecutara al terminar una conexion.
//   No devuelve nada.
// ------------------------------------------------------------------
  // cb: Callback --> instalarCallbackConexionTerminada() -->
  void instalarCallbackConexionTerminada( CallbackConexionTerminada cb ) {
	Bluefruit.Periph.setDisconnectCallback( cb );
  } // ()
//===========================================================================================================

  // .........................................................
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de getConexion( connHandle ):
//   Devuelve la conexion activa identificada por su identificador.
// ------------------------------------------------------------------
  // connHandle: N --> getConexion() --> Conexion
  BLEConnection * getConexion( uint16_t connHandle ) {
	return Bluefruit.Connection( connHandle );
  } // ()
//===========================================================================================================

}; // class

#endif
//===========================================================================================================
//===========================================================================================================




