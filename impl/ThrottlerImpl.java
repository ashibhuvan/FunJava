package org.home.core.service;

import org.home.core.Throttler;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ThrottlerImpl<T> implements Throttler<T> {

    private final int maxRequests;
    private final long windowTimeMillis;
    private final ScheduledExecutorService scheduler;

    private final Queue<Event<T>> requests = new LinkedList<>();
    private final CopyOnWriteArrayList<Consumer<ThrottleResult>> subscribers = new CopyOnWriteArrayList<>();

    public ThrottlerImpl(int maxRequests, long windowTimeMillis, int threads, int schedulerFrequency) {
        // Input Checks Can Be Seen as Excessive Sometimes. Depends on Coding Standards
        if (maxRequests <= 0) {
            throw new IllegalArgumentException("maxRequests must be greater than 0");
        }
        if (windowTimeMillis <= 0) {
            throw new IllegalArgumentException("windowTimeMillis must be greater than 0");
        }
        if (threads <= 0) {
            throw new IllegalArgumentException("threads must be greater than 0");
        }

        this.maxRequests = maxRequests;
        this.windowTimeMillis = windowTimeMillis;
        this.scheduler = Executors.newScheduledThreadPool(threads);

        // This can be configurable or set using setters
        scheduler.scheduleAtFixedRate(this::notifySubscribers, 0, schedulerFrequency, TimeUnit.MILLISECONDS);
    }

    /**
     * @return ThrottleResult
     */
    @Override
    public synchronized ThrottleResult shouldProceed() {
        final long now = System.currentTimeMillis();

        // Remove requests outside current window
        while (!requests.isEmpty() && (now - requests.peek().getTimestamp()) > windowTimeMillis) { requests.poll(); }

        return (requests.size() > maxRequests) ? ThrottleResult.DO_NOT_PROCEED : ThrottleResult.PROCEED;
    }

    /**
     * @param subscriber
     */
    @Override
    public void notifyWhenCanProceed(Consumer<ThrottleResult> subscriber) {
        if (subscriber == null) { throw new IllegalArgumentException("Subscriber Should be Non Null"); }
        subscribers.add(subscriber);
    }

    /**
     * Adds a new request to the throttler.
     *
     * @param event the event to add
     * @throws IllegalArgumentException if the event is null
     */
    public synchronized void addRequest(Event<T> event) {
        if (event == null) { throw new IllegalArgumentException("Event Must Be Non Null"); }
        requests.add(event);
    }

    /**
     * Notifies all subscribers with the current {@link ThrottleResult}.
     * Only sends notifications if the result is {@link ThrottleResult#PROCEED}.
     */
    private synchronized void notifySubscribers() {
        final ThrottleResult result = shouldProceed();
        if (result == ThrottleResult.PROCEED) {
            subscribers.forEach(sub -> {
                try {
                    sub.accept(result);
                } catch (final Exception e) {
                    System.out.println("Exception Occured Notifying Subscriber" + e.getMessage());
                }
            });
        }
    }

    public void quit() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}