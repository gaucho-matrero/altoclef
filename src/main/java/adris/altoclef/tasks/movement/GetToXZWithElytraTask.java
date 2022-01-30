package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasks.misc.RepairToolTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public class GetToXZWithElytraTask extends Task {

    private final int _x, _z;
    private boolean _isFinished;
    private boolean _isMovingElytra = false;
    private boolean _isCollectingFireWork = false;
    private boolean _isFlyRunning = false;
    private boolean _repairElytra = false;
    private double _oldCoordsY;
    private int _yGoal = 0;
    private int _fx = 0;
    private int _fz = 0;
    private final TimerGame _fireWorkTimer = new TimerGame(3);
    private final TimerGame _messageProgessTimer = new TimerGame(3);
    private final TimerGame _getToSurfaceTimer = new TimerGame(2);
    private final TimerGame _jumpTimer = new TimerGame(0.1);

    public GetToXZWithElytraTask(int x, int z) {
        _x = x;
        _z = z;
    }
    @Override
    protected void onStart(AltoClef mod) {
        _jumpTimer.reset();
        _fireWorkTimer.reset();
        _messageProgessTimer.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        double dist = mod.getPlayer().getPos().distanceTo(new Vec3d(_x, mod.getPlayer().getPos().y, _z)); //Calculate distance
        if (!_isFlyRunning) { //If we are already flying, jump that code section
            _fx = 0;
            _fz = 0;
            mod.getBehaviour().disableDefence(false); //Enable mob defence
            if ((int)dist == 0) { //We are where we need to go !
                _isFinished = true; //End the task
                return null;
            }
            if (dist < 128) { //We are near our goal
                setDebugState("Walking to goal");
                return new GetToXZTask(_x, _z); //Get to our goal
            }
            //If we don't have elytra, walk to our goal
            if (!mod.getItemStorage().hasItem(Items.ELYTRA) && !_isMovingElytra && StorageHelper.getItemStackInSlot(new PlayerSlot(6)).getItem() != Items.ELYTRA) {
                setDebugState("Walking to goal, since we don't have an elytra");
                return new GetToXZTask(_x, _z);
            }

            if (_repairElytra && RepairToolTask.needRepair(mod, new ItemTarget(Items.ELYTRA))) { //If we can repair it
                setDebugState("Repairing elytra");
                return new RepairToolTask(new ItemTarget(Items.ELYTRA)); //Run the task to repair it
            } else {
                _repairElytra = false;
            }
            int durabilityLeft = RepairToolTask.getDurabilityOfRepairableItem(mod, new ItemTarget(Items.ELYTRA)); //Get the elytra durability

            if (durabilityLeft < 35 && durabilityLeft != -1) { //If we need to repair it before flying
                if (RepairToolTask.needRepair(mod, new ItemTarget(Items.ELYTRA))) {
                    _repairElytra = true;
                    return null;
                }
                setDebugState("Walking to goal, elytra is broken / will broke");
                return new GetToXZTask(_x, _z); //Walk to our goal
            }

            //Get some fireworks if doesn't have many
            if ((mod.getItemStorage().getItemCount(Items.FIREWORK_ROCKET) < 16 || _isCollectingFireWork) && mod.getItemStorage().getItemCount(Items.FIREWORK_ROCKET) < 32) {
                _isCollectingFireWork = true;
                setDebugState("Getting some fireworks");
                return TaskCatalogue.getItemTask(Items.FIREWORK_ROCKET, 32);
            }
            _isCollectingFireWork = false;
            
            //Equip elytra, if didn't equipped,
            setDebugState("Equipping elytra");
            if (StorageHelper.getItemStackInSlot(new PlayerSlot(6)).getItem() != Items.ELYTRA) { 
                //EquipArmorTask(Items.ELYTRA) was crashing the game because of "class net.minecraft.item.ElytraItem cannot be cast to class net.minecraft.item.ArmorItem"
                _isMovingElytra = true;
                return new MoveItemToSlotFromInventoryTask(new ItemTarget(Items.ELYTRA, 1), new PlayerSlot(6));//So we move it manually
            }
            _isMovingElytra = false;

            //We move to the surface, because we can't fly in caves :)
            setDebugState("Moving to the surface");
            
            int y = getGroundHeightWithRadius(mod, (int)mod.getPlayer().getPos().x, (int)mod.getPlayer().getPos().z); //get the highest block near the player
            if (_yGoal == 0 || _yGoal < mod.getPlayer().getPos().y) { //If we don't have a goal or the player is higher than our current goal
                _yGoal = y; //Set the new goal
            }
            if (y > mod.getPlayer().getPos().y || !_getToSurfaceTimer.elapsed()) { //if the highest block is higher than the player or the _getToSurfaceTimer isn't elapsed
                if (y > mod.getPlayer().getPos().y) { //if the highest block is higher than the player
                    _getToSurfaceTimer.reset(); //reset the timer
                }
                return new GetToYTask(_yGoal+1); //Get to the surface
            }
            _yGoal = 0;
            _fireWorkTimer.forceElapse();
        }
        _isFlyRunning = true; //We will now try to fly, we don't need to check the code before this for now.

        mod.getBehaviour().disableDefence(true); //Disable MobDefence and MLG, because it get interupted by that
        
        //Get the elytra's durability
        ItemStack elytraItem = StorageHelper.getItemStackInSlot(new PlayerSlot(6));
        int durabilityLeft = elytraItem.getMaxDamage()-elytraItem.getDamage();
        
        float yaw = LookHelper.getLookRotation(mod,new Vec3d(_x, 1, _z)).getYaw(); //The players's direction
        float pitch; //Players's pitch, will be set later

        if (mod.getPlayer().getPos().y > WorldHelper.WORLD_CEILING_Y) { //When flying higher than the world's height limit
            if (_oldCoordsY > mod.getPlayer().getPos().y && _fireWorkTimer.elapsed()) {
                pitch = (float)10; //Look a bit down, when flying down
            } else {
                pitch = (float)-10; //If we are flying up or using a firework, look a bit up
            }
        } else {
            pitch = (float)-40; //When flying under the world's height limit, we need to go up, so we look up
        }
        
        setDebugState("Going to "+_x+" "+_z);

        if (durabilityLeft < 35) { //If the durability is below 35, we need to get on the ground safely before the elytra break
            if ((mod.getPlayer().getPos().distanceTo(new Vec3d(_fx, mod.getPlayer().getPos().y, _fz)) > 30) || _fx == 0 || _fz == 0) { //if we need to set the "land point"
               _fx = (int)mod.getPlayer().getPos().x; //Set a landing point where we are
               _fz = (int)mod.getPlayer().getPos().z;
            }
            if (_fx != 0 && _fz != 0) { //If there is a landing point
                dist = mod.getPlayer().getPos().distanceTo(new Vec3d(_fx, mod.getPlayer().getPos().y, _fz)); //Recalculate distance for the landing point
                yaw = LookHelper.getLookRotation(mod,new Vec3d(_fx, 1, _fz)).getYaw(); //And the players's direction too
                //Setting that will trigget the bot to land
            }
        }

        if (dist > 15) { //Things to do when flying (or trying to fly)
            if (_jumpTimer.elapsed()) {//every 0.1 sec: jump, to enable elytra
                mod.getInputControls().tryPress(Input.JUMP);
                _jumpTimer.reset();
            }
            //If we can use firework rocket, if we have one, and are under y=WorldHelper.WORLD_CEILING_Y+8
            if (_fireWorkTimer.elapsed() && mod.getPlayer().getPos().y < WorldHelper.WORLD_CEILING_Y+8 && mod.getItemStorage().hasItem(Items.FIREWORK_ROCKET)) {
                if (mod.getSlotHandler().forceEquipItem(Items.FIREWORK_ROCKET)) {//try to equip the item
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT); //and use it
                    _fireWorkTimer.reset();
                    pitch = (float)-10;
                }
            }
            //Log a message in chat
            if (_messageProgessTimer.elapsed()) {
                Debug.logMessage("Distance: "+(int)dist+", Elytra durability: "+durabilityLeft);
                _messageProgessTimer.reset();
            }
        } else { //if the distance is under 15, we need to land slowly
            setDebugState("Landing...");
            if (getGroundHeightWithRadius(mod, (int)mod.getPlayer().getPos().x, (int)mod.getPlayer().getPos().z)+50 > mod.getPlayer().getPos().y) {
                pitch = (float)20; //look a bit down
            } else {
                pitch = (float)50; //look down, to land faster
            }
        }

        if (dist > 6) { //if the distance is upper than 6
            if (!LookHelper.isLookingAt(mod, new Rotation(yaw, pitch))) {
                LookHelper.lookAt(mod, new Rotation(yaw, pitch)); //Look at the target
            }
            
        }
        //if we have landed, and the distance is under 12 or we don't have any fireworks
        if (_oldCoordsY == mod.getPlayer().getPos().y && (dist < 12 || !mod.getItemStorage().hasItem(Items.FIREWORK_ROCKET))) {
            if (StorageHelper.getItemStackInSlot(new PlayerSlot(6)).getItem() == Items.ELYTRA) { //Unequip elytra
                return new ClickSlotTask(new PlayerSlot(6)); //Click on the elytra in the armor slot
            } else if (!StorageHelper.getItemStackInCursorSlot().isEmpty()){ //Once it's in the cursor slot
                List<Slot> airslot = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, Items.AIR); //Click on a empty inv slot
                if (airslot.size() == 0) {
                    return new EnsureFreeInventorySlotTask(); //If there is no space
                } else {
                    return new ClickSlotTask(airslot.get(0)); //Click on the slot to put elytra back in inventory
                }
            } else {
                 _isFlyRunning = false; //Recheck the code at the start of this task to
            }
        }
         _oldCoordsY = mod.getPlayer().getPos().y; //save the old player y position
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().disableDefence(false);
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
    private int getGroundHeight(AltoClef mod, int x, int z) {
        for (int y = WorldHelper.WORLD_CEILING_Y; y >= WorldHelper.WORLD_FLOOR_Y; --y) {
            BlockPos check = new BlockPos(x, y, z);
            if (!WorldHelper.isAir(mod, check)) return y;
        }
        return -1;
    }
    private int getGroundHeightWithRadius(AltoClef mod, int x, int z) {
        int topY = 0;
        for (int x2 = 5; x2 >= -5; --x2) {
            for (int z2 = 5; z2 >= -5; --z2) {
                int tmpy = getGroundHeight(mod,x+x2,z+z2);
                if (tmpy > topY) {
                    topY = tmpy;
                }
            }
        }
        return topY;
    }
}
