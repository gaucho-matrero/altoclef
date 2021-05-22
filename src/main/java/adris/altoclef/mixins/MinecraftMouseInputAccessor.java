package adris.altoclef.mixins;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(Mouse.class)
public interface MinecraftMouseInputAccessor {
    @Invoker("onMouseButton")
    void mouseClick(long window, int button, int action, int mods);
}