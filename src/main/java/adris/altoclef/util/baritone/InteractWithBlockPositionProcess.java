package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


import baritone.Baritone;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.pathing.goals.GoalRunAway;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.process.IGetToBlockProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.MovementHelper;
import baritone.process.MineProcess;
import baritone.utils.BaritoneProcessHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;

// Literally a copy of baritone's GetToBlockProcess but we pass a position instead of a BOM

public class InteractWithBlockPositionProcess extends BaritoneProcessHelper {
    private BlockPos _target = null;
    private boolean _rightClickOnArrival;

    private boolean _walkInto;
    private boolean _blockOnTopMustBeRemoved;

    private boolean _cancelRightClick;

    private BlockPos start;

    private int tickCount = 0;
    private int arrivalTickCount = 0;

    public InteractWithBlockPositionProcess(Baritone baritone) {
        super(baritone);
    }

    public void getToBlock(BlockPos target, boolean rightClickOnArrival, boolean blockOnTopMustBeRemoved, boolean walkInto) {
        this.onLostControl();
        _target = target;
        _rightClickOnArrival = rightClickOnArrival;
        _blockOnTopMustBeRemoved = blockOnTopMustBeRemoved;
        _walkInto = walkInto;

        _cancelRightClick = false;

        this.start = this.ctx.playerFeet();
        this.arrivalTickCount = 0;
    }
    public void getToBlock(BlockPos target, boolean rightClickOnArrival) {
        this.getToBlock(target, rightClickOnArrival, false);
    }
    public void getToBlock(BlockPos target, boolean rightClickOnArrival, boolean blockOnTopMustBeRemoved) {
        this.getToBlock(target, rightClickOnArrival, blockOnTopMustBeRemoved, false);
    }

    public boolean isActive() {
        return _target != null;
    }

    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        Goal goal = createGoal(_target);
        if (calcFailed) {
            if ((Boolean)Baritone.settings().blacklistClosestOnFailure.value) {
                this.logDirect("Unable to find any path to " + _target + ", we're screwed...");
                return this.onTick(false, isSafeToCancel);
            } else {
                this.logDirect("Unable to find any path to " + _target + ", canceling GetToBlock");
                if (isSafeToCancel) {
                    this.onLostControl();
                }

                return new PathingCommand(goal, PathingCommandType.CANCEL_AND_SET_GOAL);
            }
        } else {
            int mineGoalUpdateInterval = (Integer)Baritone.settings().mineGoalUpdateInterval.value;

            if (goal.isInGoal(this.ctx.playerFeet()) && goal.isInGoal(this.baritone.getPathingBehavior().pathStart()) && isSafeToCancel) {
                if (!_rightClickOnArrival) {
                    this.onLostControl();
                    return new PathingCommand((Goal)null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }

                if (this.rightClick()) {
                    this.onLostControl();
                    return new PathingCommand((Goal)null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
            }

            return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
    }


    private boolean areAdjacent(BlockPos posA, BlockPos posB) {
        int diffX = Math.abs(posA.getX() - posB.getX());
        int diffY = Math.abs(posA.getY() - posB.getY());
        int diffZ = Math.abs(posA.getZ() - posB.getZ());
        return diffX + diffY + diffZ == 1;
    }

    public synchronized void cancelRightClick() {
        _cancelRightClick = true;
    }

    public synchronized void onLostControl() {
        _target = null;
        this.start = null;
        this.baritone.getInputOverrideHandler().clearAllKeys();
    }

    public String displayName0() {
        return "Get To " + _target;
    }
    private Goal createGoal(BlockPos pos) {
        if (_walkInto) {
            return new GoalTwoBlocks(pos);
        } else {
            return (Goal)(_blockOnTopMustBeRemoved && MovementHelper.isBlockNormalCube(this.baritone.bsi.get0(pos.up())) ? new GoalBlock(pos.up()) : new GoalGetToBlock(pos));
        }
    }

    private boolean rightClick() {
        Optional reachable = RotationUtils.reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());

        this.baritone.getLookBehavior().updateTarget((Rotation)reachable.get(), true);
        this.baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
        System.out.println(this.ctx.player().playerScreenHandler);
        if (!(this.ctx.player().currentScreenHandler instanceof PlayerScreenHandler)) {
            return true;
        }

        if (this.arrivalTickCount++ > 20 || _cancelRightClick) {
            this.logDirect("Right click timed out/cancelled");
            return true;
        } else {
            return false;
        }
    }

}
