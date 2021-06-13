package adris.altoclef.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class KillEntitiesTask extends DoToClosestEntityTask {

    public KillEntitiesTask(Predicate<Entity> ignorePredicate, Class... entities) {
        super(() -> {
            assert MinecraftClient.getInstance().player != null;
            return MinecraftClient.getInstance().player.getPos();
        }, KillEntityTask::new, ignorePredicate, entities);
    }

    public KillEntitiesTask(Class... entities) {
        super(() -> {
            assert MinecraftClient.getInstance().player != null;
            return MinecraftClient.getInstance().player.getPos();
        }, KillEntityTask::new, entities);
    }
}
