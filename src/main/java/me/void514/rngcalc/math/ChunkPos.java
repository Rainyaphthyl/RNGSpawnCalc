package me.void514.rngcalc.math;

public record ChunkPos(int x, int z) implements Comparable<ChunkPos> {
    @Override
    public int x() {
        return x;
    }

    @Override
    public int z() {
        return z;
    }

    @Override
    public int compareTo(ChunkPos other) {
        if (x < other.x) return -1;
        else if (x > other.x) return 1;
        else return Integer.compare(z, other.z);
    }
}
