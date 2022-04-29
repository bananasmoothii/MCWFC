package fr.bananasmoothii.mcwfc.core.util;

import fr.bananasmoothii.mcwfc.core.PieceNeighbors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class ImmutablePieceNeighborsSet extends PieceNeighborsSet {

    public ImmutablePieceNeighborsSet(PieceNeighborsSet c) {
        for (PieceNeighbors c1 : c) {
            //noinspection UseBulkOperation
            super.add(c1);
        }
    }

    @Override
    public boolean add(PieceNeighbors pieceNeighbors) {
        throw new UnsupportedOperationException("tried to modify an ImmutablePieceNeighborsSet");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("tried to modify an ImmutablePieceNeighborsSet");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("tried to modify an ImmutablePieceNeighborsSet");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("tried to modify an ImmutablePieceNeighborsSet");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("tried to modify an ImmutablePieceNeighborsSet");
    }

    @Override
    public @NotNull Iterator<PieceNeighbors> iterator() {
        // not using directly map.values().iterator() as it has a remove() method
        return new Iterator<>() {
            private final Iterator<PieceNeighbors> iterator = fr.bananasmoothii.mcwfc.core.util.ImmutablePieceNeighborsSet.super.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public PieceNeighbors next() {
                return iterator.next();
            }
        };
    }

    @Contract("-> new")
    public @NotNull PieceNeighborsSet mutable() {
        return new PieceNeighborsSet(this);
    }

    @Override
    public @NotNull ImmutablePieceNeighborsSet immutable() {
        return this;
    }
}
