package org.home.core.service;

import org.home.core.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SingleThreadedEventBus<T> implements EventBus<T> {

    private final Map<Class<? extends T>, List<FilteredEventSubscriber<T>>> subscriberMap = new HashMap<>();

    @Override
    public void publishEvent(T event) {
        if (event == null) throw new IllegalArgumentException("Event Must Be Non Null");

        final List<FilteredEventSubscriber<T>> subscribers = subscriberMap.get(event.getClass());
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        // Publish Event to Each Subscriber if Filter Passes
        subscribers.forEach(sub -> {
            if (sub.getFilter().test(event)) {
                try {
                    sub.getConsumer().accept(event);
                } catch (Exception e) {
                    System.out.println("Exception Occurred While Notifying Subscriber" + e.getClass() + e.getMessage()); // ToDo Use logging service instead and clean up print
                }
            }
        });
    }

    @Override
    public void addSubscriber(Class<? extends T> clazz, Consumer<T> subscriber) {
        if (clazz == null || subscriber == null) {
            throw new IllegalArgumentException("Class and Subscriber Must Be Non Null");
        }
        subscriberMap.computeIfAbsent(clazz, _ -> new ArrayList<>()).add(new FilteredEventSubscriber<>(subscriber, event -> true));
    }

    @Override
    public void addSubscriberForFilteredEvents(Class<? extends T> clazz, Consumer<T> subscriber, Predicate<T> filter) {
        if (clazz == null || subscriber == null || filter == null) {
            throw new IllegalArgumentException("Inputs Must Be All Non Null");
        }
        subscriberMap.computeIfAbsent(clazz, _ -> new ArrayList<>())
                .add(new FilteredEventSubscriber<>(subscriber, filter));
    }


}
