package me.void514.rngcalc.concurrent;

import me.void514.rngcalc.witch.WitchHutState;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AsyncSpawnSimulator implements Runnable {
    final long worldSeed;
    final AtomicLong regionCounter = new AtomicLong(0L);
    final Semaphore finishAck = new Semaphore(0, true);
    final Semaphore finishFlag = new Semaphore(0);
    private final ReadWriteLock taskLock = new ReentrantReadWriteLock();
    private final ReadWriteLock resultLock = new ReentrantReadWriteLock();
    private final ReadWriteLock outputLock = new ReentrantReadWriteLock();
    private final List<WitchHutState> hutStateList = new ArrayList<>();
    private final List<Thread> threadList = new ArrayList<>();
    private final float[] maxExpectedArray;
    private float maxExpectedSpawns = -1.0F;
    private int bestX = 0;
    private int bestZ = 0;
    private int threadNum = 1;
    private int minRegionAbs = 0;
    private int maxRegionAbs = 1;
    private long regionNum = 1;
    private Writer writer = new PrintWriter(System.out);
    private long nanoElapsed = 0L;

    public AsyncSpawnSimulator(List<WitchHutState> hutStateList, long worldSeed) {
        this.hutStateList.addAll(hutStateList);
        this.hutStateList.sort(WitchHutState::compareTo);
        maxExpectedArray = new float[this.hutStateList.size()];
        this.worldSeed = worldSeed;
    }

    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    public int getThreadNum() {
        Lock readLock = taskLock.readLock();
        try {
            readLock.lock();
            return threadNum;
        } finally {
            readLock.unlock();
        }
    }

    public void setThreadNum(int threadNum) {
        Lock writeLock = taskLock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.threadNum = threadNum;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    public int getMinRegionAbs() {
        Lock readLock = taskLock.readLock();
        try {
            readLock.lock();
            return minRegionAbs;
        } finally {
            readLock.unlock();
        }
    }

    public void setMinRegionAbs(int minRegionAbs) {
        Lock writeLock = taskLock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.minRegionAbs = minRegionAbs;
            resetRegionNum();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    public int getMaxRegionAbs() {
        Lock readLock = taskLock.readLock();
        try {
            readLock.lock();
            return maxRegionAbs;
        } finally {
            readLock.unlock();
        }
    }

    public void setMaxRegionAbs(int maxRegionAbs) {
        Lock writeLock = taskLock.writeLock();
        try {
            writeLock.lockInterruptibly();
            this.maxRegionAbs = maxRegionAbs;
            resetRegionNum();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void run() {
        long timeStart = System.nanoTime();
        Lock readLock = taskLock.readLock();
        try {
            readLock.lockInterruptibly();
            finishFlag.drainPermits();
            finishAck.drainPermits();
            final WitchHutState[] hutStateArray = hutStateList.toArray(new WitchHutState[0]);
            for (int i = 0; i < threadNum; ++i) {
                AsyncRegionTask task = createRegionTask(i, hutStateArray);
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                threadList.add(thread);
            }
            if (threadList.size() != threadNum) {
                throw new IllegalThreadStateException("Invalid thread creation");
            }
            regionCounter.set(0L);
            for (Thread thread : threadList) {
                thread.start();
            }
            final Timer timer = new Timer(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    long progress = regionCounter.get();
                    // This should not be in the output file
                    System.out.println("Progress: " + progress + "/" + regionNum
                            + " = " + String.format("%.2f", 100.0 * progress / regionNum) + "%");
                }
            }, 12000, 15000);
            finishFlag.acquire(threadNum);
            timer.cancel();
            finishAck.release(threadNum);
        } catch (InterruptedException | IllegalThreadStateException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
        long timeEnd = System.nanoTime();
        nanoElapsed = timeEnd - timeStart;
    }

    private void resetRegionNum() {
        long size = maxRegionAbs * 2L + 1L;
        long total = size * size;
        if (minRegionAbs > 0) {
            long core = minRegionAbs * 2L - 1L;
            long inner = core * core;
            regionNum = total - inner;
        } else {
            regionNum = total;
        }
    }

    protected abstract AsyncRegionTask createRegionTask(int i, WitchHutState[] hutStateArray);

    public void reportResult() {
        logInfoLine("Operation ran for " + nanoElapsed / 1000000
                + " milliseconds, and checked " + regionCounter.get() + " woodland mansion regions.");
        logInfoLine("Maximum efficiency is achieved with woodland mansion region: X = " + bestX + ", Z = " + bestZ);
        logInfoLine("The spawning rate is " + maxExpectedSpawns + "/gt, or "
                + ((int) (72 * maxExpectedSpawns)) + "k/h in witch spawns,");
        logInfoLine("  with the rate of the four farms being " + Arrays.toString(maxExpectedArray));
        logInfoLine("The farm efficiency is " + ((int) (144 * maxExpectedSpawns))
                + "k/h in drops without looting, or " + ((int) (360 * maxExpectedSpawns)) + "k/h with looting.");
    }

    void updateResult(int regionX, int regionZ, float expected, float[] expectedSpawns) {
        Lock writeLock = resultLock.writeLock();
        try {
            writeLock.lockInterruptibly();
            if (expected > maxExpectedSpawns) {
                bestX = regionX;
                bestZ = regionZ;
                maxExpectedSpawns = expected;
                System.arraycopy(expectedSpawns, 0, maxExpectedArray, 0, maxExpectedArray.length);
                logInfoLine("woodland mansion region: [" + bestX + ", " + bestZ + "], "
                        + maxExpectedSpawns + "/gt - " + Arrays.toString(maxExpectedArray));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    public void logInfoLine(String message) {
        if (writer instanceof PrintWriter) {
            ((PrintWriter) writer).println(message);
        } else {
            Lock writeLock = outputLock.writeLock();
            try {
                writeLock.lockInterruptibly();
                writer.write(message);
                writer.append('\n');
                writer.flush();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                writeLock.unlock();
                System.out.println(message);
            }
        }
    }
}
