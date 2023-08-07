package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.witch.WitchHutState;

import java.util.ConcurrentModificationException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public class CompetitiveTask extends AsyncRegionTask {
    private final ReadWriteLock progressLock;

    public CompetitiveTask(int threadIndex, CompetitiveSimulator parent, WitchHutState[] hutStateArray, ReadWriteLock progressLock) {
        super(threadIndex, parent, hutStateArray);
        this.progressLock = progressLock;
    }

    @Override
    protected void checkRegionSpawning(int regionX, int regionZ, long progress) {
        Lock readLock = progressLock.readLock();
        boolean flag;
        try {
            readLock.lock();
            AtomicLong counter = parent.regionCounter;
            flag = progress >= counter.get();
            if (flag) {
                if (progress != counter.get()) {
                    throw new ConcurrentModificationException("Region Task " + progress + " has been skipped");
                }
            }
        } catch (ConcurrentModificationException e) {
            parent.finishFlag.release();
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        if (flag) {
            Lock writeLock = progressLock.writeLock();
            try {
                writeLock.lockInterruptibly();
                AtomicLong counter = parent.regionCounter;
                counter.set(progress + 1);
            } catch (InterruptedException e) {
                parent.finishFlag.release();
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
            }
            ++count;
            float expected = compute(regionX, regionZ);
            parent.updateResult(regionX, regionZ, expected, expectedSpawns);
        }
    }
}
