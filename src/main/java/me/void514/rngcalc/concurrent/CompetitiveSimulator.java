package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.witch.WitchHutState;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompetitiveSimulator extends AsyncSpawnSimulator {
    private final ReadWriteLock progressLock = new ReentrantReadWriteLock(true);

    public CompetitiveSimulator(List<WitchHutState> hutStateList, long worldSeed) {
        super(hutStateList, worldSeed);
    }

    @Override
    protected AsyncRegionTask createRegionTask(int i, WitchHutState[] hutStateArray) {
        return new CompetitiveTask(i, this, hutStateArray, progressLock);
    }
}
