package de.martinmatysiak.mapracer.data;

public final class GameState {
    private GameState() { /* non-instantiable */ }

    public static final String INIT = "init";
    public static final String LOAD = "load";
    public static final String RACE = "race";
    public static final String SCORES = "scores";
}
