package com.supermercado.pos.pruebas;

import static com.supermercado.pos.pruebas.PruebasCaja.*;
import com.supermercado.pos.dominio.Usuario;
import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import com.supermercado.pos.servicio.AuthService;
import com.supermercado.pos.servicio.FotoProductoService;
import com.supermercado.pos.util.Conexion;
import com.supermercado.pos.util.FotosProducto;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import javax.imageio.ImageIO;

/** Fotos opcionales: archivos locales propios y Oracle POS_TEST; no modifica precios ni stock. */
public final class PruebasFotos {
    private final PruebasCaja datos=new PruebasCaja();
    private final FotoProductoService servicio=new FotoProductoService();
    private Usuario admin;
    private Path carpeta,fuentes,png,jpg;
    private int ejecutadas,fallos;
    @FunctionalInterface private interface Prueba{void ejecutar()throws Exception;}
    private void caso(String nombre,Prueba prueba){
        ejecutadas++;try{Files.deleteIfExists(FotosProducto.archivo(datos.arroz));prueba.ejecutar();System.out.println("OK    "+nombre);}
        catch(Exception|AssertionError e){fallos++;System.out.println("FALLO "+nombre);e.printStackTrace(System.out);}
    }
    static Path imagen(Path archivo,String formato,Color color)throws Exception{
        BufferedImage imagen=new BufferedImage(1600,1000,formato.equals("jpg")?BufferedImage.TYPE_INT_RGB:BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=imagen.createGraphics();g.setColor(color);g.fillRect(0,0,1600,1000);g.setColor(Color.WHITE);g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF,java.awt.Font.BOLD,75));
        g.drawString("IMAGEN DE PRUEBA",200,500);g.dispose();if(!ImageIO.write(imagen,formato,archivo.toFile())){throw new IllegalStateException("No hay escritor de imagen");}return archivo;
    }
    private void ejecutar(){
        caso("Producto sin foto devuelve vacio sin crear archivos",()->{verificar(servicio.cargar(datos.arroz,admin).isEmpty());verificar(!Files.exists(FotosProducto.archivo(datos.arroz)));});
        caso("PNG se guarda reducido manteniendo proporcion y original",()->{
            byte[] original=Files.readAllBytes(png);BufferedImage foto=servicio.guardar(datos.arroz,png,admin);
            verificar(foto.getWidth()==720&&foto.getHeight()==450);verificar(Arrays.equals(original,Files.readAllBytes(png)));
            verificar(servicio.cargar(datos.arroz,admin).orElseThrow().getWidth()==720);
        });
        caso("JPEG con ruta de espacios se importa como PNG administrado",()->{
            servicio.guardar(datos.arroz,jpg,admin);verificar(Files.exists(FotosProducto.archivo(datos.arroz)));verificar(servicio.cargar(datos.arroz,admin).isPresent());
        });
        caso("Reemplazar cambia solo la foto y no los datos de Oracle",()->{
            servicio.guardar(datos.arroz,png,admin);servicio.guardar(datos.arroz,jpg,admin);
            BufferedImage foto=servicio.cargar(datos.arroz,admin).orElseThrow();Color pixel=new Color(foto.getRGB(10,10),true);verificar(pixel.getBlue()>200&&pixel.getRed()<40);
            igual(numero("SELECT stock FROM producto WHERE id=?",datos.arroz),"30");igual(numero("SELECT precio FROM producto WHERE id=?",datos.arroz),"30");
        });
        caso("Archivo invalido conserva intacta la foto anterior",()->{
            servicio.guardar(datos.arroz,png,admin);byte[] antes=Files.readAllBytes(FotosProducto.archivo(datos.arroz));Path invalido=fuentes.resolve("falso.jpg");Files.writeString(invalido,"No soy una imagen");
            rechaza(ReglaNegocioException.class,()->servicio.guardar(datos.arroz,invalido,admin));verificar(Arrays.equals(antes,Files.readAllBytes(FotosProducto.archivo(datos.arroz))));
        });
        caso("Foto de mas de 10 MB rechazada sin modificar la anterior",()->{
            servicio.guardar(datos.arroz,png,admin);Path grande=fuentes.resolve("grande.png");
            try(var f=new java.io.RandomAccessFile(grande.toFile(),"rw")){f.setLength(10L*1024*1024+1);}
            rechaza(ReglaNegocioException.class,()->servicio.guardar(datos.arroz,grande,admin));verificar(servicio.cargar(datos.arroz,admin).isPresent());
        });
        caso("Cambiar extension no permite formatos distintos de PNG/JPEG",()->{
            Path gif=fuentes.resolve("disfrazado.png");ImageIO.write(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),"gif",gif.toFile());
            rechaza(ReglaNegocioException.class,()->servicio.guardar(datos.arroz,gif,admin));verificar(!Files.exists(FotosProducto.archivo(datos.arroz)));
        });
        caso("Archivo inexistente o vacio rechazado",()->{
            Path vacio=fuentes.resolve("vacio.png");Files.createFile(vacio);
            rechaza(ReglaNegocioException.class,()->servicio.guardar(datos.arroz,vacio,admin));
            rechaza(ReglaNegocioException.class,()->servicio.guardar(datos.arroz,fuentes.resolve("ausente.png"),admin));
        });
        caso("Cajero sin CATALOGO_ADMIN no lee ni cambia ni elimina fotos",()->{
            servicio.guardar(datos.arroz,png,admin);byte[] antes=Files.readAllBytes(FotosProducto.archivo(datos.arroz));
            rechaza(AutorizacionException.class,()->servicio.cargar(datos.arroz,datos.ana));rechaza(AutorizacionException.class,()->servicio.guardar(datos.arroz,jpg,datos.ana));rechaza(AutorizacionException.class,()->servicio.quitar(datos.arroz,datos.ana));
            verificar(Arrays.equals(antes,Files.readAllBytes(FotosProducto.archivo(datos.arroz))));
        });
        caso("Producto inexistente no deja una fotografia huerfana",()->rechaza(ReglaNegocioException.class,()->servicio.guardar(-1,png,admin)));
        caso("Quitar la foto permite continuar sin ella y puede repetirse",()->{
            servicio.guardar(datos.arroz,png,admin);servicio.quitar(datos.arroz,admin);servicio.quitar(datos.arroz,admin);verificar(servicio.cargar(datos.arroz,admin).isEmpty());verificar(Files.exists(png));
        });
        caso("POS y POS_TEST usan carpetas diferentes",()->{
            Path prueba=FotosProducto.archivo(datos.arroz);String anterior=System.getProperty("pos.db.usuario");
            try{System.setProperty("pos.db.usuario","pos");Path pos=FotosProducto.archivo(datos.arroz);verificar(!prueba.equals(pos)&&pos.getParent().getFileName().toString().equals("pos"));}
            finally{if(anterior==null){System.clearProperty("pos.db.usuario");}else{System.setProperty("pos.db.usuario",anterior);}}
            verificar(prueba.getParent().getFileName().toString().equals("pos_test"));
        });
    }
    public static void main(String[] args)throws Exception{
        PruebasNegocio.iniciarConexion();PruebasFotos suite=new PruebasFotos();String anterior=System.getProperty("pos.fotos");
        try{
            suite.datos.preparar();suite.carpeta=Files.createTempDirectory("pos-fotos-");suite.fuentes=Files.createDirectory(suite.carpeta.resolve("fuentes"));System.setProperty("pos.fotos",suite.carpeta.resolve("catalogo").toString());
            suite.png=imagen(suite.fuentes.resolve("foto de producto.png"),"png",Color.RED);suite.jpg=imagen(suite.fuentes.resolve("otra fotografia.jpg"),"jpg",Color.BLUE);
            char[] clave="Admin123!".toCharArray();try{suite.admin=new AuthService().autenticar("admin",clave);}finally{Arrays.fill(clave,'\0');}suite.ejecutar();
        }finally{
            try{suite.datos.limpiar();if(suite.carpeta!=null){try(var archivos=Files.walk(suite.carpeta)){for(Path p:archivos.sorted(Comparator.reverseOrder()).toList()){Files.deleteIfExists(p);}}}}
            finally{if(anterior==null){System.clearProperty("pos.fotos");}else{System.setProperty("pos.fotos",anterior);}Conexion.limpiarClaveTemporal();}
        }
        System.out.printf("%nPruebas fotos: %d; correctas: %d; fallos: %d%n",suite.ejecutadas,suite.ejecutadas-suite.fallos,suite.fallos);if(suite.fallos>0){throw new AssertionError("Fallaron pruebas de fotos");}
    }
}
