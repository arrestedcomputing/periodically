package com.aubray.periodically.util;

public abstract class LoggingCallback<T> implements Callback<T> {
    private static int number = 0;

    private final String message;
    private final int myNumber;

    public LoggingCallback(String message) {
        this.message = message;
        this.myNumber = number++;
    }

    public void log() {
        System.out.println("Callback " + myNumber + " - " + message);
    }
}
