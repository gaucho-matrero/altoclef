package adris.altoclef.tasks;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.function.Predicate;

public class KillEntitiesTask extends DoToClosestEntityTask {

    public KillEntitiesTask(Predicate<Entity> ignorePredicate, Class... entities) {
        super(KillEntityTask::new, ignorePredicate, entities);
    }

    public KillEntitiesTask(Class... entities) {
        super(KillEntityTask::new, entities);
    }
}
