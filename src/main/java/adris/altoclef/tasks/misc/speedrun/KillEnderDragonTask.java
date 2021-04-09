package adris.altoclef.tasks.misc.speedrun;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.*;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.item.Items;

/**
 * Here we go
 * the final stretch
 *
 * Until something inevitably fucks up and I gotta go back here to fix it
 * in which case this'll be pretty ironic.
 */
public class KillEnderDragonTask extends Task {

    private final Task _collectBuildMaterialsTask = new MineAndCollectTask(new ItemTarget(Items.END_STONE, 100), new Block[] {Blocks.END_STONE}, MiningRequirement.WOOD);

    @Override
    protected void onStart(AltoClef mod) {
        mod.getConfigState().push();
        mod.getConfigState().addThrowawayItems(Items.END_STONE);
        mod.getBlockTracker().trackBlock(Blocks.END_PORTAL);
        // Don't forcefield enderman.
        mod.getConfigState().addForceFieldExclusion(entity -> entity instanceof EndermanEntity);
        mod.getConfigState().setPreferredStairs(true);
    }

    @Override
    protected Task onTick(AltoClef mod) {

        // If there is a portal, enter it.
        if (mod.getBlockTracker().anyFound(Blocks.END_PORTAL)) {
            setDebugState("Entering portal to beat the game.");
            return new DoToClosestBlockTask(
                    () -> mod.getPlayer().getPos(),
                    blockPos -> new GetToBlockTask(blockPos.up(), false),
                    pos -> mod.getBlockTracker().getNearestTracking(pos, Blocks.END_PORTAL),
                    Blocks.END_PORTAL
            );
        }

        // If we have no building materials (stone + cobble + end stone), get end stone
        // If there are crystals, suicide blow em up.
        // If there are no crystals, punk the dragon if it's close.
        int MINIMUM_BUILDING_BLOCKS = 1;
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class) && mod.getInventoryTracker().getItemCount(Items.DIRT, Items.COBBLESTONE, Items.NETHERRACK, Items.END_STONE) < MINIMUM_BUILDING_BLOCKS || (_collectBuildMaterialsTask.isActive() && !_collectBuildMaterialsTask.isFinished(mod))) {
            if (mod.getInventoryTracker().miningRequirementMet(MiningRequirement.WOOD)) {
                mod.getConfigState().addProtectedItems(Items.END_STONE);
                setDebugState("Collecting building blocks to pillar to crystals");
                return _collectBuildMaterialsTask;
            }
        } else {
            mod.getConfigState().removeProtectedItems(Items.END_STONE);
        }

        // Blow up the nearest end crystal
        if (mod.getEntityTracker().entityFound(EndCrystalEntity.class)) {
            setDebugState("Kamakazeeing crystals");
            return new DoToClosestEntityTask(() -> mod.getPlayer().getPos(),
                    (toDestroy) -> {
                        if (toDestroy.isInRange(mod.getPlayer(), 7)) {
                            mod.getController().attackEntity(mod.getPlayer(), toDestroy);
                        }
                        // Go next to the crystal, arbitrary where we just need to get close.
                        return new GetToBlockTask(toDestroy.getBlockPos().add(1, 0, 0), false);
                    }, EndCrystalEntity.class);
        }

        // Punk dragon
        setDebugState("Punking dragon");

        return new KillEntitiesTask(EnderDragonEntity.class);
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getConfigState().pop();
        mod.getBlockTracker().stopTracking(Blocks.END_PORTAL);
    }

    @Override
    protected boolean isEqual(Task obj) {
        return obj instanceof KillEnderDragonTask;
    }

    @Override
    protected String toDebugString() {
        return "Killing Ender Dragon";
    }
}
