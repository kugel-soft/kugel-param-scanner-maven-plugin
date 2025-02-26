package com.github.kugelsoft.paramscanner.exceptions;

public class LimiteMaximoProgramasException extends Exception {

    public static final int LIMITE_PROGR_POR_PARAM = 100;

    public LimiteMaximoProgramasException(String paramClassName) {
        super("Parâmetro " + paramClassName + " é utilizado por mais de " + LIMITE_PROGR_POR_PARAM + " programas, ignorando próximos programas");
    }
}
