package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.math.PlaneAxis;
import me.void514.rngcalc.math.VoidRandom;
import me.void514.rngcalc.witch.WitchHutState;

import java.util.Arrays;
import java.util.Objects;

public abstract class AsyncRegionTask implements Runnable {
    /**
     * This array stores all possible arguments passed to nextInt() during a spawning attempt.
     * It is used to determine whether the sequence calls next(31) twice for a single nextInt() call.
     */
    private static final int[] POSSIBLE_NON_POWER_NEXT_INT_BOUNDS = new int[]{6, 80, 515, 516};
    /**
     * The stored expected spawns for the four witch huts.
     */
    protected final float[] expectedSpawns;
    protected final AsyncSpawnSimulator parent;
    protected final int threadNum;
    protected final int threadIndex;
    /**
     * The 64k+n element of this array stores the chance for the k-th witch hut
     * to start with 7n extra random number invocations compared with the state
     * when all previous pack spawn attempts have been blocked.
     */
    private final float[] initialStateChances = new float[256];
    private final int maxRegionAbs;
    private final VoidRandom rand = new VoidRandom();
    private final WitchHutState[] hutStateArray;
    /**
     * The sequence obtained by repeatedly invoking next(31) on a Random instance.
     * Cached to save time when querying the sequence.
     */
    private final int[] randomSequence = new int[1024];
    protected long count = 0L;

    public AsyncRegionTask(int threadIndex, AsyncSpawnSimulator parent, WitchHutState[] hutStateArray) {
        this.threadIndex = threadIndex;
        this.parent = Objects.requireNonNull(parent);
        threadNum = parent.getThreadNum();
        maxRegionAbs = parent.getMaxRegionAbs();
        this.hutStateArray = Objects.requireNonNull(hutStateArray);
        expectedSpawns = new float[this.hutStateArray.length];
    }

    @Override
    public void run() {
        long progress = 0L;
        count = 0L;
        checkRegionSpawning(0, 0, progress++);
        for (int abs = 1; abs <= maxRegionAbs; ++abs) {
            for (int rx = -abs; rx <= abs; ++rx) {
                checkRegionSpawning(rx, -abs, progress++);
                checkRegionSpawning(rx, abs, progress++);
            }
            for (int rz = -abs + 1; rz < abs; ++rz) {
                checkRegionSpawning(-abs, rz, progress++);
                checkRegionSpawning(abs, rz, progress++);
            }
        }
        parent.logInfoLine("Thread " + threadIndex + " processed " + count + " regions.");
        parent.finishFlag.release();
        try {
            parent.finishAck.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void checkRegionSpawning(int regionX, int regionZ, long progress);

    protected float compute(int regionX, int regionZ) {
        if (initialize(regionX, regionZ)) {
            return simulateHutSpawns();
        } else {
            return -1.0F;
        }
    }

    private float simulateHutSpawns() {
        for (int i = 0; i < hutStateArray.length; ++i) {
            WitchHutState state = hutStateArray[i];
            simulateHutChunkSpawn(state, i);
        }
        float sum = 0.0F;
        for (float expected : expectedSpawns) {
            sum += expected;
        }
        return sum;
    }

    private void simulateHutChunkSpawn(WitchHutState hutState, int index) {
        int chunkIndex = hutState.chunkIndex();
        if (index == 0) {
            simulateHutChunkSpawn(hutState, index, 3 * chunkIndex, 0, 1.0f);
        } else {
            float chance;
            for (int i = 0; i < 64; i++) {
                int ptr = (index - 1) << 6 | i;
                chance = initialStateChances[ptr];
                if (chance > 0.0F) {
                    simulateHutChunkSpawn(hutState, index, 3 * chunkIndex + 7 * i, i, chance);
                }
            }
        }
    }

    private void simulateHutChunkSpawn(WitchHutState hutState, int index, int ptr, int advances, float chance) {
        // PSS = pack spawn start
        // generate the pack spawn starts
        final int pssX, pssY, pssZ;
        final int yStart = hutState.yStart();
        final int yEnd = hutState.yEnd();
        pssY = randomSequence[ptr + 2] % 80;
        if (pssY < yStart || pssY > yEnd) {
            // blocked pack spawning - advance with always 0*7 extra random calls, 0 extra spawns
            initialStateChances[index << 6 | advances] += chance;
            return;
        }
        pssX = randomSequence[ptr] >> 27;
        pssZ = randomSequence[ptr + 1] >> 27;
        ptr += 3;
        simulatePackSpawning(index, advances, ptr, 4, 0, 4, pssX, pssZ, hutState.axis(), Arrays.binarySearch(hutState.yFloors(), pssY) >= 0, chance);
    }

    private void simulatePackSpawning(int index, int advances, int ptr, int attemptsRemaining, int packSize, int remainingSuccess, int pssX, int pssZ, PlaneAxis axis, boolean yValid, float chance) {
        int widthX;
        int widthZ = switch (axis) {
            case X -> {
                widthX = 9;
                yield 7;
            }
            case Z -> {
                widthX = 7;
                yield 9;
            }
            default -> throw new IllegalArgumentException("Invalid Axis");
        };
        // simulate the spawning attempts and increments the expected number of spawns on the go
        int curX = pssX;
        int curZ = pssZ;
        while ((--packSize) >= 0 && remainingSuccess > 0) {
            curX += (randomSequence[ptr] % 6 - randomSequence[ptr + 1] % 6);
            curZ += (randomSequence[ptr + 4] % 6 - randomSequence[ptr + 5] % 6);
            if (yValid && 0 <= curX && curX < widthX && 0 <= curZ && curZ < widthZ) {
                remainingSuccess--;
                expectedSpawns[index] += chance;
            }
            ptr += 7;
            ++advances;
        }
        if (attemptsRemaining > 1) {
            // pass to the next attempt, if there is another attempt remaining
            for (int nextPackSize = 1; nextPackSize <= 4; nextPackSize++) {
                simulatePackSpawning(index, advances, ptr, attemptsRemaining - 1, nextPackSize, remainingSuccess, pssX, pssZ, axis, yValid, chance * 0.25F);
            }
        } else {
            // this is the last pack spawn attempt, record the chances for the initial value of the next chunk
            initialStateChances[index << 6 | advances] += chance;
            //System.out.printf("Incrementing initialStateChances[%d] by %f\n", index << 6 | advances, chance);
        }
    }

    private boolean initialize(int regionX, int regionZ) {
        // initialize world random as a woodland mansion does
        final int seedX, seedY, seedZ;
        seedX = regionX;
        seedY = regionZ;
        seedZ = 10387319;
        final long seed = (long) seedX * 341873128712L + (long) seedY * 132897987541L + parent.worldSeed + (long) seedZ;
        rand.setSeed(seed);
        for (int i = 0; i < 4; i++) rand.nextInt(60);
        // generate the cached sequence and check for uneven values
        try {
            int value;
            for (int j = 0; j < randomSequence.length; j++) {
                value = randomSequence[j] = rand.next(31); //(Integer) RANDOM_NEXT.invoke(rand, 31);
                // this checks for uneven values; if such a value exists, refuse to process this seed
                // 39.1 out of a million seeds will be refused
                // therefore, we will reject approximately 100k seeds
                for (int bound : POSSIBLE_NON_POWER_NEXT_INT_BOUNDS) {
                    if ((value - (value % bound) + bound - 1) < 0) {
                        return false;
                    }
                }
            }
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
        // initialize the remaining fields
        Arrays.fill(expectedSpawns, 0.0F);
        Arrays.fill(initialStateChances, 0.0F);
        return true;
    }
}
