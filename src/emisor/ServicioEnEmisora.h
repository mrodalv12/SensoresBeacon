//=============================================================================================
//Nombre: ServicioEnEmisora.h
//Descripcion: Define un servicio Bluetooth (BLE) que se puede anunciar desde la emisora.
//              Contiene una clase Caracteristica (BLECharacteristic) y utilidades para
//              convertir nombres (string) a UUID de 16 bytes en orden little-endian.
//              Permite configurar propiedades, permisos y tamanyo de datos de cada
//              caracteristica, ademas de escribir/notificar datos y activar el servicio.
//Copyright:
//Fecha de creacion: 2019-07-19
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

#ifndef SERVICIO_EMISORA_H_INCLUIDO
#define SERVICIO_EMISORA_H_INCLUIDO
#include <bluefruit.h>
#include <vector>

// ----------------------------------------------------
// alReves() utilidad
// pone al reves el contenido de una array en el mismo array
// ----------------------------------------------------
// ------------------------------------------------------------------
// DISENO de alReves( p, n ):
//   Invierte el contenido del array sobre si mismo y lo devuelve.
// ------------------------------------------------------------------
// p: [ Type ]_n, n: N --> alReves() --> [ Type ]_n
template< typename T >
T *  alReves( T * p, int n ) {
  T aux;

  for( int i=0; i < n/2; i++ ) {
	aux = p[i];
	p[i] = p[n-i-1];
	p[n-i-1] = aux;
  }
  return p;
} // ()

// ----------------------------------------------------
// stringAUint8AlReves(): copia los caracteres de un string
// hacia un array de bytes, pero colocandolos al reves
// (necesario para que el UUID quede en el orden que
// espera Bluetooth, con el byte menos significativo primero).
// ----------------------------------------------------
// ------------------------------------------------------------------
// DISENO de stringAUint8AlReves( pString, pUint, tamMax ):
//   Convierte el nombre de 16 caracteres en el UUID de 16 bytes que
//   espera Bluetooth, con el byte menos significativo primero. Si el
//   nombre es mas corto que tamMax, los huecos sobrantes quedan a 0.
// ------------------------------------------------------------------
// pString: Text, pUint: [ Byte ]_tamMax, tamMax: N
//     --> stringAUint8AlReves() --> [ Byte ]_tamMax
uint8_t * stringAUint8AlReves( const char * pString, uint8_t * pUint, int tamMax ) {
  memset(pUint, 0, tamMax); // Limpia todo el array con 0s
  int longitudString = strlen( pString );
  int longitudCopiar = ( longitudString > tamMax ? tamMax : longitudString );
  
  for( int i = 0; i < longitudCopiar; i++ ) {
    pUint[ tamMax - 1 - i ] = pString[ i ];
  }
  return pUint;
}

// ----------------------------------------------------------
// ----------------------------------------------------------
//Clase ServicioEnEmisora: agrupa un BLE servicio con sus caracteristicas
// ----------------------------------------------------------
class ServicioEnEmisora {

public:


  // --------------------------------------------------------
  // --------------------------------------------------------

  // .........................................................
  //CallbackCaracteristicaEscrita: tipo de funcion que se llama
  //          cuando escriben datos en una caracteristica.
  // .........................................................
  using CallbackCaracteristicaEscrita = void ( uint16_t conn_handle,
											   BLECharacteristic * chr,
											   uint8_t * data, uint16_t len); 
  // .........................................................
  // .........................................................
  //Clase Caracteristica: representa una caracteristica BLE (cache de datos)
  class Caracteristica {
  private:
	uint8_t uuidCaracteristica[16] = { // el uuid se copia aquí (al revés) a partir de un string-c
	  // least signficant byte, el primero
	  '0', '1', '2', '3', 
	  '4', '5', '6', '7', 
	  '8', '9', 'A', 'B', 
	  'C', 'D', 'E', 'F'
	};

	// 
	// 
	// 
	BLECharacteristic laCaracteristica;

  public:

	// .........................................................
	//Constructor 1: recibe el nombre de la caracteristica y lo
	//          convierte en UUID (al reves) para crearla.
	// .........................................................

	// ------------------------------------------------------------------
// DISENO de Caracteristica( nombreCaracteristica_ )  [constructor 1]:
//   El nombre es el unico dato de entrada y determina el UUID.
// ------------------------------------------------------------------
	// nombreCaracteristica_: Text --> Caracteristica() -->
	Caracteristica( const char * nombreCaracteristica_ ) {
    stringAUint8AlReves( nombreCaracteristica_, uuidCaracteristica, 16 );
    laCaracteristica = BLECharacteristic( uuidCaracteristica );
}

