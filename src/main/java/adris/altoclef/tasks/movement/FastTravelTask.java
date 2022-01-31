package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class FastTravelTask extends Task {

    // Collect flint+steel and diamond pickaxe before entering. Or just walk.
    //private final boolean _collectMaterialsBeforeEntering;

    @Override
    protected void onStart(AltoClef mod) {

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

    /*
        function computeNetherCoordinates(BlockPos targetCoords):
            _nethercords = targetCoords/8;
            if block at _nethercords = air or lava: [ belongs in on Tick] IT MAY BE BETTER TO ENSURE THAT THERE ARE NO LAVA BLOCKS IN A 4x4x4 CUBE AROUND THE TARGET
                _nethercords = (_nethercords.x, _nethercords.y+1,_nethercords.z)
     */

    /*
        function getToNetherCoords(BlockPos finalNetherCords):
            baritone get to nether coords
     */
        private Task getToNetherCoords(AltoClef mod, BlockPos finalNetherCoords){
            //Any special checks needed go here
            return new GetToBlockTask(finalNetherCoords);
        }
    /*
        function getJourneySupplies():
            mine origin portal
            mine netherrack if we dont have at least 1.5 stacks
     */
        private Task getJourneySupplies(AltoClef mod){
            return TaskCatalogue.getItemTask(new ItemTarget(Items.OBSIDIAN));
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
