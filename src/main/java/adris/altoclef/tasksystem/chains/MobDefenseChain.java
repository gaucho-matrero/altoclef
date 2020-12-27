package adris.altoclef.tasksystem.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.RunAwayFromCreepersTask;
import adris.altoclef.tasksystem.TaskRunner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;

import java.util.ConcurrentModificationException;
import java.util.List;

public class MobDefenseChain extends SingleTaskChain {

    public MobDefenseChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    public float getPriority(AltoClef mod) {

        doForceField(mod);

        CreeperEntity blowingUp = getClosestFusingCreeper(mod);
        if (blowingUp != null) {
            Debug.logMessage("RUNNING AWAY!");
            setTask(new RunAwayFromCreepersTask(10));
            return 50 + blowingUp.getClientFuseTime(1) * 50;
        }

        return 0;
    }

    private void doForceField(AltoClef mod) {
        // Hit all hostiles close to us.
        for(Entity entity : mod.getEntityTracker().getCloseEntities()) {
            if (entity instanceof HostileEntity) {
                // TODO: Check if angerable
                mod.getControllerExtras().attack(entity);
            }
        }
    }

    private CreeperEntity getClosestFusingCreeper(AltoClef mod) {
        double worstSafety = Float.POSITIVE_INFINITY;
        CreeperEntity target = null;
        try {
            List<CreeperEntity> creepers = mod.getEntityTracker().getTrackedMobs(CreeperEntity.class);
            for (CreeperEntity creeper : creepers) {

                if (creeper == null) continue;
                if (creeper.getClientFuseTime(1) < 0.001) continue;

                // We want to pick the closest creeper, but FIRST pick creepers about to blow
                // At max fuse, the cost goes to basically zero.
                double safety = getCreeperSafety(creeper);
                if (safety < worstSafety) {
                    target = creeper;
                }
            }
        } catch (ConcurrentModificationException | ArrayIndexOutOfBoundsException | NullPointerException e ) {
            // IDK why but these exceptions happen sometimes. It's extremely bizarre and I have no idea why.
            Debug.logWarning("Weird Exception caught and ignored while scanning for creepers: " + e.getMessage());
            return target;
        }
        return target;
    }

    public static double getCreeperSafety(CreeperEntity creeper) {
        double distance = creeper.squaredDistanceTo(MinecraftClient.getInstance().player);
        float fuse = creeper.getClientFuseTime(1);

        // Not fusing. We only get fusing crepers.
        if (fuse <= 0.001f) return 0;
        return distance * (1 - fuse*fuse);
    }

    @Override
    public boolean isActive() {
        // We're always checking for mobs
        return true;
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Task is done, so I guess we move on?
    }

    @Override
    public String getName() {
        return "Mob Defense";
    }
}
