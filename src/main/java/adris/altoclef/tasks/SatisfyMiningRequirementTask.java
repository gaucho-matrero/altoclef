package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.TaskCatalogue;

public class SatisfyMiningRequirementTask extends Task {

    private MiningRequirement _requirement;

    public SatisfyMiningRequirementTask(MiningRequirement requirement) {
        _requirement = requirement;
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        switch (_requirement) {
            case HAND:
                // Will never happen if you program this right
                break;
            case WOOD:
                return TaskCatalogue.getItemTask("wooden_pickaxe", 1);
            case STONE:
                return TaskCatalogue.getItemTask("stone_pickaxe", 1);
            case IRON:
                return TaskCatalogue.getItemTask("iron_pickaxe", 1);
            case DIAMOND:
                return TaskCatalogue.getItemTask("diamond_pickaxe", 1);
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof SatisfyMiningRequirementTask) {
            SatisfyMiningRequirementTask other = (SatisfyMiningRequirementTask) obj;
            return other._requirement == _requirement;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Satisfy Mining Req: " + _requirement;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return mod.getInventoryTracker().miningRequirementMet(_requirement);
    }
}
