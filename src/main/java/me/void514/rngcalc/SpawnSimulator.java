package me.void514.rngcalc;

import me.void514.rngcalc.math.ChunkPos;
import me.void514.rngcalc.witch.WitchHutState;
import me.void514.rngcalc.witch.WitchSpawnSimulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpawnSimulator {

    private static final List<WitchHutState> HUT_STATES = new ArrayList<>();

    private float maxExpectedSpawns = -1.0f;
    private int maxX, maxZ;
    private final WitchSpawnSimulator simulator = new WitchSpawnSimulator(HUT_STATES);
    private static final long worldSeed = 1306145184061456995L;
    private final float[] maxExpectedArray = new float[4];

    private void attemptRegion(int regionX, int regionZ) {
        float expected = simulator.compute(regionX, regionZ, worldSeed);
        if (expected > maxExpectedSpawns) {
            maxX = regionX;
            maxZ = regionZ;
            maxExpectedSpawns = expected;
            System.arraycopy(simulator.expectedSpawns, 0, maxExpectedArray, 0, 4);
        }
    }

    static {
        HUT_STATES.add(new WitchHutState(new ChunkPos(3, 2), 64, 70, new int[] {64, 67, 70}));
        HUT_STATES.add(new WitchHutState(new ChunkPos(3, 13), 64, 70, new int[] {64, 67, 70}));
        HUT_STATES.add(new WitchHutState(new ChunkPos(12, 2), 65, 70, new int[] {67, 70}));
        HUT_STATES.add(new WitchHutState(new ChunkPos(13, 12), 64, 70, new int[] {64, 67, 70}));
    }

    static final boolean earlyReturn = false;

    public static void main(String[] args) {
        if (earlyReturn) main1();
        if (earlyReturn) return;
        final int MAX_ABS = 3200;
        final long startTime = System.currentTimeMillis();
        final SpawnSimulator spawnSimulator = new SpawnSimulator();
        for (int abs = 1; abs <= MAX_ABS; abs ++) {
            for (int x = -abs; x <= abs; x ++) {
                spawnSimulator.attemptRegion(x, -abs);
                spawnSimulator.attemptRegion(x, abs);
            }
            for (int z = 1 - abs; z < abs; z ++) {
                spawnSimulator.attemptRegion(abs, z);
                spawnSimulator.attemptRegion(-abs, z);
            }
        }
        System.out.println("Operation complete after " + (System.currentTimeMillis() - startTime) + " milliseconds. ");
        System.out.println("Maximum efficiency is achieved with woodland mansion region: X = "
                + spawnSimulator.maxX + ", Z = " + spawnSimulator.maxZ);
        System.out.println("The efficiency achieved is " + spawnSimulator.maxExpectedSpawns + "/gt, " +
                "or " + ((int) (72 * spawnSimulator.maxExpectedSpawns)) + "k/h in witch spawns, " +
                "or " + ((int) (360 * spawnSimulator.maxExpectedSpawns)) + "k/h in drops with looting. ");
        System.out.println("with the efficiencies of the four towers being " + Arrays.toString(spawnSimulator.maxExpectedArray));
    }

    public static void main1() {
        WitchSpawnSimulator simulator = new WitchSpawnSimulator(HUT_STATES);
        System.out.println(simulator.compute(-1149, 1272, worldSeed));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 0, 64)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 64, 128)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 128, 192)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 192, 256)));
        System.out.println(Arrays.toString(simulator.expectedSpawns));
    }
}
