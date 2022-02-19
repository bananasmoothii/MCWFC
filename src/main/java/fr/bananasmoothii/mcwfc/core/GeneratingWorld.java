package fr.bananasmoothii.mcwfc.core;

import fr.bananasmoothii.mcwfc.core.util.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratingWorld {
    private final VirtualSpace<Piece> world;
    private final long seed;
    private @Nullable PieceNeighborsSet availablePieces;
    private int pieceSize;

    public GeneratingWorld() {
        this(null);
    }

    public GeneratingWorld(final @Nullable PieceNeighborsSet availablePieces) {
        this(ThreadLocalRandom.current().nextLong(), availablePieces);
    }

    public GeneratingWorld(final long seed, final @Nullable PieceNeighborsSet availablePieces) {
        world = new VirtualSpace<>();
        this.seed = seed;
        if (availablePieces != null)
            setAvailablePieces(availablePieces);
    }

    // da big ting here
    /**
     * Generate this world in the given bounds with the given pieces according to the Wave Function Collapse algorithm.
     */
    public void generateWFC(final @NotNull Bounds bounds) {
        if (availablePieces == null)
            throw new NullPointerException("You need to set available pieces before calling generateWFC");

        // mapping coords to number of pieces for VirtualSpace<Piece> world. These coords are all-inclusive, meaning
        // world.get(xFromPiece, yFromPiece, zFromPiece) should be generated and world.get(xToPiece, yToPiece, zToPiece)
        // should also be generated.
        final Bounds pieceBounds = new Bounds(
                bounds.xMin() / pieceSize,
                bounds.yMin() / pieceSize,
                bounds.zMin() / pieceSize,
                divideRoundUp(bounds.xMax(), pieceSize),
                divideRoundUp(bounds.yMax(), pieceSize),
                divideRoundUp(bounds.zMax(), pieceSize));

        // choose a starting point
        Random globalRandom = getRandom(0, 0, 0);
        int x = globalRandom.nextInt(pieceBounds.xMin(), pieceBounds.xMax() + 1);
        int y = globalRandom.nextInt(pieceBounds.yMin(), pieceBounds.yMax() + 1);
        int z = globalRandom.nextInt(pieceBounds.zMin(), pieceBounds.zMax() + 1);

        // generate the first piece at that starting point
        final PieceNeighbors pieceChoice = availablePieces.chooseRandom(globalRandom);
        world.set(pieceChoice.getCenterPiece(), x, y, z);
        pieceChanged(x, y, z, pieceChoice.getCenterPiece());
        HashMap<Coords, PieceGeneratingTask> tasks = new HashMap<>();
        HashMap<Coords, PieceGeneratingTask> newTasks = new HashMap<>();
        tasks.put(new Coords(x, y, z), new PieceGeneratingTask(x, y, z, pieceChoice, bounds));
        while (!tasks.isEmpty()) { // TODO: randomization in task choosing
            for (PieceGeneratingTask task : tasks.values()) {
                for (PieceGeneratingTask newTask : task.generate()) {
                    final PieceGeneratingTask taskInMap = newTasks.get(newTask.getCoords());
                    if (taskInMap != null)
                        taskInMap.addPieceNeighborsOption(newTask.pieceNeighborsAtPosition);
                    else
                        newTasks.put(newTask.getCoords(), newTask);
                }
            }
            tasks = newTasks;
            newTasks = new HashMap<>();
        }
        /*
        final LinkedList<PieceGeneratingTask> tasks = new LinkedList<>();
        {
            final PieceGeneratingTask firstTask = new PieceGeneratingTask(x, y, z, pieceChoice, bounds);
            tasks.add(firstTask);
            tasksMap.put(new Coords(x, y, z), firstTask);
        } // delete variable firstTask
        // this is the best way to use a linked list I think, using only the iterator.
        final ListIterator<PieceGeneratingTask> iter = tasks.listIterator(globalRandom.nextInt(tasks.size()));
        boolean reverseBrowse = globalRandom.nextBoolean();
        PieceGeneratingTask currentElement = null;
        while (iter.hasNext() || iter.hasPrevious()) {
            // a random number between 1 and 10 inclusive (or less if the list is too small)
            int randomIndexOffset = globalRandom.nextInt(Math.min(tasks.size(), 10)) + 1;
            for (int i = 0; i < randomIndexOffset; i++) { // will loop at least 1 time
                if (!reverseBrowse) {
                    if (!iter.hasNext()) {
                        reverseBrowse = true;
                        currentElement = iter.previous();
                    } else {
                        currentElement = iter.next();
                    }
                } else {
                    if (!iter.hasPrevious()) {
                        reverseBrowse = false;
                        currentElement = iter.next();
                    } else {
                        currentElement = iter.previous();
                    }
                }
            }
            // now we are at a random element
            iter.remove();
            tasksMap.remove(new Coords(currentElement.x, currentElement.y, currentElement.z));
            for (PieceGeneratingTask task : currentElement.generate()) {
                // see if there isn't a task for these coords
                iter.add(task);
            }
        }

         */
    }

    private static int divideRoundUp(int a, int b) {
        if (a % b == 0) return a / b;
        return a / b + 1;
    }

    class PieceGeneratingTask {
        int x, y, z;
        PieceNeighbors pieceNeighborsAtPosition;
        Bounds bounds;

        public PieceGeneratingTask(int x, int y, int z, @NotNull PieceNeighbors pieceNeighborsAtPosition, Bounds bounds) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.pieceNeighborsAtPosition = pieceNeighborsAtPosition.copy();
            this.bounds = bounds;
        }

        public void addPieceNeighborsOption(@NotNull PieceNeighbors pieceNeighbors) {
            for (Map.Entry<Face, HashWeightedSet<Piece>> faceEntry : pieceNeighbors.getNeighbors().entrySet()) {
                pieceNeighborsAtPosition.addAllNeighbors(faceEntry.getKey(), faceEntry.getValue());
            }
        }

        public Coords getCoords() {
            return new Coords(x, y, z);
        }

        public List<PieceGeneratingTask> generate() {
            List<PieceGeneratingTask> newTasks = new ArrayList<>(pieceNeighborsAtPosition.getNeighbors().size());
            for (Map.Entry<Face, HashWeightedSet<Piece>> faceEntry : pieceNeighborsAtPosition.getNeighbors().entrySet()) {
                final Face face = faceEntry.getKey();
                final int newX = x + face.getModX();
                final int newY = y + face.getModY();
                final int newZ = z + face.getModZ();
                final Random newRandom = getRandom(newX, newY, newZ);
                if (!bounds.isInBounds(newX, newY, newZ)) break;
                if (world.getWithoutFill(newX, newY, newZ) == null) {
                    // generate the piece
                    final Piece chosenPiece = faceEntry.getValue().weightedChoose(newRandom);
                    world.set(chosenPiece, newX, newY, newZ);
                    pieceChanged(newX, newY, newZ, chosenPiece);

                    @SuppressWarnings("ConstantConditions")
                    PieceNeighbors[] weightedPieces = availablePieces.toArray();
                    shuffle(weightedPieces, newRandom);
                    boolean found = false;
                    for (final PieceNeighbors weightedPiece : weightedPieces) {
                        if (chosenPiece.equals(weightedPiece.getCenterPiece())) {
                            found = true;
                            newTasks.add(new PieceGeneratingTask(newX, newY, newZ, weightedPiece, bounds));
                            break;
                        }
                    }
                    if (! found)
                        throw new IllegalStateException("One of the neighbor pieces couldn't be found as center piece of one " +
                                "PieceNeighbors in the available pieces");
                }
            }
            return newTasks;
        }
    }

    @Contract(pure = true)
    private static <T> void shuffle(T[] array, Random random) {
        for (int i = 0; i < array.length; i++) {
            int randomIndexToSwap = random.nextInt(array.length);
            T temp = array[randomIndexToSwap];
            array[randomIndexToSwap] = array[i];
            array[i] = temp;
        }
    }

    public @NotNull Random getRandom(int pieceX, int pieceY, int pieceZ) {
        // yes, we are creating a new Random for each generating piece, but this is needed in order to have the same piece
        // for the same seed at the same coords
        final Random random = new Random(seed);
        random.setSeed((long) pieceX * random.nextLong() ^ (long) pieceY * random.nextLong() ^ (long) pieceZ * random.nextLong() ^ seed);
        return random;
    }

    public @Nullable PieceNeighborsSet getAvailablePieces() {
        return availablePieces;
    }

    /**
     * Even if you can set it to null, I don't know why someone would...
     */
    public void setAvailablePieces(@NotNull PieceNeighborsSet availablePieces) {
        this.availablePieces = Objects.requireNonNull(availablePieces);
        pieceSize = availablePieces.getAny().getCenterPiece().xSize; // assuming the piece is cubic
    }

    private final ArrayList<UpdateListener> listeners = new ArrayList<>();

    public void onPieceChangeEvent(UpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener the listener to be removed
     */
    public void removePieceChangeListener(UpdateListener listener) {
        listeners.remove(listener);
    }

    private void pieceChanged(int x, int y, int z, Piece piece) {
        for (UpdateListener listener : listeners) {
            listener.onChange(x, y, z, piece);
        }
    }

    @FunctionalInterface
    public interface UpdateListener {
         void onChange(int pieceX, int pieceY, int pieceZ, Piece piece);
    }
}
