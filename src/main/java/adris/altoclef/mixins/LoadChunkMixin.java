package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChunkLoadEvent;
import adris.altoclef.eventbus.events.ChunkUnloadEvent;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ClientChunkManager.class)
public class LoadChunkMixin {

    @Inject(
            method = "loadChunkFromPacket",
            at = @At("RETURN")
    )
    private void onLoadChunk(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer, CallbackInfoReturnable<WorldChunk> ci) {
        EventBus.publish(new ChunkLoadEvent(ci.getReturnValue()));
    }

    @Inject(
            method = "unload",
            at = @At("TAIL")
    )
    private void onChunkUnload(int x, int z, CallbackInfo ci) {
        EventBus.publish(new ChunkUnloadEvent(new ChunkPos(x, z)));
    }
}
