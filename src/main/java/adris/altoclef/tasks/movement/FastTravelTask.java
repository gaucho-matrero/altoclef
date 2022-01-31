package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalObsidianTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class FastTravelTask extends Task {

    // Collect flint+steel and diamond pickaxe before entering. Or just walk.
    //private final boolean _collectMaterialsBeforeEntering;

    @Override
    protected void onStart(AltoClef mod) {
        _goToOverworldTask = new EnterNetherPortalTask(new ConstructNetherPortalObsidianTask(), Dimension.OVERWORLD, goodPos -> throw new NotImplementedException("Check for close enough"));
    }

    @Override
    protected Task onTick(AltoClef mod) {
        return null;
        /*
            if we're in the overworld:
                if we're outside of TRAVEL_THRESHHOLD and NOT forcefully walking:
                    if we need to collect extra flint & steel (or fire charge) AND a diamond pickaxe:
                        collect
                    else
                        walk
                else:
                    force walk
                    walk
                GO TO NETHER
            if we're in the nether:

                if we were building the portal:
                    keep building

                if we drop a diamond pickaxe, pick it up
                if we have no flint and steel & fire charge and dropped any, pick it up
                if we dropped obsidian and have less than 10, pick it up

                if we're close enough to our calculated coordinates:
                    build portal
                go to calculated nether coordinates
            else (we're in the end, highly unlikely but may as well)
                go to overworld
         */

    }

    private int getOverworldThreshold(AltoClef mod) {
        int threshold;
        if (_threshold == null) {
            threshold = mod.getModSettings().getNetherFastTravelWalkingRange();
        } else {
            threshold = _threshold;
        }
        // We should never leave the nether and STILL be outside our walk zone.
        threshold = Math.max((int) (IN_NETHER_CLOSE_ENOUGH_THRESHOLD * 8) + 32, threshold);
        // Nether portals less than 16 blocks point to the same portal (128 overworld), so make sure we don't redo work. Just a redundancy check
        threshold = Math.max(16 * 8, threshold);
        return threshold;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return null;
    }
}
