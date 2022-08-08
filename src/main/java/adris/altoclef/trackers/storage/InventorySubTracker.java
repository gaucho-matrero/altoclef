package adris.altoclef.trackers.storage;

import adris.altoclef.trackers.Tracker;
import adris.altoclef.trackers.TrackerManager;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Keeps track of the player's inventory items
 */
public class InventorySubTracker extends Tracker {

    private final HashMap<Item, List<Slot>> _itemToSlotPlayer = new HashMap<>();
    private final HashMap<Item, List<Slot>> _itemToSlotContainer = new HashMap<>();
    private final HashMap<Item, Integer> _itemCountsPlayer = new HashMap<>();
    private final HashMap<Item, Integer> _itemCountsContainer = new HashMap<>();

    private ScreenHandler _prevScreenHandler;

    public InventorySubTracker(TrackerManager manager) {
        super(manager);
    }

    public int getItemCount(boolean playerInventory, boolean containerInventory, Item ...items) {
        ensureUpdated();
        int result = 0;
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        for (Item item : items) {
            if (playerInventory && cursorStack.getItem().equals(item))
                result += cursorStack.getCount();
            if (playerInventory)
                result += _itemCountsPlayer.getOrDefault(item, 0);
            if (containerInventory)
                result += _itemCountsContainer.getOrDefault(item, 0);
        }
        return result;
    }
    public boolean hasItem(boolean playerInventoryOnly, Item ...items) {
        ensureUpdated();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        for (Item item : items) {
            if (cursorStack.getItem().equals(item))
                return true;
            if (_itemCountsPlayer.containsKey(item))
                return true;
            if (!playerInventoryOnly && _itemCountsContainer.containsKey(item))
                return true;
        }
        return false;
    }
    public List<Slot> getSlotsWithItems(boolean playerInventory, boolean containerInventory, Item ...items) {
        ensureUpdated();
        List<Slot> result = new ArrayList<>();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        for (Item item : items) {
            if (playerInventory && cursorStack.getItem().equals(item))
                result.add(CursorSlot.SLOT);
            if (playerInventory)
                result.addAll(_itemToSlotPlayer.getOrDefault(item, Collections.emptyList()));
            if (containerInventory)
                result.addAll(_itemToSlotContainer.getOrDefault(item, Collections.emptyList()));
        }
        return result;
    }

