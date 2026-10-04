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
// ALGORITMO de alReves( p, n ):
//   1. Iniciar un bucle i desde 0 hasta n/2 - 1.
//   2. Guardar el elemento p[i] en una variable auxiliar.
//   3. Copiar p[n-i-1] en p[i].
//   4. Copiar el auxiliar (antiguo p[i]) en p[n-i-1].
//   Resultado: los n elementos del array quedan invertidos in situ.
//   5. Devolver el puntero al array ya invertido.
// ------------------------------------------------------------------
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
// ALGORITMO de stringAUint8AlReves( pString, pUint, tamMax ):
//   1. Rellenar pUint con ceros (memset) en sus tamMax bytes.
//   2. Calcular la longitud del string y cuantos caracteres se van
//      a copiar: como mucho tamMax (min(strlen, tamMax)).
//   3. Recorrer los caracteres del string desde el 0.
//   4. Colocar cada caracter en pUint[tamMax - 1 - i], empezando
//      por el final del array (orden inverso / little-endian).
//   5. Devolver el puntero pUint ya relleno.
// ------------------------------------------------------------------
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
// ALGORITMO de Caracteristica( nombreCaracteristica_ )  [constructor 1]:
//   1. Convertir el nombre de la caracteristica a un UUID de 16
//      bytes en orden inverso (stringAUint8AlReves), guardandolo
//      en uuidCaracteristica.
//   2. Construir la BLECharacteristic con ese UUID y guardarla en
//      el atributo laCaracteristica.
// ------------------------------------------------------------------
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
// ALGORITMO de Caracteristica( nombre, props, permisoRead,
//                               permisoWrite, tam )  [constructor 2]:
//   1. Delegar en el constructor 1 para generar el UUID y crear la
//      BLECharacteristic con el nombre dado.
//   2. Llamar a asignarPropiedadesPermisosYTamanyoDatos() para fijar
//      las propiedades, los permisos de lectura/escritura y el
//      tamanyo maximo de datos de la caracteristica.
// ------------------------------------------------------------------
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
// ALGORITMO de asignarPropiedades( props ):
//   1. Llamar a laCaracteristica.setProperties( props ) para fijar
//      las propiedades BLE (CHR_PROPS_READ/WRITE/NOTIFY, etc.).
// ------------------------------------------------------------------
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
// ALGORITMO de asignarPermisos( permisoRead, permisoWrite ):
//   1. Llamar a laCaracteristica.setPermission() con los permisos de
//      lectura y escritura recibidos (SECMODE_*.).
// ------------------------------------------------------------------
	void asignarPermisos( SecureMode_t permisoRead, SecureMode_t permisoWrite ) {
	  // no puedo escribir AUN si el constructor llama a esto: Serial.println( "laCaracteristica.setPermission( permisoRead, permisoWrite ); " );
	  (*this).laCaracteristica.setPermission( permisoRead, permisoWrite );
	} // ()

	// .........................................................
	//asignarTamanyoDatos(): fija la longitud maxima de los datos
	//          que cabe en la caracteristica.
	// .........................................................
	// ------------------------------------------------------------------
