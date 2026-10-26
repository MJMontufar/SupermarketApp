package com.supermercado.pos.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Fotografias locales administradas por ID; nunca almacena la ruta del original. */
public final class FotosProducto {
    private static final long MAX_BYTES=10L*1024*1024;
    private FotosProducto(){ }
    public static Path archivo(long producto){
        if(producto<=0){throw new IllegalArgumentException("Producto invalido");}
        Path config=Path.of(System.getProperty("pos.config","config.properties")).toAbsolutePath();
        Path raiz=System.getProperty("pos.fotos")==null?config.getParent().resolve("datos/fotos"):Path.of(System.getProperty("pos.fotos"));
        String usuario=Configuracion.cargar().usuario().toLowerCase(Locale.ROOT);
        if(!usuario.matches("[a-z][a-z0-9_]{0,29}")){throw new IllegalArgumentException("Cuenta de fotos invalida");}
        return raiz.toAbsolutePath().normalize().resolve(usuario).resolve(producto+".png");
    }
    public static BufferedImage preparar(Path fuente)throws IOException{
        if(fuente==null||!Files.isRegularFile(fuente)||Files.size(fuente)==0||Files.size(fuente)>MAX_BYTES){throw new IOException("Elige un archivo JPG o PNG de hasta 10 MB");}
        try(ImageInputStream entrada=ImageIO.createImageInputStream(fuente.toFile())){
            if(entrada==null){throw new IOException("No se pudo leer la fotografia");}
            Iterator<ImageReader> lectores=ImageIO.getImageReaders(entrada);
            if(!lectores.hasNext()){throw new IOException("El archivo no es una imagen JPG o PNG valida");}
            ImageReader lector=lectores.next();
            try{
                String formato=lector.getFormatName();
                if(!formato.equalsIgnoreCase("PNG")&&!formato.equalsIgnoreCase("JPEG")){throw new IOException("Usa una fotografia JPG o PNG");}
                lector.setInput(entrada,true,true);int w=lector.getWidth(0),h=lector.getHeight(0);
                if(w<=0||h<=0||w>12000||h>12000||(long)w*h>64000000){throw new IOException("La imagen tiene dimensiones demasiado grandes");}
                var parametros=lector.getDefaultReadParam();int salto=Math.max(1,(Math.max(w,h)+1023)/1024);parametros.setSourceSubsampling(salto,salto,0,0);
                BufferedImage leida=lector.read(0,parametros);if(leida==null){throw new IOException("No se pudo decodificar la imagen");}
                double escala=Math.min(1.0,720.0/Math.max(leida.getWidth(),leida.getHeight()));
                int ancho=Math.max(1,(int)Math.round(leida.getWidth()*escala)),alto=Math.max(1,(int)Math.round(leida.getHeight()*escala));
                BufferedImage salida=new BufferedImage(ancho,alto,BufferedImage.TYPE_INT_ARGB);Graphics2D g=salida.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(leida,0,0,ancho,alto,null);g.dispose();return salida;
            }finally{lector.dispose();}
        }
    }
    public static void guardar(long producto,BufferedImage imagen)throws IOException{
        Path destino=archivo(producto);Files.createDirectories(destino.getParent());Path temporal=Files.createTempFile(destino.getParent(),"foto-",".tmp");
        try{
            if(!ImageIO.write(imagen,"png",temporal.toFile())){throw new IOException("No se pudo guardar la fotografia");}
            try{Files.move(temporal,destino,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporal,destino,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporal);}
    }
    public static Optional<BufferedImage> cargar(long producto)throws IOException{
        Path p=archivo(producto);return Files.exists(p)?Optional.of(preparar(p)):Optional.empty();
    }
    public static boolean quitar(long producto)throws IOException{return Files.deleteIfExists(archivo(producto));}
}
