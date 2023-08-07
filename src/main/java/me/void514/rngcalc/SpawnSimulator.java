package me.void514.rngcalc;

import me.void514.rngcalc.math.BlockPos;
import me.void514.rngcalc.math.ChunkPos;
import me.void514.rngcalc.math.PlaneAxis;
import me.void514.rngcalc.witch.WitchHutState;
import me.void514.rngcalc.witch.WitchSpawnSimulator;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpawnSimulator {

    private static final List<WitchHutState> HUT_STATES = new ArrayList<>();

    private float maxExpectedSpawns = -1.0f;
    private int maxX, maxZ;
    private final WitchSpawnSimulator simulator = new WitchSpawnSimulator(HUT_STATES);
    private static final long worldSeed = -9223270471503497825L;
    private static final BlockPos afkPoint = new BlockPos(956, 67, 444);
    private final float[] maxExpectedArray = new float[4];

    private void attemptRegion(int regionX, int regionZ) {
        float expected = simulator.compute(regionX, regionZ, worldSeed);
        if (expected > maxExpectedSpawns) {
            maxX = regionX;
            maxZ = regionZ;
            maxExpectedSpawns = expected;
            System.arraycopy(simulator.expectedSpawns, 0, maxExpectedArray, 0, 4);
            System.out.println("woodland mansion region: [" + maxX + ", " + maxZ + "], "
                    + maxExpectedSpawns + "/gt - " + Arrays.toString(maxExpectedArray));
        }
    }

    /**
     * Spawning range start
     */
    private static final int srsX = (afkPoint.x() >> 4) - 7;
    private static final int srsZ = (afkPoint.z() >> 4) - 7;

    static {
        addWitchHut(55, 22, PlaneAxis.Z, 64);
        addWitchHut(55, 33, PlaneAxis.Z, 64);
        addWitchHut(64, 22, PlaneAxis.X, 64);
        addWitchHut(65, 32, PlaneAxis.Z, 64);
        HUT_STATES.sort(WitchHutState::compareTo);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addWitchHut(int posX, int posZ, PlaneAxis z, int yStart) {
        List<Integer> floorList = new ArrayList<>();
        for (int y = 70; y >= yStart; y -= 3) {
            floorList.add(0, y);
        }
        int[] floors = new int[floorList.size()];
        for (int i = 0; i < floors.length; ++i) {
            floors[i] = floorList.get(i);
        }
        HUT_STATES.add(new WitchHutState(new ChunkPos(posX - srsX, posZ - srsZ), z, yStart, 70, floors));
    }

    static final boolean earlyReturn = false;

    public static void main(String[] args) {
        String pathName;
        if (args != null && args.length > 0) {
            pathName = args[0];
        } else {
            pathName = "configs/NativeFaith.json";
        }
        readConfig(pathName);
        if (earlyReturn) main1();
        if (earlyReturn) return;
        final int MAX_ABS = 3200;
        final long startTime = System.currentTimeMillis();
        final SpawnSimulator spawnSimulator = new SpawnSimulator();
        for (int abs = 1; abs <= MAX_ABS; abs++) {
            for (int x = -abs; x <= abs; x++) {
                spawnSimulator.attemptRegion(x, -abs);
                spawnSimulator.attemptRegion(x, abs);
            }
            for (int z = 1 - abs; z < abs; z++) {
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

    private static void readConfig(String pathName) {
        File file = new File(pathName);
        StringBuilder jsonText = new StringBuilder();
        try (FileReader fileReader = new FileReader(file)) {
            char[] buffer = new char[4096];
            int actualLength;
            while (true) {
                Arrays.fill(buffer, (char) 0);
                actualLength = fileReader.read(buffer);
                if (actualLength > 0) {
                    jsonText.append(buffer, 0, actualLength);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JSONObject jsonObject = new JSONObject(jsonText.toString());
    }
}
