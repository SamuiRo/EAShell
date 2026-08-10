package com.eashell;

/**
 * Entry point for packaged builds (jpackage, java -jar).
 *
 * Must NOT extend Application - the JVM refuses to launch a jar/app-image whose main class
 * is a javafx.application.Application subclass running off the classpath rather than the
 * module path, failing with "JavaFX runtime components are missing". Indirecting through a
 * plain main() avoids that check entirely. See docs/PACKAGING.md §1.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
