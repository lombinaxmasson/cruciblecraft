package com.masson.cruciblecraft.compat.jade.observation;

/**
 * Typed Jade field. Missing server data is {@link #unavailable()}, never a
 * guessed zero or empty string presented as a reading.
 */
public record ObservationField<T>(boolean available, T value) {
    public static <T> ObservationField<T> of(T value) {
        return new ObservationField<>(true, value);
    }

    public static <T> ObservationField<T> unavailable() {
        return new ObservationField<>(false, null);
    }

    public T orUnavailable(T fallback) {
        return available ? value : fallback;
    }
}
