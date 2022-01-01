package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.compound.ConstructNetherPortalObsidianTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.EnterNetherPortalTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.GetToYTask;
import adris.altoclef.tasks.movement.GoInDirectionXZTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasks.movement.SearchChunksExploreTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class LocateStrongholdTask extends Task {

    private static final int EYE_THROW_MINIMUM_Y_POSITION = 68;

    private static final int EYE_RETHROW_DISTANCE = 10; // target distance to stronghold guess before rethrowing

    private static final int SECOND_EYE_THROW_DISTANCE = 30; // target distance between first throw and second throw

    private static final int PORTAL_TARGET_HEIGHT = 48; // target height for educated portal 

    private final List<BlockPos> _cachedPortalFrame = new ArrayList<>();
    private final int _targetEyes;
    private EyeDirection _cachedEyeDirection = null;
    private EyeDirection _cachedEyeDirection2 = null;
    private Entity _currentThrownEye = null;
    private Vec3d _strongholdEstimatePos = null;
    private BlockPos _netherGoalPos = null;
    private BlockPos _cachedEducatedPortal = null;
    private BlockPos _educatedPortalStart = null;
    private boolean _netherGoalReached = false;
    private int _portalBuildRange = 2;
    private final TimerGame _throwTimer = new TimerGame(5);

    private SearchStrongholdTask _searchTask;

    public LocateStrongholdTask(int targetEyes) {
        _targetEyes = targetEyes;
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME);
    }

    public boolean isSearching() {
        return _cachedEyeDirection != null;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_strongholdEstimatePos != null) {
            if (_strongholdEstimatePos != null) {
            }
        }
        if (_strongholdEstimatePos == null && mod.getCurrentDimension() != Dimension.OVERWORLD) {
            setDebugState("Going to overworld");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        // Pick up eye if we need to/want to.
        if (mod.getItemStorage().getItemCount(Items.ENDER_EYE) < _targetEyes && mod.getEntityTracker().itemDropped(Items.ENDER_EYE) && 
        !mod.getEntityTracker().entityFound(EyeOfEnderEntity.class)) {
            setDebugState("Picking up dropped ender eye.");
            return new PickupDroppedItemTask(Items.ENDER_EYE, _targetEyes);
        }

        // Handle thrown eye
        if (mod.getEntityTracker().entityFound(EyeOfEnderEntity.class)) {
            if (_currentThrownEye == null || !_currentThrownEye.isAlive()) {
                Debug.logMessage("New eye direction");
                _currentThrownEye = mod.getEntityTracker().getTrackedEntities(EyeOfEnderEntity.class).get(0);
                if (_cachedEyeDirection2 != null) {
                    _cachedEyeDirection = null;
                    _cachedEyeDirection2 = null;
                } else if (_cachedEyeDirection == null){
                    _cachedEyeDirection = new EyeDirection(_currentThrownEye.getPos());
                } else {
                    _cachedEyeDirection2 = new EyeDirection(_currentThrownEye.getPos());
                }
            }
            if (_cachedEyeDirection2 != null) {
                _cachedEyeDirection2.updateEyePos(_currentThrownEye.getPos());
            }
            else if (_cachedEyeDirection != null) {
                _cachedEyeDirection.updateEyePos(_currentThrownEye.getPos());
            }
        
            setDebugState("Waiting for eye to travel.");
            return null;
        }

        // Calculate stronghold position
        if (_cachedEyeDirection2 != null && !mod.getEntityTracker().entityFound(EyeOfEnderEntity.class) && _strongholdEstimatePos == null)
        {
            if (_cachedEyeDirection2.getAngle() >= _cachedEyeDirection.getAngle()) {
                Debug.logMessage("2nd eye thrown at wrong position, or points to different stronghold. Rethrowing");
                _cachedEyeDirection = _cachedEyeDirection2;
                _cachedEyeDirection2 = null;
            } else {
                Vec3d throwOrigin = _cachedEyeDirection.getOrigin();
                Vec3d throwOrigin2 = _cachedEyeDirection2.getOrigin();
                Vec3d throwDelta = _cachedEyeDirection.getDelta();
                Vec3d throwDelta2 = _cachedEyeDirection2.getDelta();


                _strongholdEstimatePos = calculateIntersection(throwOrigin, throwDelta, throwOrigin2, throwDelta2); // stronghold estimate
                Debug.logMessage("Stronghold is at " + (int) _strongholdEstimatePos.getX() + ", " + (int) _strongholdEstimatePos.getZ() + " (" + (int) mod.getPlayer().getPos().distanceTo(_strongholdEstimatePos)+ " blocks away)");
            }
        }


        // Re-throw the eyes after reaching the estimation to get a more accurate estimate of where the stronghold is.
        if (_strongholdEstimatePos != null) {
            if (mod.getPlayer().getPos().distanceTo(_strongholdEstimatePos) < EYE_RETHROW_DISTANCE && mod.getCurrentDimension() == Dimension.OVERWORLD) {
                _strongholdEstimatePos = null;
                _cachedEducatedPortal = null;
                _netherGoalPos = null;
                _netherGoalReached = false;
                _portalBuildRange = 2;
            }
        }

        // If we found our portal frame, we're good.
        if (mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME).size() >= 12) {
            Debug.logMessage("FOUND PORTAL AT: " + mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME).get(0));
            _cachedPortalFrame.clear();
            _cachedPortalFrame.addAll(mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME));
            // We're done.
            return null;
        }

        // Throw the eye since we don't have any eye info.
        if (!mod.getEntityTracker().entityFound(EyeOfEnderEntity.class) && _strongholdEstimatePos == null) {
            if (mod.getCurrentDimension() == Dimension.NETHER) {
                setDebugState("Going to overworld.");
                return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
            }
            if (!mod.getItemStorage().hasItem(Items.ENDER_EYE)) {
                setDebugState("Collecting eye of ender.");
                return TaskCatalogue.getItemTask(Items.ENDER_EYE, 1);
            }

            // First get to a proper throwing height
            if (_cachedEyeDirection == null) {
                setDebugState("Throwing first eye.");
                if (mod.getPlayer().getPos().y < EYE_THROW_MINIMUM_Y_POSITION) {
                    return new GetToYTask(EYE_THROW_MINIMUM_Y_POSITION + 1);
                }
            } else {
                setDebugState("Throwing second eye.");
                double sqDist = mod.getPlayer().squaredDistanceTo(_cachedEyeDirection.getOrigin());
                // If first eye thrown, go perpendicular from eye direction until a good distance away
                if (sqDist < SECOND_EYE_THROW_DISTANCE * SECOND_EYE_THROW_DISTANCE && _cachedEyeDirection != null) {
                    return new GoInDirectionXZTask(_cachedEyeDirection.getOrigin(), _cachedEyeDirection.getDelta().rotateY(MathHelper.PI / 2), 1);
                } else if (mod.getPlayer().getPos().y < 62) {
                    return new GetToYTask(63);
                }
            }
            // Throw it
            if (mod.getSlotHandler().forceEquipItem(Items.ENDER_EYE)) {
                assert MinecraftClient.getInstance().interactionManager != null;
                if (_throwTimer.elapsed()) {
                    if (LookHelper.tryAvoidingInteractable(mod)) {
                        MinecraftClient.getInstance().interactionManager.interactItem(mod.getPlayer(), mod.getWorld(), Hand.MAIN_HAND);
                        //MinecraftClient.getInstance().options.keyUse.setPressed(true);
                        _throwTimer.reset();
                    }
                } else {
                    MinecraftClient.getInstance().interactionManager.stopUsingItem(mod.getPlayer());
                    //MinecraftClient.getInstance().options.keyUse.setPressed(false);
                }
            } else {
                Debug.logWarning("Failed to equip eye of ender to throw.");
            }
            return null;
        } else if (_cachedEyeDirection != null && !_cachedEyeDirection.hasDelta() ||
                   _cachedEyeDirection2 != null && !_cachedEyeDirection2.hasDelta()) {
            setDebugState("Waiting for thrown eye to appear...");
            return null;
        }
        if (_strongholdEstimatePos != null && (_strongholdEstimatePos.distanceTo(mod.getPlayer().getPos()) > 256 || mod.getCurrentDimension() == Dimension.NETHER)) {
            if (_cachedEducatedPortal != null) {
                return new EnterNetherPortalTask(new GetToBlockTask(_cachedEducatedPortal, false), Dimension.OVERWORLD);
            }
            if (_strongholdEstimatePos.distanceTo(_cachedEyeDirection2.getOrigin()) > 400 || 
                mod.getItemStorage().getItemCount(Items.OBSIDIAN) >= 10) {
                if (mod.getCurrentDimension() != Dimension.NETHER) {
                    setDebugState("Going to nether");
                    return new DefaultGoToDimensionTask(Dimension.NETHER);
                }
                if (mod.getItemStorage().getItemCount(Items.OBSIDIAN) < 10 && !_netherGoalReached) {
                    setDebugState("Collecting obsidian");
                    return TaskCatalogue.getItemTask(Items.OBSIDIAN, 10);
                }
                if (_netherGoalPos == null) {
                    _netherGoalPos = new BlockPos(_strongholdEstimatePos.multiply(0.125, 0, 0.125));
                    _netherGoalPos = _netherGoalPos.add(0, PORTAL_TARGET_HEIGHT, 0);
                }
                if (_netherGoalPos.isWithinDistance(mod.getPlayer().getPos(), _portalBuildRange)) {
                    if (_portalBuildRange == 2) {
                        _portalBuildRange = 20;
                    }
                    if (mod.getBlockTracker().getNearestWithinRange(mod.getPlayer().getPos(), _portalBuildRange, Blocks.NETHER_PORTAL) != null) {
                        _cachedEducatedPortal = mod.getBlockTracker().getNearestWithinRange(mod.getPlayer().getPos(), _portalBuildRange, Blocks.NETHER_PORTAL);
                    }
                    if (!_netherGoalReached) {
                        _netherGoalReached = true;
                        Debug.logMessage("Educated coords reached");
                    }
                    if (mod.getBlockTracker().getNearestWithinRange(mod.getPlayer().getPos(), 3, Blocks.OBSIDIAN) != null && _educatedPortalStart == null) {
                        _educatedPortalStart = mod.getBlockTracker().getNearestWithinRange(mod.getPlayer().getPos(), 2, Blocks.OBSIDIAN);
                        _netherGoalPos = _educatedPortalStart;
                    }
                    setDebugState("Building portal");
                    return new ConstructNetherPortalObsidianTask();
                } else {
                    if (_portalBuildRange > 2) {
                    _portalBuildRange = 2;
                    Debug.logMessage("_portalBuildRange set to " + _portalBuildRange);
                    }
                }
                setDebugState("Going to educated travel coords");
                return new GetToBlockTask(_netherGoalPos);
            } 
        }
        
        // Travel to stronghold + search around stronghold if necessary.
        SearchStrongholdTask tryNewSearch = new SearchStrongholdTask(_strongholdEstimatePos);
        if ((_searchTask == null || !_searchTask.equals(tryNewSearch)) && mod.getCurrentDimension() == Dimension.OVERWORLD) {
            Debug.logMessage("New Stronghold search task");
            _searchTask = tryNewSearch;
        }
        setDebugState("Searching for stronghold+portal");
        return _searchTask; 
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LocateStrongholdTask;
    }

    @Override
    protected String toDebugString() {
        return "Locating stronghold";
    }

    public boolean portalFound() {
        return _cachedPortalFrame.size() != 0;
    }

    public List<BlockPos> getPortalFrame() {
        return _cachedPortalFrame;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return portalFound();
    }

    // Represents the direction we need to travel to get to the stronghold.
    private static class EyeDirection {
        private final Vec3d _start;
        private Vec3d _end;

        public EyeDirection(Vec3d startPos) {
            _start = startPos;
        }

        public void updateEyePos(Vec3d endPos) {
            _end = endPos;
        }

        public Vec3d getOrigin() {
            return _start;
        }

        public Vec3d getDelta() {
            if (_end == null) return Vec3d.ZERO;
            return _end.subtract(_start);
        }

        public double getAngle() {
            if (_end == null) return 0;
            return Math.atan2(getDelta().getX(), getDelta().getZ());
        }

        public boolean hasDelta() {
            return _end != null;
        }
    }
    
    static Vec3d calculateIntersection(Vec3d start1, Vec3d direction1, Vec3d start2, Vec3d direction2) {
        Vec3d s1 = start1;
        Vec3d s2 = start2;
        Vec3d d1 = direction1;
        Vec3d d2 = direction2;
        // Solved for s1 + d1 * t1 = s2 + d2 * t2
        double t2 = ( (d1.z * s2.x) - (d1.z * s1.x) - (d1.x * s2.z) + (d1.x * s1.z) ) / ( (d1.x * d2.z) - (d1.z * d2.x) );
        return start2.add(direction2.multiply(t2));
    }

    private static class SearchStrongholdTask extends SearchChunksExploreTask {

        private GetToBlockTask _goTask;

        public SearchStrongholdTask(Vec3d travelGoal) {
            _goTask = new GetToBlockTask(new BlockPos(travelGoal));
        }

        @Override
        protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
            boolean found = mod.getChunkTracker().scanChunk(pos, (block) ->
                    mod.getWorld().getBlockState(block).getBlock() == Blocks.STONE_BRICKS
            );
            if (found) {
                Debug.logMessage("Scanned chunk FOUND!");
            }
            return found;
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof SearchStrongholdTask task) {
                return task._goTask.equals(_goTask);
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