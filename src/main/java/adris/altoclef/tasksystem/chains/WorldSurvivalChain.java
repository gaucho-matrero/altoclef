package adris.altoclef.tasksystem.chains;


import adris.altoclef.AltoClef;
import adris.altoclef.tasks.EscapeFromLavaTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;


public class WorldSurvivalChain extends SingleTaskChain {
    private final Timer wasInLavaTimer = new Timer(1);
    private boolean wasAvoidingDrowning;
    
    public WorldSurvivalChain(TaskRunner runner) {
        super(runner);
    }
    
    @Override
    public float getPriority(AltoClef mod) {
        if (!mod.inGame()) return Float.NEGATIVE_INFINITY;
        
        handleDrowning(mod);
        if (isInLavaOhShit(mod)) {
            mod.getConfigState().allowWalkThroughLava(true);
            setTask(new EscapeFromLavaTask());
            return 100;
        }
        mod.getConfigState().allowWalkThroughLava(false);
        return Float.NEGATIVE_INFINITY;
    }
    
    @Override
    public String getName() {
        return "Misc World Survival Chain";
    }
    
    private void handleDrowning(AltoClef mod) {
        // Swim
        boolean drowned = true;
        if (mod.getModSettings().shouldAvoidDrowning()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                if (mod.getPlayer().isTouchingWater() && mod.getPlayer().getAir() < mod.getPlayer().getMaxAir()) {
                    // Swim up!
                    MinecraftClient.getInstance().options.keyJump.setPressed(true);
                    //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    drowned = false;
                    wasAvoidingDrowning = true;
                }
            }
        }
        // Stop swimming up if we just swam.
        if (wasAvoidingDrowning && drowned) {
            wasAvoidingDrowning = false;
            MinecraftClient.getInstance().options.keyJump.setPressed(false);
            //mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, false);
        }
    }
    
    private boolean isInLavaOhShit(AltoClef mod) {
        if (mod.getPlayer().isInLava() && !mod.getPlayer().hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            wasInLavaTimer.reset();
            return true;
        }
        return mod.getPlayer().isOnFire() && !wasInLavaTimer.elapsed();
    }
    
    @Override
    public boolean isActive() {
        // Always check for survival.
        return true;
    }
    
    @Override
    protected void onTaskFinish(AltoClef mod) {
    
    }
}
