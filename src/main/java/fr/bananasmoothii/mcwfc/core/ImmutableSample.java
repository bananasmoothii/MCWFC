package fr.bananasmoothii.mcwfc.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class ImmutableSample extends Sample {

    public ImmutableSample(@NotNull Sample c) {
        for (PieceNeighbors c1 : c) {
            //noinspection UseBulkOperation
            super.add(c1);
        }
    }

    @Override
    public boolean add(@NotNull PieceNeighbors pieceNeighborsPossibilities) {
        throw new UnsupportedOperationException("tried to modify an ImmutableSample");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("tried to modify an ImmutableSample");
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("tried to modify an ImmutableSample");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("tried to modify an ImmutableSample");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("tried to modify an ImmutableSample");
    }

    @Override
    public @NotNull Iterator<PieceNeighbors> iterator() {
        // not using directly map.values().iterator() as it has a remove() method
        return new Iterator<>() {
            private final Iterator<PieceNeighbors> iterator = ImmutableSample.super.iterator();

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
    public @NotNull Sample mutable() {
        return new Sample(this);
    }

    @Override
    public @NotNull ImmutableSample immutable() {
        return this;
    }
}

