package fr.bananasmoothii.mcwfc.core;

/*
 * Represents a central {@link Piece} along with its neighbors for each {@link Face}. There can be more than one
 * {@link Piece} for one {@link Face}, and each neighbor has a coefficient (default is 1). If you want to add a piece
 * (with {@link #addNeighbor(Face, Piece)}), but it already exists for the face you're putting it, the coefficient of
 * that existing piece gets instead incremented by one.
 */
public class PieceNeighborsPossibilities0 {}
/* implements Set<PieceNeighbors> {
    private final @NotNull Piece centerPiece;
    private final @NotNull WeightedSet<PieceNeighbors> neighbors = new WeightedSet<>();

    public PieceNeighborsPossibilities(@NotNull Piece centerPiece) {
        this.centerPiece = Objects.requireNonNull(centerPiece);
    }

    /**
     * @see #addNeighborPossibility(Face, Piece, int) (Face, Piece, int)
     *
    @Override
    public boolean add(@NotNull PieceNeighbors possibility) {
        add(possibility, 1);
        return true;
    }

    /**
     * Adds a given piece as neighbor at a specific face. If there is already the same piece at the same face, it will
     * just increment "times" by one (see below). If there is already a piece at that face, it won't be replaced (that's
     * the hole point of this class), both pieces will remain present.
     * @param times an optional weight, useful for generation. Defaults to 1 in {@link #addNeighborPossibility(PieceNeighbors)}. Must
     *              be greater or equal to 1.
     * @see #addNeighborPossibility(PieceNeighbors)
     *
    public void add(@NotNull PieceNeighbors possibility, int times) {
        Objects.requireNonNull(possibility);
        if (times < 1) throw new IllegalArgumentException("Cannot add 0 or less times a piece");

        neighbors.add(possibility, times);
    }

    public void addNeighborsOf(@NotNull PieceNeighborsPossibilities other) {
        for (PieceNeighbors otherNeighbor : other.neighbors) {
            addAllNeighbors(otherNeighbor);
        }
    }

    public void addAllNeighbors(@NotNull Collection<PieceNeighbors> pieces) {
        addAllNeighbors(face, pieces, 1);computeIfAbsent(Objects.requireNonNull(face), face1 -> new WeightedSet<>())

    }

    public void addAllNeighbors(Collection<PieceNeighbors> pieces, int weight) {
        if (pieces.containsNonNormalWeights()) throw new IllegalArgumentException("Cannot add 0 or less times a piece");
        neighbors.addAll(pieces, weight);
    }

    public @NotNull Piece getCenterPiece() {
        return centerPiece;
    }

    public void fill(@NotNull BlockData blockData) {
        centerPiece.fill(blockData);
        for (WeightedSet<Piece> pieceWeight : neighbors.values()) {
            for (Piece piece : pieceWeight) {
                piece.fill(blockData);
            }
        }
    }

    /**
     * @return The map used internally itself, that you may modify. Be careful with it. Integer is number of times that
     * piece was recorded for that Face.
     *
    public @NotNull Map<Face, WeightedSet<Piece>> getNeighbors() {
        return neighbors;
    }

    /**
     * @return a map with the pieces and the corresponding weights. It is the same as used internally, so be careful with
     * it.
     *
    public @Nullable WeightedSet<Piece> getNeighbors(@NotNull Face face) {
        return neighbors.get(face);
    }

    /**
     * This does not copy the pieces themselves !
     *
    public PieceNeighborsPossibilities copy() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece);
        copy.neighbors.putAll(neighbors);
        return copy;
    }

    /**
     * Makes a copy of this, multiplies every weight by
     *
    public PieceNeighborsPossibilities copyMultiplyWeights(int weight) {
        if (weight == 1) return copy();
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece);
        for (Map.Entry<Face, WeightedSet<Piece>> faceEntry : neighbors.entrySet()) {
            copy.neighbors.put(faceEntry.getKey(), faceEntry.getValue().copyMultiplyWeights(weight));
        }
        return copy;
    }

    /**
     * @return A set containing all possible rotated and flipped versions of this (it also contains this)
     *
    public @NotNull Set<@NotNull PieceNeighborsPossibilities> generateSiblings(boolean allowUpsideDown) {
        Set<@NotNull PieceNeighborsPossibilities> pieces = new HashSet<>();
        pieces.add(this);
        if (allowUpsideDown) {
            pieces.addAll(generateSiblings(false));
            pieces.addAll(rotateZ(D90).generateSiblings(false));
            pieces.addAll(rotateZ(D180).generateSiblings(false));
            pieces.addAll(rotateZ(D270).generateSiblings(false));
            pieces.addAll(rotateX(D90).generateSiblings(false));
            pieces.addAll(rotateX(D270).generateSiblings(false));
        } else {
            PieceNeighborsPossibilities r90 = rotateY(D90);
            if (pieces.add(r90)) {
                pieces.add(r90.flipX());
                pieces.add(r90.flipZ());
            }
            pieces.add(rotateY(D180));
            pieces.add(rotateY(D270));
            pieces.add(flipX());
            pieces.add(flipZ());
        }
        return pieces;
    }

    /**
     * @return a rotated version by <i>angle</i> degrees along the Y axis
     *
    @Contract(pure = true)
    public @NotNull PieceNeighborsPossibilities rotateX(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateX(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateX(angle), entry.getValue().mapElements(piece -> piece.rotateX(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities rotateY(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateY(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateY(angle), entry.getValue().mapElements(piece -> piece.rotateY(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities rotateZ(final @NotNull RotationAngle angle) {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.rotateZ(angle));
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().rotateZ(angle), entry.getValue().mapElements(piece -> piece.rotateZ(angle)));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipX() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipX());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipX(), entry.getValue().mapElements(Piece::flipX));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipY() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipY());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipY(), entry.getValue().mapElements(Piece::flipY));
        }
        return copy;
    }

    public @NotNull PieceNeighborsPossibilities flipZ() {
        PieceNeighborsPossibilities copy = new PieceNeighborsPossibilities(centerPiece.flipZ());
        for (Map.Entry<Face, WeightedSet<Piece>> entry : neighbors.entrySet()) {
            copy.neighbors.put(entry.getKey().flipZ(), entry.getValue().mapElements(Piece::flipZ));
        }
        return copy;
    }
    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PieceNeighborsPossibilities that = (PieceNeighborsPossibilities) o;

        if (!centerPiece.equals(that.centerPiece)) return false;
        return neighbors.equals(that.neighbors);
    }

    @Override
    public int hashCode() {
        int result = centerPiece.hashCode();
        result = 31 * result + neighbors.hashCode();
        return result;
    }

    /**
     * Simplifies the coefficients for each neighbor. For each face, it finds the greatest common factor (GCD), and all
     * coefficients for that face get divided by that factor. If there is only one {@link Piece} for one {@link Face},
     * the GCD of that face is the coefficient of that piece, so the new coefficient for that piece is 1. This method
     * is helpful for {@link WeightedSet#weightedChoose()} because {@link WeightedSet#weightedChoose()}
     * is making an array with every element being present the number of times defined by the coefficient. Therefor, it
     * is easier to choose a random element when coefficients are 1, 2 or 3 compared to when coefficient are 100, 200 or
     * 300 because it will have to add 100, 200 or 300 times an element to the array.
     *
    public void simplify() {
        neighbors.values().forEach(WeightedSet::simplify);
    }
}
*/