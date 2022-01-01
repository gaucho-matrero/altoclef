package adris.altoclef.util.control;

import adris.altoclef.AltoClef;
import adris.altoclef.mixins.ClientPlayerInteractionAccessor;
import adris.altoclef.util.csharpisbetter.Action;
import adris.altoclef.util.csharpisbetter.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PlayerExtraController {

    public final Action<BlockBrokenEvent> onBlockBroken = new Action<>();
    public final Action<BlockPlaceEvent> onBlockPlaced = new Action<>();
    private final AltoClef _mod;
    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    // TODO: Settings Parameters?
    private final TimerGame _placeTimer = new TimerGame(0.2);

    public PlayerExtraController(AltoClef mod) {
        _mod = mod;
    }

    public void onBlockBreak(BlockPos pos, double progress) {
        _blockBreakPos = pos;
        _blockBreakProgress = progress;
    }

    public void onBlockStopBreaking() {
        _blockBreakPos = null;
        _blockBreakProgress = 0;
    }

    public void onBlockBroken(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world == _mod.getWorld()) {
            BlockBrokenEvent evt = new BlockBrokenEvent();
            evt.blockPos = pos;
            evt.blockState = state;
            evt.player = player;
            onBlockBroken.invoke(evt);
        }
    }

    public void onBlockPlaced(BlockPos pos, BlockState state) {
        BlockPlaceEvent evt = new BlockPlaceEvent();
        evt.blockPos = pos;
        evt.blockState = state;
        onBlockPlaced.invoke(evt);
    }

    public BlockPos getBreakingBlockPos() {
        return _blockBreakPos;
    }

    public boolean isBreakingBlock() {
        return _blockBreakPos != null;
    }

    public double getBreakingBlockProgress() {
        return _blockBreakProgress;
    }

    public boolean inRange(Entity entity) {
        return _mod.getPlayer().isInRange(entity, _mod.getModSettings().getEntityReachRange());
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            _mod.getController().attackEntity(_mod.getPlayer(), entity);
            _mod.getPlayer().swingHand(Hand.MAIN_HAND);
        }
    }

    public void dropCurrentStack(boolean single) {
        assert MinecraftClient.getInstance().interactionManager != null;
        ((ClientPlayerInteractionAccessor) MinecraftClient.getInstance().interactionManager).doSendPlayerAction(
                single ? PlayerActionC2SPacket.Action.DROP_ITEM : PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                new BlockPos(0, 0, 0), Direction.fromRotation(0)
        );
        _mod.getItemStorage().registerSlotAction();
    }

    public boolean place() {
        if (_placeTimer.elapsed()) {
            _placeTimer.reset();
            // Shift click just for 100% container security.
            _mod.getInputControls().hold(Input.SNEAK);

            //mod.getInputControls().tryPress(Input.CLICK_RIGHT);
            // This appears to work on servers...
            HitResult mouseOver = MinecraftClient.getInstance().crosshairTarget;
            if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            Hand hand = Hand.MAIN_HAND;
            assert MinecraftClient.getInstance().interactionManager != null;
            if (MinecraftClient.getInstance().interactionManager.interactBlock(_mod.getPlayer(), _mod.getWorld(), hand, (BlockHitResult) mouseOver)  == ActionResult.SUCCESS) {
                _mod.getPlayer().swingHand(hand);
                return true;
            }

            //return true;
        }
        return false;
    }

    public void closeScreen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (!(screen instanceof GameMenuScreen)
        && !(screen instanceof GameOptionsScreen)) {
            // Close the screen if we're in-game
            _mod.getPlayer().closeHandledScreen();
        }
    }

    public static class BlockBrokenEvent {
        public BlockPos blockPos;
        public BlockState blockState;
        public PlayerEntity player;
    }

    public static class BlockPlaceEvent {
        public BlockPos blockPos;
        public BlockState blockState;
    }
}
