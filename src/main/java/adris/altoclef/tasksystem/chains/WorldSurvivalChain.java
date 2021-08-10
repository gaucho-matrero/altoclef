package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.EscapeFromLavaTask;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.utils.input.Input;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorldSurvivalChain extends SingleTaskChain {

    private final TimerGame _wasInLavaTimer = new TimerGame(1);
    private boolean _wasAvoidingDrowning;
    private boolean _wasStuckInPortal;
    private int _portalStuckTimer;

    private final HashSet<BlockPos> _dontQuickPlaceHere = new HashSet<>();
    private final TimerGame _dontQuickPlaceResetTimer = new TimerGame(30);

    private static final Item[] DEATH_HOLE_FILL_ITEMS = new Item[]{Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK};

    public WorldSurvivalChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;

        // Drowning
        handleDrowning(mod);

        // Lava Escape
        if (isInLavaOhShit(mod)) {
            mod.getBehaviour().allowWalkThroughLava(true);
            setTask(new EscapeFromLavaTask());
            return 100;
        }
        mod.getBehaviour().allowWalkThroughLava(false);

        // Portal stuck
        if (isStuckInNetherPortal(mod)) {
            _portalStuckTimer++;
            _wasStuckInPortal = true;
        } else {
            _portalStuckTimer = 0;
        }
        if (_portalStuckTimer > 10) {
            // We're stuck inside a portal, so get out.
            // Don't allow breaking while we're inside the portal.
            setTask(new SafeRandomShimmyTask());
            return 55;
        }
        _wasStuckInPortal = false;

        if (mod.getModSettings().shouldDeathHoleFill()) {
            // Extra patch:
            // If we're breaking a block above us, don't quick place there!
            if (_dontQuickPlaceResetTimer.elapsed()) {
                _dontQuickPlaceResetTimer.reset();
                _dontQuickPlaceHere.clear();
            }
            if (mod.getControllerExtras().isBreakingBlock()) {
                BlockPos breaking = mod.getControllerExtras().getBreakingBlockPos();
                if (breaking.getY() > mod.getPlayer().getBlockPos().getY() + 1) {
                    _dontQuickPlaceHere.add(breaking);
                }
            }

            // "Death hole" prevention
            if (shouldDeathHoleProtect(mod)) {
                PlaceTarget prevent = getDeathHolePreventionSpot(mod);
                if (prevent != null) {
                    setTask(new InteractWithBlockTask(new ItemTarget(DEATH_HOLE_FILL_ITEMS, 1), prevent.direction, prevent.place, false));
                    return 60;
                }
            }
        }

        return Float.NEGATIVE_INFINITY;
    }

    private boolean shouldDeathHoleProtect(AltoClef mod) {

        // TODO: `hasStructureBlocks` command, taking into account protected items.
        if (!mod.getInventoryTracker().hasItem(DEATH_HOLE_FILL_ITEMS)) {
            return false;
        }

        // the "Death Hole" scenario is when we're digging down and mobs dive down to trap and kill us.
        // Not fun.
        Entity player = mod.getPlayer();
        // Mobs above us and < 5 blocks away horizontally
        Entity closest = Util.minItem(
                mod.getEntityTracker().getHostiles().stream().filter(
                        entity -> entity.getPos().y > player.getPos().y + 2
                        && Math.abs(entity.getPos().x - player.getPos().x) < 5
                        && Math.abs(entity.getPos().z - player.getPos().z) < 5).collect(Collectors.toList()),
                entity -> entity.squaredDistanceTo(player));
        boolean mobsClose = closest != null;
        if (mobsClose) {
            // Check if we're in a hole
            // Holes exist if
            // - there's a bunch of air above us and mobs above us.
            for (int dy = 0; dy < 10; ++dy) {
                BlockPos check = player.getBlockPos().add(0, dy, 0);
                if (check.getY() >= closest.getBlockPos().getY()) break;
                if (WorldUtil.isSolid(mod, check)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static class PlaceTarget {
        public PlaceTarget(BlockPos place, Vec3i direction) {
            this.place = place;
            this.direction = Objects.requireNonNull(Direction.fromVector(direction.getX(), direction.getY(), direction.getZ())).getOpposite();
        }

        public BlockPos place;
        public Direction direction;
    }
    private PlaceTarget getDeathHolePreventionSpot(AltoClef mod) {
        final Vec3i[] offsToCheck = new Vec3i[]{
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(0, 0, -1),
                new Vec3i(0, 0, 1)
        };
        for (BlockPos check = mod.getPlayer().getBlockPos().add(0, 2, 0); check.getY() < mod.getPlayer().getBlockPos().getY() + 7; check = check.up()) {
            if (_dontQuickPlaceHere.contains(check)) continue;
            for (Vec3i offs : offsToCheck) {
                BlockPos placeAgainst = check.add(offs);
                if (WorldUtil.isSolid(mod, placeAgainst)) {
                    return new PlaceTarget(
                            placeAgainst,
                            offs);
                }
            }
        }
        return null;
    }

    private void handleDrowning(AltoClef mod) {
        // Swim
        boolean avoidedDrowning = false;
        if (mod.getModSettings().shouldAvoidDrowning()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                if (mod.getPlayer().isTouchingWater() && mod.getPlayer().getAir() < mod.getPlayer().getMaxAir()) {
                    // Swim up!
                    mod.getInputControls().hold(Input.JUMP);
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    avoidedDrowning = true;
                    _wasAvoidingDrowning = true;
                }
            }
        }
        // Stop swimming up if we just swam.
        if (_wasAvoidingDrowning && !avoidedDrowning) {
            _wasAvoidingDrowning = false;
            mod.getInputControls().release(Input.JUMP);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
    }

    private boolean isInLavaOhShit(AltoClef mod) {
        if (mod.getPlayer().isInLava() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            _wasInLavaTimer.reset();
            return true;
        }
        return mod.getPlayer().isOnFire() && !_wasInLavaTimer.elapsed();
    }

    private boolean isStuckInNetherPortal(AltoClef mod) {
        // We're stuck if we're inside a portal, are breaking it and can ONLY look at the portal.
        boolean inPortal = mod.getBlockTracker().blockIsValid(mod.getPlayer().getBlockPos(), Blocks.NETHER_PORTAL);
        boolean breakingPortal = mod.getControllerExtras().isBreakingBlock() && mod.getBlockTracker().blockIsValid(mod.getControllerExtras().getBreakingBlockPos(), Blocks.NETHER_PORTAL);
        // If we're looking at he nether portal
        assert MinecraftClient.getInstance().crosshairTarget != null;
        if (MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult currentLook = (BlockHitResult) MinecraftClient.getInstance().crosshairTarget;
            boolean collidingWithportal = (currentLook != null && mod.getBlockTracker().blockIsValid(currentLook.getBlockPos(), Blocks.NETHER_PORTAL));
            return inPortal && collidingWithportal && (breakingPortal || _wasStuckInPortal);
        }
        return false;
    }

    @Override
    public String getName() {
        return "Misc World Survival Chain";
    }

    @Override
    public boolean isActive() {
        // Always check for survival.
        return true;
    }
}
