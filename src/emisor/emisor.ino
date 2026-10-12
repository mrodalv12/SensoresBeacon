//=============================================================================================
//Nombre: emisor.ino
//Descripcion: Programa principal: inicializa la placa y la emisora BLE y, en cada
//              iteracion del loop(), publica la medicion de CO2 y la de temperatura
//              como dos anuncios iBeacon distintos.
//Copyright:
//Fecha de creacion: 2019-07-05
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

#include <bluefruit.h>

#undef min 
#undef max 

//aqui se incluyen las otras clases necesarias

#include "LED.h"
#include "PuertoSerie.h"
#include "EmisoraBLE.h"
#include "Publicador.h"
#include "Medidor.h"

//los objetos clave que tienen que estar disponibles en todo el programa
namespace Globales {
  
  LED elLED (7); //controla un led fisico en el pin 7
  PuertoSerie elPuerto ( /* velocidad = */ 115200 ); // 115200 o 9600 o ... //para la comunicacion por puerto serie (consola)
  Publicador elPublicador; //se encarga de transmitir la informacion bluetooth
  Medidor elMedidor; //se encarga de tomar las lecturas de los sensores

};

// =======================================================================================

// ------------------------------------------------------------------
// DISENO de inicializarPlaquita():
//   Punto reservado para la configuracion inicial del hardware de la
//   placa (pines, reloj, sensores, etc.). No recibe ni devuelve nada.
//   Ahora mismo el cuerpo esta vacio.
// ------------------------------------------------------------------
// --> inicializarPlaquita() -->
void inicializarPlaquita () {

    // aqui iria la configuracion inicial de la placa
     // de momento nada
} 

// ======================================= SETUP =========================================
// Inicializa todos los procesos y funciones necesarias para que el codigo funcione

// ------------------------------------------------------------------
// DISENO de setup():
//   Inicializa todo lo que el programa necesita para funcionar: el
//   puerto serie, la placa, la emisora BLE y el medidor.
//   No recibe datos ni devuelve datos.
// ------------------------------------------------------------------
// --> setup() -->

void setup() {

  //1. Inicializa la placa
  inicializarPlaquita();

  // Suspend Loop() to save power
  // suspendLoop();

  //2. enciende la emisora Bluetooth 
  Globales::elPublicador.encenderEmisora();

  // Globales::elPublicador.laEmisora.pruebaEmision(); //aqui se haria una prueba
  
  //3. prepara e inicia el sensor para medir
  Globales::elMedidor.iniciarMedidor();

  esperar( 1000 ); //pausa de 1 segundo

  //4. escribe por consola (puerto serie) que la fase de inicio termino
  Globales::elPuerto.escribir( "esto a terminado\n " );

} 
//================================================================================
//esto genera una secuencia de parpadeos en el LED para 
//    dar retroalimentacion visual de algo (se usara despues)
// la idea es que lo llames varias veces y coloques diferentes tiempos cada vez

// ------------------------------------------------------------------
// DISENO de lucecitas():
//   Genera una secuencia de parpadeos en el LED como
//   retroalimentacion visual de que el programa esta vivo.
//   No recibe datos ni devuelve datos. La idea es llamarla varias
//   veces con distintos tiempos cada vez.
// ------------------------------------------------------------------
// --> lucecitas() -->

inline void lucecitas() {
  using namespace Globales; //tienes que escribirlo para llamarlo dentro de una funcion

  elLED.brillar( 100 );  //100ms 
  delay( 400 ); 
  elLED.brillar( 100 ); //100ms
  delay( 400 );
  Globales::elLED.brillar( 100 ); //100ms
  delay( 400 ); 
  Globales::elLED.brillar( 1000 ); //1 segundo
  delay( 1000 ); 
} 

//======================================== LOOP ========================================
//ejecuta una serie de instrucciones en bucle

//esto es para tener un contador
//      para el loop .............
// ------------------------------------------------------------------
namespace Loop {
  uint8_t cont = 0;
};

//...................................

// loop(): ejecuta una serie de instrucciones en bucle. En cada
// iteracion incrementa el contador, avisa por consola, mide el CO2 y
// la temperatura y publica los dos valores como anuncios iBeacon,
// manteniendo cada anuncio 1 segundo.
// No recibe datos ni devuelve datos.
// ------------------------------------------------------------------
// DISENO de loop():
// ------------------------------------------------------------------
// --> loop() -->
void loop () {

  using namespace Loop;
  using namespace Globales;
  

  cont++; //incrementa el contador para saber en cual iteracion estamos
          //y se escribe por pantalla
  elPuerto.escribir( "\nEsta loopeando por " );
  elPuerto.escribir( cont );
  elPuerto.escribir( " vez\n" );


  lucecitas(); //lo llamamos para indicar que va a empezar con las mediciones

 //float valorTemperatura = elMedidor.medirTemperatura();
  float valorTemperatura = 1233;
  //float valorCO2 = elMedidor.medirCO2();
  float valorCO2 = 22.2;
 
  //publicas todo

  elPublicador.publicarCO2( valorCO2,
							cont, //el contador de la iteracion
						  1000 // intervalo de emisión
							);
  
  elPublicador.publicarTemperatura( valorTemperatura, 
									cont, //again
									1000 // intervalo de emisión
									);

  elPuerto.escribir( "\n valor de temperatura " );
  elPuerto.escribir( valorTemperatura );
  elPuerto.escribir( "\n valor de CO2 " );
  elPuerto.escribir( valorCO2);                
  
  //.......................................................
  //esta parte es una prueba para poder mandar mensajes personalizados 
  //  a traves de bluetooth tambien, puedes usar el char datos o colocar
  //    tu propio mensaje personalizado, pero que sean 21 caracteres

  // prueba para emitir un iBeacon y poner
  // en la carga (21 bytes = uuid 16 major 2 minor 2 txPower 1 )
  // lo que queramos (sin seguir dicho formato)
  // 
  // Al terminar la prueba hay que hacer Publicador::laEmisora privado
  
  // es decir la clase elPublicador es public ahora, hacerla private despues
  // pero solo podrasmusar funciones de esa clase ya preparadas, asique esto 
  // ya no te funcionara
/*
  char datos[21] = {
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H'
  };
  */

  // cuando quieras intercambiar entre el char de datos y un mensaje personal,
  //    comentar y descomentar

  // elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( &datos[0], 21 );
  //elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( "MolaMolaMolaMolaMolaM", 21 ); //mensaje temporal

  esperar( 2000 );

  elPublicador.laEmisora.detenerAnuncio(); //detienes el anuncio

  elPuerto.escribir( " \n\n------aqui acaba de loopear el " );
  elPuerto.escribir( cont );
  elPuerto.escribir( " ------\n" );
  
} 
//===========================================================================================

