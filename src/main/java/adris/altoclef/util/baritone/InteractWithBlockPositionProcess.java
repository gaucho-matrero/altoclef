package adris.altoclef.util.baritone;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import adris.altoclef.AltoClef;
import adris.altoclef.util.ItemTarget;
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

    private BlockPos start;

    private int tickCount = 0;
    private int arrivalTickCount = 0;

    private ItemTarget _equipTarget = null;

    private AltoClef _mod;

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

        this.start = this.ctx.playerFeet();
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

    public synchronized PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        Goal goal = createGoal(_target);
        if (calcFailed) {
            if (Baritone.settings().blacklistClosestOnFailure.value) {
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

            if (_rightClickOnArrival || (goal.isInGoal(this.ctx.playerFeet()) && goal.isInGoal(this.baritone.getPathingBehavior().pathStart()) && isSafeToCancel)) {
                if (!_rightClickOnArrival) {
                    this.onLostControl();
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }

                if (this.rightClick()) {
                    this.onLostControl();
                    return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
                }
            }

            return new PathingCommand(goal, PathingCommandType.REVALIDATE_GOAL_AND_PATH);
        }
    }

    public synchronized void onLostControl() {
        _target = null;
        this.start = null;
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
            return _blockOnTopMustBeRemoved && MovementHelper.isBlockNormalCube(this.baritone.bsi.get0(pos.up())) ? new GoalBlock(pos.up()) : new GoalGetToBlock(pos);
        }
    }

    private boolean rightClick() {
        _mod.getInventoryTracker().equipItem(_equipTarget);

        Optional<Rotation> reachable;
        if (sideDoesntMatter()) {
            reachable = RotationUtils.reachable(this.ctx.player(), _target, this.ctx.playerController().getBlockReachDistance());

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
                    return false;
                }
            }

        }
        if (reachable.isPresent()) {


            this.baritone.getLookBehavior().updateTarget(reachable.get(), true);
            if (this.baritone.getPlayerContext().isLookingAt(_target)) {
                this.baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                //System.out.println(this.ctx.player().playerScreenHandler);

                if (this.arrivalTickCount++ > 20 || _cancelRightClick) {
                    this.logDirect("Right click timed out/cancelled");
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean sideDoesntMatter() {
        return _interactSide == null;
    }

}
