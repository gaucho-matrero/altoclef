package adris.altoclef.mixins;

import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.text.Text;


@Mixin(DeathScreen.class)
public interface DeathScreenAccessor {
    @Accessor("message")
    Text getMessage();
}