// ALGORITMO de asignarTamanyoDatos( tam ):
//   1. Llamar a laCaracteristica.setMaxLen( tam ) para fijar la
//      longitud maxima de datos que puede albergar la caracteristica.
// ------------------------------------------------------------------
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
// ALGORITMO de asignarPropiedadesPermisosYTamanyoDatos( props,
//                            permisoRead, permisoWrite, tam ):
//   Funcion auxiliar de configuracion completa:
//   1. Fijar las propiedades con asignarPropiedades( props ).
//   2. Fijar los permisos con asignarPermisos( permisoRead,
//      permisoWrite ).
//   3. Fijar el tamanyo maximo con asignarTamanyoDatos( tam ).
// ------------------------------------------------------------------
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
// ALGORITMO de escribirDatos( str ):
//   1. Llamar a laCaracteristica.write( str ) para escribir el
//      string en la caracteristica BLE.
//   2. Guardar y devolver cuantos bytes se escribieron (uint16_t r).
// ------------------------------------------------------------------
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
// ALGORITMO de notificarDatos( str ):
//   1. Calcular la longitud del string (strlen).
//   2. Llamar a laCaracteristica.notify() con el string y su
//      longitud para notificarlo a un dispositivo conectado.
//   3. Devolver cuantos bytes fueron notificados.
// ------------------------------------------------------------------
uint16_t notificarDatos( const char * str ) {
    return laCaracteristica.notify( (const uint8_t*) str, strlen(str) );
}

	// .........................................................
	//instalarCallbackCaracteristicaEscrita(): registra una funcion
	//          que se ejecutara cuando escriban datos en la
	//          caracteristica desde fuera.
	// .........................................................
	// ------------------------------------------------------------------
// ALGORITMO de instalarCallbackCaracteristicaEscrita( cb ):
//   1. Registrar la funcion cb como callback de escritura de la
//      caracteristica (setWriteCallback), de modo que se ejecute
//      cuando un dispositivo externo escriba datos en ella.
// ------------------------------------------------------------------
	void instalarCallbackCaracteristicaEscrita( CallbackCaracteristicaEscrita cb ) {
	  (*this).laCaracteristica.setWriteCallback( cb );
	} // ()

	// .........................................................
	//activar(): da de alta la caracteristica en el stack BLE y
	//          escribe el resultado (error) por consola.
	// .........................................................
	// ------------------------------------------------------------------
// ALGORITMO de activar() [Caracteristica]:
//   1. Registrar la caracteristica en el stack BLE
//      (laCaracteristica.begin()).
//   2. Capturar el codigo de error devuelto (err_t).
//   3. Escribir ese codigo por consola para diagnosticar si el
//      alta fue correcta.
// ------------------------------------------------------------------
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
// ALGORITMO de ServicioEnEmisora( nombreServicio_ ) [constructor]:
//   1. Convertir el nombre del servicio a un UUID de 16 bytes en
//      orden inverso (stringAUint8AlReves), guardandolo en
//      uuidServicio.
//   2. Construir el BLEService con ese UUID y guardarlo en
//      elServicio (lista de inicializacion).
// ------------------------------------------------------------------
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
// ALGORITMO de escribeUUID():
//   1. Imprimir una linea de asteriscos de separacion.
//   2. Recorrer los 16 bytes de uuidServicio imprimiendo cada byte
//      como caracter.
//   3. Imprimir otra linea de asteriscos de cierre.
//   Resultado: ayuda visual para comprobar el UUID generado.
// ------------------------------------------------------------------
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
// ALGORITMO de anyadirCaracteristica( car ):
//   1. Anadir la direccion de la caracteristica recibida al vector
//      lasCaracteristicas del servicio (push_back(&car)).
//   Nota: se guarda hasta que activarServicio() la active.
// ------------------------------------------------------------------
	void anyadirCaracteristica( Caracteristica & car ) {
	(*this).lasCaracteristicas.push_back( & car );
  } // ()

  // .........................................................
  //activarServicio(): da de alta el servicio en el stack BLE
  //          y despues activa todas sus caracteristicas.
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de activarServicio():
//   1. Registrar el BLEService en el stack BLE (elServicio.begin())
//      y escribir su codigo de error por consola.
//   2. Recorrer el vector lasCaracteristicas y llamar a activar()
//      sobre cada caracteristica para darla de alta.
// ------------------------------------------------------------------
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
// ALGORITMO de operator BLEService&():
//   Conversion de tipo implicita de ServicioEnEmisora a BLEService:
//   1. Devolver el BLEservice interno (elServicio) para poder usar
//      esta clase donde se espere una referencia a BLEService.
// ------------------------------------------------------------------
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

