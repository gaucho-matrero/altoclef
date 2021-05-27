package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
// ... bruh

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

// Literally a copy of baritone's GetToBlockProcess but we pass a position instead of a BOM


public class InteractWithBlockPositionProcess extends BaritoneProcessHelper {
    private final AltoClef _mod;
    // How close we are expected to travel to get to the block. Increases as we fail.
    public int reachCounter;
    //private boolean rightClickOnArrival;
    private BlockPos target;
    private boolean walkInto;
    private boolean blockOnTopMustBeRemoved;
    private boolean cancelRightClick;
    private Direction interactSide;
    private Vec3i interactOffset;
    private int arrivalTickCount;
    private ItemTarget equipTarget;
    private boolean failed;
    private Input interactInput = Input.CLICK_RIGHT;
    private boolean shiftClick;

    public InteractWithBlockPositionProcess(Baritone baritone, AltoClef mod) {
        super(baritone);
        _mod = mod;
    }

    public static Optional<Rotation> getReach(BlockPos target, Direction side) {
        Optional<Rotation> reachable;
        IPlayerContext ctx = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        if (side == null) {
            assert MinecraftClient.getInstance().player != null;
            reachable = RotationUtils.reachable(ctx.player(), target, ctx.playerController().getBlockReachDistance());
        } else {
            Vec3i sideVector = side.getVector();
            Vec3d centerOffset = new Vec3d(0.5 + sideVector.getX() * 0.5, 0.5 + sideVector.getY() * 0.5, 0.5 + sideVector.getZ() * 0.5);

            Vec3d sidePoint = centerOffset.add(target.getX(), target.getY(), target.getZ());

            //reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            reachable = RotationUtils.reachableOffset(ctx.player(), target, sidePoint, ctx.playerController().getBlockReachDistance(),
                                                      false);

            // Check for right angle
            if (reachable.isPresent()) {
                // Note: If sneak, use RotationUtils.inferSneakingEyePosition
                Vec3d camPos = ctx.player().getCameraPosVec(1.0F);
                Vec3d vecToPlayerPos = camPos.subtract(sidePoint);

                double dot = vecToPlayerPos.normalize().dotProduct(new Vec3d(sideVector.getX(), sideVector.getY(), sideVector.getZ()));
                if (dot < 0) {
                    // We're perpendicular and cannot face.
                    return Optional.empty();
                }
            }
        }
        return reachable;
    }

    public void getToBlock(BlockPos target, Direction interactSide, Input interactInput, boolean blockOnTopMustBeRemoved, boolean walkInto,
                           Vec3i interactOffset, boolean shiftClick) {
        this.onLostControl();
        this.target = target;
        this.interactSide = interactSide;
        this.interactInput = interactInput;
        this.blockOnTopMustBeRemoved = blockOnTopMustBeRemoved;
        this.walkInto = walkInto;
        this.interactOffset = interactOffset;
        this.shiftClick = shiftClick;

        cancelRightClick = false;

        failed = false;

        this.arrivalTickCount = 0;

        this.equipTarget = null;

        reachCounter = 0;
    }

    public void getToBlock(BlockPos target, Input interactInput) {
        this.getToBlock(target, interactInput, false);
    }

    public void getToBlock(BlockPos target, Input interactInput, boolean blockOnTopMustBeRemoved) {
        this.getToBlock(target, null, interactInput, blockOnTopMustBeRemoved, false, Vec3i.ZERO, false);
    }

    public boolean isActive() {
        return target != null;
    }

    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {

        if (calcFailed) {
            logDebug("Failed to calculate path, increasing reach to " + reachCounter);
            reachCounter++;
        }

        Goal goal = createGoal(target, reachCounter);

        //if (rightClickOnArrival || (goal.isInGoal(this.ctx.playerFeet()) && goal.isInGoal(this.baritone.getPathingBehavior().pathStart
        // ()) && isSafeToCancel)) {
        if (interactInput == null) {
            if (shiftClick) {
                MinecraftClient.getInstance().options.keySneak.setPressed(false);
            }
            if (goal.isInGoal(this.ctx.player().getBlockPos())) {
                this.onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            } else {
                return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        }

        switch (this.rightClick()) {
            case CANT_REACH:
                return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
            case WAIT_FOR_CLICK:
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            case CLICK_ATTEMPTED:
                this.onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            default:
                return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }

    }

    public synchronized void onLostControl() {
        target = null;
        this.baritone.getInputOverrideHandler().clearAllKeys();
        // ?? Might help? Kinda redundant though.
        this.baritone.getInputOverrideHandler().setInputForceState(interactInput, false);
        if (shiftClick) {
            MinecraftClient.getInstance().options.keySneak.setPressed(false);
        }
    }

    public String displayName0() {
        return "Get To " + target;
    }

    public boolean failed() {
        return failed;
    }

    public synchronized void setInteractEquipItem(ItemTarget item) {
        equipTarget = item;
    }

    private Goal createGoal(BlockPos pos, int reachDistance) {

        boolean sideMatters = !sideDoesntMatter();
        if (sideMatters) {
            Vec3i offs = interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            pos = pos.add(offs);
        }

        if (walkInto) {
            return new GoalTwoBlocks(pos);
        } else {
            if (sideMatters) {
                // Make sure we're on the right side of the block.
                /*
                Vec3i offs = _interactSide.getVector();
                Goal sideGoal;
                if (offs.getY() == 1) {
                    sideGoal = new GoalYLevel(_target.getY() + 1);
                } else if (offs.getY() == -1) {
                    sideGoal = new GoalYLevel(_target.getY() - 1);
                } else {
                    sideGoal = new GoalXZ(_target.getX() + offs.getX(), _target.getZ() + offs.getZ());
                }*/
                Goal sideGoal = new GoalBlockSide(target, interactSide, 1);
                return new GoalAnd(sideGoal, new GoalNear(pos.add(interactOffset), reachDistance));
            } else {
                // TODO: Cleaner method of picking which side to approach from. This is only here for the lava stuff.
                return new GoalNear(pos.add(interactOffset), reachDistance);
            }
        }
    }

    private ClickResponse rightClick() {

        // Don't interact if baritone can't interact.
        if (Baritone.getAltoClefSettings().isInteractionPaused()) return ClickResponse.WAIT_FOR_CLICK;

        Optional<Rotation> reachable = getCurrentReach();
        if (reachable.isPresent()) {
            //Debug.logMessage("Reachable: UPDATE");
            this.baritone.getLookBehavior().updateTarget(reachable.get(), true);
            if (this.baritone.getPlayerContext().isLookingAt(target)) {
                if (equipTarget != null) {
                    if (!_mod.getInventoryTracker().equipItem(equipTarget)) {
                        Debug.logWarning("Failed to equip item: " + Util.arrayToString(equipTarget.getMatches()));
                    }
                }
                this.baritone.getInputOverrideHandler().setInputForceState(interactInput, true);
                if (shiftClick) {
                    MinecraftClient.getInstance().options.keySneak.setPressed(true);
                }
                //System.out.println(this.ctx.player().playerScreenHandler);

                if (this.arrivalTickCount++ > 20 || cancelRightClick) {
                    failed = true;
                    this.logDirect("Right click timed out/cancelled");
                    return ClickResponse.CLICK_ATTEMPTED;
                }
            }
            return ClickResponse.WAIT_FOR_CLICK;
        }
        if (shiftClick) {
            MinecraftClient.getInstance().options.keySneak.setPressed(false);
        }
        return ClickResponse.CANT_REACH;
    }

    private boolean sideDoesntMatter() {
        return interactSide == null;
    }

    public Optional<Rotation> getCurrentReach() {
        return getReach(target, interactSide);
    }

    private enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }
}
