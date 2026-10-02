//=============================================================================================
//Nombre: LED.h
//Descripcion: Clase para controlar un LED fisico de la placa: encender, apagar, 
//              alternar y hacerlo brillar durante un tiempo. Incluye tambien la 
//              funcion esperar(), que hace una pausa usando delay().
//Copyright: 
//Fecha de modificacion: 15-09-26
//Autor: 
//Aportacion: 
//=============================================================================================

#ifndef LED_H_INCLUIDO
#define LED_H_INCLUIDO

// ..................................................................................................
//esperar(): hace una pausa en el programa durante el tiempo indicado (en milisegundos),
//          usando delay() como desviacion temporal.
// ..................................................................................................
// ------------------------------------------------------------------
// ALGORITMO de esperar( tiempo ):
//   1. Llamar a delay( tiempo ), que bloquea el programa durante
//      los milisegundos indicados.
//   2. Devolver el control al punto que llamo a esperar().
// ------------------------------------------------------------------
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

  // .........................................................
  //Constructor: guarda el numero de pin del LED, lo configura 
  //          como salida y lo deja apagado al inicio.
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de LED( numero )  [constructor]:
//   1. Guardar el numero de pin recibido en numeroLED.
//   2. Inicializar el atributo encendido a false (apagado).
//   3. Configurar el pin numeroLED como salida (pinMode OUTPUT).
//   4. Llamar a apagar() para dejar el LED en estado LOW.
// ------------------------------------------------------------------
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
// ALGORITMO de encender():
//   1. Escribir HIGH en el pin numeroLED (digitalWrite) para que el
//      LED conduzca corriente y se encienda.
//   2. Marcar encendido = true para recordar su estado.
// ------------------------------------------------------------------
void encender () {
	digitalWrite(numeroLED, HIGH); 
	encendido = true;
  }

  // .........................................................
  //apagar(): pone el pin del LED a LOW para apagarlo.
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de apagar():
//   1. Escribir LOW en el pin numeroLED (digitalWrite) para que el
//      LED deje de conducir corriente y se apague.
//   2. Marcar encendido = false para recordar su estado.
// ------------------------------------------------------------------
void apagar () {
	  digitalWrite(numeroLED, LOW);
	  encendido = false;
  }

  // .........................................................
  //alternar(): invierte el estado del LED; si esta encendido 
  //          lo apaga, y si esta apagado lo enciende.
  // .........................................................
  // ------------------------------------------------------------------
// ALGORITMO de alternar():
//   1. Consultar el atributo encendido (estado actual del LED).
//   2. Si esta encendido, llamar a apagar() para pasarlo a off.
//   3. Si esta apagado, llamar a encender() para pasarlo a on.
//   Resultado: el estado del LED se invierte en cada llamada.
// ------------------------------------------------------------------
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
// ALGORITMO de brillar( tiempo ):
//   1. Encender el LED.
//   2. Pausar el programa durante "tiempo" ms (esperar()).
//   3. Apagar el LED.
//   Resultado: un parpadeo de la duracion indicada.
// ------------------------------------------------------------------
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
