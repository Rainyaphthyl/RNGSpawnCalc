package me.void514.rngcalc.witch;

import me.void514.rngcalc.math.ChunkPos;
import me.void514.rngcalc.math.PlaneAxis;

public record WitchHutState(
        ChunkPos pos, PlaneAxis axis, int yStart, int yEnd, int[] yFloors) implements Comparable<WitchHutState> {
    @Override
    public int compareTo(WitchHutState other) {
        return pos.compareTo(other.pos);
    }

    public int chunkIndex() {
        return 15 * pos.x() + pos.z();
    }
}
