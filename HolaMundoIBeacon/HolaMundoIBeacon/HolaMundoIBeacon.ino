//=============================================================================================
//Nombre: HolaMundoIBeacon.ino
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
// ALGORITMO de inicializarPlaquita():
//   1. Punto reservado para hacer la configuracion inicial del
//      hardware de la placa (pines, reloj, sensores, etc.).
//   2. Ahora mismo no se ejecuta ninguna instruccion: el cuerpo
//      esta vacio (solo hay un comentario indicando donde iria).
// ------------------------------------------------------------------
void inicializarPlaquita () {

    // aqui iria la configuracion inicial de la placa
     // de momento nada
} 

// ======================================= SETUP =========================================
// Inicializa todos los procesos y funciones necesarias para que el codigo funcione

// function void setup () {
//    VariableGlobal.Puerto.esperarDisponible()
//    IniciarPlaquita()
//    VariableGlobal.Publicador.encenderEmisora()
//    VariableGlobal.Medidor.iniciarMedidor()
//    VariableGlobal.Puerto.escribir(string finalDeInicializado)
// }
//..........................................................................

void setup() {

  //1. esperas a que el puerto este disponible para enviar datos
  Globales::elPuerto.esperarDisponible(); //el Globales: asi llamas al namespace

  //2. Inicializa la placa
  inicializarPlaquita();

  // Suspend Loop() to save power
  // suspendLoop();

  //3. enciende la emisora Bluetooth 
  Globales::elPublicador.encenderEmisora();

  // Globales::elPublicador.laEmisora.pruebaEmision(); //aqui se haria una prueba
  
  //4. prepara e inicia el sensor para medir
  Globales::elMedidor.iniciarMedidor();

  esperar( 1000 ); //pausa de 1 segundo

  //5. escribe por consola (puerto serie) que la fase de inicio termino
  Globales::elPuerto.escribir( "esto a terminado\n " );

} 
//================================================================================
//esto genera una secuencia de parpadeos en el LED para 
//    dar retroalimentacion visual de algo (se usara despues)
// la idea es que lo llames varias veces y coloques diferentes tiempos cada vez

// void lucecitas(){
//    using VariablesGlobales  
//    VariableGlobal.LED.brillar( int tiempoDeBrillo )
//    VariableGlobal.LED.brillar( int tiempoDeBrillo )
//    VariableGlobal.LED.brillar( int tiempoDeBrillo )
//    VariableGlobal.LED.brillar( int tiempoDeBrillo )
// }
//.........................................................................

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

// void loop () {
//    using Loop
//    using VariablesGlobales  
//    
//    Loop.contador++
//    VariablesGlobales.Puerto.Escribir(var numeroDeIteracion)
//    lucecitas()

//    int valorCO2 = VariablesGlobales.Medidor.MedirCO2()
//    int valorTemperatura = VariablesGlobales.Medidor.MedirTemperatura()

//    VariablesGlobales.Publicador.PublicarCO2(double valor, int contador, int intervalo)
//    VariablesGlobales.Publicador.PublicarTemperatura(double valor, int contador, int intervalo)

//    VariablesGlobales.Publicador.Emisora.DetenerAnuncio()
//    VariablesGlobales.Puerto.Escribir(string mensajeDeFinalDeLoop)
// }
//.............................................................................

//esto es para tener un contador
//      para el loop ...............
// ------------------------------------------------------------------
namespace Loop {
  uint8_t cont = 0;
};

//...................................

// loop(): ejecuta una serie de instrucciones en bucle (diseno commented-out arriba).
void loop () {

  using namespace Loop;
  using namespace Globales;
  

  cont++; //incrementa el contador para saber en cual iteracion estamos
          //y se escribe por pantalla
  elPuerto.escribir( "\nEsta loopeando por " );
  elPuerto.escribir( cont );
  elPuerto.escribir( " vez\n" );


  lucecitas(); //lo llamamos para indicar que va a empezar con las mediciones

  //float valorCO2 = elMedidor.medirCO2();
  float valorCO2 = 56.78;
  //float valorTemperatura = elMedidor.medirTemperatura();
  float valorTemperatura = 12.34;

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

  char datos[21] = {
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H'
  };

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

