package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.VirtualSpace.ObjectWithCoordinates;
import fr.bananasmoothii.mcwfc.core.util.Bounds;
import fr.bananasmoothii.mcwfc.core.util.Coords;
import fr.bananasmoothii.mcwfc.core.util.Face;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @param <B> the type of the blocks in the {@link Piece}s. For example, in bukkit, this is {@link org.bukkit.block.data.BlockData}
 */
public class Wave<B> {
    /**
     * This needs to be a virtual space of {@link Sample<B>} and not just {@link Set}<{@link Piece}>
     * because two {@link PieceNeighbors.Locked<B>} are different while their centerpiece might be the same.
     * At the begenning, every node is equal to {@link #sample}, but they are not the same object: each {@link Sample<B>}
     * in the {@link VirtualSpace} is unique.
     */
    private VirtualSpace<Sample<B>> wave;
    private final ImmutableSample<B> sample;
    private final long seed;
    private final List<@NotNull PieceCollapseListener<B>> pieceCollapseListeners = new ArrayList<>();
    public final boolean useModuloCoords;
    private boolean hasImpossibleStates = false;

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds) {
        this(sample, bounds, true);
    }

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds, boolean useModuloCoords) {
        this(sample, bounds, useModuloCoords, ThreadLocalRandom.current().nextLong());
    }

    public Wave(@NotNull Sample<B> sample, @NotNull Bounds bounds, boolean useModuloCoords, long seed) {
        wave = new VirtualSpace<>(bounds);
        this.sample = sample.immutable();
        this.useModuloCoords = useModuloCoords;
        this.seed = seed;
    }

    public boolean hasImpossibleStates() {
        return hasImpossibleStates;
    }

    public VirtualSpace<Sample<B>> getWave() {
        return wave;
    }

    public @NotNull Random getRandom(@NotNull Coords coords) {
        return getRandom(coords.x(), coords.y(), coords.z());
    }

    public @NotNull Random getRandom(int pieceX, int pieceY, int pieceZ) {
        // yes, we are creating a new Random for each generating piece, but this is needed in order to have the same piece
        // for the same seed at the same coords
        final Random random = new Random();
        random.setSeed((long) pieceX * random.nextLong() ^ (long) pieceY * random.nextLong() ^ (long) pieceZ * random.nextLong() ^ seed);
        return random;
    }

    public int getTotalEntropy() {
        int totalEntropy = 0;
        for (ObjectWithCoordinates<Sample<B>> node : wave) {
            totalEntropy += node.object().size();
        }
        return totalEntropy;
    }

    private Bounds currentGenerationBounds;

    private Set<Coords> propagationTasks = new HashSet<>();

    private Deque<Coords> lastChangedEntropies;

    private ObjectWithCoordinates<PieceNeighbors.Locked<B>> lastManuallyCollapsedPiece;

    private Deque<WaveState> lastStates = null;

    /**
     * Automatically collapses the hole {@link Wave}.
     */
    public void collapseAll() throws GenerationFailedException {
        collapseInBounds(wave.getBounds());
    }

    /**
     * Automatically collapses the {@link Wave} in the given {@link Bounds}.
     */
    public void collapseInBounds(final @NotNull Bounds bounds) throws GenerationFailedException {
        currentGenerationBounds = bounds;
        fillWithPossibleStates();

        final Random random0 = getRandom(0, 0, 0);
        lastChangedEntropies = new ArrayDeque<>();
        lastStates = new ArrayDeque<>();
        lastManuallyCollapsedPiece = new ObjectWithCoordinates<>(null, bounds.randomPoint(random0));
        while (true) {
            try {
                // compute all propagation tasks (fillWithPossibleStates() might have added some)
                while (!propagationTasks.isEmpty()) {
                    final Set<Coords> propagationTasksCopy = new HashSet<>(propagationTasks);
                    propagationTasks.clear();
                    for (Coords propagationTask : propagationTasksCopy) {
                        propagateCollapseTo(propagationTask.x(), propagationTask.y(), propagationTask.z());
                    }
                }
                // choose a random node
                final Coords node = chooseLowEntropyNode();
                if (node == null) {
                    // finished
                    lastChangedEntropies = null;
                    lastStates = null;
                    currentGenerationBounds = null;
                    break;
                }
                // save the current state of the wave to be able to restore it on failure
                saveSate();
                // collapse the node
                collapse(node.x(), node.y(), node.z());
                propagateCollapseLaterFrom(node.x(), node.y(), node.z());
            } catch (GenerationFailedException e) {
                try {
                    restoreLatestSate();
                    hasImpossibleStates = false;
                } catch (GenerationFailedException e2) {
                    e2.initCause(e);
                    throw e2;
                }
            }
        }
    }

    /**
     * Fills the wave with all possible states for each piece. You probably want to use {@link #collapseInBounds(Bounds)} instead.
     */
    public void fillWithPossibleStates() throws GenerationFailedException {
        if (sample.isEmpty()) throw new GenerationFailedException("Invalid sample");
        boolean isAlreadyCollapsed = sample.size() == 1;
        final PieceNeighbors.Locked<B> aPiece = sample.iterator().next();
        for (Coords node : currentGenerationBounds) {
            wave.set(new Sample<>(sample), node.x(), node.y(), node.z());
            if (isAlreadyCollapsed) {
                pieceCollapsedCallListeners(node.x(), node.y(), node.z(), aPiece);
            }
        }
        if (!isAlreadyCollapsed) {
            // remove already impossible states
            for (Coords coords : currentGenerationBounds) {
                final int x = coords.x(),
                          y = coords.y(),
                          z = coords.z();
                final Sample<B> candidates = getCollapseCandidatesAt(x, y, z);
                if (candidates.isEmpty()) throw new GenerationFailedException("No candidates at " + coords +
                        ": your sample is invalid");
                if (candidates.size() <= sample.size()) {
                    wave.set(candidates, x, y, z);
                    propagateCollapseLaterFrom(x, y, z);
                }
            }
        }
    }

    /**
     * @return the pieces that could be collapsed at that position
     */
    @Contract(pure = true)
    public @NotNull Sample<B> getCollapseCandidatesAt(int x, int y, int z) throws GenerationFailedException {
        final Sample<B> newCandidates = new Sample<>();
        final Sample<B> currentCandidates = wave.get(x, y, z, useModuloCoords);
        if (currentCandidates == null) return newCandidates;
        final Iterator<Map.Entry<PieceNeighbors.Locked<B>, Integer>> iter = currentCandidates.elementsAndWeightsIterator();
        while (iter.hasNext()) {
            final Map.Entry<PieceNeighbors.Locked<B>, Integer> entry = iter.next();
            final PieceNeighbors.Locked<B> currentCandidate = entry.getKey();
            final int currentCandidateWeight = entry.getValue();
            boolean isValidCandidate = true;
            for (Map.Entry<Face, Optional<Piece.Locked<B>>> faceEntry : currentCandidate.entrySet()) {
                final Face face = faceEntry.getKey();
                if (!useModuloCoords && !currentGenerationBounds.contains(face.addTo(x, y, z))) continue;
                final Optional<Piece.Locked<B>> expectedPiece = faceEntry.getValue();
                final @Nullable Sample<B> foundSample = wave.get(face.addTo(x, y, z), useModuloCoords);
                if (expectedPiece.isPresent()) {
                    if (foundSample == null || !(foundSample.centerPiecesContains(expectedPiece.get()) && foundSample.acceptsAt(face.getOppositeFace(), currentCandidate.getCenterPiece()))) {
                        isValidCandidate = false;
                        break;
                    }
                } else {
                    if (! useModuloCoords) throw new GenerationFailedException("Invalid sample, it has been generated without modulo coords");
                    if (foundSample != null) {
                        isValidCandidate = false;
                        break;
                    }
                }
            }
            if (isValidCandidate) {
                newCandidates.add(currentCandidate, currentCandidateWeight);
            }
        }
        return newCandidates;
    }

    /**
     * @return the collapsed {@link PieceNeighbors}
     */
    private @NotNull PieceNeighbors.Locked<B> collapse(int x, int y, int z) throws GenerationFailedException {
        final Sample<B> collapseCandidates = getCollapseCandidatesAt(x, y, z);
        return collapseWithTheseCandidates(x, y, z, collapseCandidates);
    }

    /**
     * @return the collapsed {@link PieceNeighbors}
     */
    private @NotNull PieceNeighbors.Locked<B> collapseWithTheseCandidates(int x, int y, int z, @NotNull Sample<B> collapseCandidates) throws GenerationFailedException {
        final PieceNeighbors.Locked<B> collapsed;
        if (collapseCandidates.isEmpty()) {
            hasImpossibleStates = true;
            throw new GenerationFailedException("Encountered an impossible state at " + x + " " + y + " " + z);
        } else
            collapsed = Objects.requireNonNull(collapseCandidates.weightedChoose(getRandom(x, y, z)),
                    "weightedChoose() returned null");
        final Sample<B> newSample = new Sample<>(); // a sample with a size of 1
        newSample.add(collapsed);
        wave.set(newSample, x, y, z, useModuloCoords);
        lastChangedEntropies.addLast(new Coords(x, y, z));
        lastManuallyCollapsedPiece = new ObjectWithCoordinates<>(collapsed, x, y, z);
        pieceCollapsed(x, y, z, collapsed);
        return collapsed;
    }

    /**
     * Should be called everytime an entropy is changed to 1.
     */
    private void pieceCollapsed(int x, int y, int z, @NotNull PieceNeighbors.Locked<B> collapsed) throws GenerationFailedException {
        // collapsing a piece also forces the neighbors to have the right centerpiece
        for (Map.Entry<Face, Optional<Piece.Locked<B>>> faceEntry : collapsed.entrySet()) {
            final Face face = faceEntry.getKey();
            if (!useModuloCoords && !currentGenerationBounds.contains(face.addTo(x, y, z))) continue;
            final Sample<B> sampleAtThatFace = wave.get(x + face.getModX(), y + face.getModY(), z + face.getModZ(), useModuloCoords);
            final Optional<Piece.Locked<B>> expectedPiece = faceEntry.getValue();
            if (expectedPiece.isPresent()) {
                if (sampleAtThatFace == null) {
                    hasImpossibleStates = true;
                    throw new GenerationFailedException("Encountered an impossible state at " + x + " " + y + " " + z);
                } else if (sampleAtThatFace.retainAllWithCenterPiece(expectedPiece.get())) {
                    if (sampleAtThatFace.size() == 1) {
                        pieceCollapsed(x + face.getModX(), y + face.getModY(), z + face.getModZ(), sampleAtThatFace.peek());
                    } else if (sampleAtThatFace.isEmpty()) {
                        // do not throw an exception if the problem is outside the current bounds, and we don't use modulo coords
                        hasImpossibleStates = true;
                        throw new GenerationFailedException("Encountered an impossible state at " + x + " " + y + " " + z);
                    }
                }
            }
        }
        pieceCollapsedCallListeners(x, y, z, collapsed);
    }

    /**
     * This doesn't modify the wave, it just adds propagation tasks to {@link #propagationTasks}.
     */
    private void propagateCollapseLaterFrom(int x, int y, int z) {
        for (Face cartesianFace : Face.getCartesianFaces()) {
            final Coords newCoords = cartesianFace.addTo(x, y, z);
            if (! useModuloCoords && ! currentGenerationBounds.contains(newCoords)) continue;
            propagationTasks.add(newCoords);
        }
    }

    private void propagateCollapseTo(int x, int y, int z) throws GenerationFailedException {
        if (!useModuloCoords && !currentGenerationBounds.contains(x, y, z)) return;
        final Sample<B> present = wave.getWithoutFill(x, y, z);
        if (present == null) return;
        final int sizeBefore = present.size();

        if (present.size() == 1) return;
        present.retainAll(getCollapseCandidatesAt(x, y, z));
        if (present.size() == sizeBefore) return; // nothing changed, no need to propagate
        if (present.isEmpty()) {
            hasImpossibleStates = true;
            throw new GenerationFailedException("Encountered an impossible state at " + x + " " + y + " " + z);
        } else if (present.size() == 1) {
            pieceCollapsed(x, y, z, present.peek());
        }
        entropyChanged(x, y, z);
        for (Face cartesianFace : Face.getCartesianFaces()) {
            propagationTasks.add(new Coords(x + cartesianFace.getModX(), y + cartesianFace.getModY(), z + cartesianFace.getModZ()));
        }
    }

    private void entropyChanged(int x, int y, int z) {
        final Coords coords = new Coords(x, y, z);
        lastChangedEntropies.remove(coords);
        lastChangedEntropies.addLast(coords);
    }

    /**
     * This searches a node ({@link Coords}) with a low entropy (but strictly above 1, otherwise that means the wave
     * has collapsed) among the latest changed entropies
     * @return null if the wave has collapsed
     */
    @Contract(pure = true)
    private @Nullable Coords chooseLowEntropyNode() {
        int lowestEntropy = Integer.MAX_VALUE;
        final Set<Coords> lowestEntropyNodes = new HashSet<>(); // this is a list so every piece has the same chance to be chosen
        final Iterator<Coords> iterator = lastChangedEntropies.descendingIterator();
        while (iterator.hasNext()) {
            Coords node = iterator.next();
            final Sample<B> sample = wave.get(node);
            //noinspection ConstantConditions
            if (sample.size() <= 1) continue;
            if (sample.size() < lowestEntropy) {
                lowestEntropy = sample.size();
                lowestEntropyNodes.clear();
                lowestEntropyNodes.add(node);
            } else if (sample.size() == lowestEntropy) {
                lowestEntropyNodes.add(node);
            }
        }
        if (lowestEntropyNodes.isEmpty()) {
            return chooseLowEntropyNodeTotalSearch();
        }
        return getClosestNode(lowestEntropyNodes);
    }

    /**
     * This searches a node ({@link Coords}) with a low entropy (but strictly above 1, otherwise that means the wave
     * has collapsed) among the <strong>the hole wave</strong> (not among the last changed entropies)
     * @return null if the wave has collapsed
     */
    @Contract(pure = true)
    private @Nullable Coords chooseLowEntropyNodeTotalSearch() {
        int lowestEntropy = Integer.MAX_VALUE;
        final Set<Coords> lowestEntropyNodes = new HashSet<>(); // this is a list so every piece has the same chance to be chosen
        for (ObjectWithCoordinates<Sample<B>> node : wave) {
            final Sample<B> object = node.object();
            if (1 < object.size() && object.size() < lowestEntropy) {
                lowestEntropy = object.size();
                lowestEntropyNodes.clear();
                lowestEntropyNodes.add(node.coords());
            } else if (object.size() == lowestEntropy) {
                lowestEntropyNodes.add(node.coords());
            }
        }
        return getClosestNode(lowestEntropyNodes);
    }
    
    @Contract(pure = true)
    private @Nullable Coords getClosestNode(@NotNull Collection<Coords> nodes) {
        if (nodes.isEmpty()) return null;
        final List<Coords> lowestEntropyNodes = new ArrayList<>(); // this is a list so every piece has the same chance to be chosen
        final double lowestEntropyDistance = Double.MAX_VALUE;
        for (Coords node : nodes) {
            final double distance = node.distanceFrom(lastManuallyCollapsedPiece.coords());
            if (distance < lowestEntropyDistance) {
                lowestEntropyNodes.clear();
                lowestEntropyNodes.add(node);
            } else if (distance == lowestEntropyDistance) {
                lowestEntropyNodes.add(node);
            }
        }
        if (lowestEntropyNodes.size() == 1) return lowestEntropyNodes.get(0);
        return lowestEntropyNodes.get(getRandom(lastManuallyCollapsedPiece.coords()).nextInt(lowestEntropyNodes.size()));
    }

    private class WaveState {
        private final VirtualSpace<Sample<B>> wave = new VirtualSpace<>(Wave.this.wave);
        private final Set<Coords> propagationTasks = new HashSet<>(Wave.this.propagationTasks);
        private final Deque<Coords> lastChangedEntropies = new ArrayDeque<>(Wave.this.lastChangedEntropies);
        private final ObjectWithCoordinates<PieceNeighbors.Locked<B>> lastManuallyCollapsedPiece = Wave.this.lastManuallyCollapsedPiece;

        private WaveState() {
            // deep copy
            for (ObjectWithCoordinates<Sample<B>> node : Wave.this.wave) {
                wave.set(new Sample<>(node.object()), node.coords().x(), node.coords().y(), node.coords().z());
            }
        }

        private void restore() {
            Wave.this.wave = wave;
            Wave.this.propagationTasks = propagationTasks;
            Wave.this.lastChangedEntropies = lastChangedEntropies;
            Wave.this.lastManuallyCollapsedPiece = lastManuallyCollapsedPiece;
            waveRestoredCallListeners(wave);
        }
    }

    private boolean lastActionIsSave = false;
    private boolean beforeLastActionIsSave = false;
    private int restoreSaveRestoreCount = 0;

    private void saveSate() {
        lastStates.addLast(new WaveState());
        //if (lastStates.size() > 20) lastStates.removeFirst();
        beforeLastActionIsSave = lastActionIsSave;
        lastActionIsSave = true;
    }

    private void restoreLatestSate() throws GenerationFailedException {
        if (lastStates.isEmpty())
            throw new GenerationFailedException("The wave has some impossible states that could not be resolved");
        if (!beforeLastActionIsSave && lastActionIsSave) restoreSaveRestoreCount++;
        beforeLastActionIsSave = lastActionIsSave;
        lastActionIsSave = false;
        if (restoreSaveRestoreCount > 20) {
            restoreSaveRestoreCount = 0;
            lastStates.removeLast();
            restoreLatestSate();
        } else {
            final ObjectWithCoordinates<PieceNeighbors.Locked<B>> problematicChange = lastManuallyCollapsedPiece;
            lastStates.getLast().restore();
            lastStates.removeLast();
            final Sample<B> presentSample = wave.get(problematicChange.coords());
            //noinspection ConstantConditions
            presentSample.remove(problematicChange.object());
            if (presentSample.isEmpty()) {
                hasImpossibleStates = true;
                restoreSaveRestoreCount = 0;
                restoreLatestSate();
            } else if (presentSample.size() == 1) {
                try {
                    pieceCollapsed(problematicChange.x(), problematicChange.y(), problematicChange.z(), presentSample.peek());
                } catch (GenerationFailedException e) {
                    restoreSaveRestoreCount = 0;
                    restoreLatestSate();
                }
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public boolean nodeIsCollapsed(int x, int y, int z) {
        return wave.get(x, y, z).size() == 1;
    }

    public void registerPieceCollapseListener(PieceCollapseListener<B> listener) {
        pieceCollapseListeners.add(Objects.requireNonNull(listener));
    }

    private void pieceCollapsedCallListeners(int pieceX, int pieceY, int pieceZ, PieceNeighbors.Locked<B> piece) {
        for (PieceCollapseListener<B> listener : pieceCollapseListeners) {
            listener.onCollapse(pieceX, pieceY, pieceZ, piece);
        }
    }

    private void waveRestoredCallListeners(VirtualSpace<Sample<B>> newWave) {
        for (PieceCollapseListener<B> pieceCollapseListener : pieceCollapseListeners) {
            pieceCollapseListener.onRestore(newWave);
        }
    }

    /**
     * A {@link FunctionalInterface} whose method is {@link #onCollapse(int, int, int, PieceNeighbors.Locked)}. It is called when a piece
     * of this {@link Wave} totally collapses, meaning there is only one state left. that piece is passed along with its
     * coordinates in the parameters.
     */
    public interface PieceCollapseListener<B> {
        void onCollapse(int pieceX, int pieceY, int pieceZ, PieceNeighbors.Locked<B> piece);

        void onRestore(VirtualSpace<Sample<B>> newWave);
    }

    public static class GenerationFailedException extends Exception {
        // TODO: make GenerationFailedException specify some improvements that could be made on the dataset (adding
        //       one or more (probably just one since the generation stops at the first impossible state)
        //       PieceNeighbors.Locked<B>) to make it easier to collapse the wave.
        public GenerationFailedException(String message) {
            super(message);
        }
    }

    /**
     * prints the layer yLayer
     */
    public void debugPrintY(int yLayer) {
        System.out.println("y = " + yLayer + " ; xMin = " + wave.xMin() + " ;  xMax = " + wave.xMax() + " ;  yMin = " + wave.yMin() + " ;  yMax = " + wave.xMax());

        for (int z = wave.zMin() ; z <= wave.zMax(); z++) {
            for (int x = wave.xMin(); x <= wave.xMax(); x++) {
                Sample<B> element = wave.get(x, yLayer, z);
                if (element == null || element.isEmpty()) System.out.print('!');
                else System.out.print(element.iterator().next().getCenterPiece().get(0, 0, 0));
                System.out.print(' ');
            }
            System.out.print('\n');
        }
    }
}
