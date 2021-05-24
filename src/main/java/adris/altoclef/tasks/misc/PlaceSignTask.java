package adris.altoclef.tasks.misc;


import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractItemWithBlockTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.ItemUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


public class PlaceSignTask extends Task {
    private final BlockPos target;
    private final String message;
    private boolean finished;
    
    public PlaceSignTask(BlockPos pos, String message) {
        target = pos;
        this.message = message;
    }
    
    public PlaceSignTask(String message) {
        this(null, message);
    }
    
    private static boolean isSign(Block block) {
        for (Block check : ItemUtil.WOOD_SIGNS_ALL) {
            if (check == block) return true;
        }
        return false;
    }
    
    private Task editSign(AltoClef mod) {
        SignEditScreen screen = (SignEditScreen) MinecraftClient.getInstance().currentScreen;
        assert screen != null;
        
        StringBuilder currentLine = new StringBuilder();
        
        int lines = 0;
        
        final int SIGN_TEXT_MAX_WIDTH = 90;
        
        for (char c : message.toCharArray()) {
            currentLine.append(c);
            
            if (c == '\n' || MinecraftClient.getInstance().textRenderer.getWidth(currentLine.toString()) > SIGN_TEXT_MAX_WIDTH) {
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
                //Debug.logMessage("NEW LINE ADDED BEFORE: " + c);
            }
            // keycode don't matter
            //int keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(c);
            screen.charTyped(c, -1);
            //screen.keyPressed(keyCode, -1, )
        }
        screen.onClose();
        finished = true;
        
        return null;
    }
    
    @Override
    public boolean isFinished(AltoClef mod) {
        return finished;
    }
    
    @Override
    protected void onStart(AltoClef mod) {
        finished = false;
    }
    
    @Override
    protected Task onTick(AltoClef mod) {
        
        if (editingSign()) {
            return editSign(mod);
        }
        
        // Make sure we have a sign to place
        if (!mod.getInventoryTracker().hasItem("sign")) {
            return TaskCatalogue.getItemTask("sign", 1);
        }
        
        // Place sign
        if (placeAnywhere()) {
            return new PlaceBlockNearbyTask(ItemUtil.WOOD_SIGNS_ALL);
        } else {
            
            assert MinecraftClient.getInstance().world != null;
            BlockState b = MinecraftClient.getInstance().world.getBlockState(target);
            
            if (!isSign(b.getBlock()) && !b.isAir() && b.getBlock() != Blocks.WATER && b.getBlock() != Blocks.LAVA) {
                return new DestroyBlockTask(target);
            }
            
            return new InteractItemWithBlockTask(new ItemTarget("sign", 1), Direction.UP, target.down(), true);
        }
    }
    
    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getPlayer().closeHandledScreen();
    }
    
    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof PlaceSignTask) {
            PlaceSignTask task = (PlaceSignTask) obj;
            if (!task.message.equals(message)) return false;
            if ((task.target == null) != (target == null)) return false;
            if (task.target != null) {
                return task.target.equals(target);
            }
            return true;
        }
        return false;
    }
    
    @Override
    protected String toDebugString() {
        if (placeAnywhere()) {
            return "Place Sign Anywhere";
        }
        return "Place Sign at " + target.toShortString();
    }
    
    private boolean placeAnywhere() {
        return target == null;
    }
    
    private boolean editingSign() {
        return MinecraftClient.getInstance().currentScreen instanceof SignEditScreen;
    }
}
