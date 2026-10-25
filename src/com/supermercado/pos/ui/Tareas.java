package com.supermercado.pos.ui;

import java.awt.Window;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** JDBC y PBKDF2 nunca se ejecutan sobre el hilo de eventos de Swing. */
final class Tareas {
    private Tareas() { }
    static <T> void ejecutar(Window ventana, Callable<T> trabajo, Consumer<T> exito, Runnable terminar) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return trabajo.call(); }
            @Override protected void done() {
                try { T resultado = get(); if (ventana.isDisplayable()) { exito.accept(resultado); } }
                catch (Exception e) { if (ventana.isDisplayable()) { Mensajes.error(ventana, e); } }
                finally { terminar.run(); }
            }
        }.execute();
    }
}
