package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.*;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.SearchChunkForBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;
import java.util.function.Predicate;

public class GetSmithingTemplateTask extends ResourceTask {

    private final Task _searcher = new SearchChunkForBlockTask(Blocks.BLACKSTONE);
    private BlockPos _chestloc = null;
    private final int _count;

    public GetSmithingTemplateTask(int count){
        super(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,count);
        _count=count;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBlockTracker().trackBlock(Blocks.CHEST);
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        // We must go to the nether.
        if (WorldHelper.getCurrentDimension() != Dimension.NETHER) {
            setDebugState("Going to nether");
            return new DefaultGoToDimensionTask(Dimension.NETHER);
        }
        //if (_bastionloc != null && !mod.getChunkTracker().isChunkLoaded(_bastionloc)) {
        //    Debug.logMessage("Bastion at " + _bastionloc + " too far away. Re-searching.");
        //    _bastionloc = null;
        // }
        if (_chestloc == null) {
            for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.CHEST)) {
                if (WorldHelper.isInteractableBlock(mod, pos)) {
                    _chestloc = pos;
                    break;
                }
            }
        }
        if (_chestloc != null) {
            //if (!_chestloc.isWithinDistance(mod.getPlayer().getPos(), 150)) {
            setDebugState("Destroying Chest"); // TODO: Make It check the chest instead of destroying it
            if (WorldHelper.isInteractableBlock(mod, _chestloc)) {
                return new DestroyBlockTask(_chestloc);
            }else{
                _chestloc=null;
                for (BlockPos pos : mod.getBlockTracker().getKnownLocations(Blocks.CHEST)) {
                    if (WorldHelper.isInteractableBlock(mod, pos)) {
                        _chestloc = pos;
                        break;
                    }
                }
            }
            //}
        }
        setDebugState("Searching for/Traveling around bastion");
        return _searcher;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBlockTracker().stopTracking(Blocks.CHEST);
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        return other instanceof GetSmithingTemplateTask;
    }

    @Override
    protected String toDebugStringName() {
        return "Collect " + _count + " smithing templates";
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }
}
