package adris.altoclef.tasks.misc.speedrun;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.construction.ClearLiquidTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceStructureBlockTask;
import adris.altoclef.tasks.misc.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.WorldUtil;
import adris.altoclef.util.csharpisbetter.Timer;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.HashSet;


public class ConstructNetherPortalBucketTask extends Task {
    
    // Order here matters
    private static final Vec3i[] PORTAL_FRAME = {
            // Left side
            new Vec3i(0, 0, -1),
            new Vec3i(0, 1, -1),
            new Vec3i(0, 2, -1),
            // Right side
            new Vec3i(0, 0, 2),
            new Vec3i(0, 1, 2),
            new Vec3i(0, 2, 2),
            // Bottom
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, 1),
            // Top
            new Vec3i(0, 3, 0),
            new Vec3i(0, 3, 1)
    };
    
    private static final Vec3i[] PORTAL_INTERIOR = {
            new Vec3i(0, 0, 0),
            new Vec3i(0, 1, 0),
            new Vec3i(0, 2, 0),
            new Vec3i(0, 0, 1),
            new Vec3i(0, 1, 1),
            new Vec3i(0, 2, 1)
    };
    
    private static final Vec3i[] CAST_FRAME = {
            new Vec3i(0, -1, 0),
            new Vec3i(0, 0, -1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, 0),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 1, 0)
    };
    
    // The "portalable" region includes the portal (1 x 6 x 4 structure) and an outer buffer for its construction and water bullshit.
    // The "portal origin relative to region" corresponds to the portal origin with respect to the "portalable" region (see _portalOrigin).
    // This can only really be explained visually, sorry!
    private static final Vec3i PORTALABLE_REGION_SIZE = new Vec3i(4, 6, 6);
    private static final Vec3i PORTAL_ORIGIN_RELATIVE_TO_REGION = new Vec3i(1, 0, 2);
    private final Timer lavaSearchTimer = new Timer(5);
    private final MovementProgressChecker progressChecker = new MovementProgressChecker(5);
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(25);
    // Stored here to cache lava blacklist
    private final Task collectLavaTask = TaskCatalogue.getItemTask("lava_bucket", 1);
    private final Timer refreshTimer = new Timer(11);
    private BlockPos portalOrigin;
    private BlockPos currentLavaTarget;
    private BlockPos currentDestroyTarget;
    private BlockPos currentPlaceTarget;
    private BlockPos currentCastTarget;
    private boolean firstSearch;
    
    @Override
    protected void onStart(AltoClef mod) {
        
        currentDestroyTarget = null;
        
        mod.getBlockTracker().trackBlock(Blocks.LAVA);
        mod.getConfigState().push();
        mod.getConfigState().setSearchAnywhereFlag(true);
        
        // Avoid breaking portal frame if we're obsidian.
        // Also avoid placing on the lava + water
        // Also avoid breaking the cast frame
        mod.getConfigState().avoidBlockBreaking(block -> {
            if (portalOrigin != null) {
                // Don't break CURRENT cast
                if (currentLavaTarget != null) {
                    for (Vec3i castPosRelativeToLava : CAST_FRAME) {
                        BlockPos castPos = currentLavaTarget.add(castPosRelativeToLava);
                        if (block.equals(castPos)) {
                            return true;
                        }
                    }
                }
                // Don't break frame
                for (Vec3i framePosRelative : PORTAL_FRAME) {
                    BlockPos framePos = portalOrigin.add(framePosRelative);
                    if (block.equals(framePos)) {
                        return mod.getWorld().getBlockState(framePos).getBlock() == Blocks.OBSIDIAN;
                    }
                }
            }
            return false;
        });
        
        // We need to tell baritone to not make paths that require
        // bridging on avoidPlacing blocks.
        // Avoid placing stuff in the lava/water position of our cast.
        mod.getConfigState().avoidBlockPlacing(block -> {
            if (currentLavaTarget != null) {
                BlockPos waterTarget = currentLavaTarget.up();
                return block.equals(currentLavaTarget) || block.equals(waterTarget);
            }
            return false;
        });
        
        // Protect some used items
        mod.getConfigState().addProtectedItems(Items.WATER_BUCKET, Items.LAVA_BUCKET, Items.FLINT_AND_STEEL);
        
        progressChecker.reset();
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        if (refreshTimer.elapsed()) {
            Debug.logMessage("Duct tape: Refreshing inventory again just in case");
            refreshTimer.reset();
            mod.getInventoryTracker().refreshInventory();
        }
        
        //If too far, reset.
        if (portalOrigin != null && !portalOrigin.isWithinDistance(mod.getPlayer().getPos(), 2000)) {
            portalOrigin = null;
            currentCastTarget = null;
            currentLavaTarget = null;
            currentDestroyTarget = null;
            currentPlaceTarget = null;
        }
        
        if (currentDestroyTarget != null) {
            if (!WorldUtil.isSolid(mod, currentDestroyTarget)) {
                currentDestroyTarget = null;
            } else {
                return new DestroyBlockTask(currentDestroyTarget);
            }
        }
        if (currentPlaceTarget != null) {
            if (WorldUtil.isSolid(mod, currentPlaceTarget)) {
                currentPlaceTarget = null;
            } else {
                return new PlaceStructureBlockTask(currentPlaceTarget);
            }
        }
        
        
        if (wanderTask.isActive() && !wanderTask.isFinished(mod)) {
            setDebugState("Wandering before retrying...");
            progressChecker.reset();
            return wanderTask;
        }
        
        
        // Get bucket if we don't have one.
        int bucketCount = mod.getInventoryTracker().getItemCount(Items.BUCKET, Items.LAVA_BUCKET, Items.WATER_BUCKET);
        if (bucketCount < 2) {
            setDebugState("Getting buckets");
            progressChecker.reset();
            return TaskCatalogue.getItemTask("bucket", 2);
        }
        
        // Get flint & steel if we don't have one
        if (!mod.getInventoryTracker().hasItem(Items.FLINT_AND_STEEL)) {
            setDebugState("Getting flint & steel");
            progressChecker.reset();
            return TaskCatalogue.getItemTask("flint_and_steel", 1);
        }
        
        boolean needsToLookForPortal = portalOrigin == null;
        if (needsToLookForPortal) {
            progressChecker.reset();
            // Get water before searching, just for convenience.
            if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                setDebugState("Getting water");
                progressChecker.reset();
                return TaskCatalogue.getItemTask("water_bucket", 1);
            }
            
            boolean foundSpot = false;
            
            if (firstSearch || lavaSearchTimer.elapsed()) {
                firstSearch = false;
                lavaSearchTimer.reset();
                Debug.logMessage("(Searching for lava lake with portalable spot nearby...)");
                BlockPos lavaPos = findLavaLake(mod, mod.getPlayer().getBlockPos());
                if (lavaPos != null) {
                    // We have a lava lake, set our portal origin!
                    BlockPos foundPortalRegion = getPortalableRegion(lavaPos, mod.getPlayer().getBlockPos(), new Vec3i(-1, 0, 0),
                                                                     PORTALABLE_REGION_SIZE, 20);
                    if (foundPortalRegion == null) {
                        Debug.logWarning("Failed to find portalable region nearby. Consider increasing the search timeout range");
                    } else {
                        portalOrigin = foundPortalRegion.add(PORTAL_ORIGIN_RELATIVE_TO_REGION);
                        foundSpot = true;
                    }
                } else {
                    Debug.logMessage("(lava lake not found)");
                }
            }
            
            if (!foundSpot) {
                setDebugState("(timeout: Looking for lava lake)");
                return new TimeoutWanderTask(100);
            }
        }
        
        // We have a portal, now build it.
        for (Vec3i framePosRelative : PORTAL_FRAME) {
            BlockPos framePos = portalOrigin.add(framePosRelative);
            Block frameBlock = mod.getWorld().getBlockState(framePos).getBlock();
            if (frameBlock == Blocks.OBSIDIAN) {
                // Already satisfied, clear water above if need be.
                BlockPos waterCheck = framePos.up();
                if (mod.getWorld().getBlockState(waterCheck).getBlock() == Blocks.WATER && WorldUtil.isSourceBlock(mod, waterCheck, true)) {
                    setDebugState("Clearing water from cast");
                    return new ClearLiquidTask(waterCheck);
                }
                continue;
            }
            
            // Get lava early so placing it is faster
            if (!mod.getInventoryTracker().hasItem(Items.LAVA_BUCKET) && frameBlock != Blocks.LAVA) {
                setDebugState("Collecting lava");
                progressChecker.reset();
                return collectLavaTask;
            }
            
            if (currentCastTarget == null || !currentLavaTarget.equals(framePos)) {
                // We need to place obsidian here.
                progressChecker.reset();
                currentLavaTarget = framePos;
                currentCastTarget = null;
            }
            
            // Build the cast frame
            
            if (currentCastTarget != null && WorldUtil.isSolid(mod, currentCastTarget)) {
                // Current cast frame already built.
                currentCastTarget = null;
            }
            if (currentCastTarget == null) {
                // Find new cast frame
                for (Vec3i castPosRelative : CAST_FRAME) {
                    BlockPos castPos = currentLavaTarget.add(castPosRelative);
                    if (!WorldUtil.isSolid(mod, castPos)) {
                        currentCastTarget = castPos;
                        break;
                    }
                }
            }
            if (currentCastTarget != null) {
                setDebugState("Building cast");
                currentPlaceTarget = currentCastTarget;
                return null;
                //return new PlaceStructureBlockTask(_currentCastTarget);
            }
            
            // Cast frame built. Now, place lava.
            if (frameBlock != Blocks.LAVA) {
                
                if (WorldUtil.isSolid(mod, framePos)) {
                    setDebugState("Clearing space around lava");
                    currentDestroyTarget = framePos;
                    return null;
                    //return new DestroyBlockTask(framePos);
                }
                // Clear the upper two as well, to make placing more reliable.
                if (WorldUtil.isSolid(mod, framePos.up())) {
                    setDebugState("Clearing space around lava");
                    currentDestroyTarget = framePos.up();
                    return null;
                }
                if (WorldUtil.isSolid(mod, framePos.up(2))) {
                    setDebugState("Clearing space around lava");
                    currentDestroyTarget = framePos.up(2);
                    return null;
                }
                
                // Make sure we have water, juuust in case we have another creeper appear run end here
                if (!mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                    return TaskCatalogue.getItemTask("water_bucket", 1);
                }
                
                // Don't place lava at our position!
                // Would lead to an embarrassing death.
                BlockPos targetPos = currentLavaTarget.add(-1, 1, 0);
                if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getInventoryTracker().hasItem(Items.LAVA_BUCKET)) {
                    setDebugState("Positioning player before lava");
                    return new GetToBlockTask(targetPos, false);
                }
                
                setDebugState("Placing lava for cast");
                
                return new InteractItemWithBlockTask(new ItemTarget("lava_bucket", 1), Direction.WEST, currentLavaTarget.add(1, 0, 0),
                                                     false);
            }
            // Lava placed, Now, place water.
            BlockPos waterCheck = framePos.up();
            if (mod.getWorld().getBlockState(waterCheck).getBlock() != Blocks.WATER) {
                setDebugState("Placing water for cast");
                
                if (WorldUtil.isSolid(mod, waterCheck)) {
                    currentDestroyTarget = waterCheck;
                    return null;
                    //return new DestroyBlockTask(waterCheck);
                    
                }
                if (WorldUtil.isSolid(mod, waterCheck.up())) {
                    currentDestroyTarget = waterCheck.up();
                    return null;
                    //return new DestroyBlockTask(waterCheck.up());
                }
                
                // Get to position to avoid weird stuck scenario
                BlockPos targetPos = currentLavaTarget.add(-1, 1, 0);
                if (!mod.getPlayer().getBlockPos().equals(targetPos) && mod.getInventoryTracker().hasItem(Items.WATER_BUCKET)) {
                    setDebugState("Positioning player before water");
                    return new GetToBlockTask(targetPos, false);
                }
                
                return new InteractItemWithBlockTask(new ItemTarget("water_bucket", 1), Direction.WEST, currentLavaTarget.add(1, 1, 0),
                                                     true);
            }
        }
        // No more obsidian targets necessary.
        currentLavaTarget = null;
        
        // Now, clear the inside.
        for (Vec3i offs : PORTAL_INTERIOR) {
            BlockPos p = portalOrigin.add(offs);
            assert MinecraftClient.getInstance().world != null;
            if (!MinecraftClient.getInstance().world.getBlockState(p).isAir()) {
                setDebugState("Clearing inside of portal");
                currentDestroyTarget = p;
                return null;
                //return new DestroyBlockTask(p);
            }
        }
        
        setDebugState("Flinting and Steeling");
        
        // Flint and steel it baby
        return new InteractItemWithBlockTask(new ItemTarget("flint_and_steel", 1), Direction.UP, portalOrigin.down(), true);
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.LAVA);
        mod.getConfigState().pop();
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof ConstructNetherPortalBucketTask;
    }
    
    @Override
    protected String toDebugString() {
        return "Construct Nether Portal";
    }
    
    private BlockPos findLavaLake(AltoClef mod, BlockPos playerPos) {
        HashSet<BlockPos> alreadyExplored = new HashSet<>();
        
        double nearestSqDistance = Double.POSITIVE_INFINITY;
        BlockPos nearestLake = null;
        for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.LAVA)) {
            if (alreadyExplored.contains(pos)) continue;
            double sqDist = playerPos.getSquaredDistance(pos);
            if (sqDist < nearestSqDistance) {
                int depth = getNumberOfBlocksAdjacent(alreadyExplored, pos);
                if (depth != 0) {
                    Debug.logMessage("Found with depth " + depth);
                    if (depth >= 12) {
                        nearestSqDistance = sqDist;
                        nearestLake = pos;
                    }
                }
            }
        }
        
        return nearestLake;
    }
    
    // Used to flood-scan for blocks of lava.
    private int getNumberOfBlocksAdjacent(HashSet<BlockPos> alreadyExplored, BlockPos origin) {
        // Base case: We already explored this one
        if (alreadyExplored.contains(origin)) return 0;
        alreadyExplored.add(origin);
        
        // Base case: We hit a non-full lava block.
        assert MinecraftClient.getInstance().world != null;
        BlockState s = MinecraftClient.getInstance().world.getBlockState(origin);
        if (s.getBlock() != Blocks.LAVA) {
            return 0;
        } else {
            // We may not be a full lava block
            if (!s.getFluidState().isStill()) return 0;
            int level = s.getFluidState().getLevel();
            //Debug.logMessage("TEST LEVEL: " + level + ", " + height);
            // Only accept FULL SOURCE BLOCKS
            if (level != 8) return 0;
        }
        
        BlockPos[] toCheck = { origin.north(), origin.south(), origin.east(), origin.west(), origin.up(), origin.down() };
        
        int bonus = 0;
        for (BlockPos check : toCheck) {
            // This block is new! Explore out from it.
            bonus += getNumberOfBlocksAdjacent(alreadyExplored, check);
        }
        
        return bonus + 1;
    }
    
    // Get a region that a portal can fit into
    private BlockPos getPortalableRegion(BlockPos lava, BlockPos playerPos, Vec3i sizeOffset, Vec3i sizeAllocation, int timeoutRange) {
        Vec3i[] directions = { new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1) };
        
        double minDistanceToPlayer = Double.POSITIVE_INFINITY;
        BlockPos bestPos = null;
        
        for (Vec3i direction : directions) {
            
            // Inch along
            for (int offs = 1; offs < timeoutRange; ++offs) {
                
                Vec3i offset = new Vec3i(direction.getX() * offs, direction.getY() * offs, direction.getZ() * offs);
                
                boolean found = true;
                // check for collision with lava in box
                moveAlongLine:
                // We have an extra buffer to make sure we never break a block NEXT to lava.
                for (int dx = -1; dx < sizeAllocation.getX() + 1; ++dx) {
                    for (int dz = -1; dz < sizeAllocation.getZ() + 1; ++dz) {
                        for (int dy = -1; dy < sizeAllocation.getY(); ++dy) {
                            BlockPos toCheck = lava.add(offset).add(sizeOffset).add(dx, dy, dz);
                            assert MinecraftClient.getInstance().world != null;
                            BlockState state = MinecraftClient.getInstance().world.getBlockState(toCheck);
                            if (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.BEDROCK) {
                                found = false;
                                break moveAlongLine;
                            }
                        }
                    }
                }
                
                if (found) {
                    BlockPos foundBoxCorner = lava.add(offset).add(sizeOffset);
                    double sqDistance = foundBoxCorner.getSquaredDistance(playerPos);
                    if (sqDistance < minDistanceToPlayer) {
                        minDistanceToPlayer = sqDistance;
                        bestPos = foundBoxCorner;
                    }
                    break;
                }
            }
            
        }
        
        return bestPos;
    }
}
