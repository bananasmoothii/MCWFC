package fr.bananasmoothii.mcwfc.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Deprecated
public class Sample implements Set<PieceNeighborsPossibilities> {
    private final Map<Piece, PieceNeighborsPossibilities> map = new HashMap<>();

    public Sample() {
    }

    public Sample(@Nullable Collection<? extends PieceNeighborsPossibilities> from) {
        if (from != null)
            addAll(from);
    }

    @Override
    public int size() {
        return map.size();
    }

    public @Nullable PieceNeighborsPossibilities getNeighborsFor(Piece piece) {
        return map.get(piece);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof PieceNeighborsPossibilities pieceNeighborsPossibilities)
            return map.containsValue(pieceNeighborsPossibilities);
        return false;
    }

    @NotNull
    @Override
    public Iterator<PieceNeighborsPossibilities> iterator() {
        return map.values().iterator();
    }

    @NotNull
    @Override
    public PieceNeighborsPossibilities @NotNull [] toArray() {
        return map.values().toArray(new PieceNeighborsPossibilities[0]);
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

    @Override
    public boolean add(@NotNull PieceNeighborsPossibilities pieceNeighborsPossibilities) {
        return map.merge(pieceNeighborsPossibilities.getCenterPiece(), pieceNeighborsPossibilities,
                (p1, p2) -> {
                    PieceNeighborsPossibilities merged = new PieceNeighborsPossibilities(p1);
                    merged.addAll(p2);
                    return merged;
                })  != pieceNeighborsPossibilities;
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof PieceNeighborsPossibilities pieceNeighborsPossibilities)
            return map.remove(pieceNeighborsPossibilities.getCenterPiece(), pieceNeighborsPossibilities);
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return map.values().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends PieceNeighborsPossibilities> c) {
        boolean addedAll = true;
        for (PieceNeighborsPossibilities pieceNeighborsPossibilities : c) {
            if (!add(pieceNeighborsPossibilities)) addedAll = false;
        }
        return addedAll;
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
        if (o instanceof Sample sample)
            return map.equals(sample.map);
        return false;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    public PieceNeighborsPossibilities peek() {
        final Iterator<PieceNeighborsPossibilities> iterator = iterator();
        if (!iterator().hasNext()) throw new IllegalArgumentException("The set is empty");
        return iterator.next();
    }

    public Set<Piece> getCenterPieces() {
        return new HashSet<>(map.keySet());
    }

    public PieceNeighborsPossibilities chooseRandom() {
        return chooseRandom(ThreadLocalRandom.current());
    }

    public PieceNeighborsPossibilities chooseRandom(Random random) {
        if (isEmpty()) throw new IllegalArgumentException("The set is empty");
        int randomIndex = random.nextInt(map.size());
        final Iterator<PieceNeighborsPossibilities> iter = iterator();
        for (int i = 0; i < randomIndex; i++) {
            iter.next();
        }
        return iter.next();
    }

    /**
     * Simplifies the coefficients for each {@link PieceNeighborsPossibilities}. This method just calls {@link PieceNeighborsPossibilities#simplify()},
     * but be aware because that can be a time-consuming task.
     * @see PieceNeighborsPossibilities#simplify()
     */
    public void simplify() {
        forEach(PieceNeighborsPossibilities::simplify);
    }

    public @NotNull ImmutableSample immutable() {
        return new ImmutableSample(this);
    }
}
