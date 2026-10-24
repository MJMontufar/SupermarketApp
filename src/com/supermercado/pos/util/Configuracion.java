package com.supermercado.pos.util;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuracion no secreta. La clave de Oracle no se lee de archivos. */
public record Configuracion(String url, String usuario, BigDecimal efectivoMinimo) {
    public static Configuracion cargar() {
        Path archivo = Path.of(System.getProperty("pos.config", "config.properties"));
        Properties propiedades = new Properties();
        try {
            leer(archivo, propiedades);
            Path local = archivo.resolveSibling("config.local.properties");
            if (Files.exists(local)) {
                leer(local, propiedades);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la configuracion: " + archivo, e);
        }
        String url = System.getProperty("pos.db.url", propiedades.getProperty("db.url", "")).trim();
        String usuario = System.getProperty("pos.db.usuario", propiedades.getProperty("db.usuario", "")).trim();
        if (!url.startsWith("jdbc:oracle:thin:@") || usuario.isBlank()) {
            throw new IllegalStateException("Revisa db.url y db.usuario en config.properties");
        }
        BigDecimal minimo = Dinero.desdeTexto(propiedades.getProperty("efectivo.minimo", "500.00"));
        if (minimo.signum() < 0) {
            throw new IllegalStateException("El efectivo minimo no puede ser negativo");
        }
        return new Configuracion(url, usuario, minimo);
    }

    private static void leer(Path archivo, Properties propiedades) throws IOException {
        try (Reader lector = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            propiedades.load(lector);
        }
    }
}
