package me.void514.rngcalc.witch;

import me.void514.rngcalc.math.ChunkPos;

public record WitchHutState(
        ChunkPos pos, int yStart, int yEnd, int[] yFloors) implements Comparable<WitchHutState> {
    @Override
    public int compareTo(WitchHutState other) {
        return pos.compareTo(other.pos);
    }
}
