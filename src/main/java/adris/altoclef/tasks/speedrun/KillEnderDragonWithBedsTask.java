package adris.altoclef.tasks.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.construction.PutOutFireTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class KillEnderDragonWithBedsTask extends Task {
    private final Task _whenNotPerchingTask;

    private BlockPos _endPortalTop;
    private Task _positionTask;

    public KillEnderDragonWithBedsTask(IDragonWaiter notPerchingOverride) {
        _whenNotPerchingTask = (Task) notPerchingOverride;
    }

    private static BlockPos locateExitPortalTop(AltoClef mod) {
        if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) return null;
        int height = WorldHelper.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
        if (height != -1) return new BlockPos(0, height, 0);
        return null;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        /*
            If dragon is perching:
                If we're not in position (XZ):
                    Get in position (XZ)
                If there's no bed:
                    If we can't "reach" the top of the pillar:
                        Jump
                    Place a bed
                If the dragon's head hitbox is close enough to the bed:
                    Right click the bed
            Else:
                // Perform "Default Wander" mode and avoid dragon breath.
         */
        if (_endPortalTop == null) {
            _endPortalTop = locateExitPortalTop(mod);
            if (_endPortalTop != null) {
                ((IDragonWaiter) _whenNotPerchingTask).setExitPortalTop(_endPortalTop);
            }
        }

        if (_endPortalTop == null) {
            setDebugState("Searching for end portal top.");
            return new GetToXZTask(0, 0);
        }

        if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
            setDebugState("No dragon found.");
            return new GetToXZTask(0, 0);
        }
        EnderDragonEntity dragon = mod.getEntityTracker().getTrackedEntities(EnderDragonEntity.class).get(0);

        Phase dragonPhase = dragon.getPhaseManager().getCurrent();
        boolean perching = dragonPhase.getType() == PhaseType.LANDING || dragonPhase.isSittingOrHovering();

        if (dragon.getY() < _endPortalTop.getY() + 2) {
            // Dragon is already perched.
            perching = false;
        }

        ((IDragonWaiter) _whenNotPerchingTask).setPerchState(perching);

        // When the dragon is not perching...
        if (_whenNotPerchingTask.isActive() && !_whenNotPerchingTask.isFinished(mod)) {
            setDebugState("Dragon not perching, performing special behavior...");
            return _whenNotPerchingTask;
        }

        if (perching) {
            mod.getFoodChain().shouldStop(true);
            BlockPos targetStandPosition = _endPortalTop.add(-1, -1, 0);
            BlockPos playerPosition = mod.getPlayer().getBlockPos();

            // If we're not positioned (above is OK), go there and make sure we're at the right height.
            if (_positionTask != null && _positionTask.isActive() && !_positionTask.isFinished(mod)) {
                setDebugState("Going to position for bed cycle...");
                return _positionTask;
            }
            if (!WorldHelper.inRangeXZ(WorldHelper.toVec3d(targetStandPosition), mod.getPlayer().getPos(), 1)
                    || playerPosition.getY() < targetStandPosition.getY()
            ) {
                _positionTask = new GetToBlockTask(targetStandPosition);
                setDebugState("Moving to target stand position");
                return _positionTask;
            }

            // We're positioned. Perform bed strats!
            BlockPos bedTargetPosition = _endPortalTop.up();
            boolean bedPlaced = mod.getBlockTracker().blockIsValid(bedTargetPosition, ItemHelper.itemsToBlocks(ItemHelper.BED));
            if (!bedPlaced) {
                if (mod.getWorld().getBlockState(bedTargetPosition).getBlock() == Blocks.FIRE) {
                    setDebugState("Taking out fire.");
                    if (mod.getPlayer().isOnGround()) {
                        // Jump
                        mod.getInputControls().tryPress(Input.JUMP);
                    }
                    return new PutOutFireTask(bedTargetPosition);
                }
                setDebugState("Placing bed");
                // If no bed, place bed.
                // Fire messes up our "reach" so we just assume we're good when we're above a height.
                boolean canPlace = LookHelper.getCameraPos(mod).y > bedTargetPosition.getY();
                //Optional<Rotation> placeReach = LookHelper.getReach(bedTargetPosition.down(), Direction.UP);
                if (canPlace) {
                    // Look at and place!
                    if (mod.getSlotHandler().forceEquipItem(ItemHelper.BED)) {
                        LookHelper.lookAt(mod, bedTargetPosition.down(), Direction.UP);
                        //mod.getClientBaritone().getLookBehavior().updateTarget(placeReach.get(), true);
                        //if (mod.getClientBaritone().getPlayerContext().isLookingAt(bedTargetPosition.down())) {
                        // There could be fire so eh place right away
                        mod.getInputControls().tryPress(Input.CLICK_RIGHT);
                        //}
                    }
                } else {
                    if (mod.getPlayer().isOnGround()) {
                        // Jump
                        mod.getInputControls().tryPress(Input.JUMP);
                    }
                }
            } else {
                setDebugState("Wait for it...");
                // Make sure we're standing on the ground so we don't blow ourselves up lmfao
                if (!mod.getPlayer().isOnGround()) {
                    // Wait to fall
                    return null;
                }
                // Wait for dragon head to be close enough to the bed's head...
                BlockPos bedHead = WorldHelper.getBedHead(mod, bedTargetPosition);
                assert bedHead != null;
                Vec3d headPos = dragon.head.getBoundingBox().getCenter(); // dragon.head.getPos();
                double dist = headPos.distanceTo(WorldHelper.toVec3d(bedHead));
                Debug.logMessage("Dist: " + dist);
                if (dist < BeatMinecraft2Task.getConfig().dragonHeadCloseEnoughClickBedRange) {
                    // Interact with the bed.
                    return new InteractWithBlockTask(bedTargetPosition);
                }
                // Wait for it...
            }
            return null;
        }
        mod.getFoodChain().shouldStop(false);
        // Start our "Not perching task"
        return _whenNotPerchingTask;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getFoodChain().shouldStop(false);
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return super.isFinished(mod);
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof KillEnderDragonWithBedsTask;
    }

    @Override
    protected String toDebugString() {
        return "Bedding the Ender Dragon";
    }
}
