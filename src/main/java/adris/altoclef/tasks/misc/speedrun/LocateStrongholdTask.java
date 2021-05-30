package adris.altoclef.tasks.misc.speedrun;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.GetToYTask;
import adris.altoclef.tasks.GoInDirectionXZTask;
import adris.altoclef.tasks.PickupDroppedItemTask;
import adris.altoclef.tasks.SearchChunksExploreTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.LookUtil;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;


public class LocateStrongholdTask extends Task {

    private static final int EYE_THROW_MINIMUM_Y_POSITION = 68;

    private static final int EYE_RETHROW_DISTANCE = 1000;

    private final List<BlockPos> cachedPortalFrame = new ArrayList<>();
    private final int targetEyes;
    private final Timer throwTimer = new Timer(5);
    private EyeDirection cachedEyeDirection;
    private Entity currentThrownEye;
    private Vec3d lastThrowPos;
    private SearchStrongholdTask searchTask;

    public LocateStrongholdTask(int targetEyes) {
        this.targetEyes = targetEyes;
    }

    public boolean isSearching() {
        return cachedEyeDirection != null;
    }

    public boolean portalFound() {
        return !cachedPortalFrame.isEmpty();
    }

    public List<BlockPos> getPortalFrame() {
        return cachedPortalFrame;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return portalFound();
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // Pick up eye if we need to/want to.
        if (mod.getInventoryTracker().getItemCount(Items.ENDER_EYE) < targetEyes && mod.getEntityTracker().itemDropped(Items.ENDER_EYE)) {
            setDebugState("Picking up dropped ender eye.");
            return new PickupDroppedItemTask(Items.ENDER_EYE, targetEyes, true);
        }

        // Handle thrown eye
        if (mod.getEntityTracker().entityFound(EyeOfEnderEntity.class)) {
            if (currentThrownEye == null || !currentThrownEye.isAlive()) {
                Debug.logMessage("New eye direction");
                currentThrownEye = mod.getEntityTracker().getTrackedEntities(EyeOfEnderEntity.class).get(0);
                cachedEyeDirection = null;
            }
            if (cachedEyeDirection == null) {
                cachedEyeDirection = new EyeDirection(currentThrownEye.getPos());
            } else {
                cachedEyeDirection.updateEyePos(currentThrownEye.getPos());
            }
            setDebugState("Waiting for eye to travel.");
            lastThrowPos = mod.getPlayer().getPos();
            return null;
        }

        // Re-throw the eye after traveling a bit to get a more accurate estimate of where the stronghold is.
        if (lastThrowPos != null) {
            double sqDist = mod.getPlayer().squaredDistanceTo(lastThrowPos);
            if (sqDist > EYE_RETHROW_DISTANCE * EYE_RETHROW_DISTANCE) {
                cachedEyeDirection = null;
            }
        }

        // If we found our portal frame, we're good.
        if (mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME).size() >= 12) {
            Debug.logMessage("FOUND PORTAL AT: " + mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME).get(0));
            cachedPortalFrame.clear();
            cachedPortalFrame.addAll(mod.getBlockTracker().getKnownLocations(Blocks.END_PORTAL_FRAME));
            // We're done.
            return null;
        }

        // Throw the eye since we don't have any eye info.
        if (cachedEyeDirection == null) {

            if (!mod.getInventoryTracker().hasItem(Items.ENDER_EYE)) {
                setDebugState("Collecting eye of ender.");
                return TaskCatalogue.getItemTask("eye_of_ender", 1);
            }

            setDebugState("Throwing eye.");
            // First get to a proper throwing height
            if (mod.getPlayer().getPos().y < EYE_THROW_MINIMUM_Y_POSITION) {
                return new GetToYTask(EYE_THROW_MINIMUM_Y_POSITION + 1);
            }
            // Throw it
            if (mod.getInventoryTracker().equipItem(Items.ENDER_EYE)) {
                assert MinecraftClient.getInstance().interactionManager != null;
                if (throwTimer.elapsed()) {
                    if (LookUtil.tryAvoidingInteractable(mod)) {
                        MinecraftClient.getInstance().interactionManager.interactItem(mod.getPlayer(), mod.getWorld(), Hand.MAIN_HAND);
                        //MinecraftClient.getInstance().options.keyUse.setPressed(true);
                        throwTimer.reset();
                    }
                } else {
                    MinecraftClient.getInstance().interactionManager.stopUsingItem(mod.getPlayer());
                    //MinecraftClient.getInstance().options.keyUse.setPressed(false);
                }
            } else {
                Debug.logWarning("Failed to equip eye of ender to throw.");
            }
            return null;
        }

        // Travel to stronghold + search around stronghold if necessary.
        SearchStrongholdTask tryNewSearch = new SearchStrongholdTask(cachedEyeDirection);
        if (!tryNewSearch.equals(searchTask)) {
            Debug.logMessage("New Stronghold search task");
            searchTask = tryNewSearch;
        }
        setDebugState("Searching for stronghold+portal");
        return searchTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL_FRAME);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof LocateStrongholdTask;
    }

    @Override
    protected String toDebugString() {
        return "Locating stronghold";
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
            return _end.subtract(_start);
        }
    }


    private static class SearchStrongholdTask extends SearchChunksExploreTask {

        private final GoInDirectionXZTask _goTask;

        public SearchStrongholdTask(EyeDirection travelDirection) {
            _goTask = new GoInDirectionXZTask(travelDirection.getOrigin(), travelDirection.getDelta(), 3);
        }

        @Override
        protected boolean isEqual(Task obj) {
            if (obj instanceof SearchStrongholdTask) {
                SearchStrongholdTask task = (SearchStrongholdTask) obj;
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

        @Override
        protected boolean isChunkWithinSearchSpace(AltoClef mod, ChunkPos pos) {
            boolean found = mod.getChunkTracker().scanChunk(pos, (block) -> mod.getWorld().getBlockState(block).getBlock() ==
                                                                            Blocks.STONE_BRICKS);
            if (found) {
                Debug.logMessage("Scanned chunk FOUND!");
            }
            return found;
        }
    }
}