//==============================================================================================
//Nombre: logicaWeb.test.js
//Descripcion: Pruebas de la pagina web: comprueba que los ficheros y los colores
//              esperados existen.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

const { test, describe } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

describe('Página Web - Lógica', () => {
    test('Archivo logicaWeb.php existe', () => {
        const p = path.join(__dirname, '../../frontend_business_logic/logicaWeb.php');
        assert.ok(fs.existsSync(p));
    });
    
    test('Archivo index.php existe', () => {
        const p = path.join(__dirname, '../index.php');
        assert.ok(fs.existsSync(p));
    });
    
    // Los colores del diseno viven en css/variables.css, no en style.css:
    // style.css solo consume variables. La comprobacion se hace sobre el
    // fichero de variables, que es el que hay que editar para cambiar el
    // aspecto de la pagina.
    //
    // Se comprueban los colores de fondo y de acento que la pagina usa de
    // verdad. Si se cambia la paleta hay que actualizar estas dos referencias.
    test('Archivo css/variables.css existe con colores correctos', () => {
        const p = path.join(__dirname, '../css/variables.css');
        assert.ok(fs.existsSync(p));
        const content = fs.readFileSync(p);
        assert.ok(content.includes('#0E363D'), 'falta el color de fondo de la pagina');
        assert.ok(content.includes('#7BCDB8'), 'falta el color de acento');
    });

    // index.php tiene que cargar las dos hojas de estilo: variables.css
    // antes que style.css, porque style.css consume esas variables.
    test('index.php carga variables.css antes que style.css', () => {
        const html = fs.readFileSync(path.join(__dirname, '../index.php'), 'utf8');
        const posVars = html.indexOf('css/variables.css');
        const posStyle = html.indexOf('css/style.css');

        assert.ok(posVars !== -1, 'index.php no enlaza css/variables.css');
        assert.ok(posStyle !== -1, 'index.php no enlaza css/style.css');
        assert.ok(posVars < posStyle, 'variables.css debe cargarse antes que style.css');
    });

    // Ninguna regla de style.css debe traer un color suelto: si aparece un
    // hexadecimal fuera de variables.css, el diseno deja de poder cambiarse
    // editando un solo fichero.
    test('style.css no define colores propios, los toma de variables.css', () => {
        const style = fs.readFileSync(path.join(__dirname, '../css/style.css'), 'utf8');
        const props = style
            .split('\n')
            .filter(line => !line.trim().startsWith('*') && !line.trim().startsWith('/*'));
        const sueltos = props
            .filter(line => /#[0-9A-Fa-f]{3,8}\b/.test(line))
            .filter(line => !line.includes('var(--'));
        assert.deepEqual(sueltos, []);
    });

    // Si style.css usa un token, variables.css tiene que definirlo: los dos
    // ficheros tienen que seguir en sincronia.
    test('Toda variable usada en style.css esta definida en variables.css', () => {
        const style = fs.readFileSync(path.join(__dirname, '../css/style.css'), 'utf8');
        const vars = fs.readFileSync(path.join(__dirname, '../css/variables.css'), 'utf8');

        const usadas = [...new Set([...style.matchAll(/var\((--[\w-]+)\)/g)].map(m => m[1]))];
        const definidas = new Set([...vars.matchAll(/^\s*(--[\w-]+):/gm)].map(m => m[1]));

        const sinDefinir = usadas.filter(v => !definidas.has(v));
        assert.deepEqual(sinDefinir, []);
    });
    
    test('Archivo js/app.js existe', () => {
        const p = path.join(__dirname, '../js/app.js');
        assert.ok(fs.existsSync(p));
    });
    
    // La GUI no debe hacer transporte: app.js no puede contener fetch ni XHR;
    // toda comunicacion HTTP vive en el proxy frontend_business_logic/js/mediciones.js.
    test('GUI desacoplada: app.js no contiene fetch', () => {
        const js = fs.readFileSync(path.join(__dirname, '../js/app.js'), 'utf8');
        assert.ok(!/fetch\s*\(/.test(js), 'app.js no debe usar fetch directamente');
        assert.ok(!/XMLHttpRequest/.test(js), 'app.js no debe usar XMLHttpRequest');
    });

    test('API mediciones.php existe', () => {
        const p = path.join(__dirname, '../../frontend_business_logic/api/mediciones.php');
        assert.ok(fs.existsSync(p));
    });

    test('Proxy frontend: mediciones.js existe y encapsula el fetch', () => {
        const p = path.join(__dirname, '../../frontend_business_logic/js/mediciones.js');
        assert.ok(fs.existsSync(p));
        const js = fs.readFileSync(p, 'utf8');
        assert.ok(/fetch\s*\(/.test(js), 'el proxy debe encapsular la comunicacion HTTP');
    });

    // index.php debe cargar el proxy antes que app.js: la GUI no puede
    // ejecutarse si su logica de negocio de cliente no esta aun definida.
    test('index.php carga mediciones.js antes que app.js', () => {
        const html = fs.readFileSync(path.join(__dirname, '../index.php'), 'utf8');
        const scripts = [...html.matchAll(/<script src="([^"]+)"><\/script>/g)].map(m => m[1]);
        const posProxy = scripts.indexOf('/frontend_business_logic/js/mediciones.js');
        const posApp = scripts.indexOf('js/app.js');

        assert.ok(posProxy !== -1, 'index.php no enlaza el proxy mediciones.js');
        assert.ok(posApp !== -1, 'index.php no enlaza js/app.js');
        assert.ok(posProxy < posApp, 'el proxy debe cargarse antes que app.js');
    });
});