    public List<ItemStack> getInventoryStacks(boolean includeCursor) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.getInventory() == null)
            return Collections.emptyList();
        PlayerInventory inv = player.getInventory();
        // 36 player + 1 offhand + 4 armor
        List<ItemStack> result = new ArrayList<>(41 + (includeCursor ? 1 : 0));
        if (includeCursor) {
            result.add(StorageHelper.getItemStackInCursorSlot());
        }
        result.addAll(inv.main);
        result.addAll(inv.armor);
        result.addAll(inv.offHand);
        return result;
    }

    private List<Slot> getSlotsThatCanFit(HashMap<Item, List<Slot>> list, ItemStack item, boolean acceptPartial) {
        List<Slot> result = new ArrayList<>();
        // First add fillable slots
        for (Slot toCheckStackable : list.getOrDefault(item.getItem(), Collections.emptyList())) {
            // Ignore cursor slot.
            if (Slot.isCursor(toCheckStackable))
                continue;
            ItemStack stackToAddTo = StorageHelper.getItemStackInSlot(toCheckStackable);
            // We must have SOME room left, then we decide whether we care about having ENOUGH
            if (!stackToAddTo.isEmpty() && ItemHelper.canStackTogether(item, stackToAddTo)) {
                int roomLeft = stackToAddTo.getMaxCount() - stackToAddTo.getCount();
                if (acceptPartial || roomLeft > item.getCount()) {
                    result.add(toCheckStackable);
                }
            }
        }
        // Then add air slots that can insert our item
        if (MinecraftClient.getInstance().player != null) {
            ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
            for (Slot airSlot : list.getOrDefault(Items.AIR, Collections.emptyList())) {
                // Ignore cursor slot
                if (airSlot.equals(CursorSlot.SLOT))
                    continue;
                int windowCheck = airSlot.getWindowSlot();
                // ignore 2x2 crafting grid -- it isn't meant for storage
                if(windowCheck>=1 && windowCheck <=4){
                    continue;
                }
                // Special case: Armor/shield, we wish to ignore these if our inventory is not open.
                if (windowCheck < handler.slots.size() && handler.getSlot(windowCheck).canInsert(item)) {
                    result.add(airSlot);
                }
            }
        }
        return result;
    }

    public List<Slot> getSlotsThatCanFit(boolean includePlayer, boolean includeContainer, ItemStack item, boolean acceptPartial) {
        ensureUpdated();
        final List<Slot> result = new ArrayList<>();
        if (includePlayer)
            result.addAll(getSlotsThatCanFit(_itemToSlotPlayer, item, acceptPartial));
        if (includeContainer)
            result.addAll(getSlotsThatCanFit(_itemToSlotContainer, item, acceptPartial));
        return result;
    }

    public boolean hasEmptySlot(boolean playerInventoryOnly) {
        return hasItem(playerInventoryOnly, Items.AIR);
    }

    private void registerItem(ItemStack stack, Slot slot, boolean isSlotPlayerInventory) {
        Item item = stack.getItem();
        int count = stack.getCount();
        if (stack.isEmpty()) {
            // If our cursor slot is empty, IGNORE IT as we don't want to treat it as a valid slot.
            item = Items.AIR;
            count = 0;
        }

        if (isSlotPlayerInventory) {
            _itemCountsPlayer.put(item, _itemCountsPlayer.getOrDefault(item, 0) + count);
        } else {
            _itemCountsContainer.put(item, _itemCountsContainer.getOrDefault(item, 0) + count);
        }

        if (slot != null) {
            HashMap<Item, List<Slot>> toAdd = isSlotPlayerInventory? _itemToSlotPlayer : _itemToSlotContainer;
            if (!toAdd.containsKey(item))
                toAdd.put(item, new ArrayList<>());
            toAdd.get(item).add(slot);
        }
    }

    @Override
    protected void updateState() {
        _prevScreenHandler = MinecraftClient.getInstance().player != null? MinecraftClient.getInstance().player.currentScreenHandler : null;

        _itemToSlotPlayer.clear();
        _itemToSlotContainer.clear();
        _itemCountsPlayer.clear();
        _itemCountsContainer.clear();
        if (MinecraftClient.getInstance().player == null)
            return;
        ScreenHandler handler = MinecraftClient.getInstance().player.currentScreenHandler;
        if (handler == null)
            return;
        for (Slot slot : Slot.getCurrentScreenSlots()) {
            // Ignore cursor slot, that's handled separately.
            if (slot.equals(CursorSlot.SLOT))
                continue;
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            // Add separately if we're in a container vs player inventory.

            if (!shouldIgnoreSlotForContainer(slot)) {
                registerItem(stack, slot, slot.isSlotInPlayerInventory());
            }
        }
    }

    @Override
    protected void reset() {
        _itemToSlotPlayer.clear();
        _itemToSlotContainer.clear();
        _itemCountsPlayer.clear();
        _itemCountsContainer.clear();
    }

    @Override
    protected boolean isDirty() {
        ScreenHandler handler = MinecraftClient.getInstance().player != null? MinecraftClient.getInstance().player.currentScreenHandler : null;
        return super.isDirty() || handler != _prevScreenHandler;
    }

    private static boolean shouldIgnoreSlotForContainer(Slot slot) {
        // IMPORTANT NOTE:!!!!
        // Ignore crafting table output when calculating container slots.
        //
        // Why?
        // Because we don't want the bot to think we "have" an item if it's in our output slot. Otherwise it will
        // softlock because it will assume we're all good (we got the item!) when in reality we need to grab that item.
        //
        // We also don't want our bot to think we "have" an item if it's in our armor/crafting output/shield slots.
        if (slot instanceof CraftingTableSlot && slot.equals(CraftingTableSlot.OUTPUT_SLOT))
            return true;
        if (slot instanceof PlayerSlot) {
            // Ignore non-normal inventory slots
            int window = slot.getWindowSlot();
            return window == 0 || (window > 4 && window < 9) || window > 44; // true if not a crafting input slot, or inventory slot.
            // see https://www.bing.com/images/search?view=detailV2&ccid=GXPg52lU&id=39603603AB7D76576A2105B2F6BE2A0FC2B2D105&thid=OIP.GXPg52lU6gsFhvkpox6s7gAAAA&mediaurl=https%3A%2F%2Fproxy.spigotmc.org%2Fe591497c17b455c6ca7fce480c2b1107d70959f7%3Furl%3Dhttp%3A%252F%252Fwiki.vg%252Fimages%252F1%252F13%252FInventory-slots.png&cdnurl=https%3A%2F%2Fth.bing.com%2Fth%2Fid%2FR.1973e0e76954ea0b0586f929a31eacee%3Frik%3DBdGywg8qvvayBQ%26pid%3DImgRaw%26r%3D0%26sres%3D1%26sresct%3D1&exph=331&expw=352&q=minecraft+player+slots&simid=607989819895736642&form=IRPRST&ck=5F38F8F6548913579460DF7D15D859C2&selectedindex=13&ajaxhist=0&ajaxserp=0&vt=0
        }
        return false;
    }
}
