package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

//TODO
// SPLIT INTO LocateStrongholdCoordinates and FastTravel Tasks
//  - ADD DELIMITERS TO SEPERATE SECTIONS
//  - FRAME SECTIONS
//  - TEST FIND_STRONGHOLD
//  - TEST FAST TRAVEL

public class GoToStrongholdPortalTask extends Task {

    private LocateStrongholdCoordinatesTask _locateCoordsTask;
    private BlockPos _stronghold_coordinates;

    public GoToStrongholdPortalTask(int targetEyes){
        _stronghold_coordinates = null;
        _locateCoordsTask = new LocateStrongholdCoordinatesTask(targetEyes);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_stronghold_coordinates==null){
            _stronghold_coordinates = _locateCoordsTask.getStrongholdCoordinates();
            return _locateCoordsTask;
        } else {
            return new FastTravelTask(_stronghold_coordinates, 300,true);
        }
        /*
            If we don't know where stronghold is, find out where stronghold is.
            If we do know where stronghold is, fast travel there
            If there search it
         */
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof GoToStrongholdPortalTask;
    }

    @Override
    protected String toDebugString() {
        return "Locating Stronghold";
    }

    private static class SearchStrongholdTask extends SearchChunkForBlockTask {

        private final GetToBlockTask _goTask;

        public SearchStrongholdTask(Vec3d travelGoal) {
            super(Blocks.STONE_BRICKS);
            _goTask = new GetToBlockTask(new BlockPos(travelGoal));
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof GoToStrongholdPortalTask.SearchStrongholdTask task) {
                return task._goTask.equals(_goTask) && super.isEqual(other);
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Searching for/around stronghold";
        }

        @Override
        protected Task getWanderTask(AltoClef mod) {
            return _goTask;
        }
    }
}