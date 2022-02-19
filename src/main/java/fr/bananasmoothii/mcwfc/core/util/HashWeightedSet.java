package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A simple implementation of {@link WeightedSet} using a {@link HashMap}.
 */
public class HashWeightedSet<E> implements WeightedSet<E> {

    private final Map<E, Integer> map = new HashMap<>();
    private int totalWeight = 0;

    public HashWeightedSet() {
    }

    public HashWeightedSet(WeightedSet<E> other) {
        addAll(other);
    }


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        Object[] array = new Object[totalWeight];
        int i = 0;
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            final E e = entry.getKey();
            int iMax = i + entry.getValue();
            for (; i < iMax; i++) {
                array[i] = e; // adding it entry.getValue() times
            }
        }
        return array;
    }

    @SuppressWarnings({"unchecked"})
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] array) {
        if (array.length < totalWeight) {
            array = (T[]) Arrays.copyOf(array, totalWeight, array.getClass());
        }
        int i = 0;
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            final E e = entry.getKey();
            int iMax = i + entry.getValue();
            for (; i < iMax; i++) {
                array[i] = (T) e; // adding it entry.getValue() times
            }
        }
        return array;
    }

    @Override
    public void add(E e, int weight) {
        map.merge(e, weight, Integer::sum);
        totalWeight += weight;
    }

    @Override
    public boolean remove(Object o) {
        if (map.remove(o) != null) {
            totalWeight--;
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.keySet().containsAll(c);
    }

    @Override
    public void clear() {
        map.clear();
        totalWeight = 0;
    }

    @Override
    public int getTotalWeight() {
        return totalWeight;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof HashWeightedSet<?> other)
            return map.equals(other.map);
        return false;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * @return the weight for element e or 0 if it doesn't exist
     */
    @Override
    public int getWeight(E e) {
        @Nullable Integer weight = map.get(e);
        if (weight == null) return 0;
        return weight;
    }

    @Override
    public Iterator<Map.Entry<E, Integer>> elementsAndWeightsIterator() {
        return new ElementsAndWeightsIterator<>(this, map.entrySet().iterator());
    }

    @Override
    public void forEach(BiConsumer<? super E, ? super Integer> action) {
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * This only allows mapping of the element, the weight will not change
     */
    @Contract(pure = true)
    @Override
    public HashWeightedSet<E> mapElements(Function<? super E, ? extends E> mappingFunction) {
        final HashWeightedSet<E> result = new HashWeightedSet<>();
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            result.add(mappingFunction.apply(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * @return {@code true} if some weights are 0 or less
     */
    @Override
    public boolean containsNonNormalWeights() {
        for (int weight : map.values()) {
            if (weight <= 0) return true;
        }
        return false;
    }

    @Override
    public HashWeightedSet<E> copyMultiplyWeights(int weight) {
        HashWeightedSet<E> copy = new HashWeightedSet<>();
        copy.addAll(this, weight);
        return copy;
    }
}
