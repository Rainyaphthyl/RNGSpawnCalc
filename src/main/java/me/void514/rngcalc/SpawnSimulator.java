package me.void514.rngcalc;

import me.void514.rngcalc.math.ChunkPos;
import me.void514.rngcalc.math.PlaneAxis;
import me.void514.rngcalc.witch.WitchHutState;
import me.void514.rngcalc.witch.WitchSpawnSimulator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpawnSimulator {

    private static final List<WitchHutState> hutStateList = new ArrayList<>();
    private static final int WORLD_MAX_ABS = (29999984 / 16) / 80;
    private static int maxAbs = 3200;
    private float maxExpectedSpawns = -1.0f;
    private int bestX, bestZ;
    private final WitchSpawnSimulator simulator = new WitchSpawnSimulator(hutStateList);
    private static long worldSeed = 0L;
    /**
     * Spawning range start
     */
    private static int srsX = 0;
    private static int srsZ = 0;
    private final float[] maxExpectedArray = new float[4];

    private void attemptRegion(int regionX, int regionZ) {
        float expected = simulator.compute(regionX, regionZ, worldSeed);
        if (expected > maxExpectedSpawns) {
            bestX = regionX;
            bestZ = regionZ;
            maxExpectedSpawns = expected;
            System.arraycopy(simulator.expectedSpawns, 0, maxExpectedArray, 0, 4);
            System.out.println("woodland mansion region: [" + bestX + ", " + bestZ + "], "
                    + maxExpectedSpawns + "/gt - " + Arrays.toString(maxExpectedArray));
        }
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
        hutStateList.add(new WitchHutState(new ChunkPos(posX - srsX, posZ - srsZ), z, yStart, 70, floors));
    }

    static final boolean earlyReturn = false;

    public static void main(String[] args) {
        String configPath;
        if (args != null && args.length > 0) {
            configPath = args[0];
        } else {
            configPath = "configs/NativeFaith.json";
        }
        initFromConfig(configPath);
        System.out.println("Searching according to \"" + configPath + "\"...");
        if (earlyReturn) main1();
        if (earlyReturn) return;
        final long startTime = System.currentTimeMillis();
        final SpawnSimulator spawnSimulator = new SpawnSimulator();
        for (int abs = 1; abs <= maxAbs; abs++) {
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
                + spawnSimulator.bestX + ", Z = " + spawnSimulator.bestZ);
        System.out.println("The efficiency achieved is " + spawnSimulator.maxExpectedSpawns + "/gt, " +
                "or " + ((int) (72 * spawnSimulator.maxExpectedSpawns)) + "k/h in witch spawns, " +
                "or " + ((int) (360 * spawnSimulator.maxExpectedSpawns)) + "k/h in drops with looting. ");
        System.out.println("with the efficiencies of the four towers being " + Arrays.toString(spawnSimulator.maxExpectedArray));
    }

    public static void main1() {
        WitchSpawnSimulator simulator = new WitchSpawnSimulator(hutStateList);
        System.out.println(simulator.compute(-1149, 1272, worldSeed));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 0, 64)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 64, 128)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 128, 192)));
        System.out.println(Arrays.toString(Arrays.copyOfRange(simulator.initialStateChances, 192, 256)));
        System.out.println(Arrays.toString(simulator.expectedSpawns));
    }

    private static void initFromConfig(String pathName) {
        File file = new File(pathName);
        StringBuilder jsonText = new StringBuilder();
        try (FileReader fileReader = new FileReader(file)) {
            char[] buffer = new char[4096];
            int actualLength;
            while (true) {
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
        try {
            JSONObject jsonGlobal = new JSONObject(jsonText.toString());
            JSONObject jsonGameInfo = jsonGlobal.getJSONObject("gameInfo");
            JSONObject jsonAfkPoint = jsonGameInfo.getJSONObject("afkPoint");
            int x = jsonAfkPoint.getInt("x");
            int z = jsonAfkPoint.getInt("z");
            srsX = (x >> 4) - 7;
            srsZ = (z >> 4) - 7;
            worldSeed = jsonGameInfo.getLong("worldSeed");
            JSONArray jsonHutList = jsonGameInfo.getJSONArray("witchHutList");
            if (jsonHutList.length() != 4) {
                throw new JSONException("witchHutList.length() != 4");
            }
            for (int i = 0; i < 4; ++i) {
                JSONObject jsonWitchHut = jsonHutList.getJSONObject(i);
                int cx = jsonWitchHut.getInt("cx");
                int cz = jsonWitchHut.getInt("cz");
                PlaneAxis axis = jsonWitchHut.getEnum(PlaneAxis.class, "axis");
                int yStart = jsonWitchHut.getInt("yStart");
                addWitchHut(cx, cz, axis, yStart);
            }
            hutStateList.sort(WitchHutState::compareTo);
            JSONObject jsonOption = jsonGlobal.optJSONObject("option");
            if (jsonOption != null) {
                maxAbs = jsonOption.optInt("maxRegionAbs", maxAbs);
            }
            if (maxAbs > WORLD_MAX_ABS) {
                maxAbs = WORLD_MAX_ABS;
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
