package com.supermercado.pos.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/** Boton con estados visibles y foco de teclado, sin dependencias externas. */
public final class BotonUI extends BasicButtonUI {
    public static ComponentUI createUI(JComponent c){return new BotonUI();}
    @Override protected void installDefaults(AbstractButton b){
        super.installDefaults(b);b.setOpaque(false);b.setContentAreaFilled(false);b.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));b.setRolloverEnabled(true);
    }
    private boolean primario(AbstractButton b){return "primario".equals(b.getClientProperty("pos.variante"));}
    private boolean peligro(AbstractButton b){return "peligro".equals(b.getClientProperty("pos.variante"));}
    @Override public void paint(Graphics g,JComponent c){
        AbstractButton b=(AbstractButton)c;Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Color fondo=primario(b)?Tema.VERDE:Color.WHITE;
        if(peligro(b)){fondo=new Color(0xFFF1F2);}if(b.getModel().isRollover()){fondo=primario(b)?new Color(0x047857):new Color(0xE9EEF5);}
        if(b.getModel().isPressed()){fondo=primario(b)?new Color(0x065F46):new Color(0xDAE3EE);}if(!b.isEnabled()){fondo=new Color(0xE9EDF2);}
        d.setColor(fondo);d.fillRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,12,12);
        d.setColor(b.isFocusOwner()?Tema.VIOLETA:new Color(0xD3DCE7));d.drawRoundRect(0,0,c.getWidth()-1,c.getHeight()-1,12,12);
        if(b.isFocusOwner()){d.drawRoundRect(2,2,c.getWidth()-5,c.getHeight()-5,10,10);}d.dispose();super.paint(g,c);
    }
    @Override protected void paintText(Graphics g,AbstractButton b,Rectangle rect,String texto){
        g.setColor(!b.isEnabled()?new Color(0x8794A7):primario(b)?Color.WHITE:peligro(b)?new Color(0xBE123C):Tema.TEXTO);
        BasicGraphicsUtils.drawStringUnderlineCharAt(g,texto,b.getDisplayedMnemonicIndex(),rect.x,rect.y+g.getFontMetrics().getAscent());
    }
}
