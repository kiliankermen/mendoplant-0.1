package com.mendoplant.app.model;

/**
 * Letra del comprobante fiscal emitido. Letra C: monotributo / consumidor final
 * (no discrimina IVA). Letra A: responsable inscripto con receptor inscripto de CIUF.
 */
public enum LetraFactura {
    A,
    C
}