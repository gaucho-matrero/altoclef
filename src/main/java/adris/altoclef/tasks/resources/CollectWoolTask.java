package adris.altoclef.tasks.resources;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.AbstractDoToEntityTask;
import adris.altoclef.tasks.MineAndCollectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;

import java.util.Arrays;
import java.util.HashSet;


public class CollectWoolTask extends ResourceTask {
    private final int count;
    private final HashSet<DyeColor> colors;
    private final Item[] wools;
    
    public CollectWoolTask(DyeColor[] colors, int count) {
        super(new ItemTarget(ItemUtil.WOOL, count));
        this.colors = new HashSet<>(Arrays.asList(colors));
        this.count = count;
        wools = getWoolColorItems(colors);
    }
    
    public CollectWoolTask(DyeColor color, int count) {
        this(new DyeColor[]{ color }, count);
    }
    
    public CollectWoolTask(int count) {
        this(DyeColor.values(), count);
    }
    
    private static Item[] getWoolColorItems(DyeColor[] colors) {
        Item[] result = new Item[colors.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = ItemUtil.getColorfulItems(colors[i]).wool;
        }
        return result;
    }
    
    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }
    
    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Util.itemsToBlocks(wools));
    }
    
    @Override
    protected Task onResourceTick(AltoClef mod) {
        
        // TODO: If we don't find good color wool blocks
        // and we DONT find good color sheep:
        // USE DYES + REGULAR WOOL TO CRAFT THE WOOL COLOR!!
        
        // If we find a wool block, break it.
        Block[] woolBlocks = Util.itemsToBlocks(wools);
        if (mod.getBlockTracker().anyFound(woolBlocks)) {
            return new MineAndCollectTask(new ItemTarget(wools), woolBlocks, MiningRequirement.HAND);
        }
        
        // If we have shears, right click nearest sheep
        // Otherwise, kill + loot wool.
        
        // Dimension
        if (isInWrongDimension(mod) && !mod.getEntityTracker().entityFound(SheepEntity.class)) {
            return getToCorrectDimensionTask(mod);
        }
        
        if (mod.getInventoryTracker().hasItem(Items.SHEARS)) {
            // Shear sheep.
            return new ShearSheepTask();
        }
        
        // Only option left is to Kill la Kill.
        return new KillAndLootTask(SheepEntity.class, entity -> {
            if (entity instanceof SheepEntity) {
                SheepEntity sheep = (SheepEntity) entity;
                // Hunt sheep of the same color.
                if (!colors.contains(sheep.getColor())) return false;
                return ((SheepEntity) entity).isSheared();
            }
            return false;
        }, new ItemTarget(wools, count));
    }
    
    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Util.itemsToBlocks(wools));
    }
    
    @Override
    protected boolean isEqualResource(ResourceTask obj) {
        return obj instanceof CollectWoolTask && ((CollectWoolTask) obj).count == count;
    }
    
    @Override
    protected String toDebugStringName() {
        return "Collect " + count + " wool.";
    }
    
    
    static class ShearSheepTask extends AbstractDoToEntityTask {
        
        public ShearSheepTask() {
            super(0, -1, -1);
        }
        
        @Override
        protected boolean isSubEqual(AbstractDoToEntityTask other) {
            return other instanceof ShearSheepTask;
        }
        
        @Override
        protected Task onEntityInteract(AltoClef mod, Entity entity) {
            if (!mod.getInventoryTracker().hasItem(Items.SHEARS)) {
                Debug.logWarning("Failed to shear sheep because you have no shears.");
                return null;
            }
            if (mod.getInventoryTracker().equipItem(Items.SHEARS)) {
                mod.getController().interactEntity(mod.getPlayer(), entity, Hand.MAIN_HAND);
            } else {
                Debug.logWarning("Failed to equip shears for some reason.");
            }
            
            
            return null;
        }
        
        @Override
        protected Entity getEntityTarget(AltoClef mod) {
            Entity found = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), (entity) -> {
                if (entity instanceof SheepEntity) {
                    SheepEntity sheep = (SheepEntity) entity;
                    return !sheep.isShearable() || sheep.isSheared();
                }
                
                return true;
            }, SheepEntity.class);
            return found;
        }
        
        @Override
        protected String toDebugString() {
            return "Shearing Sheep";
        }
    }
}
