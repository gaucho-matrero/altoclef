package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.ItemTarget;
import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.utils.BaritoneProcessHelper;

import java.util.Optional;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

// Literally a copy of baritone's GetToBlockProcess but we pass a position instead of a BOM

public class InteractWithBlockPositionProcess extends BaritoneProcessHelper {
    private BlockPos _target = null;
    private boolean _rightClickOnArrival;

    private boolean _walkInto;
    private boolean _blockOnTopMustBeRemoved;

    private boolean _cancelRightClick;

    private Direction _interactSide;

    private int arrivalTickCount = 0;

    private ItemTarget _equipTarget = null;

    private AltoClef _mod;

    private boolean _failed;

    public InteractWithBlockPositionProcess(Baritone baritone, AltoClef mod) {
        super(baritone); _mod = mod;
    }

    public void getToBlock(BlockPos target, Direction interactSide, boolean rightClickOnArrival, boolean blockOnTopMustBeRemoved, boolean walkInto) {
        this.onLostControl();
        _target = target;
        _interactSide = interactSide;
        _rightClickOnArrival = rightClickOnArrival;
        _blockOnTopMustBeRemoved = blockOnTopMustBeRemoved;
        _walkInto = walkInto;

        _cancelRightClick = false;

        _failed = false;

        this.arrivalTickCount = 0;
    }
    public void getToBlock(BlockPos target, boolean rightClickOnArrival) {
        this.getToBlock(target, rightClickOnArrival, false);
    }
    public void getToBlock(BlockPos target, boolean rightClickOnArrival, boolean blockOnTopMustBeRemoved) {
        this.getToBlock(target, null, rightClickOnArrival, blockOnTopMustBeRemoved, false);
    }

    public boolean isActive() {
        return _target != null;
    }

    public boolean failed() {
        return _failed;
    }

    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        Goal goal = createGoal(_target);

        //if (_rightClickOnArrival || (goal.isInGoal(this.ctx.playerFeet()) && goal.isInGoal(this.baritone.getPathingBehavior().pathStart()) && isSafeToCancel)) {
        if (!_rightClickOnArrival) {
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
        _target = null;
        this.baritone.getInputOverrideHandler().clearAllKeys();
    }

    public synchronized void setInteractEquipItem(ItemTarget item) {
        _equipTarget = item;
    }

    public String displayName0() {
        return "Get To " + _target;
    }
    private Goal createGoal(BlockPos pos) {

        if (!sideDoesntMatter()) {
            Vec3i offs = _interactSide.getVector();
            if (offs.getY() == -1) {
                // If we're below, place ourselves two blocks below.
                offs = offs.down();
            }
            pos = pos.add(offs);
        }

        if (_walkInto) {
            return new GoalTwoBlocks(pos);
        } else {
            return new GoalNear(pos, 1);
            //return new GoalGetToBlock(pos);
            // Is the following better? Commented out was the old way copied from baritone.
            //return new _blockOnTopMustBeRemoved && MovementHelper.isBlockNormalCube(this.baritone.bsi.get0(pos.up())) ? new GoalBlock(pos.up()) : new GoalGetToBlock(pos);
        }
    }

    private ClickResponse rightClick() {

        Optional<Rotation> reachable;
        if (sideDoesntMatter()) {
            reachable = RotationUtils.reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            if (!reachable.isPresent()) {
                Debug.logMessage("Fail: " + _target);
            }
        } else {
            Vec3i sideVector = _interactSide.getVector();
            Vec3d centerOffset = new Vec3d(0.5 + sideVector.getX() * 0.5, 0.5 + sideVector.getY() * 0.5, 0.5 + sideVector.getZ() * 0.5);

            Vec3d sidePoint = centerOffset.add(_target.getX(), _target.getY(), _target.getZ());

            //reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());
            reachable = RotationUtils.reachableOffset(ctx.player(), _target, sidePoint, ctx.playerController().getBlockReachDistance(), false);

            // Check for right angle
            if (reachable.isPresent()) {
                // Note: If sneak, use RotationUtils.inferSneakingEyePosition
                Vec3d camPos = ctx.player().getCameraPosVec(1.0F);
                Vec3d vecToPlayerPos = camPos.subtract(sidePoint);

                double dot = vecToPlayerPos.normalize().dotProduct(new Vec3d(sideVector.getX(), sideVector.getY(), sideVector.getZ()));
                if (dot < 0) {
                    // We're perpendicular and cannot face.
                    Debug.logMessage("DOT PRODUCT FAIL: " + dot);
                    return ClickResponse.CANT_REACH;
                }
            } else {
                Debug.logMessage("Fail: " + _target + " : " + sidePoint);
            }

        }
        if (reachable.isPresent()) {
            //Debug.logMessage("Reachable: UPDATE");
            this.baritone.getLookBehavior().updateTarget(reachable.get(), true);
            if (this.baritone.getPlayerContext().isLookingAt(_target)) {
                if (_equipTarget != null) _mod.getInventoryTracker().equipItem(_equipTarget);
                this.baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                //System.out.println(this.ctx.player().playerScreenHandler);

                if (this.arrivalTickCount++ > 20 || _cancelRightClick) {
                    _failed = true;
                    this.logDirect("Right click timed out/cancelled");
                    return ClickResponse.CLICK_ATTEMPTED;
                }
            }
            return ClickResponse.WAIT_FOR_CLICK;
        }
        return ClickResponse.CANT_REACH;
    }

    private enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }

    private boolean sideDoesntMatter() {
        return _interactSide == null;
    }

}
