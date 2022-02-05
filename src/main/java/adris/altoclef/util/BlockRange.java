package adris.altoclef.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class BlockRange {
    public BlockPos start;
    public BlockPos end;

    // For deserialization
    private BlockRange() {}

    public BlockRange(BlockPos start, BlockPos end) {
        this.start = start;
        this.end = end;
    }

    public boolean contains(BlockPos pos) {
        return (start.getX() <= pos.getX() && pos.getX() <= end.getX() &&
                start.getZ() <= pos.getZ() && pos.getZ() <= end.getZ() &&
                start.getY() <= pos.getY() && pos.getY() <= end.getY());
    }

    @JsonIgnore
    public BlockPos getCenter() {
        BlockPos sum = start.add(end);
        return new BlockPos(sum.getX() / 2, sum.getY() / 2, sum.getZ() / 2);
    }

    public String toString() {
        return "[" + start.toShortString() + " -> " + end.toShortString() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockRange that = (BlockRange) o;
        return Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
