//=============================================================================================
//Nombre: PuertoSerie.h
//Descripcion: Clase que simplifica el uso del puerto serie (consola) de la placa. 
//              Se encarga de abrir el puerto a una velocidad dada, esperar a que 
//              este disponible y escribir mensajes (de cualquier tipo) por consola.
//Copyright:
//Fecha de creacion: 2019-07-05
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

#ifndef PUERTO_SERIE_H_INCLUIDO
#define PUERTO_SERIE_H_INCLUIDO

// ----------------------------------------------------------
// ----------------------------------------------------------
//Clase PuertoSerie: envuelve a Serial para facilitar las salidas por consola
// ----------------------------------------------------------
class PuertoSerie  {

public:
  // .........................................................
  //Constructor: abre el puerto serie a la velocidad indicada 
  //          (por ejemplo 9600 o 115200 baudios).
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de PuertoSerie( baudios ) [constructor]:
//   1. Llamar a Serial.begin( baudios ) para abrir el puerto serie
//      con la velocidad indicada (por ejemplo 9600 o 115200 baudios).
//   2. A partir de aqui, el puerto queda disponible para enviar y
//      recibir datos por consola.
// ------------------------------------------------------------------
PuertoSerie (long baudios) {
	Serial.begin( baudios ); 
  } // ()

  // .........................................................
  //esperarDisponible(): bloquea el programa hasta que el 
  //          puerto serie este listo para enviar datos.
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de esperarDisponible():
//   1. Mientras Serial sea falsy (es decir, el puerto serie no este
//      listo), hacer una pausa de 10 ms (delay(10)).
//   2. En cuanto Serial sea truthy, salir del bucle: el puerto
//      ya puede usarse.
//   Nota: es un bucle bloqueante; el programa no avanza hasta que
//   el puerto serie esta disponible.
// ------------------------------------------------------------------
void esperarDisponible() {

	while ( !Serial ) {
	  delay(10);   
	}

  } // ()

  // .........................................................
  //escribir(): imprime por consola (puerto serie) cualquier 
  //          mensaje, sea texto, numero, etc. (tipos genericos).
  // .........................................................
// ------------------------------------------------------------------
// ALGORITMO de escribir( mensaje ):
//   Plantilla generica (puede recibir texto, numero, etc.):
//   1. Llamar a Serial.print( mensaje ) para escribir el valor
//      recibido por el puerto serie (consola).
//   2. Terminar la funcion sin devolver nada.
// ------------------------------------------------------------------
template<typename T>
void escribir (T mensaje) {
	Serial.print( mensaje );
  } // ()
  
}; // class PuertoSerie

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
#endif