	// .........................................................
	//Constructor 2: ademas del nombre, permite configurar ya
	//          las propiedades, permisos y tamanyo de datos
	//          llamando al primer constructor y despues a
	//          asignarPropiedadesPermisosYTamanyoDatos().
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de Caracteristica( nombre, props, permisoRead,
//                           permisoWrite, tam )  [constructor 2]:
//   Ademas del nombre, fija ya las propiedades, los permisos y el
//   tamanyo maximo de datos. No devuelve nada.
// ------------------------------------------------------------------
	// nombreCaracteristica_: Text, props: N, permisoRead: [Type],
	// permisoWrite: [Type], tam: N
	//     --> Caracteristica() -->
	Caracteristica( const char * nombreCaracteristica_ ,
					uint8_t props,
					SecureMode_t permisoRead,
					SecureMode_t permisoWrite, 
					uint8_t tam ) 
	  :
	  Caracteristica( nombreCaracteristica_ ) // llamada al otro constructor
	{
	  (*this).asignarPropiedadesPermisosYTamanyoDatos( props, permisoRead, permisoWrite, tam );
	} // ()

  private:
	// .........................................................
	//asignarPropiedades(): fija las propiedades de la caracteristica.
	//          Valores posibles: CHR_PROPS_WRITE, CHR_PROPS_READ, CHR_PROPS_NOTIFY.
	// .........................................................
	// CHR_PROPS_WRITE , CHR_PROPS_READ ,  CHR_PROPS_NOTIFY 
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de asignarPropiedades( props ):
//   No devuelve nada; solo fija las propiedades BLE.
// ------------------------------------------------------------------
	// props: N --> asignarPropiedades() -->
	void asignarPropiedades ( uint8_t props ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( " laCaracteristica.setProperties( props ); ");
	  (*this).laCaracteristica.setProperties( props );
	} // ()

	// .........................................................
	//asignarPermisos(): fija los permisos de lectura y escritura
	//          de la caracteristica. Valores posibles:
	//          SECMODE_OPEN, SECMODE_NO_ACCESS.
	// .........................................................
	// SECMODE_OPEN  , SECMODE_NO_ACCESS
	// .........................................................
// ------------------------------------------------------------------
// DISENO de asignarPermisos( permisoRead, permisoWrite ):
//   No devuelve nada; solo fija los permisos de lectura y escritura.
// ------------------------------------------------------------------
	// permisoRead: [Type], permisoWrite: [Type]
	//     --> asignarPermisos() -->
	void asignarPermisos( SecureMode_t permisoRead, SecureMode_t permisoWrite ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( "laCaracteristica.setPermission( permisoRead, permisoWrite ); " );
	  (*this).laCaracteristica.setPermission( permisoRead, permisoWrite );
	} // ()

	// .........................................................
	//asignarTamanyoDatos(): fija la longitud maxima de los datos
	//          que cabe en la caracteristica.
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de asignarTamanyoDatos( tam ):
//   No devuelve nada; solo fija la longitud maxima de datos.
// ------------------------------------------------------------------
	// tam: N --> asignarTamanyoDatos() -->
	void asignarTamanyoDatos( uint8_t tam ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.print( " (*this).laCaracteristica.setFixedLen( tam = " );
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( tam );
	  // (*this).laCaracteristica.setFixedLen( tam );
	  (*this).laCaracteristica.setMaxLen( tam );
	} // ()

  public:
	// .........................................................
	//asignarPropiedadesPermisosYTamanyoDatos(): configuracion
	//          completa de la caracteristica: propiedades, permisos
	//          de lectura/escritura y tamanyo de datos.
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de asignarPropiedadesPermisosYTamanyoDatos( props,
//                                     permisoRead, permisoWrite, tam ):
//   Configuracion completa de la caracteristica. No devuelve nada.
// ------------------------------------------------------------------
	// props: N, permisoRead: [Type], permisoWrite: [Type], tam: N
	//     --> asignarPropiedadesPermisosYTamanyoDatos() -->
	void asignarPropiedadesPermisosYTamanyoDatos( uint8_t props,
												 SecureMode_t permisoRead,
												 SecureMode_t permisoWrite, 
												 uint8_t tam ) {
	  asignarPropiedades( props );
	  asignarPermisos( permisoRead, permisoWrite );
	  asignarTamanyoDatos( tam );
	} // ()
												 

	// .........................................................
	//escribirDatos(): escribe el string recibido dentro de la
	//          caracteristica y devuelve cuantos bytes se escribieron.
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de escribirDatos( str ):
//   Devuelve cuantos bytes se han podido escribir.
// ------------------------------------------------------------------
	// str: Text --> escribirDatos() --> r: N
	uint16_t escribirDatos( const char * str ) {
	  // Serial.print( " return (*this).laCaracteristica.write( str  = " );
	  // Serial.println( str );

	  uint16_t r = (*this).laCaracteristica.write( str );

	  // Serial.print( ">>>Escritos " ); Serial.print( r ); Serial.println( " bytes con write() " );

	  return r;
	} // ()

	// .........................................................
	//notificarDatos(): envia el string como notificacion a un
	//          dispositivo conectado. Devuelve los bytes enviados.
	// .........................................................
// ------------------------------------------------------------------
// DISENO de notificarDatos( str ):
//   Envia el texto como notificacion a los dispositivos conectados.
//   Devuelve cuantos bytes se han notificado.
// ------------------------------------------------------------------
	// str: Text --> notificarDatos() --> r: N
uint16_t notificarDatos( const char * str ) {
    return laCaracteristica.notify( (const uint8_t*) str, strlen(str) );
}

	// .........................................................
	//instalarCallbackCaracteristicaEscrita(): registra una funcion
	//          que se ejecutara cuando escriban datos en la
	//          caracteristica desde fuera.
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de instalarCallbackCaracteristicaEscrita( cb ):
//   Registra la funcion que se ejecutara cuando un dispositivo
//   externo escriba datos. No devuelve nada.
// ------------------------------------------------------------------
	// cb: Callback --> instalarCallbackCaracteristicaEscrita() -->
	void instalarCallbackCaracteristicaEscrita( CallbackCaracteristicaEscrita cb ) {
	  (*this).laCaracteristica.setWriteCallback( cb );
	} // ()

	// .........................................................
	//activar(): da de alta la caracteristica en el stack BLE y
	//          escribe el resultado (error) por consola.
	// .........................................................
	// ------------------------------------------------------------------
// DISENO de activar() [Caracteristica]:
//   No recibe datos ni devuelve datos: da de alta la caracteristica en
//   el stack BLE e informa por consola del codigo de error.
// ------------------------------------------------------------------
	// --> activar() -->
	void activar() {
	  err_t error = (*this).laCaracteristica.begin();
	  Serial.print(  " (*this).laCaracteristica.begin(); error = " );
	  Serial.print( error );
	} // ()

  }; // class Caracteristica
  
  // --------------------------------------------------------
  // --------------------------------------------------------
private:
  
  uint8_t uuidServicio[16] = { // el uuid se copia aquí (al revés) a partir de un string-c
	// least signficant byte, el primero
	'0', '1', '2', '3', 
	'4', '5', '6', '7', 
	'8', '9', 'A', 'B', 
	'C', 'D', 'E', 'F'
  };

  //
  //
  //
  BLEService elServicio;

  //
  //lista de caracteristicas que tiene este servicio
  //
  std::vector< Caracteristica * > lasCaracteristicas;

public:
  
  // .........................................................
  //Constructor: recibe el nombre del servicio y lo convierte
  //          en UUID (al reves) para crear el BLEService.
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de ServicioEnEmisora( nombreServicio_ ) [constructor]:
//   El nombre es el unico dato de entrada y determina el UUID.
// ------------------------------------------------------------------
	// nombreServicio_: Text --> ServicioEnEmisora() -->
	ServicioEnEmisora( const char * nombreServicio_ )
	:
	elServicio( stringAUint8AlReves( nombreServicio_, &uuidServicio[0], 16 ) )
  {
	
  } // ()
  
  // .........................................................
  //escribeUUID(): imprime por consola el UUID del servicio
  //          (util para comprobar si se genero bien).
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de escribeUUID():
//   No recibe datos ni devuelve datos: imprime por consola el UUID
//   generado, util para comprobar si se ha creado bien.
// ------------------------------------------------------------------
	// --> escribeUUID() -->
	void escribeUUID() {
	Serial.println ( "**********" );
	for (int i=0; i<= 15; i++) {
	  Serial.print( (char) uuidServicio[i] );
	}
	Serial.println ( "\n**********" );
  } // ()

  // .........................................................
  //anyadirCaracteristica(): guarda una caracteristica en la
  //          lista del servicio para activarla mas tarde.
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de anyadirCaracteristica( car ):
//   Anade la caracteristica a la lista del servicio; se quedara
//   pendiente de activar hasta que se llame a activarServicio().
//   No devuelve nada.
// ------------------------------------------------------------------
	// car: Caracteristica --> anyadirCaracteristica() -->
	void anyadirCaracteristica( Caracteristica & car ) {
	(*this).lasCaracteristicas.push_back( & car );
  } // ()

  // .........................................................
  //activarServicio(): da de alta el servicio en el stack BLE
  //          y despues activa todas sus caracteristicas.
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de activarServicio():
//   No recibe datos ni devuelve datos: da de alta el servicio en el
//   stack BLE y activa despues todas sus caracteristicas.
// ------------------------------------------------------------------
	// --> activarServicio() -->
	void activarServicio( ) {
	// entiendo que al llegar aquí ya ha sido configurado
	// todo: características y servicio

	err_t error = (*this).elServicio.begin();
	Serial.print( " (*this).elServicio.begin(); error = " );
	Serial.println( error );

	for( auto pCar : (*this).lasCaracteristicas ) {
	  (*pCar).activar();
	} // for

  } // ()

  // .........................................................
  //operator BLEService&(): conversion de tipo. Permite usar
  //          una ServicioEnEmisora donde se espera un BLEService.
  // .........................................................
  // ------------------------------------------------------------------
// DISENO de operator BLEService&():
//   Conversion de tipo implicita: devuelve el servicio BLE interno,
//   para poder usar esta clase donde se espere un BLEService.
// ------------------------------------------------------------------
	// --> operator BLEService&() --> Servicio
	operator BLEService&() {
	// "conversión de tipo": si pongo esta clase en un sitio donde necesitan un BLEService
	return elServicio;
  } // ()
	
}; // class

#endif

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------

