package adris.altoclef.tasksystem;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;

import java.util.ArrayList;


public class TaskRunner {
    private final ArrayList<TaskChain> chains = new ArrayList<>();
    private final AltoClef mod;
    private boolean active;
    private TaskChain cachedCurrentTaskChain;
    
    public TaskRunner(AltoClef mod) {
        this.mod = mod;
        active = false;
    }
    
    public void tick() {
        if (!active) return;
        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for (TaskChain chain : chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority(mod);
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (cachedCurrentTaskChain != null && maxChain != cachedCurrentTaskChain) {
            cachedCurrentTaskChain.onInterrupt(mod, maxChain);
        }
        cachedCurrentTaskChain = maxChain;
        if (maxChain != null) {
            maxChain.tick(mod);
        }
    }
    
    public void addTaskChain(TaskChain chain) {
        chains.add(chain);
    }
    
    public void enable() {
        if (!active) {
            mod.getConfigState().push();
            mod.getConfigState().setPauseOnLostFocus(false);
        }
        active = true;
    }
    
    public void disable() {
        if (active) {
            mod.getConfigState().pop();
        }
        for (TaskChain chain : chains) {
            chain.stop(mod);
        }
        active = false;
        
        Debug.logMessage("Stopped");
    }
    
    public TaskChain getCurrentTaskChain() {
        return cachedCurrentTaskChain;
    }
    
}
