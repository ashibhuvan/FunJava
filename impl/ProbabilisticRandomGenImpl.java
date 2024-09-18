package org.home.core.service;

import org.home.core.ProbabilisticRandomGen;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ProbabilisticRandomGenImpl implements ProbabilisticRandomGen {
    private final int size;
    private final int[] alias;
    private final double[] probability;
    private final Random random;

    private List<NumAndProbability> weightedNums;

    public ProbabilisticRandomGenImpl(final List<NumAndProbability> weightedNums, final Random random) {

        if (weightedNums == null) { throw new NullPointerException("WeightedNums must be non null"); }
        if (weightedNums.isEmpty()) { throw new IllegalArgumentException("WeightedNums must be non empty"); }

        this.random = random != null ? random : new Random();
        this.size = weightedNums.size();
        this.weightedNums = weightedNums;
        this.alias = new int[size];
        this.probability = new double[size];
        process();
    }

    // Using Alias Method for Weighted Random Sampling
    private void process() {

        Stack<Integer> smaller = new Stack<>();
        Stack<Integer> larger = new Stack<>();

        IntStream.range(0, size).forEach(i -> {
            probability[i] = weightedNums.get(i).getProbabilityOfSample() * size;
            (probability[i] < 1.0 ? smaller : larger).add(i);
        });

        // Distribute the probabilities to have at most 2 in a bucket with the sum close to 1
        while (!smaller.isEmpty() && !larger.isEmpty()) {
            int smallIndex = smaller.pop();
            int largeIndex = larger.pop();

            // Set alias of small prob to large one. Now we have distributed probability from the large index to small
            // and need to reduce the probability of the large index
            alias[smallIndex] = largeIndex;
            probability[largeIndex] = (probability[largeIndex] + probability[smallIndex]) - 1.0;

            // now that probability of large index has been reduced, it has to go back into the queue to be distributed
            (probability[largeIndex] < 1.0 ? smaller : larger).add(largeIndex);
        }

        while (!larger.isEmpty()) {
            probability[larger.pop()] = 1.0;
        }

        while (!smaller.isEmpty()) {
            probability[smaller.pop()] = 1.0;
        }
    }

    @Override
    public int nextFromSample() {
        final int column = random.nextInt(size);
        final int index = random.nextDouble() < probability[column] ? column : alias[column];
        return weightedNums.get(index).getNumber();
    }
}