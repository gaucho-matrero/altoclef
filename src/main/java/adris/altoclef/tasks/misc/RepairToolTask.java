package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Items;
import baritone.api.utils.input.Input;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import adris.altoclef.util.time.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import baritone.api.utils.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

public class RepairToolTask extends Task {

    private ItemTarget[] _toRepair;
    private boolean _finished;
    private final TimerGame _throwTimer = new TimerGame(0.5);

    public RepairToolTask(ItemTarget... toRepair) {
        _toRepair = toRepair;
    }
    public RepairToolTask() { //If this task is called without itemtarget, repair anything we can
        this(
            new ItemTarget(ItemHelper.NETHERITE_ARMORS),
            new ItemTarget(ItemHelper.NETHERITE_TOOLS),
            new ItemTarget(Items.ELYTRA),
            new ItemTarget(ItemHelper.DIAMOND_ARMORS),
            new ItemTarget(ItemHelper.DIAMOND_TOOLS),
            new ItemTarget(ItemHelper.IRON_ARMORS),
            new ItemTarget(ItemHelper.IRON_TOOLS),
            new ItemTarget(ItemHelper.GOLDEN_ARMORS),
            new ItemTarget(ItemHelper.GOLDEN_TOOLS),
            new ItemTarget(ItemHelper.STONE_TOOLS),
            new ItemTarget(ItemHelper.LEATHER_ARMORS),
            new ItemTarget(ItemHelper.WOODEN_TOOLS),
            new ItemTarget(Items.FISHING_ROD),
            new ItemTarget(Items.FLINT_AND_STEEL),
            new ItemTarget(Items.CARROT_ON_A_STICK),
            new ItemTarget(Items.SHEARS),
            new ItemTarget(Items.BOW),
            new ItemTarget(Items.SHIELD),
            new ItemTarget(Items.TRIDENT),
            new ItemTarget(Items.CROSSBOW),
            new ItemTarget(Items.WARPED_FUNGUS_ON_A_STICK),
            new ItemTarget(Items.BOW)
        );
    }   
    @Override
    protected void onStart(AltoClef mod) {
        _throwTimer.reset();
        _toRepair = Arrays.stream(_toRepair).filter(target -> needRepair(mod, target)).toArray(ItemTarget[]::new); //Filter out items type we can't repair
        Optional<ItemTarget> ItemTargetOPTRepair = Arrays.stream(_toRepair).findFirst(); //Get the first element in our list
        if (!ItemTargetOPTRepair.isPresent()) {
            Debug.logWarning("There is nothing to repair");
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        //We start this task by filtering out every item type that we can't repair :
        //All items without mending or with no damage
        _toRepair = Arrays.stream(_toRepair).filter(target -> needRepair(mod, target)).toArray(ItemTarget[]::new);
        
        //After that, we get the first item type to repair on the list
        Optional<ItemTarget> ItemTargetOPTRepair = Arrays.stream(_toRepair).findFirst();
        
        if (ItemTargetOPTRepair.isPresent()) { //If the list is not empty
            ItemTarget ItemTargetRepair = ItemTargetOPTRepair.get(); //We get the (real) first item on the list
            
            List<Slot> SlotRepair = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, ItemTargetRepair.getMatches()); //And we get a list of every slot with that item
            
            Slot SlotRepairTarget = new PlayerSlot(-1); //Placeholder slot, will never get used if not replaced
            boolean FoundSomethingToRepair = false; //Will be set if we find the slot with the item to repair
            for (int i = 0; i < SlotRepair.size(); ++i) { //For every item slot that is on our list
                if (!FoundSomethingToRepair & StorageHelper.getItemStackInSlot(SlotRepair.get(i)).getDamage() != 0) { //if we can repair it
                    if (haveMending(mod,StorageHelper.getItemStackInSlot(SlotRepair.get(i)))) { //and it have mending
                        SlotRepairTarget = SlotRepair.get(i); //Replace the placeholder slot with the slot we found
                        FoundSomethingToRepair = true;
                    }
                }
            }
            if (FoundSomethingToRepair) { //If we found our slot, we can now repair the item !
                setDebugState("Repairing "+StorageHelper.getItemStackInSlot(SlotRepairTarget).getName().getString());
                if (!_throwTimer.elapsed()){ //If we just used a experience bottle, get the item in our hand to repair
                    mod.getSlotHandler().forceEquipSlot(SlotRepairTarget);
                    return null;
                }
                //Get the nearest experience orb
                Optional<Entity> expentityOPT = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), ExperienceOrbEntity.class);
                if (expentityOPT.isPresent()) { //if there is one
                    Entity expentity = expentityOPT.get(); //get it
                    if (expentity.getBlockPos().isWithinDistance(mod.getPlayer().getPos(), 3)) { //and if the orb is near the player
                        mod.getSlotHandler().forceEquipSlot(SlotRepairTarget); //get the item in our hand to repair the item
                    };
                    return new GetToBlockTask(expentity.getBlockPos()); //go to the orb too
                }
                if (mod.getItemStorage().hasItem(Items.EXPERIENCE_BOTTLE)) { //if we have some experience bottle
                    if (_throwTimer.elapsed()) { //the timer for throwing a experience bottle
                        if (!LookHelper.isLookingAt(mod, new Rotation(0, 90))) {
                            LookHelper.lookAt(mod, new Rotation(0, 90)); //Look at our feet
                        }
                        mod.getSlotHandler().forceEquipItem(Items.EXPERIENCE_BOTTLE); //equip it
                        mod.getInputControls().tryPress(Input.CLICK_RIGHT); //and throw it
                        _throwTimer.reset();
                    }
                    return null;
                }
                
                //So we don't have any experience bottle, we will kill some zombies to get XP
                Optional<Entity> entity = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), ZombieEntity.class); //Find a zombie
                if (entity.isPresent()) {
                    return new KillEntityTask(entity.get()); //And kill it
                }
                return new TimeoutWanderTask(); //If there is no zombie, wander
            }
        } //If there is no items in the list of itemtype to repair, it means there is nothing to repair :)
        _finished = true;
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }
    //Check if a type of item can be repaired.
    public static boolean needRepair(AltoClef mod, ItemTarget target) {
        List<Slot> SlotRepair = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());
        boolean FoundSomethingToRepair = false;
        for (int i = 0; i < SlotRepair.size(); ++i) {
            if (!FoundSomethingToRepair & StorageHelper.getItemStackInSlot(SlotRepair.get(i)).getDamage() != 0) {
                if (haveMending(mod,StorageHelper.getItemStackInSlot(SlotRepair.get(i)))) {
                    FoundSomethingToRepair = true;
                }
            }
        }
        return FoundSomethingToRepair;
    }
    //Will get the durability of an item in accordance of the ItemTarget.
    //Return the durability of one of the item, or -1 if all targeted items is repaired or doesn't have the targeted item
    public static int getDurabilityOfRepairableItem(AltoClef mod, ItemTarget target) {
        List<Slot> SlotRepair = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());
        for (int i = 0; i < SlotRepair.size(); ++i) {
            if (StorageHelper.getItemStackInSlot(SlotRepair.get(i)).getDamage() != 0) {
                if (haveMending(mod,StorageHelper.getItemStackInSlot(SlotRepair.get(i)))) {
                    return StorageHelper.getItemStackInSlot(SlotRepair.get(i)).getMaxDamage()-StorageHelper.getItemStackInSlot(SlotRepair.get(i)).getDamage();
                }
            }
        }
        return -1;
    }

    //Test if the itemstack have mending on it
    public static boolean haveMending(AltoClef mod, ItemStack target) {
        boolean canRepair = false;
        for (NbtElement elm : target.getEnchantments()) {
            NbtCompound comp = (NbtCompound) elm;
            if (comp.getString("id").equals("minecraft:mending")) {
                canRepair = true;
            }
        }
        return canRepair;
    }
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof RepairToolTask task) {
            return Arrays.equals(task._toRepair, _toRepair);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Repairing an item";
    }

}