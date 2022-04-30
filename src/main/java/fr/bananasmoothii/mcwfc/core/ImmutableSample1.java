package fr.bananasmoothii.mcwfc.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

public class ImmutableSample1 extends Sample1 {

    public ImmutableSample1(@NotNull Sample1 c) {
        for (PieceNeighbors1 c1 : c) {
            //noinspection UseBulkOperation
            super.add(c1);
        }
    }

    @Override
    public boolean add(@NotNull PieceNeighbors1 pieceNeighborsPossibilities) {
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
    public @NotNull Iterator<PieceNeighbors1> iterator() {
        // not using directly map.values().iterator() as it has a remove() method
        return new Iterator<>() {
            private final Iterator<PieceNeighbors1> iterator = ImmutableSample1.super.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public PieceNeighbors1 next() {
                return iterator.next();
            }
        };
    }

    @Contract("-> new")
    public @NotNull Sample1 mutable() {
        return new Sample1(this);
    }

    @Override
    public @NotNull ImmutableSample1 immutable() {
        return this;
    }
}

