package com.alkywallet.entity;

/**
 * Categoría de gasto/ingreso asociada a una Transaccion, usada para
 * agrupar y reportar movimientos (ver TransaccionRepository /
 * GastoPorCategoriaDTO).
 */
public enum CategoriaTransaccion {
    COMIDA,
    TRANSPORTE,
    SERVICIOS,
    ENTRETENIMIENTO,
    SALUD,
    EDUCACION,
    INVERSION,
    TRANSFERENCIA,
    OTROS
}
