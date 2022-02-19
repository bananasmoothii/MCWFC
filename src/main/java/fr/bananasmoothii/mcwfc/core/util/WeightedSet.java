package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A Set where each element have a weight. The default weight is 1.
 */
public interface WeightedSet<E> extends Set<E> {

    /**
     * This should always return {@code true}
     */
    @Override
    default boolean add(E e) {
        add(e, 1);
        return true;
    }

    /**
     * Adds an element with a specific weight
     */
    void add(E e, int weight);

    @Override
    default boolean addAll(@NotNull Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return true;
    }

    default void addAll(@NotNull WeightedSet<E> other) {
        addAll(other, 1);
    }

    /**
     * Adds avery element with a certain weight
     */
    default void addAll(@NotNull WeightedSet<E> other, int weight) {
        final Iterator<Map.Entry<E, Integer>> iterator = other.elementsAndWeightsIterator();
        while (iterator.hasNext()) {
            final Map.Entry<E, Integer> next = iterator.next();
            add(next.getKey(), next.getValue() * weight);
        }
    }

    @Override
    default boolean retainAll(@NotNull Collection<?> c) {
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
    default boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) changed = true;
        }
        return changed;
    }

    @Override
    default boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (! contains(o)) return false;
        }
        return true;
    }

    int getTotalWeight();

    default E weightedChoose() {
        return weightedChoose(ThreadLocalRandom.current());
    }

    @SuppressWarnings("unchecked")
    default E weightedChoose(@NotNull Random random) {
        if (isEmpty()) throw new IllegalArgumentException("cannot choose anything from an empty WeightedSet");
        return (E) toArray()[random.nextInt(getTotalWeight())];
    }

    /**
     * @return the first element given by {@link #iterator()}
     * @throws IllegalArgumentException if this set is empty
     */
    default E getAny() {
        final Iterator<E> iterator = iterator();
        if (!iterator().hasNext()) throw new IllegalArgumentException("The set is empty");
        return iterator.next();
    }

    /**
     * @return the weight for element e or 0 if it doesn't exist
     */
    int getWeight(E e);

    Iterator<Map.Entry<E, Integer>> elementsAndWeightsIterator();

    void forEach(BiConsumer<? super E, ? super Integer> action);

    /**
     * This only allows mapping of the element, the weight will not change
     */
    @Contract(pure = true)
    WeightedSet<E> mapElements(Function<? super E, ? extends E> mappingFunction);

    /**
     * @return {@code true} if some weights are 0 or less
     */
    boolean containsNonNormalWeights();

    /**
     * Returns an array with every element duplicated the amount of times specified by their weight.
     */
    @SuppressWarnings("NullableProblems")
    @NotNull
    @Override
    Object[] toArray();

    /**
     * Returns an array with every element duplicated the amount of times specified by their weight.
     */
    @SuppressWarnings("NullableProblems")
    @NotNull
    @Override
    <T> T[] toArray(@NotNull T[] a);

    /**
     * @return a copy (but the elements themselves are not changed !) with all weights mulitiplied by weight.
     */
    WeightedSet<E> copyMultiplyWeights(int weight);

    /**
     * For simplifying subclasses' code
     */
    @SuppressWarnings("ClassCanBeRecord")
    class ElementsAndWeightsIterator<E> implements Iterator<Map.Entry<E, Integer>> {

        private final WeightedSet<E> thisRef;
        private final Iterator<Map.Entry<E, Integer>> iterator;

        ElementsAndWeightsIterator(WeightedSet<E> thisRef, Iterator<Map.Entry<E, Integer>> iterator) {
            this.thisRef = thisRef;
            this.iterator = iterator;
        }

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
                    thisRef.add(next().getKey(), value);
                    return old;
                }
            };
        }
    }
}
