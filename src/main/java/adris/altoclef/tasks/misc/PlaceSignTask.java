package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.baritone.GoalGetToPosition;
import adris.altoclef.util.baritone.PlaceBlockSchematic;
import adris.altoclef.util.csharpisbetter.Timer;
import baritone.api.schematic.AbstractSchematic;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.util.math.BlockPos;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

// TODO: "isPosValid". Break
// TODO: Abstract this out to "DoStuffAtPlacedBlockTask" where the only extra work here is the writing of the sign.

public class PlaceSignTask extends Task {

    private BlockPos _target;
    private final String _message;

    private AbstractSchematic _placing = null;

    private boolean _finished = false;

    private Task _wanderTask = null;//new TimeoutWanderPositionTask(3);

    private Timer _placeTimeout = new Timer(3);

    private static final double TARGET_CLOSE_RANGE = 2;

    private BlockPos _bullshitLastWorkingRandomDelta = null;
    private int _bullshitFailCounter = 0;

    public PlaceSignTask(BlockPos target, String message) {
        _target = target;
        _message = message;
    }
    public PlaceSignTask(String message) {
        this(null, message);
    }

    @Override
    protected void onStart(AltoClef mod) {
        _placing = null;
        _finished = false;
        mod.getClientBaritone().getBuilderProcess().onLostControl();
        mod.getClientBaritone().getCustomGoalProcess().onLostControl();
    }

    @Override
    protected Task onTick(AltoClef mod) {

        boolean signing = (MinecraftClient.getInstance().currentScreen instanceof SignEditScreen);

        boolean placingBaritone = mod.getClientBaritone().getBuilderProcess().isActive();

        if (signing) {
            // Our last random delta was a success
            _bullshitFailCounter = 5;
            setDebugState("SIGNING");

            doSigning(_message);

            _placing = null;
            return null;
        }

        // Wandering/Timeout
        if (_wanderTask != null && _wanderTask.isActive() && !_wanderTask.isFinished(mod)) {
            //mod.getClientBaritone().getBuilderProcess().onLostControl();
            setDebugState("Failed to place, Wandering...");
            return _wanderTask;
        }

        // Get close to sign
        if (!placingBaritone && _target != null && !mod.getPlayer().getBlockPos().isWithinDistance(_target, TARGET_CLOSE_RANGE)) {
            setDebugState("Going to target pos");

            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                int randRange = 1;
                BlockPos random;
                if (false && _bullshitFailCounter > 0 && _bullshitLastWorkingRandomDelta != null) {
                    random = _bullshitLastWorkingRandomDelta;
                } else {
                    random = new BlockPos(getRand(randRange), 0, getRand(randRange));
                }
                _bullshitLastWorkingRandomDelta = random;
                BlockPos moveTarget = _target.add(random);
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalGetToPosition(moveTarget.getX(), moveTarget.getY(), moveTarget.getZ()));
            }

            return null;
            //return new GetToBlockTask(_target, false);
        }


        // Wait for placing
        if (placingBaritone && _placing != null) {
            setDebugState("Waiting for placement...");

            if (_placeTimeout.elapsed()) {
                // We failed to place. Trying again.
                //mod.getClientBaritone().getBuilderProcess().onLostControl();
                _placing = null;
                int randRange = 1;
                _wanderTask = new GetToBlockTask(_target.add(getRand(randRange), 0, getRand(randRange)), false);
                if (_bullshitFailCounter-- == 0 && _bullshitLastWorkingRandomDelta != null) {
                    // oof
                    Debug.logWarning("oof");
                    _bullshitLastWorkingRandomDelta = null;
                }
                return _wanderTask;
            }
            return null;
        }

        // Get signs
        if (!mod.getInventoryTracker().hasItem(ItemTarget.WOOD_SIGN)) {
            setDebugState("Getting a sign");
            return TaskCatalogue.getItemTask("sign", 1);
        }

        // Place signs
        setDebugState("Placing a sign");

        if (!placingBaritone) {
            BlockPos origin = mod.getPlayer().getBlockPos();
            Block[] blocks = ItemTarget.WOOD_SIGNS_ALL;
            if (_target == null) {
                throw new NotImplementedException();
                //_placing = new PlaceBlockNearbySchematic(origin, blocks, false);
            } else {
                _placing = new PlaceBlockSchematic(blocks);
            }
            mod.getClientBaritone().getBuilderProcess().build("Place Sign", _placing , _target == null? origin : _target);
            _placeTimeout.reset();
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getClientBaritone().getBuilderProcess().onLostControl();
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceSignTask) {
            PlaceSignTask task = (PlaceSignTask) obj;
            if (!task._message.equals(_message)) return false;
            if ((task._target == null) != (_target == null)) return false;
            assert task._target != null;
            //noinspection RedundantIfStatement
            if (!task._target.equals(_target)) return false;
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Placing sign that says \"" + _message + "\" " + (_target != null? ("at " + _target) : "anywhere");
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        return _finished;
    }

    private void doSigning(String message) {
        SignEditScreen screen = (SignEditScreen) MinecraftClient.getInstance().currentScreen;
        assert screen != null;

        StringBuilder currentLine = new StringBuilder();

        int lines = 0;

        final int SIGN_TEXT_MAX_WIDTH = 90;

        for (char c : message.toCharArray()) {
            currentLine.append(c);

            if ( c == '\n' || MinecraftClient.getInstance().textRenderer.getWidth(currentLine.toString()) > SIGN_TEXT_MAX_WIDTH) {
                currentLine.delete(0, currentLine.length());
                if (c != '\n') {
                    currentLine.append(c);
                }
                lines++;
                if (lines >= 4) {
                    Debug.logWarning("Too much text to fit on sign! Got Cut off.");
                    break;
                }

                // Add newline
                screen.keyPressed(257, 36, 0);
                Debug.logMessage("NEW LINE ADDED BEFORE: " + c);
            }
            // keycode don't matter
            //int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(c);
            screen.charTyped(c, -1);
            //screen.keyPressed(keyCode, -1, )
        }
        screen.onClose();
        _finished = true;
    }


    private static int getRand(int range) {
        return (int) Math.round( range * (Math.random() * 2.0 - 1.0) );
    }
}
