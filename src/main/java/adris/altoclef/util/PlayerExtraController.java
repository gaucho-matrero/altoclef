package adris.altoclef.util;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PlayerExtraController {

    private AltoClef _mod;

    private ClientPlayNetworkHandler _networkHandler;

    private BlockPos _blockBreakPos;
    private double _blockBreakProgress;

    private static final double INTERACT_RANGE = 6;

    private Method _sendActionMethod = null;

    public PlayerExtraController(AltoClef mod) {
        _mod = mod;

        try {
            _sendActionMethod = ClientPlayerInteractionManager.class.getDeclaredMethod("sendPlayerAction", PlayerActionC2SPacket.Action.class, BlockPos.class, Direction.class);
            _sendActionMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Debug.logError("SOMETHING BASIC EFFED UP");
        }

    }

    public void onBlockBreak(BlockPos pos, double progress) {
        _blockBreakPos = pos;
        _blockBreakProgress = progress;
    }
    public void onBlockStopBreaking() {
        _blockBreakPos = null;
        _blockBreakProgress = 0;
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
        return _mod.getPlayer().isInRange(entity, INTERACT_RANGE);
    }

    public void attack(Entity entity) {
        if (inRange(entity)) {
            _mod.getController().attackEntity(_mod.getPlayer(), entity);
        }
    }

    public void dropCurrentStack(boolean single) {
        try {
            _sendActionMethod.invoke(MinecraftClient.getInstance().interactionManager,
                    single? PlayerActionC2SPacket.Action.DROP_ITEM : PlayerActionC2SPacket.Action.DROP_ALL_ITEMS,
                    new BlockPos(0, 0, 0), Direction.fromRotation(0)
            );
            _mod.getInventoryTracker().setDirty();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
