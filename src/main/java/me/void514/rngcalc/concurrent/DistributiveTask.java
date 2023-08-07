package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.witch.WitchHutState;

public class DistributiveTask extends AsyncRegionTask {
    public DistributiveTask(int threadIndex, DistributiveSimulator parent, WitchHutState[] hutStateArray) {
        super(threadIndex, parent, hutStateArray);
    }

    @Override
    protected void checkRegionSpawning(int regionX, int regionZ, long progress) {
        if (progress % threadNum == threadIndex) {
            float expected = compute(regionX, regionZ);
            parent.regionCounter.incrementAndGet();
            ++count;
            parent.updateResult(regionX, regionZ, expected, expectedSpawns);
        }
    }
}
