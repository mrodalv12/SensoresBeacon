package com.upv.sensoresbeacon;

/**
 * Diseno logico (Logical_Design_RE_Rules_v3.pdf, seccion 2.1 - Enumerations):
 *
 * MedicionesID = { co2, temperatura, ruido }
 *
 * Los codigos coinciden con el campo "major" de la trama iBeacon.
 */
public enum MedicionesID {

    co2(11),
    temperatura(12),
    ruido(13);

    private final int codigo;

    MedicionesID(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }

    /**
     * codigo: Z --> getNombre() --> Text
     */
    public String getNombre() {
        return name();
    }

    /**
     * codigo: Z --> getTipo() --> Text
     *
     * Devuelve el nombre del tipo de medicion asociado al codigo (major) o
     * TIPO_DESCONOCIDO si el codigo no pertenece a la enumeracion.
     */
    public static String getTipo(int codigo) {
        for (MedicionesID medicion : values()) {
            if (medicion.codigo == codigo) {
                return medicion.name();
            }
        }
        return TIPO_DESCONOCIDO;
    }

    /**
     * codigo: Z --> existe() --> B
     */
    public static boolean existe(int codigo) {
        for (MedicionesID medicion : values()) {
            if (medicion.codigo == codigo) {
                return true;
            }
        }
        return false;
    }

    public static final String TIPO_DESCONOCIDO = "desconocido";
}
