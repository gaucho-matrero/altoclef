package adris.altoclef.eventbus.events;

import net.minecraft.client.util.math.MatrixStack;

public class ClientRenderEvent {
    public MatrixStack stack;
    public float tickDelta;

    public ClientRenderEvent(MatrixStack stack, float tickDelta) {
        this.stack = stack;
        this.tickDelta = tickDelta;
    }
}
