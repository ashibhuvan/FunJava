package org.home.core.service;

import org.home.core.EventBus;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MultiThreadedEventBus <T> implements EventBus<T> {

    private final ConcurrentHashMap<Class <? extends T>, List<FilteredEventSubscriber<T>>> subscriberMap = new ConcurrentHashMap<>();
    private final ExecutorService consumerThreadPool;
    private final BlockingQueue<Event> queue;

    private final ConcurrentHashMap<Class<? extends T>, Event<T>> mostRecentEvent = new ConcurrentHashMap<>();

    public MultiThreadedEventBus(int threadPoolSize) {
        this.consumerThreadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.queue = new LinkedBlockingDeque<>();
        start();
    }

    /**
     * @param event
     */
    @Override
    public void publishEvent(T event) {
        if (event == null) throw new IllegalArgumentException("Event Must Be Non Null");
        Event<T> eventObj = new Event<>(event);

        // Add latest event
        // Depending on the requirements we can clean up the allowed types and type checking
        mostRecentEvent.put((Class<? extends T>) event.getClass(), eventObj);

        try {
            queue.put(eventObj);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error Occured Adding Event to Queue: " + e.getMessage());
        }
    }

    /**
     * @param clazz
     * @param subscriber
     */
    @Override
    public void addSubscriber(Class<? extends T> clazz, Consumer<T> subscriber) {
        if (clazz == null || subscriber == null) {
            throw new IllegalArgumentException("Class and Subscriber Must Be Non Null");
        }
        // Using CopyOnWrite Array List as most likely reads much larger than writes. Alternative is to use synchronized writes to ArrayList
        // Can also use CopyOnWriteArraySet as middle ground option. It's dependent on requirements of system and trade off to have
        subscriberMap.computeIfAbsent(clazz, _ -> new CopyOnWriteArrayList<>())
                .add(new FilteredEventSubscriber<>(subscriber, event -> true));
    }

    /**
     * @param clazz
     * @param subscriber
     * @param filter
     */
    @Override
    public void addSubscriberForFilteredEvents(Class<? extends T> clazz, Consumer<T> subscriber, Predicate<T> filter) {
        if (clazz == null || subscriber == null || filter == null) {
            throw new IllegalArgumentException("Inputs Must Be All Non Null");
        }
        subscriberMap.computeIfAbsent(clazz, _ -> new CopyOnWriteArrayList<>())
                .add(new FilteredEventSubscriber<>(subscriber, filter));
    }

    private void start() {
        consumerThreadPool.submit( () -> {

            while (!Thread.currentThread().isInterrupted()) {
                try {

                    // Get Latest Event and Publish to Subscribers
                    Event<T> eventObj = queue.take();
                    T event = eventObj.get();

                    Event<T> latestEventObj = mostRecentEvent.get(event.getClass());
                    final T latestEvent = (latestEventObj != null) ? latestEventObj.get() : event;

                    // For Each Subscriber for Event, Submit to Thread Pool for Consumption
                    List<FilteredEventSubscriber<T>> subscribers = subscriberMap.get(event.getClass());
                    if (subscribers == null || subscribers.isEmpty()) { continue; }

                    subscribers.forEach(sub -> {
                        if (sub.getFilter().test(latestEvent)) {
                            consumerThreadPool.submit(() -> {
                                try {
                                    sub.getConsumer().accept(latestEvent);
                                } catch (Exception e) {
                                    System.out.println("Error notifying subscriber for event" + e.getMessage());
                                }
                            });
                        }
                    });
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    // This can be more customized based on requirements or take in configuration or using setters
    public void shutdown() {
        consumerThreadPool.shutdown();
        try {
            if (!consumerThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                consumerThreadPool.shutdownNow();
            }
        } catch (final InterruptedException e) {
            consumerThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
