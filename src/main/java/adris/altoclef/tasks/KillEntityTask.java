package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;


public class KillEntityTask extends AbstractKillEntityTask {
    private final Entity target;
    
    public KillEntityTask(Entity entity) {
        target = entity;
    }
    
    public KillEntityTask(Entity entity, double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
        target = entity;
    }
    
    @Override
    protected boolean isSubEqual(AbstractDoToEntityTask obj) {
        if (obj instanceof KillEntityTask) {
            return ((KillEntityTask) obj).target.equals(target);
        }
        return false;
    }
    
    @Override
    protected Entity getEntityTarget(AltoClef mod) {
        return target;
    }
    
    @Override
    protected String toDebugString() {
        return "Killing " + target.getEntityName();
    }
}
