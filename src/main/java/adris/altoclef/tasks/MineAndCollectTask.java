package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.resources.SatisfyMiningRequirementTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.CursorInventorySlot;
import adris.altoclef.util.slots.PlayerInventorySlot;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MineAndCollectTask extends ResourceTask {

    private final Block[] _blocksToMine;

    private final MiningRequirement _requirement;

    private final TimerGame _cursorStackTimer = new TimerGame(3);

    private final MineOrCollectTask _subtask;

    public MineAndCollectTask(ItemTarget[] itemTargets, Block[] blocksToMine, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;
        _blocksToMine = blocksToMine;
        _subtask = new MineOrCollectTask(_blocksToMine, _itemTargets);
    }

    public MineAndCollectTask(ItemTarget[] blocksToMine, MiningRequirement requirement) {
        this(blocksToMine, itemTargetToBlockList(blocksToMine), requirement);
    }

    public MineAndCollectTask(ItemTarget target, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget[]{target}, blocksToMine, requirement);
    }

    public MineAndCollectTask(Item item, int count, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget(item, count), blocksToMine, requirement);
    }

    public static Block[] itemTargetToBlockList(ItemTarget[] targets) {
        List<Block> result = new ArrayList<>(targets.length);
        for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
                result.add(Block.getBlockFromItem(item));
            }
        }
        return Util.toArray(Block.class, result);
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();
        mod.getBlockTracker().trackBlock(_blocksToMine);

        // We're mining, so don't throw away pickaxes.
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);

        _subtask.resetSearch();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // Picking up is controlled by a separate task here.
        return true;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {

        if (!mod.getInventoryTracker().miningRequirementMet(_requirement)) {
            return new SatisfyMiningRequirementTask(_requirement);
        }

        if (_subtask.isMining()) {
            makeSureToolIsEquipped(mod);
        }

        // Wrong dimension check.
        if (_subtask.wasWandering() && isInWrongDimension(mod)) {
            return getToCorrectDimensionTask(mod);
        }

        return _subtask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(_blocksToMine);
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof MineAndCollectTask) {
            MineAndCollectTask task = (MineAndCollectTask) other;
            return Util.arraysEqual(task._blocksToMine, _blocksToMine);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Mine And Collect";
    }

    private void makeSureToolIsEquipped(AltoClef mod) {
        if (_cursorStackTimer.elapsed() && !mod.getFoodChain().isTryingToEat()) {
            assert MinecraftClient.getInstance().player != null;
            ItemStack cursorStack = MinecraftClient.getInstance().player.inventory.getCursorStack();
            if (cursorStack != null && !cursorStack.isEmpty()) {
                // We have something in our cursor stack
                Item item = cursorStack.getItem();
                if (item.isEffectiveOn(mod.getWorld().getBlockState(_subtask.miningPos()))) {
                    // Our cursor stack would help us mine our current block
                    Item currentlyEquipped = mod.getInventoryTracker().getItemStackInSlot(PlayerInventorySlot.getEquipSlot(EquipmentSlot.MAINHAND)).getItem();
                    if (item instanceof MiningToolItem) {
                        if (currentlyEquipped instanceof MiningToolItem) {
                            MiningToolItem currentPick = (MiningToolItem) currentlyEquipped;
                            MiningToolItem swapPick = (MiningToolItem) item;
                            if (swapPick.getMaterial().getMiningLevel() > currentPick.getMaterial().getMiningLevel()) {
                                // We can equip a better pickaxe.
                                mod.getInventoryTracker().equipSlot(new CursorInventorySlot());
                            }
                        } else {
                            // We're not equipped with a pickaxe...
                            mod.getInventoryTracker().equipSlot(new CursorInventorySlot());
                        }
                    }
                }
            }
            _cursorStackTimer.reset();
        }
    }

    private static class MineOrCollectTask extends AbstractDoToClosestObjectTask<Object> {

        private final Block[] _blocks;
        private final ItemTarget[] _targets;
        private final Set<BlockPos> _blacklist = new HashSet<>();
        private final MovementProgressChecker _progressChecker = new MovementProgressChecker(1);
        private final Task _pickupTask;
        private BlockPos _miningPos;
        private AltoClef _mod;

        public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
            _blocks = blocks;
            _targets = targets;
            _pickupTask = new PickupDroppedItemTask(_targets, true);
        }

        @Override
        protected Vec3d getPos(AltoClef mod, Object obj) {
            if (obj instanceof BlockPos) {
                BlockPos b = (BlockPos) obj;
                return new Vec3d(b.getX(), b.getY(), b.getZ());
            }
            if (obj instanceof ItemEntity) {
                ItemEntity item = (ItemEntity) obj;
                return item.getPos();
            }
            throw new UnsupportedOperationException("Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected Object getClosestTo(AltoClef mod, Vec3d pos) {
            BlockPos closestBlock = null;
            if (mod.getBlockTracker().anyFound(_blocks)) {
                closestBlock = mod.getBlockTracker().getNearestTracking(pos, (check) -> {
                    if (_blacklist.contains(check)) return true;
                    // Filter out blocks that will get us into trouble. TODO: Blacklist
                    return !MineProcess.plausibleToBreak(new CalculationContext(mod.getClientBaritone()), check);
                }, _blocks);
            }
            ItemEntity closestDrop = null;
            if (mod.getEntityTracker().itemDropped(_targets)) {
                closestDrop = mod.getEntityTracker().getClosestItemDrop(pos, _targets);
            }

            double blockSq = closestBlock == null ? Double.POSITIVE_INFINITY : closestBlock.getSquaredDistance(pos, false);
            double dropSq = closestDrop == null ? Double.POSITIVE_INFINITY : closestDrop.squaredDistanceTo(pos);

            // We can't mine right now.
            if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
                Debug.logInternal("(temp) interactions are paused");
                return closestDrop;
            }

            if (dropSq <= blockSq) {
                return closestDrop;
            } else {
                return closestBlock;
            }
        }

        @Override
        protected Vec3d getOriginPos(AltoClef mod) {
            return mod.getPlayer().getPos();
        }

        @Override
        protected Task onTick(AltoClef mod) {
            _mod = mod;
            if (_miningPos != null && !_progressChecker.check(mod)) {
                Debug.logMessage("Failed to mine block. Suggesting it may be unreachable.");
                mod.getBlockTracker().requestBlockUnreachable(_miningPos, 2);
                _blacklist.add(_miningPos);
                _miningPos = null;
                _progressChecker.reset();
            }
            return super.onTick(mod);
        }

        @Override
        protected Task getGoalTask(Object obj) {
            if (obj instanceof BlockPos) {
                BlockPos newPos = (BlockPos) obj;
                if (_miningPos == null || !_miningPos.equals(newPos)) {
                    _progressChecker.reset();
                }
                _miningPos = newPos;
                return new DestroyBlockTask(_miningPos);
            }
            if (obj instanceof ItemEntity) {
                _miningPos = null;

                if (!ResourceTask.ensureInventoryFree(_mod)) {
                    Debug.logInternal("FAILED TO DROP ITEMS");
                }

                return _pickupTask;
            }
            throw new UnsupportedOperationException("Shouldn't try to get the goal from object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected boolean isValid(AltoClef mod, Object obj) {
            if (obj instanceof BlockPos) {
                BlockPos b = (BlockPos) obj;
                return mod.getBlockTracker().blockIsValid(b, _blocks) && WorldUtil.canBreak(mod, b);
            }
            if (obj instanceof ItemEntity) {
                ItemEntity drop = (ItemEntity) obj;
                Item item = drop.getStack().getItem();
                for (ItemTarget target : _targets) {
                    if (target.matches(item)) return true;
                }
                return false;
            }
            return false;
        }

        @Override
        protected void onStart(AltoClef mod) {
            _progressChecker.reset();
            _miningPos = null;
        }

        @Override
        protected void onStop(AltoClef mod, Task interruptTask) {

        }

        @Override
        protected boolean isEqual(Task obj) {
            if (obj instanceof MineOrCollectTask) {
                MineOrCollectTask task = (MineOrCollectTask) obj;
                return Util.arraysEqual(task._blocks, _blocks) && Util.arraysEqual(task._targets, _targets);
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Mining or Collecting";
        }

        public boolean isMining() {
            return _miningPos != null;
        }

        public BlockPos miningPos() {
            return _miningPos;
        }
    }

}
