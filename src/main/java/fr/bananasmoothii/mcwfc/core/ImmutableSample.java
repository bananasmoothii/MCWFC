package fr.bananasmoothii.mcwfc.core;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

/**
 * @param <B> the type of the blocks in the {@link Piece}s. For example, in bukkit, this is {@link org.bukkit.block.data.BlockData}
 */
public class ImmutableSample<B> extends Sample<B> {

    public ImmutableSample(@NotNull Sample<B> c) {
        for (PieceNeighbors.Locked<B> c1 : c) {
            super.add(c1);
        }
    }

    @Override
    public boolean add(@NotNull PieceNeighbors.Locked<B> pieceNeighborsPossibilities) {
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
    public @NotNull Iterator<PieceNeighbors.Locked<B>> iterator() {
        // not using directly map.values().iterator() as it has a remove() method
        return new Iterator<>() {
            private final Iterator<PieceNeighbors.Locked<B>> iterator = ImmutableSample.super.iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public PieceNeighbors.Locked<B> next() {
                return iterator.next();
            }
        };
    }

    @Contract("-> new")
    public @NotNull Sample<B> mutable() {
        return new Sample<>(this);
    }

    @Override
    public @NotNull ImmutableSample<B> immutable() {
        return this;
    }
}

