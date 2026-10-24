package com.supermercado.pos.util;

import java.io.Console;
import java.util.Arrays;

public final class GenerarHash {
    private GenerarHash() { }

    public static void main(String[] args) {
        char[] clave;
        if (args.length == 1 && "--demo".equals(args[0])) {
            // Credencial publica de entrenamiento exigida por el PDF.
            clave = "Admin123!".toCharArray();
        } else {
            Console consola = System.console();
            if (consola == null) {
                throw new IllegalStateException("Ejecuta GenerarHash desde una terminal");
            }
            clave = consola.readPassword("Clave para generar el hash (minimo 8 caracteres): ");
        }
        try {
            if (clave == null || clave.length < 8) {
                throw new IllegalArgumentException("La clave requiere al menos 8 caracteres");
            }
            System.out.println(Seguridad.hash(clave));
        } finally {
            if (clave != null) {
                Arrays.fill(clave, '\0');
            }
        }
    }
}
