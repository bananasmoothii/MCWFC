package fr.bananasmoothii.mcwfc.core.util;

import fr.bananasmoothii.mcwfc.core.Piece;
import fr.bananasmoothii.mcwfc.core.PieceNeighbors;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PieceNeighborsSet implements Set<PieceNeighbors> {
    private final Map<Piece, PieceNeighbors> map = new HashMap<>();

    public PieceNeighborsSet() {
    }

    public PieceNeighborsSet(Collection<? extends PieceNeighbors> c) {
        addAll(c);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof PieceNeighbors pieceNeighbors)
            return map.containsValue(pieceNeighbors);
        return false;
    }

    @NotNull
    @Override
    public Iterator<PieceNeighbors> iterator() {
        return map.values().iterator();
    }

    @NotNull
    @Override
    public PieceNeighbors @NotNull [] toArray() {
        PieceNeighbors[] array = new PieceNeighbors[map.size()];
        int i = 0;
        for (PieceNeighbors value : map.values()) {
            array[i++] = value;
        }
        return array;
    }

    /**
     * @deprecated use {@link #toArray()}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        return (T[]) toArray();
    }

    /**
     * Always returns {@code true}
     */
    @Override
    public boolean add(PieceNeighbors pieceNeighbors) {
        PieceNeighbors inMap = map.get(pieceNeighbors.getCenterPiece());
        if (inMap == null) {
            map.put(pieceNeighbors.getCenterPiece(), pieceNeighbors);
        } else {
            inMap.addNeighborsOf(pieceNeighbors);
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof PieceNeighbors pieceNeighbors)
            return map.remove(pieceNeighbors.getCenterPiece(), pieceNeighbors);
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.values().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends PieceNeighbors> c) {
        for (PieceNeighbors pieceNeighbors : c) {
            add(pieceNeighbors);
        }
        return true;
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
    public void clear() {
        map.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PieceNeighborsSet pieceNeighbors)
            return map.equals(pieceNeighbors.map);
        return false;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public PieceNeighbors getAny() {
        final Iterator<PieceNeighbors> iterator = iterator();
        if (!iterator().hasNext()) throw new IllegalArgumentException("The set is empty");
        return iterator.next();
    }

    public Set<Piece> getCenterPieces() {
        return new HashSet<>(map.keySet());
    }

    public PieceNeighbors chooseRandom() {
        return chooseRandom(ThreadLocalRandom.current());
    }

    public PieceNeighbors chooseRandom(Random random) {
        if (isEmpty()) throw new IllegalArgumentException("The set is empty");
        int randomIndex = random.nextInt(map.size());
        final Iterator<PieceNeighbors> iter = iterator();
        for (int i = 0; i < randomIndex; i++) {
            iter.next();
        }
        return iter.next();
    }

    /**
     * Simplifies the coefficients for each {@link PieceNeighbors}. This method just calls {@link PieceNeighbors#simplify()},
     * but be aware because that can be a time-consuming task.
     * @see PieceNeighbors#simplify()
     */
    public void simplify() {
        forEach(PieceNeighbors::simplify);
    }

    public @NotNull ImmutablePieceNeighborsSet immutable() {
        return new ImmutablePieceNeighborsSet(this);
    }
}
