package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.slot.ClickSlotTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class LootContainerTask extends Task {
    private final List<Item> _protected = new ArrayList<>();
    private boolean _weDoneHere = false;
    public final BlockPos chest;
    public final List<Item> targets = new ArrayList<>();

    public LootContainerTask(BlockPos chestPos, List<Item> items) {
        chest = chestPos;
        targets.addAll(items);
    }

    @Override
    protected void onStart(AltoClef mod) {
        mod.getBehaviour().push();
        for (Item item : targets) {
            if (!mod.getBehaviour().isProtected(item)) {
                mod.getBehaviour().addProtectedItems(item);
                _protected.add(item);
            }
        }
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if(!ContainerType.screenHandlerMatches(ContainerType.CHEST)) {
            setDebugState("Interact with container");
            return new InteractWithBlockTask(chest);
        }
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!cursor.isEmpty()) {
            Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
            if (toFit.isPresent()) {
                setDebugState("Putting cursor in inventory");
                return new ClickSlotTask(toFit.get());
            } else {
                setDebugState("Ensuring space");
                return new EnsureFreeInventorySlotTask();
            }
        }
        Optional<Slot> optimal = getAMatchingSlot(mod);
        if (optimal.isEmpty()) {
            _weDoneHere = true;
            return null;
        }
        setDebugState("Looting items: " + targets);
        return new ClickSlotTask(optimal.get());
    }

    @Override
    protected void onStop(AltoClef mod, Task task) {
        if (ContainerType.screenHandlerMatches(ContainerType.CHEST)) StorageHelper.closeScreen();
        for (Item item : _protected) {
            mod.getBehaviour().removeProtectedItems(item);
        }
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof LootContainerTask && targets == ((LootContainerTask) other).targets;
    }

    private Optional<Slot> getAMatchingSlot(AltoClef mod) {
        for (Item item : targets) {
            List<Slot> slots = mod.getItemStorage().getSlotsWithItemContainer(item);
            if (!slots.isEmpty()) for (Slot slot : slots) {
                boolean hasBinding = false;
                for (NbtElement elm : StorageHelper.getItemStackInSlot(slot).getEnchantments()) {
                    NbtCompound comp = (NbtCompound) elm;
                    if (comp.getString("id").equals("minecraft:binding_curse")) {
                        hasBinding = true;
                        break;
                    }
                }
                if (!hasBinding) return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _weDoneHere || (ContainerType.screenHandlerMatches(ContainerType.CHEST) &&
                getAMatchingSlot(mod).isEmpty());
    }

    @Override
    protected String toDebugString() {
        return "Looting a container";
    }
}
