package adris.altoclef.tasks;

import net.minecraft.client.MinecraftClient;

public class KillEntitiesTask extends DoToClosestEntityTask {

    public KillEntitiesTask(Class... entities) {
        super(() -> {
            assert MinecraftClient.getInstance().player != null;
            return MinecraftClient.getInstance().player.getPos();
        }, KillEntityTask::new, entities);
    }
}
