package adris.altoclef.eventbus.events;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.client.MinecraftClient;

public class PlayerDeathEvent {
    public DamageSource source;

    public PlayerDeathEvent(DamageSource source) {
        this.source = source;
    }

    public String deathMessage() {
        return source.getDeathMessage(MinecraftClient.getInstance().player).getString();
    }
}
