package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.*;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.baritone.GoalAnd;
import adris.altoclef.util.csharpisbetter.Timer;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;

/**
 * Here we go
 * the final stretch
 *
 * Until something inevitably fucks up and I gotta go back here to fix it
 * in which case this'll be pretty ironic.
 */
public class KillEnderDragonTask extends Task {

    // Don't accidentally anger endermen lol
    private final Timer _lookDownTimer = new Timer(0.5);

    private final Task _collectBuildMaterialsTask = new MineAndCollectTask(new ItemTarget(Items.END_STONE, 100), new Block[] {Blocks.END_STONE}, MiningRequirement.WOOD);

    private final PunkEnderDragonTask _punkTask = new PunkEnderDragonTask();

    private BlockPos _exitPortalTop;

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().addThrowawayItems(Items.END_STONE);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL);
        // Don't forcefield endermen.
        mod.getConfigState().addForceFieldExclusion(entity -> entity instanceof EndermanEntity || entity instanceof EnderDragonEntity || entity instanceof EnderDragonPart);
        mod.getConfigState().setPreferredStairs(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (_exitPortalTop == null) {
            _exitPortalTop = locateExitPortalTop(mod);
        }


        if (!isRailingOnDragon() && _lookDownTimer.elapsed()) {
            if (mod.getPlayer().isOnGround()) {
                _lookDownTimer.reset();
                mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0f, -90f), true);
            }
        }

        // If there is a portal, enter it.
        if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
            setDebugState("Entering portal to beat the game.");
            return new DoToClosestBlockTask(
                    () -> mod.getPlayer().getPos(),
                    blockPos -> new GetToBlockTask(blockPos.up(), false),
                    pos -> mod.getBlockTracker().getNearestTracking(pos, Blocks.END_PORTAL),
                    Blocks.END_PORTAL
            );
        }

        // If we have no building materials (stone + cobble + end stone), get end stone
        // If there are crystals, suicide blow em up.
        // If there are no crystals, punk the dragon if it's close.
        int MINIMUM_BUILDING_BLOCKS = 1;
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class) && mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.END_STONE) < MINIMUM_BUILDING_BLOCKS || (_collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod))) {
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD)) {
                mod.getConfigState().addProtectedItems(Items.END_STONE);
                setDebugState("Collecting building blocks to pillar to crystals");
                return _collectBuildMaterialsTask;
            }
        } else {
            mod.getConfigState().removeProtectedItems(Items.END_STONE);
        }

        // Blow up the nearest end crystal
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class)) {
            setDebugState("Kamakazeeing crystals");
            return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(),
                    (toDestroy) -> {
                        if (toDestroy.isInRange(mod.getPlayer(), 7)) {
                            mod.getController().attackEntity(mod.getPlayer(), toDestroy);
                        }
                        // Go next to the crystal, arbitrary where we just need to get close.
                        return new GetToBlockTask(toDestroy.getBlockPos().add(1, 0, 0), false);
                    }, EndCrystalEntity.class);
        }

        // Punk dragon
        if (mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
            setDebugState("Punking dragon");
            return _punkTask;
        }
        setDebugState("Couldn't find ender dragon... This can be very good or bad news.");
        return null;
        //return new KillEntitiesTask(EnderDragonEntity.class);
    }


    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof KillEnderDragonTask;
    }

    @Override
    protected String toDebugString() {
        return "Killing Ender Dragon";
    }

    private boolean isRailingOnDragon() {
        return _punkTask.getMode() == Mode.RAILING;
    }

    private BlockPos locateExitPortalTop(AltoClef mod) {
        if (!mod.getChunkTracker().isChunkLoaded(new BlockPos(0, 64, 0))) return null;
        int height = WorldUtil.getGroundHeight(mod, 0, 0, Blocks.BEDROCK);
        if (height != -1) return new BlockPos(0, height, 0);
        return null;
    }

    private enum Mode {
        WAITING_FOR_PERCH,
        RAILING
    }

    private class PunkEnderDragonTask extends Task {

        private final HashMap<BlockPos, Double> _breathCostMap = new HashMap<>();

        private Mode _mode = Mode.WAITING_FOR_PERCH;

        private final Timer _hitHoldTimer = new Timer(0.1);
        private final Timer _hitResetTimer = new Timer(2);

        /*
        private final Timer _dragonPerchChecker = new Timer(3);
        private static final double DRAGON_PERCH_ORBIT_RADIUS = 10;
         */
        private BlockPos _randomWanderPos;

        private PunkEnderDragonTask() {
        }

        public Mode getMode() {
            return _mode;
        }

        private boolean _wasHitting;
        private boolean _wasReleased;

        private void hit(AltoClef mod) {
            mod.getExtraBaritoneSettings().setInteractionPaused(true);
            if (!_wasHitting) {
                _wasHitting = true;
                _wasReleased = false;
                _hitHoldTimer.reset();
                _hitResetTimer.reset();
                Debug.logInternal("HIT");
                mod.getControllerExtras().mouseClickOverride(0, true);
            }
            if (_hitHoldTimer.elapsed()) {
                if (!_wasReleased) {
                    Debug.logInternal("    up");
                    mod.getControllerExtras().mouseClickOverride(0, false);
                    _wasReleased = true;
                }
            }
            if (_hitResetTimer.elapsed() || mod.getPlayer().getAttackCooldownProgress(0) > 0.99) {
                _wasHitting = false;
            }
        }
        private void stopHitting(AltoClef mod) {
            if (_wasHitting) {
                //MinecraftClient.getInstance().options.keyAttack.setPressed(false);
                if (!_wasReleased) {
                    mod.getControllerExtras().mouseClickOverride(0, false);
                    mod.getExtraBaritoneSettings().setInteractionPaused(false);
                    _wasReleased = true;
                }
                _wasHitting = false;
            }
        }


        @Override
        protected void onStart(AltoClef mod) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
        }

        @Override
        protected Task onTick(AltoClef mod) {

            updateBreathCostMap(mod);

            if (!mod.getEntityTracker().entityFound(EnderDragonEntity.class)) {
                setDebugState("No dragon found.");
                return null;
            }
            EnderDragonEntity dragon = mod.getEntityTracker().getTrackedEntities(EnderDragonEntity.class).get(0);

            Phase dragonPhase = dragon.getPhaseManager().getCurrent();
            //Debug.logInternal("PHASE: " + dragonPhase);

            boolean perchingOrGettingReady = dragonPhase.getType() == PhaseType.LANDING || dragonPhase.isSittingOrHovering();

            switch (_mode) {
                case RAILING:

                    if (!perchingOrGettingReady) {
                        Debug.logMessage("Dragon no longer perching.");
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                        _mode = Mode.WAITING_FOR_PERCH;
                        break;
                    }

                    //DamageSource.DRAGON_BREATH
                    Entity head = dragon.partHead;
                    // Go for the head
                    if (head.isInRange(mod.getPlayer(), 7.5) && dragon.ticksSinceDeath <= 1) {
                        // Equip weapon
                        AbstractKillEntityTask.equipWeapon(mod);
                        float hitProg = mod.getPlayer().getAttackCooldownProgress(0);
                        if (hitProg >= 0.99) {
                            //mod.getController().attackEntity(mod.getPlayer(), head);
                        } else {
                            //stopHitting(mod);
                        }
                        // Look torwards da dragon
                        Vec3d targetLookPos = head.getPos().add(0, 3, 0);
                        Rotation targetRotation = RotationUtils.calcRotationFromVec3d(mod.getClientBaritone().getPlayerContext().playerHead(), targetLookPos, mod.getClientBaritone().getPlayerContext().playerRotations());
                        mod.getClientBaritone().getLookBehavior().updateTarget(targetRotation, true);
                        // Also look towards da dragon
                        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                        hit(mod);
                    } else {
                        stopHitting(mod);
                    }
                    if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                        // Set goal to closest block within the pillar that's by the head.
                        if (_exitPortalTop != null) {
                            int bottomYDelta = - 3;
                            BlockPos closest = null;
                            double closestDist = Double.POSITIVE_INFINITY;
                            for (int dx = -2; dx <= 2; ++dx) {
                                for (int dz = -2; dz <= 2; ++dz) {
                                    // We have sort of a rounded circle here.
                                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                                    BlockPos toCheck = _exitPortalTop.add(dx, bottomYDelta, dz);
                                    double distSq = toCheck.getSquaredDistance(head.getPos(), false);
                                    if (distSq < closestDist) {
                                        closest = toCheck;
                                        closestDist = distSq;
                                    }
                                }
                            }
                            if (closest != null) {
                                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                        new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(closest))
                                );
                            }
                        }
                    }
                    setDebugState("Railing on dragon");
                    break;
                case WAITING_FOR_PERCH:
                    stopHitting(mod);
                    if (perchingOrGettingReady) {
                        // We're perching!!
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                        Debug.logMessage("Dragon perching detected. Dabar duosiu į snuki.");
                        _mode = Mode.RAILING;
                        break;
                    }
                    // Run around aimlessly, dodging dragon fire
                    if (_randomWanderPos != null && mod.getPlayer().getBlockPos().isWithinDistance(_randomWanderPos, 2)) {
                        _randomWanderPos = null;
                    }
                    if (_randomWanderPos == null) {
                        _randomWanderPos = getRandomWanderPos(mod);
                        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                    }
                    if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                        mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(
                                new GoalAnd(new AvoidDragonFireGoal(), new GoalGetToBlock(_randomWanderPos))
                        );
                    }
                    setDebugState("Waiting for perch");
                    break;
            }
            return null;
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, false);
            mod.getControllerExtras().mouseClickOverride(0, false);
        }

        @Override
        protected boolean isEqual(Task obj) {
            return obj instanceof PunkEnderDragonTask;
        }

        @Override
        protected String toDebugString() {
            return "Punking the dragon";
        }

        private BlockPos getRandomWanderPos(AltoClef mod) {
            double RADIUS_RANGE = 60;
            double MIN_RADIUS = 7;
            BlockPos pos = null;
            int allowed = 5000;

            while (pos == null) {
                if (allowed-- < 0) {
                    Debug.logWarning("Failed to find random solid ground in end, this may lead to problems.");
                    return null;
                }
                double radius = MIN_RADIUS + (RADIUS_RANGE - MIN_RADIUS) * Math.random();
                double angle = Math.PI * 2 * Math.random();
                int x = (int)(radius*Math.cos(angle)),
                        z = (int)(radius*Math.sin(angle));
                int y = WorldUtil.getGroundHeight(mod, x, z);
                if (y == -1) continue;
                BlockPos check = new BlockPos(x, y, z);
                if (mod.getWorld().getBlockState(check).getBlock() == Blocks.END_STONE) {
                    // We found a spot!
                    pos = check.up();
                }
            }
            return pos;
        }


        private void updateBreathCostMap(AltoClef mod) {
            _breathCostMap.clear();
            double radius = 4;
            for (AreaEffectCloudEntity cloud : mod.getEntityTracker().getTrackedEntities(AreaEffectCloudEntity.class)) {
                Vec3d c = cloud.getPos();
                for (int x = (int)(c.getX() - radius); x <= (int)(c.getX() + radius); ++x) {
                    for (int z = (int)(c.getZ() - radius); z <= (int)(c.getZ() + radius); ++z) {
                        BlockPos p = new BlockPos(x, cloud.getBlockPos().getY(), z);
                        double sqDist = p.getSquaredDistance(c, false);
                        if (sqDist < radius) {
                            double cost = 1000.0 / (sqDist + 1);
                            _breathCostMap.put(p, cost);
                            _breathCostMap.put(p.up(), cost);
                            _breathCostMap.put(p.down(), cost);
                        }
                    }
                }
            }
        }

        private class AvoidDragonFireGoal implements Goal {

            @Override
            public boolean isInGoal(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return !_breathCostMap.containsKey(pos);
            }

            @Override
            public double heuristic(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return _breathCostMap.getOrDefault(pos, 0.0);
            }
        }
    }
}
