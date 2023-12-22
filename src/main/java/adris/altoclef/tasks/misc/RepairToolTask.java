package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.entity.KillEntityTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.entity.DoToClosestEntityTask;
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
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.item.ItemStack;
import java.util.List;
import java.util.Optional;
import java.util.Arrays;

public class RepairToolTask extends Task {

    private final ItemTarget[] _toRepair;

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
    }

    @Override
    protected Task onTick(AltoClef mod) {
        //We start this task by filtering out every item type that we can't repair :
        //All items without mending or with no damage
        ItemTarget[] shouldRepair = Arrays.stream(_toRepair).filter(target -> needRepair(mod, target)).toArray(ItemTarget[]::new);
        
        //After that, we get the first item type to repair on the list
        Optional<ItemTarget> itemTargetOPTRepair = Arrays.stream(shouldRepair).findFirst();

        if (itemTargetOPTRepair.isPresent()) { //If the list is not empty
            ItemTarget itemTargetRepair = itemTargetOPTRepair.get(); //We get the (real) first item on the list

            List<Slot> slotRepairs = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, itemTargetRepair.getMatches()); //And we get a list of every slot with that item

            Optional<Slot> slotRepairTarget = Optional.empty();
            for (Slot couldRepair : slotRepairs) {
                if (slotRepairTarget.isEmpty() && StorageHelper.getItemStackInSlot(couldRepair).getDamage() != 0) { //if we can repair it
                    if (EnchantmentHelper.get(StorageHelper.getItemStackInSlot(couldRepair)).containsKey(Enchantments.MENDING)) { //and it have mending
                        slotRepairTarget = Optional.of(couldRepair); //Replace the placeholder slot with the slot we found
                    }
                }
            }
            if (slotRepairTarget.isPresent()) { //If we found our slot, we can now repair the item !
                final Slot ItemToEquip = slotRepairTarget.get();
                setDebugState("Repairing " +  StorageHelper.getItemStackInSlot(ItemToEquip).getName().getString());
                if (!_throwTimer.elapsed()){ //If we just used a experience bottle, get the item in our hand to repair
                    mod.getSlotHandler().forceEquipSlot(ItemToEquip);
                    return null;
                }
                //Get the nearest experience orb
                boolean isExpPresent = mod.getEntityTracker().entityFound(ExperienceOrbEntity.class);
                if (isExpPresent) { //if there is one
                    setDebugState("Collecting EXP Orbs");
                    return new DoToClosestEntityTask(entity -> { //Get to the entity
                        if (entity.isInRange(mod.getPlayer(), 3)) { //and if the orb is near the player
                            mod.getSlotHandler().forceEquipSlot(ItemToEquip); //get the item in our hand to repair the item
                        }
                        return new GetToEntityTask(entity, 0);
                    }, ExperienceOrbEntity.class);
                }
                if (mod.getItemStorage().hasItem(Items.EXPERIENCE_BOTTLE)) { //if we have some experience bottle
                    setDebugState("Throwing EXP Bottles for EXP");
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

                setDebugState("Killing Zombies for EXP");
                return new DoToClosestEntityTask(KillEntityTask::new, ZombieEntity.class);
            }
        } //If there is no items in the list of itemtype to repair, it means there is nothing to repair :)
        setDebugState("Done");
        _finished = true;
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }
    //Check if a type of item can be repaired.
    public static boolean needRepair(AltoClef mod, ItemTarget target) {
        List<Slot> slotRepair = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());
        for (Slot couldRepair : slotRepair) {
            if (StorageHelper.getItemStackInSlot(couldRepair).getDamage() != 0) {
                if (EnchantmentHelper.get(StorageHelper.getItemStackInSlot(couldRepair)).containsKey(Enchantments.MENDING)) {
                    return true;
                }
            }
        }
        return false;
    }
    //Will get the durability of an item in accordance of the ItemTarget.
    //Return the durability of one of the item, or -1 if all targeted items is repaired or doesn't have the targeted item
    public static int getDurabilityOfRepairableItem(AltoClef mod, ItemTarget target) {
        List<Slot> slotRepairs = mod.getItemStorage().getSlotsWithItemPlayerInventory(false, target.getMatches());
        for (Slot couldRepair : slotRepairs) {
            if (StorageHelper.getItemStackInSlot(couldRepair).getDamage() != 0) {
                if (EnchantmentHelper.get(StorageHelper.getItemStackInSlot(couldRepair)).containsKey(Enchantments.MENDING)) {
                    return StorageHelper.getItemStackInSlot(couldRepair).getMaxDamage() - StorageHelper.getItemStackInSlot(couldRepair).getDamage();
                }
            }
        }
        return -1;
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
        return "Repairing: " + Arrays.toString(_toRepair);
    }

}