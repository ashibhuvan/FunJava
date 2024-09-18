package org.home.core;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface EventBus<T> {

    void publishEvent(T event);

    void addSubscriber(Class<? extends T> clazz, Consumer<T> subscriber);

    void addSubscriberForFilteredEvents(Class<? extends T> clazz, Consumer<T> subscriber, Predicate<T> filter);

    static class FilteredEventSubscriber<T> {
        private final Consumer<T> consumer;
        private final Predicate<T> filter;

        public FilteredEventSubscriber(Consumer<T> consumer, Predicate<T> filter) {
            this.consumer = consumer;
            this.filter = filter;
        }

        public Consumer<T> getConsumer() { return consumer; }
        public Predicate<T> getFilter() { return filter; }
    }

    static class Event<T> {
        private final T event;

        public Event(T event) { this.event = event; }

        public T get() { return event; }
    }
}
