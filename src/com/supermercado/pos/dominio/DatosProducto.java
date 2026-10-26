package com.supermercado.pos.dominio;

import java.math.BigDecimal;

/** Valores capturados en el formulario; el servicio aplica sus validaciones. */
public record DatosProducto(String codigo,String nombre,BigDecimal precio,BigDecimal impuesto,int stock,boolean activo) { }
