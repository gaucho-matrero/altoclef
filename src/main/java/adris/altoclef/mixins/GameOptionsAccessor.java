package adris.altoclef.mixins;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {
    @Accessor
    SimpleOption<Double> getGamma();

    @Accessor( "gamma" )
    void setGamma( SimpleOption<Double> gamma );
    @Accessor
    SimpleOption<Boolean> getAutoJump();

    @Accessor( "autoJump" )
    void setAutoJump( SimpleOption<Boolean> autoJump );
}
