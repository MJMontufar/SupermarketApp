package com.supermercado.pos.ui;

import com.supermercado.pos.excepcion.AutorizacionException;
import com.supermercado.pos.excepcion.ReglaNegocioException;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;

final class Mensajes {
    private Mensajes() { }
    static String texto(Throwable error) {
        Throwable causa = error instanceof ExecutionException && error.getCause() != null ? error.getCause() : error;
        if (causa instanceof AutorizacionException || causa instanceof ReglaNegocioException) {
            return causa.getMessage();
        }
        causa.printStackTrace(System.err);
        return "No se pudo completar la operacion. Revisa la conexion e intenta nuevamente.";
    }
    static void error(Component padre, Throwable error) {
        JOptionPane.showMessageDialog(padre, texto(error), "Aviso", JOptionPane.ERROR_MESSAGE);
    }
}
