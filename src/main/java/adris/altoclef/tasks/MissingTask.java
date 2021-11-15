package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import baritone.process.BuilderProcess;
import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public class MissingTask extends Task {

    private boolean finished = false;
    BuilderProcess builder;
    @Override
    protected void onStart(AltoClef mod) {
        builder = mod.getClientBaritone().getBuilderProcess();
        builder.resume();
        builder.build("test8.schem", new BlockPos(MinecraftClient.getInstance().player.getPos()));
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (builder.isActive()) {
            if (builder.isPaused()) {
                if (builder.getApproxPlaceable().isEmpty()) return null;
                Object o = builder.getApproxPlaceable().get(0);
                Map<BlockState, Integer> missing = (Map<BlockState, Integer>) o;
                System.out.println(missing.entrySet().stream()
                        .map(e -> String.format("%sx %s", e.getValue(), e.getKey()))
                        .collect(Collectors.joining("\n")));
            }
        } else {
            finished = true;
        }
        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {}

    @Override
    public boolean isFinished(AltoClef mod) {
        return finished;
    }

    @Override
    protected boolean isEqual(Task other) {
        return false;
    }

    @Override
    protected String toDebugString() {
        return null;
    }
}
