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
// ALGORITMO de EmisoraBLE(...) [constructor]:
//   1. Recibir el nombre de la emisora, el fabricanteID y el txPower.
//   2. Guardarlos en los atributos privados nombreEmisora,
//      fabricanteID y txPower mediante la lista de inicializacion.
//   3. Cuerpo vacio: no se enciende el hardware aqui.
// ------------------------------------------------------------------
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
// ALGORITMO de encenderEmisora():
//   1. Llamar a Bluefruit.begin() para inicializar el stack BLE.
//   2. Llamar a detenerAnuncio() por si acaso, garantizando que
//      ninguna emision quede activa antes de empezar a anunciar.
// ------------------------------------------------------------------
  void encenderEmisora() {
		// Serial.println ( "Bluefruit.begin() " );
		Bluefruit.begin(); 
		(*this).detenerAnuncio(); // por si acaso, para asegurarse de que no haya ningun anuncio activo
  } 

//funcion encender emisora : con parametros callback
//	sobrecarga a la funcion anterior que ademas de encender la emisora registra los callback 
//		de conexion y desconexion

  // ------------------------------------------------------------------
// ALGORITMO de encenderEmisora( cbce, cbct ):
//   Sobrecarga de encenderEmisora() que ademas registra callbacks:
//   1. Llamar a encenderEmisora() sin argumentos (Bluefruit.begin()).
//   2. Registrar cbce como callback de conexion establecida.
//   3. Registrar cbct como callback de conexion terminada.
// ------------------------------------------------------------------
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
// ALGORITMO de detenerAnuncio():
//   1. Preguntar a Bluefruit si hay algun anuncio en curso
//      (estaAnunciando()).
//   2. Si lo hay, llamar a Bluefruit.Advertising.stop() para
//      detener la emision.
//   3. Si no hay ninguno, no hacer nada.
// ------------------------------------------------------------------
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
// ALGORITMO de estaAnunciando():
//   1. Preguntar a Bluefruit.Advertising.isRunning().
//   2. Devolver true si hay una emision en curso o false si no.
// ------------------------------------------------------------------
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
// ALGORITMO de emitirAnuncioIBeacon( beaconUUID, major, minor, rssi ):
//   1. Detener cualquier anuncio previo que pueda seguir activo.
//   2. Crear un BLEBeacon con el UUID, major, minor y rssi dados.
//   3. Asignar el fabricanteID al beacon.
//   4. Configurar la potencia de transmision (txPower), el nombre
//      de la emisora y anadir el nombre a la respuesta de escaneo.
//   5. Registrar el beacon como dato de advertising.
//   6. Permitir reiniciar el anuncio al reconectar (restartOnDisconnect).
//   7. Fijar el intervalo de emision a 100 ms (unidades de 0.625 ms).
//   8. Arrancar el anuncio en modo indefinido (start(0)); otro
//      codigo se encargara de detenerlo cuando toque.
// ------------------------------------------------------------------
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
  // Para enviar como carga libre los últimos 21 bytes de un iBeacon (lo que normalmente sería uuid-16 major-2 minor-2 txPower-1)
  // .........................................................
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
// ALGORITMO de emitirAnuncioIBeaconLibre( carga, tamanyoCarga ):
//   1. Detener cualquier anuncio anterior y limpiar Advertising y
//      ScanResponse.
//   2. Fijar el nombre de la emisora y anadirlo al ScanResponse.
//   3. Anexar los flags de advertising (LE only, general discoverable).
//   4. Preparar un array de 4 + 21 bytes con la cabecera fija de
//      iBeacon (companyID 0x4c00, tipo 0x02, longitud 21).
//   5. Copiar la carga recibida en el hueco de 21 bytes restante,
//      recortando a 21 si la carga fuese mas larga (memcpy).
//   6. Anadir ese array como Manufacturer Specific Data (un solo
//      addData).
//   7. Fijar restartOnDisconnect, intervalo 100 ms y timeout rapido
//      de 1 segundo.
//   8. Arrancar el anuncio indefinido (start(0)) e informar por
//      consola.
// ------------------------------------------------------------------
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
// ALGORITMO de anyadirServicio( servicio ):
//   1. Pedir a Bluefruit.Advertising.addService() que registre el
//      servicio BLE (usando la conversion ServicioEnEmisora a
//      BLEService).
//   2. Si el registro falla, escribir un aviso por consola.
//   3. Devolver true/false segun el resultado de addService().
// ------------------------------------------------------------------
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
// ALGORITMO de anyadirServicioConSusCaracteristicas( servicio )  [caso base]:
//   1. Devolver directamente el resultado de anyadirServicio(), sin
//      anadir ninguna caracteristica (caso final de la recursion).
// ------------------------------------------------------------------
  bool anyadirServicioConSusCaracteristicas( ServicioEnEmisora & servicio ) { 
	return (*this).anyadirServicio( servicio );
  } // 
//===========================================================================================================

  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de anyadirServicioConSusCaracteristicas( servicio,
//                          caracteristica, restoCaracteristicas... ):
//   Recursivo, va anadiendo caracteristicas una a una:
//   1. Anadir la caracteristica actual al servicio
//      (servicio.anyadirCaracteristica(caracteristica)).
//   2. Llamarse a si misma con el resto de caracteristicas.
//   3. En el caso base (sin caracteristicas) se delega en
//      anyadirServicio() para registrar el servicio en BLE.
// ------------------------------------------------------------------
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
// ALGORITMO de anyadirServicioConSusCaracteristicasYActivar( servicio,
//                                             restoCaracteristicas... ):
//   1. Llamar a anyadirServicioConSusCaracteristicas() para registrar
//      el servicio con todas sus caracteristicas.
//   2. Activar el servicio (servicio.activarServicio()): da de alta
//      en el stack BLE el servicio y sus caracteristicas.
//   3. Devolver el resultado de la llamada del paso 1.
// ------------------------------------------------------------------
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
// ALGORITMO de instalarCallbackConexionEstablecida( cb ):
//   1. Registrar la funcion cb como callback de conexion establecida
//      en el periferico (Bluefruit.Periph.setConnectCallback(cb)).
// ------------------------------------------------------------------
  void instalarCallbackConexionEstablecida( CallbackConexionEstablecida cb ) {
	Bluefruit.Periph.setConnectCallback( cb );
  } // ()
//===========================================================================================================

  // .........................................................
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de instalarCallbackConexionTerminada( cb ):
//   1. Registrar la funcion cb como callback de desconexion en el
//      periferico (Bluefruit.Periph.setDisconnectCallback(cb)).
// ------------------------------------------------------------------
  void instalarCallbackConexionTerminada( CallbackConexionTerminada cb ) {
	Bluefruit.Periph.setDisconnectCallback( cb );
  } // ()
//===========================================================================================================

  // .........................................................
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de getConexion( connHandle ):
//   1. Pedir a Bluefruit la conexion activa identificada por
//      connHandle (Bluefruit.Connection(connHandle)).
//   2. Devolver el puntero BLEConnection correspondiente.
// ------------------------------------------------------------------
  BLEConnection * getConexion( uint16_t connHandle ) {
	return Bluefruit.Connection( connHandle );
  } // ()
//===========================================================================================================

}; // class

#endif
//===========================================================================================================
//===========================================================================================================




