package me.void514.rngcalc.witch;

import me.void514.rngcalc.math.ChunkPos;
import me.void514.rngcalc.math.PlaneAxis;
import me.void514.rngcalc.math.VoidRandom;

import java.util.Arrays;
import java.util.List;

public class WitchSpawnSimulator {
    /**
     * This array stores all possible arguments passed to nextInt() during a spawning attempt.
     * It is used to determine whether the sequence calls next(31) twice for a single nextInt() call.
     */
    private static final int[] POSSIBLE_NON_POWER_NEXT_INT_BOUNDS = new int[]{6, 80, 515, 516};

    private final VoidRandom rand = new VoidRandom();

    private final List<WitchHutState> hutStates;

    /**
     * The sequence obtained by repeatedly invoking next(31) on a Random instance.
     * Cached to save time when querying the sequence.
     */
    private final int[] randomSequence = new int[1024];

    /**
     * The stored expected spawns for the four witch huts.
     */
    public final float[] expectedSpawns = new float[4];

    /**
     * The 64k+n element of this array stores the chance for the k-th witch hut
     * to start with 7n extra random number invocations compared with the state
     * when all previous pack spawn attempts have been blocked.
     */
    public final float[] initialStateChances = new float[256];

    public WitchSpawnSimulator(List<WitchHutState> hutStates) {
        this.hutStates = hutStates;
    }

    public boolean initialize(int regionX, int regionZ, long worldSeed) {
        // initialize world random as a woodland mansion does
        final int seedX, seedY, seedZ;
        seedX = regionX;
        seedY = regionZ;
        seedZ = 10387319;
        final long seed = (long) seedX * 341873128712L + (long) seedY * 132897987541L + worldSeed + (long) seedZ;
        this.rand.setSeed(seed);
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
                    if ((value - (value % bound) + bound - 1) < 0) return false;
                }
            }
        } catch (Throwable throwable) {
            throw new AssertionError(throwable);
        }
        // initialize the remaining fields
        Arrays.fill(expectedSpawns, 0.0f);
        Arrays.fill(initialStateChances, 0.0f);
        return true;
    }

    /**
     * Does the actual computation. The inputted list must be sorted in ascending order and contain precisely
     * 4 elements.
     */
    public float compute(int regionX, int regionZ, long worldSeed) {
        if (!initialize(regionX, regionZ, worldSeed)) return -1.0f;
        return simulateHutSpawns(hutStates);
    }

    private static int chunkIndex(WitchHutState hutState) {
        ChunkPos pos = hutState.pos();
        return 15 * pos.x() + pos.z();
    }

    public float simulateHutSpawns(List<WitchHutState> hutStates) {
        int i = 0;
        for (WitchHutState state : hutStates) {
            simulateHutChunkSpawn(state, i);
            i++;
        }
        float sum = 0.0f;
        for (float expected : expectedSpawns) sum += expected;
        return sum;
    }

    private void simulateHutChunkSpawn(WitchHutState hutState, int index) {
        int chunkIndex = chunkIndex(hutState);
        if (index == 0) {
            simulateHutChunkSpawn(hutState, index, 3 * chunkIndex, 0, 1.0f);
        } else {
            final float[] initialStateChances = this.initialStateChances;
            float chance;
            for (int i = 0; i < 64; i++) {
                if ((chance = initialStateChances[(index - 1) << 6 | i]) > 0.0f) {
                    simulateHutChunkSpawn(hutState, index, 3 * chunkIndex + 7 * i, i, chance);
                }
            }
        }
    }

    private void simulateHutChunkSpawn(WitchHutState hutState, int index, int ptr, int advances, float chance) {
        // PSS = pack spawn start
        // generate the pack spawn starts
        final int pssx, pssy, pssz;
        final int yStart = hutState.yStart();
        final int yEnd = hutState.yEnd();
        final int[] randomSequence = this.randomSequence;
        final float[] initialStateChances = this.initialStateChances;
        pssy = randomSequence[ptr + 2] % 80;
        if (pssy < yStart || pssy > yEnd) {
            // blocked pack spawning - advance with always 0*7 extra random calls, 0 extra spawns
            //expectedSpawns[index] += chance * 0.0f;
            //initialStateChances[index << 6] += chance * 1.0f;
            //System.out.printf("pssy = %d, Incrementing initialStateChances[%d] by %f\n", pssy, index << 6, chance);
            initialStateChances[index << 6 | advances] += chance;
            return;
        }
        pssx = randomSequence[ptr] >> 27;
        pssz = randomSequence[ptr + 1] >> 27;
        ptr += 3;
        simulatePackSpawning(index, advances, ptr,
                4, 0, 4,
                pssx, pssz, hutState.axis(), Arrays.binarySearch(hutState.yFloors(), pssy) >= 0, chance);
    }

    private void simulatePackSpawning(int index, int advances, int ptr,
                                      int attemptsRemaining, int packSize, int remainingSuccess,
                                      int pssx, int pssz, PlaneAxis axis, boolean yValid, float chance) {
        //System.out.printf("simulatePackSpawning(index=%d, adv=%d, ptr=%d, AR=%d, PS=%d, RS=%d, sx=%d" +
        //                ", sz=%d, yV=%d, chance=%f)\n",
        //        index, advances, ptr, attemptsRemaining, packSize, remainingSuccess, pssx, pssz, yValid ? 1 : 0, chance);
        final int[] randomSequence = this.randomSequence;
        final float[] expectedSpawns = this.expectedSpawns;
        final float[] initialStateChances = this.initialStateChances;
        int curX = pssx;
        int curZ = pssz;
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
        while ((--packSize) >= 0 && remainingSuccess > 0) {
            curX += (randomSequence[ptr] % 6 - randomSequence[ptr + 1] % 6);
            curZ += (randomSequence[ptr + 4] % 6 - randomSequence[ptr + 5] % 6);
            if (yValid && 0 <= curX && curX < widthX && 0 <= curZ && curZ < widthZ) {
                remainingSuccess--;
                expectedSpawns[index] += chance;
            }
            ptr += 7;
            advances++;
        }
        if (attemptsRemaining > 1) {
            // pass to the next attempt, if there is another attempt remaining
            for (int nextPackSize = 1; nextPackSize <= 4; nextPackSize++) {
                simulatePackSpawning(index, advances, ptr,
                        attemptsRemaining - 1, nextPackSize, remainingSuccess,
                        pssx, pssz, axis, yValid, chance * 0.25f);
            }
        } else {
            // this is the last pack spawn attempt, record the chances for the initial value of the next chunk
            initialStateChances[index << 6 | advances] += chance;
            //System.out.printf("Incrementing initialStateChances[%d] by %f\n", index << 6 | advances, chance);
        }
    }
}
