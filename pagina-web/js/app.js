//==============================================================================================
//Nombre: app.js
//Descripcion: Logica cliente de la pagina: obtiene las mediciones del servidor REST y
//              actualiza las tarjetas y la tabla.
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
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 */

// obtenerMedicion(): pide las mediciones al API y devuelve el array recibido;
// si la peticion falla, avisa por pantalla y devuelve una lista vacia.
async function obtenerMedicion() {
    try {
        const response = await fetch('/api/mediciones.php');
        if (!response.ok) {
            throw new Error('Error en la petición HTTP');
        }
        const data = await response.json();
        return Array.isArray(data) ? data : [];
    } catch (error) {
        console.error('Error al obtener mediciones:', error);
        mostrarError('No se ha podido conectar con el servidor REST. Verifica la conexión.');
        return [];
    }
}

// mostrarError(): pinta el mensaje de error en el contenedor de errores.
function mostrarError(mensaje) {
    const errorContainer = document.getElementById('error-container');
    if (errorContainer) {
        errorContainer.innerHTML = `<div class="error-message">${mensaje}</div>`;
    }
}

// limpiarError(): vacia el contenedor de errores.
function limpiarError() {
    const errorContainer = document.getElementById('error-container');
    if (errorContainer) {
        errorContainer.innerHTML = '';
    }
}

// formatearFecha(): pasa la fecha que llega del servidor a un formato espanol
// legible; si no puede, la devuelve tal cual.
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

// actualizarTarjetas(): busca la ultima medicion de CO2 y la de temperatura y
// las muestra en sus tarjetas.
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

// actualizarTabla(): rellena la tabla con las ultimas 10 mediciones.
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

// cargarMediciones(): pide las mediciones y refresca tarjetas y tabla.
async function cargarMediciones() {
    limpiarError();
    const mediciones = await obtenerMedicion();
    actualizarTarjetas(mediciones);
    actualizarTabla(mediciones);
}

document.addEventListener('DOMContentLoaded', () => {
    cargarMediciones();
    setInterval(cargarMediciones, 5000); // Actualizar cada 5 segundos
});
