package org.home.core;

import java.util.function.Consumer;

public interface Throttler<T> {

    ThrottleResult shouldProceed();

    void notifyWhenCanProceed(Consumer<ThrottleResult> subscriber);

    enum ThrottleResult {
        PROCEED, DO_NOT_PROCEED
    }

    static class Event<T> {
        private T event;
        private long timestamp;

        public Event(T event) {
            this.event = event;
            timestamp = System.currentTimeMillis();;
        }

        public T get() { return event; }
        public long getTimestamp() { return timestamp; }
    }
}