package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.InventoryTracker;
import adris.altoclef.util.CubeBounds;
import adris.altoclef.util.Utils;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import baritone.api.schematic.ISchematic;
import baritone.process.BuilderProcess;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.io.File;
import java.util.*;
import java.util.List;

public class SchematicBuildTask extends Task {
    private boolean finished;
    private BuilderProcess builder;
    private String schematicFileName;
    private BlockPos startPos;
    private int allowedResourceStackCount;
    private Map<BlockState, Integer> needToSource;
    private boolean gotBackup;
    private boolean needBackup;
    private Vec3i schemSize;
    private CubeBounds bounds;
    private Map<BlockState, Integer> missing;
    private boolean sourced;
    private boolean pause;
    private boolean addedAvoidance;
    //private final MovementProgressChecker _progressChecker = new MovementProgressChecker(3);
    private BlockPos _currentTry = null;
    private boolean clearRunning = false;
    private String name;
    private ISchematic schematic;
    private static final int FOOD_UNITS = 120;
    private static final int MIN_FOOD_UNITS = 32;

    public SchematicBuildTask(final String schematicFileName) {
        this(schematicFileName, new BlockPos(MinecraftClient.getInstance().player.getPos()));
    }

    public SchematicBuildTask(final String schematicFileName, final BlockPos startPos) {
        this(schematicFileName, startPos, 3);
    }

    public SchematicBuildTask(final String schematicFileName, final BlockPos startPos, final int allowedResourceStackCount) {
        this();
        this.schematicFileName = schematicFileName;
        this.startPos = startPos;
        this.allowedResourceStackCount = allowedResourceStackCount;
    }

    public SchematicBuildTask() {
        this.needToSource = new HashMap<>();
        this.gotBackup = false;
        this.needBackup = false;
        this.sourced = false;
        this.addedAvoidance = false;
    }

    public SchematicBuildTask(String name, ISchematic schematic, final BlockPos startPos) {
        this();
        this.name = name;
        this.schematic = schematic;
        this.startPos = startPos;
    }

    @Override
    protected void onStart(AltoClef mod) {
        this.finished = false;

        if (isNull(builder)) {
            builder = mod.getClientBaritone().getBuilderProcess();
        }

        final File file = new File("schematics/" + schematicFileName);
        if (!file.exists()) {
            Debug.logMessage("Could not locate schematic file. Terminating...");
            this.finished = true;
            return;
        }

        //System.out.println("New start: " + schematicFileName);
        builder.clearState();

        if (Utils.isNull(this.schematic)) {
            builder.build(schematicFileName, startPos, true); //TODO: I think there should be a state queue in baritone
        } else {
            builder.build(this.name, this.schematic, startPos);
        }

        if (isNull(schemSize)) {
            this.schemSize = builder.getSchemSize();
        }

        if (!isNull(schemSize) && builder.isFromAltoclef() && !this.addedAvoidance) {
            //mod.getPlayer().getPitch()
            this.bounds = new CubeBounds(mod.getPlayer().getBlockPos(), this.schemSize.getX(), this.schemSize.getY(), this.schemSize.getZ());
            mod.addToAvoidanceFile(this.bounds);
            this.addedAvoidance = true;
            mod.reloadAvoidanceFile();
        }

        this.pause = false;
    }

    private List<BlockState> getTodoList(final AltoClef mod, final Map<BlockState, Integer> missing) {
        final InventoryTracker inventory = mod.getInventoryTracker();
        int finishedStacks = 0;
        final List<BlockState> listOfFinished = new ArrayList<>();

        for (final BlockState state : missing.keySet()) {
            final Item item = state.getBlock().asItem();
            final int count = inventory.getItemCount(item);
            final int maxCount = item.getMaxCount();

            if (finishedStacks < this.allowedResourceStackCount) {
                listOfFinished.add(state);
                if (count >= missing.get(state)) {
                    finishedStacks++;
                    listOfFinished.remove(state);
                } else if (count >= maxCount) {
                    finishedStacks += Math.ceil(count / maxCount);

                    if (finishedStacks >= this.allowedResourceStackCount) {
                        listOfFinished.remove(state);
                    }
                }
            }
        }

        return listOfFinished;
    }

    private boolean isNull(Object o) {
        return o == null;
    }

    private void overrideMissing() {
        this.missing = builder.getMissing();
    }

    private Map<BlockState, Integer> getMissing() {
        if (isNull(this.missing)) {
            overrideMissing();
        }

        return this.missing;
    }

    @Override
    protected Task onTick(AltoClef mod) {
        if (mod.getInventoryTracker().totalFoodScore() < MIN_FOOD_UNITS) {
            return new CollectFoodTask(FOOD_UNITS);
        }

        if (clearRunning && builder.isActive()) {
            return null;
        }

        clearRunning = false;

        _currentTry = builder.getAboveBreak();
        if (!isNull(_currentTry)) {
            //System.out.println("above");
            if (WorldHelper.isSolid(mod, _currentTry)) {
                if (mod.inAvoidance(this.bounds)) {
                    mod.unsetAvoidanceOf(this.bounds);
                    //System.out.println("Disabled: " + mod.unsetAvoidanceOf(this.bounds));
                }

                BlockPos p = mod.getPlayer().getBlockPos();
                builder.clearArea(p, new BlockPos(p.getX(), p.getY() + 5, p.getZ()));
                clearRunning = true;
                return null;
                //return new DestroyBlockTask(_currentTry);
            }

            if (!mod.inAvoidance(this.bounds)) {
                mod.setAvoidanceOf(this.bounds);
                //System.out.println("Enabled");
            }

            builder.setAboveBreak(null);
            builder.resume();
            this.sourced = false;
            Debug.logMessage("Resuming build process...");

            /*return new DoToClosestBlockTask(
                    (BlockPos blockPos) -> {
                        if (_currentTry != blockPos) {
                            _progressChecker.reset();
                        }
                        _currentTry = blockPos;
                        // If block above is solid, break it.
                        if (WorldHelper.isSolid(mod, blockPos)) {
                            return new DestroyBlockTask(blockPos);
                        }

                    },
                    validPred,
                    Blocks.CHEST);*/
        }
        if (builder.isFromAltoclef()) {
            //System.out.println("B: OVERRIDE");
            overrideMissing();
        }

        if (/*builder.isPausedBecauseOfMissingMaterials() &&*/ !isNull(getMissing()) && !getMissing().isEmpty()) {
            if (!mod.inAvoidance(this.bounds)) {
                mod.setAvoidanceOf(this.bounds);
                //System.out.println("Enabled");
            }

            //mod.getInventoryTracker().getInventorySlotsWithItem()

            for (final BlockState state : getTodoList(mod, missing)) {
                return TaskCatalogue.getItemTask(state.getBlock().asItem(), missing.get(state));
            }

            this.sourced = true;
        }

        if (this.sourced == true && !builder.isActive()) {
            if (mod.inAvoidance(this.bounds)) {
                mod.unsetAvoidanceOf(this.bounds);
                //System.out.println("Disabled: " + mod.unsetAvoidanceOf(this.bounds));
            }

            builder.resume();
            this.sourced = false;
            Debug.logMessage("Resuming build process...");
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        builder.pause();
        this.pause = true;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof SchematicBuildTask;
    }

    @Override
    protected String toDebugString() {
        return "SchematicBuilderTask";
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        if (!isNull(builder) && builder.isFromAltoclefFinished() || this.finished == true) {
            mod.loadAvoidanceFile();
            return true;
        }

        return false;
    }
}
