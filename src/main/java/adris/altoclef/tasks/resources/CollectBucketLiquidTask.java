package adris.altoclef.tasks.resources;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.DefaultGoToDimensionTask;
import adris.altoclef.tasks.DoToClosestBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.ActionListener;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.csharpisbetter.Util;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.utils.Rotation;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;

import java.util.HashSet;
import java.util.Optional;
import java.util.function.Function;


public class CollectBucketLiquidTask extends ResourceTask {
    private final HashSet<BlockPos> blacklist = new HashSet<>();
    private final Timer reachTimer = new Timer(2);
    private final Timer tryImmediatePickupTimer = new Timer(3);
    private final Timer pickedUpTimer = new Timer(0.5);
    //private IProgressChecker<Double> _checker = new LinearProgressChecker(5, 0.1);
    private final int count;
    private final Item target;
    private final Block toCollect;
    private final String liquidName;
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(15f);
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private BlockPos targetLiquid;
    
    public CollectBucketLiquidTask(String liquidName, Item filledBucket, int targetCount, Block toCollect) {
        super(filledBucket, targetCount);
        this.liquidName = liquidName;
        target = filledBucket;
        count = targetCount;
        this.toCollect = toCollect;
    }
    
    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }
    
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onResourceStart(AltoClef mod) {
        // Track fluids
        mod.getConfigState().push();
        mod.getConfigState().setRayTracingFluidHandling(RaycastContext.FluidHandling.SOURCE_ONLY);
        mod.getConfigState().setSearchAnywhereFlag(true); // If we don't set this, lava will never be found.
        mod.getBlockTracker().trackBlock(toCollect);
        
        // Avoid breaking / placing blocks at our liquid
        mod.getConfigState().avoidBlockBreaking((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == toCollect);
        mod.getConfigState().avoidBlockPlacing((pos) -> MinecraftClient.getInstance().world.getBlockState(pos).getBlock() == toCollect);
        
        //_blacklist.clear();
        
        wanderTask.resetWander();
        
        progressChecker.reset();
        reachTimer.reset();
    }
    
    
    @Override
    protected Task onResourceTick(AltoClef mod) {
        
        // Run one update to prevent the false fail bug?
        progressChecker.check(mod);
        
        // If we're standing inside a liquid, go pick it up.
        if (tryImmediatePickupTimer.elapsed()) {
            Block standingInside = mod.getWorld().getBlockState(mod.getPlayer().getBlockPos()).getBlock();
            if (standingInside == toCollect) {
                mod.getClientBaritone().getLookBehavior().updateTarget(new Rotation(0, 90), true);
                //Debug.logMessage("Looking at " + _toCollect + ", picking up right away.");
                tryImmediatePickupTimer.reset();
                if (!mod.getInventoryTracker().equipItem(Items.BUCKET)) {
                    Debug.logWarning("Failed to equip bucket.");
                } else {
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    MinecraftClient.getInstance().options.keyUse.setPressed(true);
                    mod.getExtraBaritoneSettings().setInteractionPaused(true);
                    pickedUpTimer.reset();
                    progressChecker.reset();
                    return null;
                }
            }
        }
        
        if (!pickedUpTimer.elapsed()) {
            MinecraftClient.getInstance().options.keyUse.setPressed(false);
            mod.getExtraBaritoneSettings().setInteractionPaused(false);
            progressChecker.reset();
            // Wait for force pickup
            return null;
        }
        
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            setDebugState("Failed to receive: Wandering.");
            reachTimer.reset();
            progressChecker.reset();
            return wanderTask;
        }
        
        // Get buckets if we need em
        int bucketsNeeded = count - mod.getInventoryTracker().getItemCount(Items.BUCKET) - mod.getInventoryTracker().getItemCount(target);
        if (bucketsNeeded > 0) {
            setDebugState("Getting bucket...");
            reachTimer.reset();
            return TaskCatalogue.getItemTask("bucket", bucketsNeeded);
        }
        
        Function<Vec3d, BlockPos> getNearestLiquid = ppos -> mod.getBlockTracker().getNearestTracking(mod.getPlayer().getPos(),
                                                                                                      (blockPos -> {
                                                                                                          if (blacklist.contains(
                                                                                                                  blockPos)) {
                                                                                                              return true;
                                                                                                          }
                                                                                                          if (mod.getBlockTracker()
                                                                                                                 .unreachable(blockPos)) {
                                                                                                              return true; // I think
                                                                                                          }
                                                                                                          // there was a bug here?
                                                                                                          // Doesn't hurt to include though.
                                                                                                          assert MinecraftClient.getInstance().world !=
                                                                                                                 null;
            
                                                                                                          // Lava, we break the block
                                                                                                          // above. If it's bedrock, ignore.
                                                                                                          if (toCollect == Blocks.LAVA &&
                                                                                                              mod.getWorld()
                                                                                                                 .getBlockState(
                                                                                                                         blockPos.up())
                                                                                                                 .getBlock() ==
                                                                                                              Blocks.BEDROCK) {
                                                                                                              return true;
                                                                                                          }
            
                                                                                                          return !WorldUtil.isSourceBlock(
                                                                                                                  mod, blockPos, false);
                                                                                                      }), toCollect);
        
        // Find nearest water and right click it
        BlockPos nearestLiquid = getNearestLiquid.apply(mod.getPlayer().getPos());
        targetLiquid = nearestLiquid;
        if (nearestLiquid != null) {
            // We want to MINIMIZE this distance to liquid.
            setDebugState("Trying to collect...");
            //Debug.logMessage("TEST: " + RayTraceUtils.fluidHandling);
            
            // If we're able to reach the block but we fail...
            if (mod.getCustomBaritone().getInteractWithBlockPositionProcess().isActive()) {
                Optional<Rotation> reach = mod.getCustomBaritone().getInteractWithBlockPositionProcess().getCurrentReach();
                if (reach.isPresent()) {
                    if (reachTimer.elapsed()) {
                        reachTimer.reset();
                        Debug.logMessage("Failed to collect liquid at " + nearestLiquid +
                                         ", probably an invalid source block. blacklisting and trying another one.");
                        blacklist.add(nearestLiquid);
                        mod.getBlockTracker().requestBlockUnreachable(nearestLiquid);
                        // Try again.
                        return null;
                    }
                } else {
                    reachTimer.reset();
                }
            } else {
                reachTimer.reset();
            }
            
            return new DoToClosestBlockTask(() -> mod.getPlayer().getPos(), (BlockPos blockpos) -> {
                
                // Clear above if lava because we can't enter.
                if (toCollect == Blocks.LAVA) {
                    if (WorldUtil.isSolid(mod, blockpos.up())) {
                        if (!progressChecker.check(mod)) {
                            Debug.logMessage("Failed to break, blacklisting & wandering");
                            mod.getBlockTracker().requestBlockUnreachable(blockpos);
                            blacklist.add(blockpos);
                            return wanderTask;
                        }
                        return new DestroyBlockTask(blockpos.up());
                    }
                }
                
                InteractItemWithBlockTask task = new InteractItemWithBlockTask(new ItemTarget(Items.BUCKET, 1), blockpos,
                                                                               toCollect != Blocks.LAVA, new Vec3i(0, 1, 0));
                // noinspection rawtypes
                task.timedOut.addListener(new ActionListener() {
                    @Override
                    public void invoke(Object value) {
                        Debug.logInternal("CURRENT BLACKLIST: " + Util.arrayToString(blacklist.toArray()));
                        Debug.logMessage("Blacklisted " + blockpos);
                        mod.getBlockTracker().requestBlockUnreachable(blockpos);
                        blacklist.add(blockpos);
                        
                    }
                });
                return task;
            }, getNearestLiquid, toCollect);
            //return task;
        }
        
        // Dimension
        if (toCollect == Blocks.WATER && mod.getCurrentDimension() == Dimension.NETHER) {
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }
        
        // Oof, no liquid found.
        setDebugState("Searching for liquid by wandering around aimlessly");
        
        return new TimeoutWanderTask(Float.POSITIVE_INFINITY);
    }
    
    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(toCollect);
        mod.getConfigState().pop();
        //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
        MinecraftClient.getInstance().options.keyUse.setPressed(false);
        mod.getExtraBaritoneSettings().setInteractionPaused(false);
    }
    
    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        if (obj instanceof CollectBucketLiquidTask) {
            CollectBucketLiquidTask task = (CollectBucketLiquidTask) obj;
            if (task.count != count) return false;
            return task.toCollect == toCollect;
        }
        return false;
    }
    
    @Override
    protected String toDebugStringName() {
        return "Collect " + count + " " + liquidName + " buckets";
    }
    
    public static class CollectWaterBucketTask extends CollectBucketLiquidTask {
        public CollectWaterBucketTask(int targetCount) {
            super("water", Items.WATER_BUCKET, targetCount, Blocks.WATER);
        }
    }
    
    
    public static class CollectLavaBucketTask extends CollectBucketLiquidTask {
        public CollectLavaBucketTask(int targetCount) {
            super("lava", Items.LAVA_BUCKET, targetCount, Blocks.LAVA);
        }
    }
    
}
