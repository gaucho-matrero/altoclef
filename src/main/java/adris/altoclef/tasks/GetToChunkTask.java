package adris.altoclef.tasks;


import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.baritone.GoalChunk;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.pathing.goals.Goal;
import net.minecraft.util.math.ChunkPos;


public class GetToChunkTask extends CustomBaritoneGoalTask {
    private final ChunkPos pos;
    
    public GetToChunkTask(ChunkPos pos) {
        // Override checker to be more lenient, as we are traversing entire chunks here.
        checker = new MovementProgressChecker(20, 0.5, 5, 0.001, 3);
        this.pos = pos;
    }
    
    @Override
    protected Goal newGoal(AltoClef mod) {
        return new GoalChunk(pos);
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof GetToChunkTask) {
            return ((GetToChunkTask) obj).pos.equals(pos);
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        return "Get to chunk: " + pos.toString();
    }
}
