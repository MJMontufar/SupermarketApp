package com.supermercado.pos.ui;

final class PermisosUI {
    private PermisosUI(){ }
    static String modulo(String codigo){
        if(codigo.startsWith("VENTA")){return "Venta";}
        if(codigo.startsWith("TURNO")||codigo.startsWith("CORTE")){return "Turno / caja";}
        if(codigo.startsWith("REVERSION")){return "Reversion";}
        if(codigo.startsWith("CATALOGO")){return "Catalogos";}
        return "Administracion";
    }
}
