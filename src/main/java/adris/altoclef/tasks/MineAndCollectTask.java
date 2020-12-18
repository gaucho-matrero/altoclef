package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.progresscheck.DistanceProgressChecker;
import adris.altoclef.util.progresscheck.IProgressChecker;
import adris.altoclef.util.progresscheck.LinearProgressChecker;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.PathingCommand;
import baritone.api.utils.BlockOptionalMeta;
import baritone.process.MineProcess;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MineAndCollectTask extends ResourceTask {

    private List<BlockOptionalMeta> _targetBoms = new ArrayList<>();

    private final MiningRequirement _requirement;

    private BlockPos _cachedTargetMineBlock;

    private static final float STOP_MINING_ITEM_DROP_IS_CLOSE_ENOUGH_THRESHOLD = 20f;
    private static final float KEEP_MINING_THRESHOLD = 6;

    private PathingCommand _cachedCommand = null;

    private final Timer _mineCheck = new Timer(10.0);

    private final IProgressChecker<Double> _mineProgressChecker = new LinearProgressChecker(4, 0.01f);
    private final DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(5, 0.1f);

    public MineAndCollectTask(List<ItemTarget> itemTargets, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;

    }

    public MineAndCollectTask(ItemTarget target, MiningRequirement requirement) {
        this(Collections.singletonList(target), requirement);
    }

    public MineAndCollectTask(Item item, int targetCount, MiningRequirement requirement) {
        super(item, targetCount);
        _requirement = requirement;
    }
    // Am lazy
    public MineAndCollectTask(Item item, MiningRequirement requirement) {
        super(item, 99999999);
        _requirement = requirement;
    }

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

        // If we don't have the proper tool, satisfy it.
        if (!mod.getInventoryTracker().miningRequirementMet(_requirement)) {
            return new SatisfyMiningRequirementTask(_requirement);
        }

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
            Debug.logInternal("NEW SET OF BLOCKS TO MINE");

            BlockOptionalMeta[] bomsArray = new BlockOptionalMeta[boms.size()];
            boms.toArray(bomsArray);

            mod.getClientBaritone().getMineProcess().cancel();
            mod.getClientBaritone().getMineProcess().mine(bomsArray);

            _targetBoms = boms;

            _mineProgressChecker.reset();
            _distanceProgressChecker.reset(mod.getPlayer().getPos());
        }

        boolean failed = false;
        boolean mining = mod.getController().isBreakingBlock();

        if (mining) {
            double progress = mod.getControllerExtras().getBreakingBlockProgress();
            _mineProgressChecker.setProgress(progress);
            if (_mineProgressChecker.failed()) {
                Debug.logMessage("Failed to mine block. Blacklisting.");
                failed = true;
            }
            // Reset other checker (independent, one blocks another)
            _distanceProgressChecker.reset(mod.getPlayer().getPos());
        } else {
            _distanceProgressChecker.setProgress(mod.getPlayer().getPos());
            if (_distanceProgressChecker.failed()) {
                Debug.logMessage("Failed to make progress moving to our block. Blacklisting.");
                failed = true;
            }
            // Reset other checker (independent, one blocks another)
            _mineProgressChecker.reset();
        }
        if (failed) {
            try {
                blacklistCurrentTarget(mod);
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
                // oof
                Debug.logMessage("oof, this might mean something was imported or compiled incorrectly :(");
                e.printStackTrace();
            }
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
            //if (!us.matches(them.getAnyBlockState())) return false;
        }
        return true;
    }

    /*
    private void addToMineProcessBlacklist(AltoClef mod, BlockPos block) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // This object will be used for access
        //UserClass userClassObj = new UserClass();
        MineProcess proc = mod.getClientBaritone().getMineProcess();
        // Single Field Access
        Field f = MineProcess.class.getDeclaredField("blacklist");
        // Set flag true for accessing private field
        f.setAccessible(true);
        Object list = f.get(proc);

        Method addItem = list.getClass().getDeclaredMethod("add", Object.class);
        addItem.setAccessible(true);
        addItem.invoke(list, block);
    }
     */

    private void blacklistCurrentTarget(AltoClef mod) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        // This object will be used for access
        //UserClass userClassObj = new UserClass();
        MineProcess proc = mod.getClientBaritone().getMineProcess();
        Field blacklistField = MineProcess.class.getDeclaredField("blacklist");
        blacklistField.setAccessible(true);
        Field knownField = MineProcess.class.getDeclaredField("knownOreLocations");
        knownField.setAccessible(true);

        List<BlockPos> blackList = (List<BlockPos>) blacklistField.get(proc);
        List<BlockPos> knownLocations = (List<BlockPos>) knownField.get(proc);

        blackList.addAll(knownLocations);
        knownLocations.clear();

        /*
        Method addItem = list.getClass().getDeclaredMethod("add", Object.class);
        addItem.setAccessible(true);
        addItem.invoke(list, block);
         */

    }

}
