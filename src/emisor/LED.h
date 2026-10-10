//=============================================================================================
//Nombre: LED.h
//Descripcion: Clase para controlar un LED fisico de la placa: encender, apagar, 
//              alternar y hacerlo brillar durante un tiempo. Incluye tambien la 
//              funcion esperar(), que hace una pausa usando delay().
//Copyright:
//Fecha de creacion: 2019-07-05
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Jordi Bataller
//Aportacion: limpieza y mas comentarios
//=============================================================================================

#ifndef LED_H_INCLUIDO
#define LED_H_INCLUIDO

// ..................................................................................................
//esperar(): hace una pausa en el programa durante el tiempo indicado (en milisegundos),
//          usando delay() como desviacion temporal.
// ..................................................................................................
// ------------------------------------------------------------------
// DISENO de esperar( tiempo ):
//   El tiempo de espera es el unico dato de entrada; no devuelve nada.
// ------------------------------------------------------------------
// tiempo: N --> esperar() -->
void esperar (long tiempo) {
  delay (tiempo);
}

// ----------------------------------------------------------
// ----------------------------------------------------------
//Clase LED: representa el LED fisico conectado a un pin de la placa
// ----------------------------------------------------------
class LED {
private:
  int numeroLED;
  bool encendido;
public:

//Constructor: guarda el numero de pin del LED, lo configura 
  //          como salida y lo deja apagado al inicio.
  // ------------------------------------------
  // numero: N --> LED() -->
  // ------------------------------------------
  LED (int numero)
	: numeroLED (numero), encendido(false)
  {
	pinMode(numeroLED, OUTPUT);
	apagar ();
  }

  // .........................................................
  //encender(): pone el pin del LED a HIGH para encenderlo.
  // .........................................................
// ------------------------------------------------------------------
// DISENO de encender():
//   No recibe datos ni devuelve datos: solo conmuta el estado del LED.
// ------------------------------------------------------------------
  // --> encender() -->
  void encender () {
	digitalWrite(numeroLED, HIGH); 
	encendido = true;
  }

  // .........................................................
  //apagar(): pone el pin del LED a LOW para apagarlo.
  // .........................................................
// ------------------------------------------------------------------
// DISENO de apagar():
//   No recibe datos ni devuelve datos: solo conmuta el estado del LED.
// ------------------------------------------------------------------
  // --> apagar() -->
  void apagar () {
	  digitalWrite(numeroLED, LOW);
	  encendido = false;
  }

  // .........................................................
  //alternar(): invierte el estado del LED; si esta encendido 
  //          lo apaga, y si esta apagado lo enciende.
  // .........................................................
// ------------------------------------------------------------------
// DISENO de alternar():
//   No recibe datos ni devuelve datos: invierte el estado actual.
// ------------------------------------------------------------------
  // --> alternar() -->
  void alternar () {
	if (encendido) {
	  apagar();
	} else {
	  encender ();
	}
  } // ()

  // .........................................................
  //brillar(): enciende el LED, espera el tiempo indicado y 
  //          lo apaga, generando un parpadeo de ese duracion.
  // .........................................................
// ------------------------------------------------------------------
// DISENO de brillar( tiempo ):
//   La duracion del parpadeo es el unico dato de entrada.
// ------------------------------------------------------------------
  // tiempo: N --> brillar() -->
  void brillar (long tiempo) {
	encender ();
	esperar(tiempo); 
	apagar ();
  }
}; // class

// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
// ----------------------------------------------------------
#endif
