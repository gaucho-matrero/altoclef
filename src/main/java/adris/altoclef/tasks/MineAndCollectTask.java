package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasks.resources.SatisfyMiningRequirementTask;
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MineAndCollectTask extends ResourceTask {

    private List<Block> _blocksToMine;

    private List<BlockOptionalMeta> _targetBoms = new ArrayList<>();

    private final MiningRequirement _requirement;

    private BlockPos _cachedTargetMineBlock;

    private static final float STOP_MINING_ITEM_DROP_IS_CLOSE_ENOUGH_THRESHOLD = 20f;
    private static final float KEEP_MINING_THRESHOLD = 6;
    private static final int DISTANCE_FAIL_TOO_MUCH_COUNT = 3;

    private List<BlockPos> _cachedBlacklist = new ArrayList<>();

    private PathingCommand _cachedCommand = null;

    private final Timer _mineCheck = new Timer(10.0);
    private final IProgressChecker<Double> _mineProgressChecker = new LinearProgressChecker(4, 0.01f);
    private final DistanceProgressChecker _distanceProgressChecker = new DistanceProgressChecker(10, 0.1f);

    private final Timer _tickIntervalCheck = new Timer(1);

    private final IProgressChecker<Double> _longTermProgressChecker = new LinearProgressChecker(20, 0.1);

    private final TimeoutWanderTask _wanderTask = new TimeoutWanderTask(40);
    private int _distanceFailCounter = 0;

    public MineAndCollectTask(List<ItemTarget> itemTargets, List<Block> blocksToMine, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;
        _blocksToMine = blocksToMine;
    }

    public MineAndCollectTask(ItemTarget target, Block[] blocksToMine, MiningRequirement requirement) {
        this(Collections.singletonList(target), Arrays.asList(blocksToMine), requirement);
    }
    public MineAndCollectTask(List<ItemTarget> blocksToMine, MiningRequirement requirement) {
        this(blocksToMine, itemTargetToBlockList(blocksToMine), requirement);
    }

    private static List<Block> itemTargetToBlockList(List<ItemTarget> targets) {
        List<Block> result = new ArrayList<>(targets.size());
        for (ItemTarget target : targets) {
            for(Item item : target.getMatches()) {
                result.add(Block.getBlockFromItem(item));
            }
        }
        return result;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        // Baritone is buggy here. For example:
        // If I try to mine diamonds, it will stop and grab every egg on the ground.
        mod.getConfigState().push();
        mod.getConfigState().setMineScanDroppedItems(false);
        _cachedBlacklist.clear();
        _distanceFailCounter = 0;

        _distanceProgressChecker.reset(mod.getPlayer().getPos());
        _mineProgressChecker.reset();
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

            setDebugState("CHECK for drops...");
            //Debug.logMessage("CHECK MINE PROCESS");
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

        if (_tickIntervalCheck.elapsed()) {
            // The time between ticks is big enough so we gotta reset
            resetCheckers(mod);
        }
        _tickIntervalCheck.reset();

        if (_wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            resetCheckers(mod);
            setDebugState("Wandering...");
            return _wanderTask;
        }

        // If we don't have the proper tool, satisfy it.
        if (!mod.getInventoryTracker().miningRequirementMet(_requirement)) {
            setDebugState("Getting tool: " + _requirement);
            return new SatisfyMiningRequirementTask(_requirement);
        }

        // Mine
        List<BlockOptionalMeta> boms = new ArrayList<>();

        StringBuilder state = new StringBuilder();
        state.append(" ----- ");
        for (ItemTarget target : _itemTargets) {
            if (mod.getInventoryTracker().targetMet(target)) continue;
            state.append("Need ").append(target.toString()).append(" ----- ");
            for (Block block : _blocksToMine) {
                BlockOptionalMeta bom = new BlockOptionalMeta(block);
                boms.add(bom);
            }
        }

        setDebugState(state.toString());

        if (!miningCorrectBlocks(mod, boms)) {
            //Debug.logMessage("New set of blocks to mine...");

            boolean wasRunningBefore = mod.getClientBaritone().getMineProcess().isActive();

            BlockOptionalMeta[] bomsArray = new BlockOptionalMeta[boms.size()];
            boms.toArray(bomsArray);

            mod.getClientBaritone().getMineProcess().cancel();
            mod.getClientBaritone().getMineProcess().mine(bomsArray);
            // Re-update blacklist
            try {
                addToProcBlacklistRaw(mod, _cachedBlacklist);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Debug.logMessage("oof, this might mean something was imported or compiled incorrectly (place #2) :(");
                e.printStackTrace();
            }

            _targetBoms = boms;

            if (wasRunningBefore) {
                resetCheckers(mod);
            }
            Debug.logInternal("Starting to mine.");
        }

        boolean failed = false;
        boolean mining = mod.getController().isBreakingBlock();

        if (mining) {
            double progress = mod.getControllerExtras().getBreakingBlockProgress();
            _mineProgressChecker.setProgress(progress);
            if (_mineProgressChecker.failed()) {
                Debug.logMessage("Failed to mine block. Blacklisting.");
                failed = true;
                _mineProgressChecker.reset();
            }
            // Reset other checker (independent, one blocks another)
            _distanceProgressChecker.reset(mod.getPlayer().getPos());
        } else {
            _distanceProgressChecker.setProgress(mod.getPlayer().getPos());
            if (_distanceProgressChecker.failed()) {
                Debug.logMessage("Failed to make progress moving to our block. Blacklisting.");
                Debug.logWarning("OOF");
                failed = true;
                // If we've failed too much, wander.
                _distanceFailCounter++;
                if (_distanceFailCounter >= DISTANCE_FAIL_TOO_MUCH_COUNT) {
                    Debug.logMessage("Failed too much, Going somewhere else.");
                    _distanceFailCounter = 0;
                    _wanderTask.reset();
                    return _wanderTask;
                }
                _distanceProgressChecker.reset(mod.getPlayer().getPos());
            }
            // Reset other checker (independent, one blocks another)
            _mineProgressChecker.reset();
        }
        if (failed) {
            try {
                blacklistCurrentTarget(mod);
            } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
                // oof
                Debug.logMessage("oof, this might mean something was imported or compiled incorrectly (place #1) :(");
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getMineProcess().cancel();
        mod.getConfigState().pop();
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
        if (_targetBoms.size() != targetBoms.size()) return false;
        for (int i = 0; i < _targetBoms.size(); ++i) {
            BlockOptionalMeta us = _targetBoms.get(i);
            BlockOptionalMeta them = targetBoms.get(i);
            if (!us.matches(them.getBlock())) return false;
            //if (!us.matches(them.getAnyBlockState())) return false;
        }
        return true;
    }

    private void resetCheckers(AltoClef mod) {
        _distanceProgressChecker.reset(mod.getPlayer().getPos());
        _mineProgressChecker.reset();
        _longTermProgressChecker.reset();
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

    @SuppressWarnings("unchecked")
    private void blacklistCurrentTarget(AltoClef mod) throws NoSuchFieldException, NoSuchMethodException, IllegalAccessException {
        // This object will be used for access
        //UserClass userClassObj = new UserClass();
        MineProcess proc = mod.getClientBaritone().getMineProcess();
        Field knownField = MineProcess.class.getDeclaredField("knownOreLocations");
        knownField.setAccessible(true);

        List<BlockPos> knownLocations = (List<BlockPos>) knownField.get(proc);

        addToProcBlacklistRaw(mod, knownLocations);

        knownLocations.clear();

        /*
        Method addItem = list.getClass().getDeclaredMethod("add", Object.class);
        addItem.setAccessible(true);
        addItem.invoke(list, block);
         */
    }

    private void addToProcBlacklistRaw(AltoClef mod, List<BlockPos> toAdd) throws NoSuchFieldException, IllegalAccessException {
        MineProcess proc = mod.getClientBaritone().getMineProcess();
        Field blacklistField = MineProcess.class.getDeclaredField("blacklist");
        blacklistField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<BlockPos> blackList = (List<BlockPos>) blacklistField.get(proc);
        blackList.addAll(toAdd);

        _cachedBlacklist.clear();
        _cachedBlacklist.addAll(blackList);
        //Debug.logInternal("SIZE: " + _cachedBlacklist.size());
    }

}
