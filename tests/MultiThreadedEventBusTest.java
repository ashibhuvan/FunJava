package org.home.core.service;

import org.home.core.EventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class MultiThreadedEventBusTest {

    private MultiThreadedEventBus<String> eventBus;
    private ExecutorService executorService;
    private BlockingQueue<EventBus.Event<String>> queue;

    @Before
    public void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        eventBus = new MultiThreadedEventBus<>(2);
    }

    @After
    public void quit() {
        executorService.shutdownNow();
    }

    @Test
    public void itCanAddSubscriber() throws ExecutionException, InterruptedException {
        // Arrange
        String testEvent = "itCanAddSubscriber";
        CompletableFuture<String> future = new CompletableFuture<>();

        // Act
        eventBus.addSubscriber(String.class, future::complete);
        eventBus.publishEvent(testEvent);

        // Assert
        String resp = future.get();
        assertEquals(testEvent, resp);
    }

    @Test
    public void itCanNotifySubscribersMatchingFilter() {

    }

}
