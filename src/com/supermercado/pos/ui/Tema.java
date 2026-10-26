package com.supermercado.pos.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;

/** Estilo Swing centralizado, realizado solo con el JDK. */
public final class Tema {
    public static final Color FONDO=new Color(0xF1F4F8),TEXTO=new Color(0x172033),MUTED=new Color(0x64748B);
    public static final Color VERDE=new Color(0x047857),VIOLETA=new Color(0x7C3AED),OSCURO=new Color(0x142033);
    private Tema(){ }
    public static void instalar() throws javax.swing.UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel());
        var fuente=new FontUIResource(Font.SANS_SERIF,Font.PLAIN,13);
        for(String clave:new String[]{"defaultFont","Label.font","Button.font","TextField.font","PasswordField.font","ComboBox.font","Table.font","TextArea.font","CheckBox.font","Spinner.font","OptionPane.messageFont"}){UIManager.put(clave,fuente);}
        color("control",FONDO);color("nimbusBase",new Color(0x334155));color("nimbusBlueGrey",new Color(0xDFE6EE));
        color("nimbusLightBackground",Color.WHITE);color("text",TEXTO);color("textForeground",TEXTO);color("nimbusFocus",VERDE);
        color("nimbusSelectionBackground",VERDE);color("nimbusSelectedText",Color.WHITE);color("textHighlight",VERDE);color("textHighlightText",Color.WHITE);
        color("Table.background",Color.WHITE);color("Table.foreground",TEXTO);color("Table.alternateRowColor",new Color(0xF7F9FC));
        color("Table.selectionBackground",VERDE);color("Table.selectionForeground",Color.WHITE);
        UIManager.put("Table.rowHeight",28);UIManager.put("TableHeader.font",new FontUIResource(Font.SANS_SERIF,Font.BOLD,13));
        UIManager.put("ButtonUI",BotonUI.class.getName());
        UIManager.put("Button.font",new FontUIResource(Font.SANS_SERIF,Font.BOLD,13));
        UIManager.put("ScrollBar.width",12);
    }
    private static void color(String clave,Color valor){UIManager.put(clave,new ColorUIResource(valor));}
    public static String escapar(String texto){return texto.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    public static void acomodar(java.awt.Window ventana){
        java.awt.Rectangle espacio=java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        ventana.setSize(Math.min(ventana.getWidth(),Math.max(320,espacio.width-24)),Math.min(ventana.getHeight(),Math.max(300,espacio.height-24)));
        ventana.setLocationRelativeTo(ventana.getOwner());
    }
    public static void primario(JButton boton){boton.putClientProperty("pos.variante","primario");}
    public static void peligro(JButton boton){boton.putClientProperty("pos.variante","peligro");}
    public static JLabel titulo(String texto){JLabel l=new JLabel(texto);l.setFont(new Font(Font.SANS_SERIF,Font.BOLD,25));l.setForeground(TEXTO);return l;}
    public static JLabel secundario(String texto){JLabel l=new JLabel(texto);l.setForeground(MUTED);return l;}
    public static JPanel cabecera(String titulo,String detalle){
        JPanel p=new JPanel(new java.awt.GridLayout(0,1,0,6));p.setOpaque(false);p.add(titulo(titulo));p.add(secundario(detalle));return p;
    }
    public static JPanel oscuro(){
        return new JPanel(){
            private static final long serialVersionUID=1L;
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g);Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setPaint(new GradientPaint(0,0,OSCURO,getWidth(),getHeight(),new Color(0x28334D)));d.fillRect(0,0,getWidth(),getHeight());
                d.setColor(new Color(255,255,255,10));d.fillOval(getWidth()-160,-80,300,300);d.fillOval(-180,getHeight()-190,330,330);
                d.setColor(new Color(16,185,129,60));d.fillOval(getWidth()-100,getHeight()-130,180,180);d.dispose();
            }
        };
    }
    public static void tarjeta(JButton boton,String letra){
        boton.setIcon(new Icon(){
            @Override public int getIconWidth(){return 40;}@Override public int getIconHeight(){return 40;}
            @Override public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(boton.isEnabled()?new Color(0xEDE9FE):new Color(0xE2E8F0));d.fillRoundRect(x,y,40,40,12,12);
                d.setColor(boton.isEnabled()?VIOLETA:new Color(0x94A3B8));d.setFont(new Font(Font.SANS_SERIF,Font.BOLD,15));
                int ancho=d.getFontMetrics().stringWidth(letra);d.drawString(letra,x+(40-ancho)/2,y+26);d.dispose();
            }
        });boton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);boton.setIconTextGap(12);boton.setMargin(new java.awt.Insets(18,16,18,16));
    }
    public static Icon sinFoto(){
        return new Icon(){
            @Override public int getIconWidth(){return 220;}@Override public int getIconHeight(){return 200;}
            @Override public void paintIcon(Component c,Graphics g,int x,int y){
                Graphics2D d=(Graphics2D)g.create();double escala=Math.min(1.0,Math.min(c.getWidth()/220.0,c.getHeight()/200.0));d.translate((c.getWidth()-220*escala)/2,(c.getHeight()-200*escala)/2);d.scale(escala,escala);d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                d.setColor(new Color(0xF0F4F8));d.fillRoundRect(0,0,220,200,18,18);d.setStroke(new BasicStroke(3));d.setColor(new Color(0xA5B4C6));
                d.drawRoundRect(63,48,94,82,12,12);d.drawOval(122,63,12,12);d.drawPolyline(new int[]{72,96,115,131,148},new int[]{116,88,108,96,116},5);
                d.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));d.drawString("Sin fotografia",69,158);d.dispose();
            }
        };
    }
    public static ImageIcon miniatura(BufferedImage imagen,int ancho,int alto){
        return new ImageIcon(imagen){
            private static final long serialVersionUID=1L;
            @Override public int getIconWidth(){return ancho;}
            @Override public int getIconHeight(){return alto;}
            @Override public synchronized void paintIcon(Component c,Graphics g,int x,int y){
                double escala=Math.min((double)Math.min(ancho,c.getWidth())/imagen.getWidth(),(double)Math.min(alto,c.getHeight())/imagen.getHeight());
                int w=Math.max(1,(int)Math.round(imagen.getWidth()*escala)),h=Math.max(1,(int)Math.round(imagen.getHeight()*escala));
                Graphics2D d=(Graphics2D)g.create();d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                d.drawImage(imagen,(c.getWidth()-w)/2,(c.getHeight()-h)/2,w,h,null);d.dispose();
            }
        };
    }
}
