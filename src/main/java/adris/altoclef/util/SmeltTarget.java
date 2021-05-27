package adris.altoclef.util;


import java.util.Objects;


public class SmeltTarget {
    private final ItemTarget sourceItem;
    private final ItemTarget targetItem;

    public SmeltTarget(ItemTarget sourceItem, ItemTarget targetItem) {
        this.sourceItem = sourceItem;
        this.targetItem = targetItem;
        this.targetItem.targetCount = this.sourceItem.targetCount;
    }

    public ItemTarget getSourceItem() {
        return sourceItem;
    }

    public ItemTarget getTargetItem() {
        return targetItem;
    }

    @Override
    public int hashCode() {
        int result = sourceItem != null ? sourceItem.hashCode() : 0;
        result = 31 * result + (targetItem != null ? targetItem.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SmeltTarget smeltTarget = (SmeltTarget) o;
        return Objects.equals(sourceItem, smeltTarget.sourceItem) &&
               Objects.equals(targetItem, smeltTarget.targetItem);
    }
}
