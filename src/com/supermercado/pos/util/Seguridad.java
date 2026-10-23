package com.supermercado.pos.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Seguridad {
    private static final int ITERACIONES = 120_000;
    private static final int LONGITUD_BITS = 256;
    private static final int TAMANO_SAL = 16;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Seguridad() {
    }

    /** El llamador debe limpiar su char[] con Arrays.fill al terminar. */
    public static String hash(char[] clave) {
        Objects.requireNonNull(clave, "La clave es obligatoria");
        byte[] sal = new byte[TAMANO_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] resultado = derivar(clave, sal, ITERACIONES);
        try {
            return ITERACIONES + ":" + Base64.getEncoder().encodeToString(sal)
                    + ":" + Base64.getEncoder().encodeToString(resultado);
        } finally {
            Arrays.fill(resultado, (byte) 0);
        }
    }

    public static boolean verificar(char[] clave, String almacenado) {
        if (clave == null || almacenado == null) {
            return false;
        }
        byte[] calculado = null;
        try {
            String[] partes = almacenado.split(":", -1);
            if (partes.length != 3) {
                return false;
            }
            int iteraciones = Integer.parseInt(partes[0]);
            // Un registro corrupto no debe pedir un trabajo ilimitado al procesador.
            if (iteraciones <= 0 || iteraciones > 1_000_000) {
                return false;
            }
            byte[] sal = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            if (sal.length != TAMANO_SAL || esperado.length != LONGITUD_BITS / 8) {
                return false;
            }
            calculado = derivar(clave, sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            if (calculado != null) {
                Arrays.fill(calculado, (byte) 0);
            }
        }
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(clave, sal, iteraciones, LONGITUD_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 no esta disponible", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
