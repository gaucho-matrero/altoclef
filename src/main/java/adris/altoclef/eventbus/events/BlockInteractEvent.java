package adris.altoclef.eventbus.events;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;

public class BlockInteractEvent {
    public BlockHitResult hitResult;
    public ClientWorld world;

    public BlockInteractEvent(BlockHitResult hitResult, ClientWorld world) {
        this.hitResult = hitResult;
        this.world = world;
    }
}
