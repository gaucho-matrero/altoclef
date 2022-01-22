package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import net.minecraft.item.Items;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.Debug;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.tasksystem.Task;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public class GetToXZWithElytraTask extends Task {

    private final int _x, _z;
    private boolean _isFinished;
    private boolean _isMovingElytra = false;
    private boolean _isCollectingFireWork = false;

    public GetToXZWithElytraTask(int x, int z) {
        _x = x;
        _z = z;
    }

    @Override
    protected void onStart(AltoClef mod) {
        
    }

    @Override
    protected Task onTick(AltoClef mod) {

        //If we don't have elytra, cancel the task
        if (!mod.getItemStorage().hasItem(Items.ELYTRA) && !_isMovingElytra) {
            Debug.logWarning("We don't have an elytra");
            _isFinished = true;
            return null;
            
        }

        //Get some fireworks if doesn't have many
        if ((mod.getItemStorage().getItemCount(Items.FIREWORK_ROCKET) < 16 || _isCollectingFireWork) && mod.getItemStorage().getItemCount(Items.FIREWORK_ROCKET) < 32) {
            _isCollectingFireWork = true;
            return TaskCatalogue.getItemTask(Items.FIREWORK_ROCKET, 32);
        }
        _isCollectingFireWork = false;
        
        //Equip elytra, if didn't equipped,
        if (StorageHelper.getItemStackInSlot(new PlayerSlot(6)).getItem() != Items.ELYTRA) { 
            //EquipArmorTask(Items.ELYTRA) crash the game for unknown reason
            _isMovingElytra = true;
            return new MoveItemToSlotFromInventoryTask(new ItemTarget(Items.ELYTRA, 1), new PlayerSlot(6));//So we move it manually
        }
        _isMovingElytra = false;


        _isFinished = true;
        return null;
        
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof GetToXZWithElytraTask;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _isFinished;
    }

    @Override
    protected String toDebugString() {
        return "Moving using Elytra";
    }
}
