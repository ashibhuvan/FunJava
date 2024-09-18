package org.home.core.service;

import org.home.core.Throttler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ThrottlerImplTest {

    private static final int maxRequests = 5;
    private static final long windowTime = 1000;
    private static final int threads = 1;
    private static final int schedulerFreq = 100;

    private ThrottlerImpl<String> throttler;
    private Consumer<Throttler.ThrottleResult> subscriber;

    @BeforeEach
    public void setUp() {
        throttler = new ThrottlerImpl<>(maxRequests, windowTime, threads, schedulerFreq);
        subscriber = mock(Consumer.class);
        System.out.println("Initialized Throttler " + (throttler != null));
    }

    @Test
    public void itCanNotifySubscribersBelowLimit() {
        IntStream.range(0, maxRequests).forEach(i -> throttler.addRequest(new Throttler.Event<>("Event" + i)));
        assertEquals(Throttler.ThrottleResult.PROCEED, throttler.shouldProceed());
    }

    @Test
    public void itCanThrottleRequestsOverLimit() {
        IntStream.range(0, maxRequests + 5).forEach(i -> throttler.addRequest(new Throttler.Event<>("Event" + i)));
        assertEquals(Throttler.ThrottleResult.DO_NOT_PROCEED, throttler.shouldProceed());
    }

    @Test
    public void itCanNotifySubscribersToProceed() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Consumer<Throttler.ThrottleResult> notificationSubscriber = _ -> {
            try {
                ArgumentCaptor<Throttler.ThrottleResult> captor = ArgumentCaptor.forClass(Throttler.ThrottleResult.class);
                captor.capture();
                assertEquals(Throttler.ThrottleResult.DO_NOT_PROCEED, captor.getValue());
            } finally {
                latch.countDown();
            }
        };

        throttler.notifyWhenCanProceed(notificationSubscriber);

        IntStream.range(0, maxRequests).forEach(i -> throttler.addRequest(new Throttler.Event<>("Capture Me" + i)));
        throttler.addRequest(new Throttler.Event<>("Should not be captured"));

        // Wait for the notification to be processed
        latch.await(schedulerFreq * 2, TimeUnit.MILLISECONDS);

    }

    //ToDo Test Methods for illegal arguments. More indepth threaded testing
}
