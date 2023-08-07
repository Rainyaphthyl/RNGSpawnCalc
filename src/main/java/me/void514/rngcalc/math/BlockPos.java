package me.void514.rngcalc.math;

public record BlockPos(int x, int y, int z) {
    @Override
    public int x() {
        return x;
    }

    @Override
    public int y() {
        return y;
    }

    @Override
    public int z() {
        return z;
    }

    public BlockPos up(int off) {
        return new BlockPos(x, y + off, z);
    }
}
