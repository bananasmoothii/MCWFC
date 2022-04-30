package fr.bananasmoothii.mcwfc.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

@Deprecated
public class ImmutableSample extends Sample {

    public ImmutableSample(@NotNull Sample c) {
        for (PieceNeighborsPossibilities c1 : c) {
            //noinspection UseBulkOperation
            super.add(c1);
        }
    }

    @Override
    public boolean add(@NotNull PieceNeighborsPossibilities pieceNeighborsPossibilities) {
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
    public @NotNull Iterator<PieceNeighborsPossibilities> iterator() {
        // not using directly map.values().iterator() as it has a remove() method
        return new Iterator<>() {
            private final Iterator<PieceNeighborsPossibilities> iterator = ImmutableSample.super.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public PieceNeighborsPossibilities next() {
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
