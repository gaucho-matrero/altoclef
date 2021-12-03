package adris.altoclef.util;

import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.io.IOException;
import java.util.function.Predicate;

public class CubeBounds {
    final BlockPos low;
    final BlockPos high;
    final Predicate<BlockPos> predicate;

    public CubeBounds(final BlockPos startPos, final int sizeX, final int sizeY, final int sizeZ) {
        //Debug.logMessage(startPos.getX() + " " + startPos.getY() + " " + startPos.getZ());
        //Debug.logMessage((startPos.getX() + sizeX) + " " + (startPos.getY() + sizeY) + " " + (startPos.getZ() + sizeZ));
        this.low = new BlockPos(startPos.getX() - 1, startPos.getY() - 1, startPos.getZ() - 1);
        this.high = new BlockPos(low.getX() + sizeX + 1, low.getY() + sizeY + 1, low.getZ() + sizeZ + 1);

        this.predicate = (BlockPos e) ->
            low.getX() <= e.getX() &&
            low.getY() <= e.getY() &&
            low.getZ() <= e.getZ() &&
            e.getX() <= high.getX() &&
            e.getY() <= high.getY() &&
            e.getZ() <= high.getZ();
    }

    public boolean inside(final int x, final int y, final int z) {
        //System.out.println(x + " " + y + " " + z + " vs " + low.getX() + " " + low.getY() + " " + low.getZ() + " || " +  high.getX() + " " + high.getY() + " " + high.getZ() + " = " + (low.getX() <= x) + " " + (low.getY() <= y) + " " + (low.getZ() <= z) + " " + (high.getX() >= x) + " " + (high.getY() >= y) + " " + (high.getZ() >= z));
        return low.getX() <= x && low.getY() <= y && low.getZ() <= z && high.getX() >= x && high.getY() >= y && high.getZ() >= z;
    }//[11:45:25] [Render thread/INFO] (Minecraft) [STDOUT]: -35 4 172 vs -34 4 172 || -26 13 180 = false true true true true true

    public boolean inside(final Vec3i vec) {
        return inside(vec.getX(), vec.getY(), vec.getZ());
    }

    //this would change the predicate original ref i think.
    /*
    public void move(final int x, final int y, final int z) {
        low.add(x, y, z);
        high.add(x, y, z);
    }

    public void move(final Vec3i vec) {
        low.add(vec);
        high.add(vec);
    }*/

    public BlockPos getLow() {
        return this.low;
    }

    public BlockPos getHigh() {
        return this.high;
    }

    public Predicate<BlockPos> getPredicate() {
        return this.predicate;
    }
}
