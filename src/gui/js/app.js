//==============================================================================================
//Nombre: app.js
//Descripcion: Logica cliente de la pagina: usa el proxy de frontend_business_logic para
//              obtener las mediciones y actualiza las tarjetas y la tabla.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica cliente para obtener y mostrar mediciones
 * 
 * La GUI no hace transporte: obtiene los datos llamando al proxy
 * mediciones.obtenerMedicion() definido en frontend_business_logic/js/mediciones.js
 * (cargado antes que este fichero en index.php).
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 */

//--------------------
// mensaje: Text --> mostrarError() -->
//   Pinta el mensaje de error en el contenedor de errores.
//--------------------
function mostrarError(mensaje) {
    const errorContainer = document.getElementById('error-container');
    if (errorContainer) {
        errorContainer.innerHTML = `<div class="error-message">${mensaje}</div>`;
    }
}

//--------------------
// limpiarError() -->
//   Vacia el contenedor de errores.
//--------------------
function limpiarError() {
    const errorContainer = document.getElementById('error-container');
    if (errorContainer) {
        errorContainer.innerHTML = '';
    }
}

//--------------------
// fechaYHora: Text --> formatearFecha() --> Text
//   Pasa la fecha que llega del servidor a un formato espanol legible; si no
//   puede, la devuelve tal cual.
//--------------------
function formatearFecha(fechaYHora) {
    if (!fechaYHora) return '-';
    try {
        const fecha = new Date(fechaYHora.replace(' ', 'T'));
        return fecha.toLocaleString('es-ES', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    } catch (e) {
        return fechaYHora;
    }
}

//--------------------
// mediciones: [ Medicion ] --> actualizarTarjetas() -->
//   Busca la ultima medicion de CO2 y la de temperatura y las muestra en sus
//   tarjetas.
//--------------------
function actualizarTarjetas(mediciones) {
    if (!Array.isArray(mediciones) || mediciones.length === 0) return;
    
    const ultima = mediciones[mediciones.length - 1];
    
    // Buscar últimas mediciones de CO2 y Temperatura
    let ultimaCO2 = null;
    let ultimaTemp = null;
    
    for (let i = mediciones.length - 1; i >= 0; i--) {
        const m = mediciones[i];
        const tipo = (m.tipo || '').toLowerCase();
        if (ultimaCO2 === null && (tipo.includes('co2') || tipo.includes('co₂') || tipo.includes('co'))) {
            ultimaCO2 = m;
        }
        if (ultimaTemp === null && (tipo.includes('temp') || tipo.includes('temperatura'))) {
            ultimaTemp = m;
        }
        if (ultimaCO2 !== null && ultimaTemp !== null) break;
    }
    
    if (ultimaCO2) {
        const co2Value = document.getElementById('co2-value');
        if (co2Value) co2Value.textContent = Math.round(ultimaCO2.valor);
    }
    
    if (ultimaTemp) {
        const tempValue = document.getElementById('temp-value');
        if (tempValue) tempValue.textContent = Number(ultimaTemp.valor).toFixed(1);
    }
}

//--------------------
// mediciones: [ Medicion ] --> actualizarTabla() -->
//   Rellena la tabla con las ultimas 10 mediciones.
//--------------------
function actualizarTabla(mediciones) {
    const tbody = document.getElementById('tabla-body');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    const ultimas10 = mediciones.slice(-10);
    
    ultimas10.forEach(medicion => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${formatearFecha(medicion.fecha_y_hora)}</td>
            <td>${medicion.tipo || '-'}</td>
            <td>${medicion.valor !== undefined ? medicion.valor : '-'}</td>
        `;
        tbody.appendChild(tr);
    });
}

//--------------------
// cargarMediciones() -->
//   Pide las mediciones al proxy de frontend_business_logic y refresca tarjetas
//   y tabla; si el proxy notifica error, lo muestra.
//--------------------
async function cargarMediciones() {
    limpiarError();
    const lista = await mediciones.obtenerMedicion();
    if (mediciones.errorActual()) {
        mostrarError(mediciones.errorActual());
    }
    actualizarTarjetas(lista);
    actualizarTabla(lista);
}

// Tiempo que espera la pagina entre peticiones al API. Se mide en
// MILISEGUNDOS, que es la unidad que espera setInterval.
//
// Este es el UNICO sitio del proyecto donde se cambia la frecuencia de
// refresco: se edita este numero y la pagina pide las mediciones cada vez
// durante mas tiempo.
//
//   30 000 -> medio minuto (refresco rapido, para pruebas)
//  300 000 -> 5 minutos (valor por defecto)
// 600 000 -> 10 minutos (refresco lento, ahorra bateria y peticiones)
const INTERVALO_REFRESCO_MS = 300000;

// Convertimos a segundos para poder ponerlo en el texto de la pagina:
// Math.floor(300000 / 1000) son 300 segundos, y 300 / 60 son 5 minutos.
const REFRESCO_SEGUNDOS = INTERVALO_REFRESCO_MS / 1000;
const REFRESCO_MINUTOS = INTERVALO_REFRESCO_MS / 60000;

// El script se carga al final del <body> (ver index.php), con lo que el HTML
// ya esta completo cuando se ejecuta este codigo: los elementos #co2-value,
// #temp-value, #tabla-body y #error-container ya existen y se pueden usar
// directamente, sin esperar a ningun evento.
cargarMediciones();

// Pide las mediciones nada mas abrir la pagina, para que el usuario vea datos
// enseguida y no espere hasta el final del primer intervalo.
mostrarProximaActualizacion();

//--------------------
// callback() -->
//   Refresco automatico: vuelve a pedir las mediciones cada INTERVALO_REFRESCO_MS
//   milisegundos (5 minutos por defecto), de modo que las cifras de la pagina se
//   actualizan solas con lo que llega del REST.
//--------------------
setInterval(() => {
    cargarMediciones();
    mostrarProximaActualizacion();
}, INTERVALO_REFRESCO_MS);

//--------------------
// mostrarProximaActualizacion() -->
//   Escribe en la pagina cada cuanto se va a refrescar. Es solo informacion para
//   el usuario: el temporizador de verdad lo pone setInterval de arriba.
//--------------------
function mostrarProximaActualizacion() {
    const elemento = document.getElementById('proxima-actualizacion');
    if (!elemento) return;

    const minutos = REFRESCO_MINUTOS;
    const texto = minutos >= 1
        ? `${Number(minutos.toFixed(1))} minuto${minutos === 1 ? '' : 's'}`
        : `${REFRESCO_SEGUNDOS} segundos`;

    elemento.textContent = `Actualización automática cada ${texto}.`;
}
