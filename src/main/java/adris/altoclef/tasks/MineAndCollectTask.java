package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.BaritoneHelper;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.Timer;
import adris.altoclef.util.Util;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.BlockUtils;
import baritone.process.MineProcess;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class MineAndCollectTask extends ResourceTask {

    private List<BlockOptionalMeta> _targetBoms = new ArrayList<>();

    public MineAndCollectTask(List<ItemTarget> itemTargets) {
        super(itemTargets);
    }

    public MineAndCollectTask(Item item, int targetCount) {
        super(item, targetCount);
    }
    // Am lazy
    public MineAndCollectTask(Item item) {
        super(item, 99999999);
    }

    private BlockPos _cachedTargetMineBlock;

    private static final float STOP_MINING_ITEM_DROP_IS_CLOSE_ENOUGH_THRESHOLD = 20f;
    private static final float KEEP_MINING_THRESHOLD = 6;

    private PathingCommand _cachedCommand = null;

    private Timer _mineCheck = new Timer(10.0);

    @Override
    protected void onResourceStart(AltoClef mod) {
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // These are the conditions for which we pick up:
        // 1) It is closer to pick up the item than finding the next mining thing
        // 2) The dropped entities contain enough items to finish our task (and they are close enough)

        ClientPlayerEntity player = mod.getPlayer();

        if (!mod.getEntityTracker().itemDropped(Util.toArray(ItemTarget.class, _itemTargets))) {
            // I mean, yeah we should avoid picking up since no items were found.
            return true;
        }

        ItemEntity closestDrop = mod.getEntityTracker().getClosestItemDrop(player.getPos(), Util.toArray(ItemTarget.class, _itemTargets));

        // Get mining heuristic
        MineProcess mineProc = mod.getClientBaritone().getMineProcess();

        if (mineProc == null) return false;

        if (_mineCheck.elapsed()) {

            _mineCheck.reset();

            Debug.logMessage("CHECK MINE PROCESS");
            try {
                onResourceTick(mod);
                _cachedCommand = mineProc.onTick(false, true);
                mineProc.cancel();
            } catch (NullPointerException e) {
                return false;
            }
        }
        if (_cachedCommand == null) return false;
        Goal goal = _cachedCommand.goal;
        if (goal == null) return false;
        double playerMineCost = goal.heuristic(player.getBlockPos());

        // Get drop heuristic
        double playerPickupCost = BaritoneHelper.calculateGenericHeuristic(player.getPos(), closestDrop.getPos());

        if (playerPickupCost < playerMineCost && playerMineCost > KEEP_MINING_THRESHOLD) {
            // It's better to get the drop, considering we might have to go out of our way to mine.
            return false;
        } else {
            // It MIGHT be better to keep mining.
            // HOWEVER: We might have enough items laying around so that we can just pick them up.
            Item item = closestDrop.getStack().getItem();
            int count = closestDrop.getStack().getCount();

            int currentCount = mod.getInventoryTracker().getItemCount(item);
            // Calculate target
            int target = 0;
            for (ItemTarget t : _itemTargets) {
                if (t.matches(item)) {
                    target = t.targetCount;
                    break;
                }
            }

            int howManyStillNeeded = target - currentCount;

            // We have enough in the stack and the stack is still close enough.
            //noinspection RedundantIfStatement
            if (count > howManyStillNeeded && playerPickupCost < STOP_MINING_ITEM_DROP_IS_CLOSE_ENOUGH_THRESHOLD) {
                return false;
            }

            return true;
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // Mine
        List<BlockOptionalMeta> boms = new ArrayList<>();

        StringBuilder state = new StringBuilder();
        state.append(" ----- ");
        for (ItemTarget target : _itemTargets) {
            if (mod.getInventoryTracker().targetReached(target)) continue;
            state.append("Need ").append(target.toString()).append(" ----- ");
            for (Item item : target.getMatches()) {
                Block block = Block.getBlockFromItem(item);
                BlockOptionalMeta bom = new BlockOptionalMeta(block);
                boms.add(bom);
            }
        }
        setDebugState(state.toString());

        if (!miningCorrectBlocks(mod, boms)) {
            Debug.logInternal("NEW SET OF BLOCKS TO MINE!");

            BlockOptionalMeta[] bomsArray = new BlockOptionalMeta[boms.size()];
            boms.toArray(bomsArray);

            mod.getClientBaritone().getMineProcess().cancel();
            mod.getClientBaritone().getMineProcess().mine(bomsArray);

            _targetBoms = boms;
        }

        return null;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getMineProcess().cancel();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof MineAndCollectTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Mine And Collect";
    }

    private boolean miningCorrectBlocks(AltoClef mod, List<BlockOptionalMeta> targetBoms) {
        MineProcess p = mod.getClientBaritone().getMineProcess();
        // If we're not mining, of course we're not correct!
        if (!p.isActive()) return false;

        // Our target boms much match
        // TODO: Make something more thorough. Consider what happens when there are different types of stone...
        if (_targetBoms.size() != targetBoms.size()) return false;
        for (int i = 0; i < _targetBoms.size(); ++i) {
            BlockOptionalMeta us = _targetBoms.get(i);
            BlockOptionalMeta them = targetBoms.get(i);
            if (!us.matches(them.getBlock())) return false;
            if (!us.matches(them.getAnyBlockState())) return false;
        }
        return true;
    }

}
