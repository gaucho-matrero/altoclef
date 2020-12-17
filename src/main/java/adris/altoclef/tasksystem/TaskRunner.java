package adris.altoclef.tasksystem;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import baritone.api.BaritoneAPI;

import java.util.ArrayList;

public class TaskRunner {

    private ArrayList<TaskChain> _chains = new ArrayList<>();
    private AltoClef _mod;
    private boolean _active;

    public TaskRunner(AltoClef mod) {
        _mod = mod;
        _active = false;
    }

    public void tick() {
        if (!_active) return;
        // Get highest priority chain and run
        TaskChain maxChain = null;
        float maxPriority = Float.NEGATIVE_INFINITY;
        for(TaskChain chain : _chains) {
            if (!chain.isActive()) continue;
            float priority = chain.getPriority();
            if (priority > maxPriority) {
                maxPriority = priority;
                maxChain = chain;
            }
        }
        if (maxChain != null) {
            maxChain.tick(_mod);
        }
    }

    public void addTaskChain(TaskChain chain) {
        _chains.add(chain);
    }

    public void enable() {
        _active = true;
    }
    public void disable() {
        for (TaskChain chain : _chains) {
            chain.stop(_mod);
        }
        _active = false;

        // Extra reset. Sometimes baritone is laggy and doesn't properly reset our press
        _mod.getClientBaritone().getInputOverrideHandler().clearAllKeys();

        Debug.logMessage("Stopped");
    }

}
