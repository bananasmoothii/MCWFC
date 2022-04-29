package fr.bananasmoothii.mcwfc.core.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Debug;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A Set where each element have a weight. The default weight is 1.
 */
@Debug.Renderer(childrenArray = "List a = new ArrayList(this.size()); a.addAll(this); return a.toArray();")
public class WeightedSet<E> implements Set<E> {

    private final Map<E, Integer> map = new HashMap<>();
    private int totalWeight = 0;

    public WeightedSet() {
    }

    @SuppressWarnings("CopyConstructorMissesField")
    public WeightedSet(WeightedSet<E> other) {
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

    public void addAll(@NotNull WeightedSet<E> other) {
        addAll(other, 1);
    }

    /**
     * Adds avery element with a certain weight
     */
    public void addAll(@NotNull WeightedSet<E> other, int weight) {
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
        if (o instanceof WeightedSet<?> other)
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

    public E weightedChoose(@NotNull Random random) {
        if (isEmpty()) throw new IllegalArgumentException("cannot choose anything from an empty WeightedSet");
        final int targetWeight = random.nextInt(totalWeight) + 1;
        int currentWeight = 0;
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            currentWeight += entry.getValue();
            if (currentWeight >= targetWeight) return entry.getKey();
        }
        throw new IllegalStateException("totalWeight is too big and not possible or there are some weights below or equal to 0");
    }

    /**
     * @return the first element given by {@link #iterator()}
     * @throws IllegalArgumentException if this set is empty
     */
    public E getAny() {
        final Iterator<E> iterator = iterator();
        if (!iterator().hasNext()) throw new IllegalStateException("The set is empty");
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
                     * Warning: this uses {@link WeightedSet#add(Object, int)} which doesn't replace the value but adds
                     * it instead.
                     */
                    @Override
                    public Integer setValue(Integer value) {
                        int old = next().getValue();
                        WeightedSet.this.add(next().getKey(), value);
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
    public WeightedSet<E> mapElements(Function<? super E, ? extends E> mappingFunction) {
        final WeightedSet<E> result = new WeightedSet<>();
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

    public WeightedSet<E> copyMultiplyWeights(int weight) {
        WeightedSet<E> copy = new WeightedSet<>();
        copy.addAll(this, weight);
        return copy;
    }

    /**
     * This calculates the {@link #gcd(int, int) GCD} of the weights and divides each weight by that GCD
     */
    public void simplify() {
        Iterator<Integer> iterator = map.values().iterator();
        if (! iterator.hasNext()) return;
        int gcd = iterator.next();
        while (iterator.hasNext()) {
            int weight = iterator.next();
            gcd = gcd(gcd, weight);
        }
        if (gcd == 1) return;
        for (Map.Entry<E, Integer> entry : map.entrySet()) {
            final int oldValue = entry.getValue();
            final int newValue = oldValue / gcd;
            entry.setValue(newValue);
            totalWeight -= oldValue - newValue;
        }
    }

    /**
     * This method has nothing to do really with {@link WeightedSet} but I won't make a new "Util" class just for one
     * method...
     * @return the greatest common divisor.
     */
    public static int gcd(int a, int b) {
        int tempB;
        while (true) {
            // gcd(a, b) = b == 0 ? a : gcd(b, a % b)
            if (b == 0) return a;
            tempB = b;
            b = a % b;
            a = tempB;
        }
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
