package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.BaritoneHelper;
import adris.altoclef.util.ItemTarget;
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

    private BlockPos _cachedTargetMineBlock;

    private static final float STOP_MINING_ITEM_DROP_IS_CLOSE_ENOUGH_THRESHOLD = 20f;
    private static final float KEEP_MINING_THRESHOLD = 6;

    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // These are the conditions for which we pick up:
        // 1) It is closer to pick up the item than finding the next mining thing
        // 2) The dropped entities contain enough items to finish our task (and they are close enough)

        ClientPlayerEntity player = mod.getPlayer();

        Item[] items = ItemTarget.getItemArray(mod, _itemTargets);

        if (!mod.getEntityTracker().itemDropped(items)) {
            // I mean, yeah we should avoid picking up since no items were found.
            return true;
        }

        ItemEntity closestDrop = mod.getEntityTracker().getClosestItemDrop(player.getPos(), items);

        // Get mining heuristic
        MineProcess mineProc = mod.getClientBaritone().getMineProcess();
        if (mineProc == null) return false;
        PathingCommand c = mineProc.onTick(false, false);
        if (c == null) return false;
        Goal goal = c.goal;
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
                if (ItemTarget.itemEquals(item, t.item)) {
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
        setDebugState("Mining...");
        // Mine
        List<BlockOptionalMeta> boms = new ArrayList<>();

        for (ItemTarget target : _itemTargets) {
            if (mod.getInventoryTracker().targetReached(target)) continue;
            Item item = target.item;
            Block block = Block.getBlockFromItem(item);
            BlockOptionalMeta bom = new BlockOptionalMeta(block);
            boms.add(bom);
        }

        if (!miningCorrectBlocks(mod, boms)) {
            Debug.logInternal("NEW SET OF BLOCKS!");

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
