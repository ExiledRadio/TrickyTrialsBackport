package com.exiledradio.trickytrialsbackport.trial;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 1.21's {@code WeightedList}: values with integer weights, one drawn in proportion to its weight. */
public final class Weighted<T> {

    private final List<T> values = new ArrayList<T>();
    private final List<Integer> weights = new ArrayList<Integer>();
    private int total;

    public Weighted<T> add(T value, int weight) {
        if (weight > 0) {
            this.values.add(value);
            this.weights.add(weight);
            this.total += weight;
        }
        return this;
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public List<T> values() {
        return Collections.unmodifiableList(this.values);
    }

    public int weight(int index) {
        return this.weights.get(index);
    }

    @Nullable
    public T pick(Random random) {
        if (this.values.isEmpty()) {
            return null;
        }
        if (this.values.size() == 1) {
            return this.values.get(0);
        }
        int n = random.nextInt(this.total);
        for (int i = 0; i < this.values.size(); i++) {
            n -= this.weights.get(i);
            if (n < 0) {
                return this.values.get(i);
            }
        }
        return this.values.get(this.values.size() - 1);
    }
}
