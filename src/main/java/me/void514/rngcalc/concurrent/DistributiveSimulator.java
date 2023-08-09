package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.witch.WitchHutState;

import java.util.List;

public class DistributiveSimulator extends AsyncSpawnSimulator {
    public DistributiveSimulator(List<WitchHutState> hutStateList, long worldSeed) {
        super(hutStateList, worldSeed);
    }

    @Override
    protected AsyncRegionTask createRegionTask(int i, WitchHutState[] hutStateArray) {
        return new DistributiveTask(i, this, hutStateArray);
    }
}
