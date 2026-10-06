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
        const p = path.join(__dirname, '../php/logicaWeb.php');
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
    
    test('API mediciones.php existe', () => {
        const p = path.join(__dirname, '../api/mediciones.php');
        assert.ok(fs.existsSync(p));
    });
});
