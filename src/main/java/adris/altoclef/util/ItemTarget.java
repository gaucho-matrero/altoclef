package adris.altoclef.util;

import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.csharpisbetter.Util;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.*;

public class ItemTarget {
    private Item[] _itemMatches;
    public int targetCount;

    private String _catalogueName = null;

    private boolean _infinite = false;

    public ItemTarget(Item[] items, int targetCount) {
        _itemMatches = items;
        this.targetCount = targetCount;
    }

    public ItemTarget(String catalogueName, int targetCount) {
        if (catalogueName == null) return;
        _catalogueName = catalogueName;
        _itemMatches = TaskCatalogue.getItemMatches(catalogueName);
        this.targetCount = targetCount;
        if (_itemMatches == null) {
            Debug.logError("Invalid catalogue name for item target: \"" + catalogueName + "\". Something isn't robust!");
        }
    }

    public ItemTarget(String catalogueName) {
        this(catalogueName, 99999999); _infinite = true;
    }

    public ItemTarget(Item item, int targetCount) {
        this(new Item[] {item}, targetCount);
    }

    public ItemTarget(Item[] items) {
        this(items, 9999999);
        _infinite = true;
    }
    public ItemTarget(Item item) {
        this(item, 9999999);
        _infinite = true;
    }

    public ItemTarget(ItemTarget toCopy) {
        _itemMatches = new Item[toCopy._itemMatches.length];
        System.arraycopy(toCopy._itemMatches, 0, _itemMatches,  0, toCopy._itemMatches.length);
        _catalogueName = toCopy._catalogueName;
        targetCount = toCopy.targetCount;
        _infinite = toCopy._infinite;
    }

    public Item[] getMatches() {
        return _itemMatches;
    }

    public boolean matches(Item item) {
        for (Item match : _itemMatches) {
            if (match == null) continue;
            if (match.equals(item)) return true;
        }
        return false;
    }

    public boolean isCatalogueItem() {
        return _catalogueName != null;
    }
    public String getCatalogueName() {
        return _catalogueName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemTarget) {
            ItemTarget other = (ItemTarget) obj;
            if (_infinite) {
                if (!other._infinite) return false;
            } else {
                // Neither are infinite
                if (targetCount != other.targetCount) return false;
            }
            if ((other._itemMatches == null) != (_itemMatches == null)) return false;
            boolean isNull = (other._itemMatches == null);
            if (isNull) return true;
            if (_itemMatches.length != other._itemMatches.length) return false;
            for (int i = 0; i < _itemMatches.length; ++i) {
                if (other._itemMatches[i] == null) {
                    if ((other._itemMatches[i] == null) != (_itemMatches[i] == null)) return false;
                } else {
                    if (!other._itemMatches[i].equals(_itemMatches[i])) return false;
                }
            }
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return _itemMatches == null || _itemMatches.length == 0;
    }

    @Override
    public String toString() {

        StringBuilder result = new StringBuilder();
        if (isEmpty()) {
            result.append("(empty)");
        } else if (isCatalogueItem()) {
            result.append(_catalogueName);
        } else {
            result.append("[");
            int counter = 0;
            for (Item item : _itemMatches) {
                if (item == null) {
                    result.append("(null??)");
                } else {
                    result.append(ItemUtil.trimItemName(item.getTranslationKey()));
                }
                if (++counter != _itemMatches.length) {
                    result.append(",");
                }
            }
            result.append("]");
        }
        if (!_infinite && !isEmpty()) {
            result.append(" x ").append(targetCount);
        }

        return result.toString();
    }

    public static ItemTarget EMPTY = new ItemTarget(new Item[0], 0);

    public static Item[] getMatches(ItemTarget... targets) {
        Set<Item> result = new HashSet<>();
        for (ItemTarget target : targets) {
            result.addAll(Arrays.asList(target.getMatches()));
        }
        return Util.toArray(Item.class, result);
    }


}
