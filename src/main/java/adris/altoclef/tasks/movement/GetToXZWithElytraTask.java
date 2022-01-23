package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.movement.GetToYTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.block.BlockState;
import baritone.api.utils.Rotation;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.world.event.GameEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Optional;

import static net.minecraft.world.event.GameEvent.ELYTRA_FREE_FALL;

public class GetToXZWithElytraTask extends Task {

    private final int _x, _z;
    private boolean _isFinished;
    private boolean _isMovingElytra = false;
    private boolean _isCollectingFireWork = false;
    private boolean _isFlyRunning = false;
    private boolean _hasJumped = false;
    private boolean _wasMovingToSurface = false;
    private boolean _wasDoingWanderTask = false;
    private double _oldCoordsY;
    private final TimerGame _fireWorkTimer = new TimerGame(3);
    private final TimerGame _wanderTimer = new TimerGame(5);
    private final TimerGame _jumpTimer = new TimerGame(0.1);

    public GetToXZWithElytraTask(int x, int z) {
        _x = x;
        _z = z;
    }
    @Override
    protected void onStart(AltoClef mod) {
        _jumpTimer.reset();
        _fireWorkTimer.reset();
    }

    @Override
    protected Task onTick(AltoClef mod) {
        double dist = mod.getPlayer().getPos().distanceTo(new Vec3d(_x, mod.getPlayer().getPos().y, _z)); //Calculate distance
        if (!_isFlyRunning) {
            mod.getBehaviour().disableDefence(false); //Enable mob defence
            if ((int)dist == 0) { //We are where we need to go !
                _isFinished = true;
                return null;
            }
            if (dist < 64) { //We are near, but not exactly where we need to go
                setDebugState("Walking to goal");
                return new GetToXZTask(_x, _z);
            }

            //If we don't have elytra, cancel the task
            if (!mod.getItemStorage().hasItem(Items.ELYTRA) && !_isMovingElytra && StorageHelper.getItemStackInSlot(new PlayerSlot(6)).getItem() != Items.ELYTRA) {
                setDebugState("Walking to goal, since we don't have an elytra");
                return new GetToXZTask(_x, _z);
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
                //EquipArmorTask(Items.ELYTRA) crash the game for unknown reason
                _isMovingElytra = true;
                return new MoveItemToSlotFromInventoryTask(new ItemTarget(Items.ELYTRA, 1), new PlayerSlot(6));//So we move it manually
            }
            _isMovingElytra = false;

            //We move to the surface, because we can't fly in caves :)
            setDebugState("Moving to the surface");
            int y = WorldHelper.getGroundHeight(mod, (int)mod.getPlayer().getPos().x, (int)mod.getPlayer().getPos().z);
            if (y > mod.getPlayer().getPos().y && !_wasDoingWanderTask) {
                _wasMovingToSurface = true;
                _wanderTimer.reset();
                return new GetToYTask(y); //Get to the surface
            }
            //Find a place to take off
            setDebugState("Finding a place to take off");
            if (_wasMovingToSurface && !_wanderTimer.elapsed()) {
                _wasDoingWanderTask = true;
                return new TimeoutWanderTask(); //move randomly until we find a place to fly
            } else {
                if (haveSpaceToTakeOff(mod,mod.getPlayer().getBlockPos())) { //if there is a space to fly
                    _wasMovingToSurface = false;
                    _wasDoingWanderTask = false;
                    //this will allow the bot to continue and start trying to fly
                } else {
                    _wanderTimer.reset();
                    return new TimeoutWanderTask();//wander again if there is some blocks
                }
            }
        }
        _isFlyRunning = true;
        mod.getBehaviour().disableDefence(true); //Disable MobDefence and MLG, because it get interupted by that
        
        float yaw = LookHelper.getLookRotation(mod,new Vec3d(_x, 1, _z)).getYaw();
        float pitch;

        if (mod.getPlayer().getPos().y > 255) { //When flying up upper y=255
            if (_oldCoordsY > mod.getPlayer().getPos().y && _fireWorkTimer.elapsed()) {
                pitch = (float)10; //When flying down
            } else {
                pitch = (float)-10; // If we are flying up or using a firework
            }
        } else {
            pitch = (float)-40; //When flying up under y=255, need to go in the sky!
        }

        setDebugState("Going to "+_x+" "+_z);
        if (dist > 15) {
            if (_jumpTimer.elapsed()) {//every 0.1 sec, jump, to enable elytra
                mod.getInputControls().tryPress(Input.JUMP);
                _jumpTimer.reset();
            }
            //If we can use firework rocket (every 5 secs), if we have one, and are under y=260
            if (_fireWorkTimer.elapsed() && mod.getPlayer().getPos().y < 260 && mod.getItemStorage().hasItem(Items.FIREWORK_ROCKET)) {
                int y = WorldHelper.getGroundHeight(mod, (int)mod.getPlayer().getPos().x, (int)mod.getPlayer().getPos().z); //Look if there is a block on top of us
                if (y > mod.getPlayer().getPos().y) { //if there is one
                    _isFlyRunning = false; //cancel the flight
                    return null;
                }
                if (mod.getSlotHandler().forceEquipItem(Items.FIREWORK_ROCKET)) {//try to equip the item
                    mod.getInputControls().tryPress(Input.CLICK_RIGHT); //and use it
                    _fireWorkTimer.reset();
                    pitch = (float)-10;
                }
            }
        } else { //if the distance is under 15, we need to land slowly
            setDebugState("Landing...");
            pitch = (float)20; //look a bit down
        }

        if (dist > 6) { //if the distance is upper than 6
            if (!LookHelper.isLookingAt(mod, new Rotation(yaw, pitch))) {
                LookHelper.lookAt(mod, new Rotation(yaw, pitch)); //Look at where to look
            }
            
        }
        //if we have landed, and the distance is under 12 or we don't have any fireworks
        if (_oldCoordsY == mod.getPlayer().getPos().y && (dist < 12 || !mod.getItemStorage().hasItem(Items.FIREWORK_ROCKET))) {
            _isFlyRunning = false; //reset the whole task
        }
         _oldCoordsY = mod.getPlayer().getPos().y; //save old player y position
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
    private boolean haveSpaceToTakeOff(AltoClef mod, BlockPos pos) {
        final Vec3i[] CHECK = new Vec3i[]{
                new Vec3i(0, 0, 0),
                new Vec3i(-1, 0, 0),
                new Vec3i(1, 0, 0),
                new Vec3i(0, 0, 1),
                new Vec3i(-1, 0, 1),
                new Vec3i(1, 0, 1),
                new Vec3i(0, 0, -1),
                new Vec3i(-1, 0, -1),
                new Vec3i(1, 0, -1),
                new Vec3i(0, 1, 0),
                new Vec3i(-1, 1, 0),
                new Vec3i(1, 1, 0),
                new Vec3i(0, 1, 1),
                new Vec3i(-1, 1, 1),
                new Vec3i(1, 1, 1),
                new Vec3i(0, 1, -1),
                new Vec3i(-1, 1, -1),
                new Vec3i(1, 1, -1),
        };
        for (Vec3i offs : CHECK) {
            if (WorldHelper.isSolid(mod, pos.add(offs))) return false;
        }
        return true;
    }
}
