package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A Set where each element have a weight. The default weight is 1.
 */
public class HashWeightedSet<E> implements Set<E> {

    private final Map<E, Integer> map = new HashMap<>();
    private int totalWeight = 0;

    public HashWeightedSet() {
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public HashWeightedSet(HashWeightedSet<E> other) {
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
    public Object @NotNull [] toArray() {
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
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] array) {
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
    public boolean add(E e) {
        add(e, 1);
        return true;
    }

    public void add(E e, int weight) {
        map.merge(e, weight, Integer::sum);
        totalWeight += weight;
    }

    public boolean addAll(@NotNull Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return true;
    }

    public void addAll(@NotNull HashWeightedSet<E> other) {
        addAll(other, 1);
    }

    /**
     * Adds avery element with a certain weight
     */
    public void addAll(@NotNull HashWeightedSet<E> other, int weight) {
        final Iterator<Map.Entry<E, Integer>> iterator = other.elementsAndWeightsIterator();
        while (iterator.hasNext()) {
            final Map.Entry<E, Integer> next = iterator.next();
            add(next.getKey(), next.getValue() * weight);
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (! contains(o)) {
                remove(o);
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) changed = true;
        }
        return changed;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.keySet().containsAll(c);
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
    public void clear() {
        map.clear();
        totalWeight = 0;
    }

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

    public E weightedChoose() {
        return weightedChoose(ThreadLocalRandom.current());
    }

    @SuppressWarnings("unchecked")
    public E weightedChoose(@NotNull Random random) {
        if (isEmpty()) throw new IllegalArgumentException("cannot choose anything from an empty WeightedSet");
        return (E) toArray()[random.nextInt(totalWeight)];
    }

    /**
     * @return the first element given by {@link #iterator()}
     * @throws IllegalArgumentException if this set is empty
     */
    public E getAny() {
        final Iterator<E> iterator = iterator();
        if (!iterator().hasNext()) throw new IllegalArgumentException("The set is empty");
        return iterator.next();
    }

    /**
     * @return the weight for element e or 0 if it doesn't exist
     */
    public int getWeight(E e) {
        @Nullable Integer weight = map.get(e);
        if (weight == null) return 0;
        return weight;
    }

    public Iterator<Map.Entry<E, Integer>> elementsAndWeightsIterator() {
        return new Iterator<>() {

            private final Iterator<Map.Entry<E, Integer>> iterator = map.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<E, Integer> next() {
                final Map.Entry<E, Integer> next = iterator.next();
                return new Map.Entry<>() {
                    @Override
                    public E getKey() {
                        return next.getKey();
                    }

                    @Override
                    public Integer getValue() {
                        return next.getValue();
                    }

                    /**
                     * Warning: this uses {@link HashWeightedSet#add(Object, int)} which doesn't replace the value but adds
                     * it instead.
                     */
                    @Override
                    public Integer setValue(Integer value) {
                        int old = next().getValue();
                        HashWeightedSet.this.add(next().getKey(), value);
                        return old;
                    }
                };
            }
        };
    }

    public void forEach(BiConsumer<? super E, ? super Integer> action) {
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * This only allows mapping of the element, the weight will not change
     */
    @Contract(pure = true)
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
    public boolean containsNonNormalWeights() {
        for (int weight : map.values()) {
            if (weight <= 0) return true;
        }
        return false;
    }

    public HashWeightedSet<E> copyMultiplyWeights(int weight) {
        HashWeightedSet<E> copy = new HashWeightedSet<>();
        copy.addAll(this, weight);
        return copy;
    }
}
