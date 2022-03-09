package adris.altoclef.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionAccessor {
    @Invoker("sendPlayerAction")
    void doSendPlayerAction(PlayerActionC2SPacket.Action action, BlockPos pos, Direction direction);

}
