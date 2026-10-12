//=============================================================================================
//Nombre: PuertoSerie.h
//Descripcion: Clase que simplifica el uso del puerto serie (consola) de la placa. 
//              Se encarga de abrir el puerto a una velocidad dada y escribir mensajes
//              (de cualquier tipo) por consola.
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
// DISENO de PuertoSerie( baudios ) [constructor]:
//   La velocidad de comunicacion es el unico dato de entrada.
// ------------------------------------------------------------------
  // baudios: N --> PuertoSerie() -->
  PuertoSerie (long baudios) {
	Serial.begin( baudios ); 
  } // ()

  // .........................................................
  //escribir(): imprime por consola (puerto serie) cualquier 
  //          mensaje, sea texto, numero, etc. (tipos genericos).
  // .........................................................
// ------------------------------------------------------------------
// DISENO de escribir( mensaje ):
//   El mensaje a mostrar es el unico dato de entrada, y admite
//   cualquier tipo logico (Text, N, Z, R, B). No devuelve nada.
// ------------------------------------------------------------------
  // mensaje: [Type] --> escribir() -->
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
