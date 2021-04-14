package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.util.WorldUtil;
import baritone.Baritone;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaceBlockNearbyProcess extends BaritoneProcessHelper {

    private Block[] _toPlace = null;

    private BlockPos _placed = null;

    private boolean _onlyPlaceOnGround;

    private AltoClef _mod;

    private int _placeTimer;

    private static final Vec3i[] OFFSETS;
    static {
        List<Vec3i> offs = new ArrayList<>();
        int range = 7;
        for (int dx = -range; dx <=range; ++dx) {
            for (int dy = -range; dy <= range; ++dy) {
                for (int dz = -range; dz <= range; ++dz) {
                    if (dx * dx + dy * dy + dz * dz <= range * range) {
                        offs.add(new Vec3i(dx, dy, dz));
                    }
                }
            }
        }
        OFFSETS = new Vec3i[offs.size()];
        offs.toArray(OFFSETS);
    }

    public PlaceBlockNearbyProcess(Baritone baritone, AltoClef mod) {
        super(baritone); _mod = mod;
    }

    @Override
    public boolean isActive() {
        return _toPlace != null;
    }

    public void place(Block[] toPlace, boolean onlyPlaceOnGround) {
        _toPlace = toPlace;
        _placed = null;
        _placeTimer = 0;
        _onlyPlaceOnGround = onlyPlaceOnGround;
    }
    public void place(Block[] toPlace) {
        this.place(toPlace, true);
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {

        if (!isActive()) return null;

        // Wait while placing.
        if (_placeTimer-- > 0) {
            //Debug.logMessage("(wait)");
            baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        // Wait for place
        if (_placed != null) {
            baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
            BlockState check = ctx.world().getBlockState(_placed);
            if (isTargetPlace(check.getBlock())) {

                synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                    _mod.getBlockTracker().addBlock(check.getBlock(), _placed);
                }

                onLostControl();
                return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
            } else {
                //Debug.logMessage("Invalid place: " + _placed);
                _placed = null;
            }
            //return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        // If we're facing a placable block, just go for it.
        HitResult hit = MinecraftClient.getInstance().crosshairTarget;
        if (hit instanceof BlockHitResult) {
            BlockHitResult bhit = (BlockHitResult) hit;
            BlockPos bpos = bhit.getBlockPos();//.subtract(bhit.getSide().getVector());
            //Debug.logMessage("TEMP: A: " + bpos);
            if (MovementHelper.canPlaceAgainst(ctx, bpos)) {
                BlockPos placePos = bhit.getBlockPos().add(bhit.getSide().getVector());
                //Debug.logMessage("TEMP: B (actual): " + placePos);
                if (!Baritone.getAltoClefSettings().shouldAvoidPlacingAt(placePos.getX(), placePos.getY(), placePos.getZ())) {
                    if (equipBlock()) {
                        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                        //Debug.logMessage("TEMP: Placed at " + placePos);
                        _placed = placePos;
                        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                    }
                }
            }
        }

        // Try to place
        for (Vec3i offs : OFFSETS) {
            assert MinecraftClient.getInstance().player != null;
            BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
            BlockPos tryPos = playerPos.add(offs);
            if (tryPlaceAt(tryPos)) {
                _placed = tryPos;
                //Debug.logMessage("TEMP222: Try at " + tryPos);
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }

        // We failed to find a spot to quickly place
        for (Vec3i offs : OFFSETS) {
            BlockPos playerPos = MinecraftClient.getInstance().player.getBlockPos();
            BlockPos tryPos = playerPos.add(offs);
            if (makeSpace(tryPos)) {
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }

        _toPlace = null;
        logDirect("Failed to find nearby spot to place.");

        // Failure.
        return new PathingCommand(null, PathingCommandType.CANCEL_AND_SET_GOAL);
    }

    public BlockPos placedBlock() {
        return _placed;
    }

    @Override
    public void onLostControl() {
        MinecraftClient.getInstance().options.keySneak.setPressed(false);
        _toPlace = null;
    }

    @Override
    public String displayName0() {
        return "Placing Block Nearby";
    }

    private boolean isTargetPlace(ItemStack stack) {
        return isTargetPlace(Block.getBlockFromItem(stack.getItem()));
    }
    private boolean isTargetPlace(Block check) {
        for (Block block : _toPlace) {
            if (block.equals(check)) return true;
        }
        return false;
    }

    private boolean makeSpace(BlockPos pos) {
        Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);

        if (rot.isPresent()) {

            BlockState state = ctx.world().getBlockState(pos);

            if (!state.isAir()) {
                if (MovementHelper.avoidBreaking(baritone.bsi, pos.getX(), pos.getY(), pos.getZ(), state)) {
                    return false;
                }
                // Break
                MovementHelper.switchToBestToolFor(ctx, ctx.world().getBlockState(pos));
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                //baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, false);
                baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                return true;
            }
        }
        return false;
    }

    private boolean tryPlaceAt(BlockPos pos) {

        // Only place if it's air or water
        BlockState state = ctx.world().getBlockState(pos);

        if (!canPlaceIn(state.getBlock())) {
            //Debug.logInternal("Failed to place " + pos.toShortString() + " in " + state.getBlock().getTranslationKey());
            return false;
        }
        // We can't place here.
        if (Baritone.getAltoClefSettings().shouldAvoidPlacingAt(pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        // Block is solid and we can't break.
        if (WorldUtil.isSolid(_mod, pos) && !WorldUtil.canBreak(_mod, pos)) {
            return false;
        }

        // Place
        Vec3d placeBelow = new Vec3d(0.5, 0, 0.5);
        Vec3d[] placeOffsets = new Vec3d[] {
                new Vec3d(0, 0.5, 0.5),
                new Vec3d(0.5, 0.5, 0),
                new Vec3d(1, 0.5, 0.5),
                new Vec3d(0.5, 0.5, 1),
                new Vec3d(0.5, 1, 0.5)
        };

        boolean placed = false;
        if (tryPlaceOffs(pos, placeBelow)) {
            placed = true;
        }
        if (!_onlyPlaceOnGround) {
            for (Vec3d offs : placeOffsets) {
                if (placed) break;
                if (tryPlaceOffs(pos, offs)) placed = true;
            }
        }
        return placed;
    }

    private boolean tryPlaceOffs(BlockPos pos, Vec3d offs) {

        if (!isActive()) return false;

        // pos is where the object will be placed
        // placeOn is what block it will be placed ON.
        Vec3d centerOffs = offs.subtract(0.5, 0.5, 0.5).normalize();
        BlockPos placeOn = pos.add(centerOffs.x, centerOffs.y, centerOffs.z);

        Optional<Rotation> rot = RotationUtils.reachable(ctx.player(), placeOn, ctx.playerController().getBlockReachDistance());//RotationUtils.reachableOffset(ctx.player(), placeOn, new Vec3d(pos.getX() + offs.x, pos.getY() + offs.y, pos.getZ() + offs.z), ctx.playerController().getBlockReachDistance(), false);
        if (rot.isPresent()) {
            HitResult result = RayTraceUtils.rayTraceTowards(ctx.player(), rot.get(), ctx.playerController().getBlockReachDistance());
            //if (result instanceof BlockHitResult && ((BlockHitResult) result).getSide() == Direction.UP) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                if (ctx.isLookingAt(placeOn)) { // TODO: Fix this part.
                    BlockState hitState = ctx.world().getBlockState(new BlockPos(result.getPos()));
                    // Sneak if we need to.
                    if (blockIsContainer(hitState.getBlock())) {
                        baritone.getInputOverrideHandler().clearAllKeys();
                        MinecraftClient.getInstance().options.keySneak.setPressed(true);
                        //baritone.getInputOverrideHandler().setInputForceState(Input.SNEAK, true);
                    }
                    // If we are within player, jump and say we're false.
                    if (withinPlayer(pos, offs)) {
                        //baritone.getInputOverrideHandler().clearAllKeys();
                        //baritone.getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                        //Debug.logInternal("(within player) " + offs);
                        return false;
                    }
                    if (!equipBlock()) {
                        Debug.logWarning("Did not have any blocks to place, cancelling.");
                        onLostControl();
                        return false;
                    }
                    Debug.logInternal("Placed " + offs);
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);

                    return true;
                } else {
                    //Debug.logInternal("Timer SET");
                    _placeTimer = 5;
                }
            //}
        }
        return false;
    }

    private boolean equipBlock() {
        for (Block block : _toPlace) {
            if (!_mod.getExtraBaritoneSettings().isInteractionPaused() && _mod.getInventoryTracker().hasItem(block.asItem())) {
                if (_mod.getInventoryTracker().equipItem(block.asItem())) return true;
            }
        }
        return false;
    }

    private static boolean blockIsContainer(Block block) {
        return block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.ENDER_CHEST || block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }
    private static boolean canPlaceIn(Block block) {
        return block == Blocks.WATER || WorldUtil.isAir(block);
    }

    private static boolean withinPlayer(BlockPos pos, Vec3d offs) {
        Vec3d centerOffs = offs.subtract(0.5, 0.5, 0.5).normalize();

        Vec3d center = new Vec3d(pos.getX() + centerOffs.x, pos.getY() + centerOffs.y, pos.getZ() + centerOffs.z);
        assert MinecraftClient.getInstance().player != null;
        Vec3d playerPos = MinecraftClient.getInstance().player.getPos();
        Vec3d playerTop = playerPos.add(new Vec3d(0, MinecraftClient.getInstance().player.getHeight(), 0));
        double buffer = 1.5;
        double heightBuffer = 2;
        Vec3d minPos = playerPos.subtract(buffer, heightBuffer, buffer);
        Vec3d maxPos = playerTop.add(buffer, heightBuffer, buffer);

        return minPos.x < center.x && center.x < maxPos.x &&
                minPos.y < center.y && center.y < maxPos.y &&
                minPos.z < center.z && center.z < maxPos.z;
        //Debug.logInternal("Failed: " + center.toString() + ", " + playerPos + ", " + playerTop);
    }

}
