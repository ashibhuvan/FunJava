package org.home.core.service;


import org.home.core.ProbabilisticRandomGen;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProbabilisticRandomGenImplTest {

    @Test
    public void itCanThrowExceptionForInvalidInput() {
       Exception exceptionNullInput = assertThrows(NullPointerException.class, () -> {
          new ProbabilisticRandomGenImpl(null, null);
       });
       Exception exceptionEmptyList = assertThrows(IllegalArgumentException.class, () -> {
            new ProbabilisticRandomGenImpl(List.of(), null);
       });

       assertEquals(exceptionNullInput.getMessage(), "WeightedNums must be non null");
       assertEquals(exceptionEmptyList.getMessage(), "WeightedNums must be non empty");
    }

    @Test
    public void itCanCreateGenWithProbabilities() {
       ProbabilisticRandomGenImpl gen = new ProbabilisticRandomGenImpl(getMockWeightedNums(), null);
       ProbabilisticRandomGenImpl genWithRandom = new ProbabilisticRandomGenImpl(getMockWeightedNums(), new Random());

       assertNotNull(gen);
       assertNotNull(genWithRandom);
    }

    @Test
    public void itCanCallNextSample() {
        ThreadLocalRandom mockRandom = mock(ThreadLocalRandom.class);
        when(mockRandom.nextInt(anyInt())).thenReturn(2);
        when(mockRandom.nextDouble()).thenReturn(0.3);

        ProbabilisticRandomGenImpl gen = new ProbabilisticRandomGenImpl(getMockWeightedNums(), mockRandom);

        int result = gen.nextFromSample();
        assertEquals(result, 3);
    }


    private static List<ProbabilisticRandomGen.NumAndProbability> getMockWeightedNums() {
        return List.of(
                new ProbabilisticRandomGen.NumAndProbability(1, 0.1f),
                new ProbabilisticRandomGen.NumAndProbability(2, 0.3f),
                new ProbabilisticRandomGen.NumAndProbability(3, 0.2f),
                new ProbabilisticRandomGen.NumAndProbability(4, 0.2f),
                new ProbabilisticRandomGen.NumAndProbability(5, 0.15f),
                new ProbabilisticRandomGen.NumAndProbability(6, 0.05f)
        );
    }


}
