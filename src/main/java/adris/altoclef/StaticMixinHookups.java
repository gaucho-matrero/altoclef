package adris.altoclef;


import adris.altoclef.commandsystem.CommandException;
import baritone.api.event.events.ChatEvent;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;


/**
 * Mixins have no way (currently) to access our mod.
 * <p>
 * As a result I'll do this statically.
 * <p>
 * However, I want to avoid grabbing AltoClef in a static context, so this class serves at the "static dumpster" which is the only spot
 * where I allow the bad practice of singletons to flourish
 */
public class StaticMixinHookups {
    // TODO: 2021-05-22 would be much nicer  to have a static `getInstance()` method in `AltoClef`.
    
    private static AltoClef mod;
    // for SOME REASON baritone triggers a block cancel breaking every other frame, so we have a 2 frame requirement for that?
    private static int breakCancelFrames;
    
    public static void hookupMod(AltoClef mod) {
        StaticMixinHookups.mod = mod;
    }
    
    public static void onInitializeLoad() {
        mod.onInitializeLoad();
    }
    
    public static void onClientTick() {
        mod.onClientTick();
    }
    
    public static void onClientRenderOverlay(MatrixStack stack) {
        mod.onClientRenderOverlay(stack);
    }
    
    // Every chat message can be interrupted by us
    public static void onChat(ChatEvent e) {
        String line = e.getMessage();
        if (mod.getCommandExecutor().isClientCommand(line)) {
            e.cancel();
            try {
                mod.getCommandExecutor().Execute(line);
            } catch (CommandException ex) {
                Debug.logWarning(ex.getMessage());
                //ex.printStackTrace();
            }
        }
    }
    
    public static void onBlockBreaking(BlockPos pos, double progress) {
        mod.getControllerExtras().onBlockBreak(pos, progress);
        breakCancelFrames = 2;
    }
    
    public static void onBlockCancelBreaking() {
        if (breakCancelFrames-- == 0) {
            mod.getControllerExtras().onBlockStopBreaking();
        }
    }
    
    public static void onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        //Debug.logMessage("BLOCK BROKEN: " + (world == _mod.getWorld()) + " : " + pos + " " + state.getBlock().getTranslationKey() + " "
        // + player.getName().getString());
        mod.getControllerExtras().onBlockBroken(world, pos, state, player);
    }
    
    public static void onBlockPlaced(BlockPos pos, BlockState state) {
        mod.getControllerExtras().onBlockPlaced(pos, state);
    }
    
    public static void onScreenOpenBegin(Screen screen) {
        if (screen == null) {
            mod.getContainerTracker().onScreenClose();
        }
    }
    
    public static void onScreenOpenEnd(Screen screen) {
        mod.getContainerTracker().onScreenOpenFirstTick(screen);
    }
    
    public static void onBlockInteract(BlockHitResult hitResult, BlockState blockState) {
        mod.getContainerTracker().onBlockInteract(hitResult.getBlockPos(), blockState.getBlock());
    }
    
    public static void onChunkLoad(WorldChunk chunk) {
        mod.onChunkLoad(chunk);
    }
    
    public static void onChunkUnload(int x, int z) {
        mod.onChunkUnload(new ChunkPos(x, z));
    }
    
    public static void onWhisperReceive(String user, String message) {
        mod.getButler().receiveWhisper(user, message);
    }
    
    public static void onGameMessage(String message, boolean nonChat) {
        mod.onGameMessage.invoke(message);
        if (nonChat) {
            mod.getButler().receiveMessage(message);
        }
    }
    
    public static void onGameOverlayMessage(String message) {
        mod.onGameOverlayMessage.invoke(message);
    }
}
