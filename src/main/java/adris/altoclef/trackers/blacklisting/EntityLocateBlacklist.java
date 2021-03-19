package adris.altoclef.trackers.blacklisting;

import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;

public class EntityLocateBlacklist extends AbstractObjectBlacklist<ItemEntity> {
    @Override
    protected Vec3d getPos(ItemEntity item) {
        return item.getPos();
    }
}
