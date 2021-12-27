package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.entity.KillEntitiesTask;
import adris.altoclef.tasks.movement.CustomBaritoneGoalTask;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.KillAura;
import adris.altoclef.util.baritone.BaritoneHelper;
import adris.altoclef.util.csharpisbetter.TimerGame;
import adris.altoclef.util.helpers.LookHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HuntMobChain extends SingleTaskChain{

    private static final double CREEPER_KEEP_DISTANCE = 10;
    private static final double ARROW_KEEP_DISTANCE_HORIZONTAL = 2;//4;
    private static final double ARROW_KEEP_DISTANCE_VERTICAL = 10;//15;

    private static final double DANGER_KEEP_DISTANCE = 15 * 2;

    private static final double SAFE_KEEP_DISTANCE = 8;
    // Kind of a silly solution
    public static Class TargetMob =EndermanEntity.class;

    private final HashMap<Entity, TimerGame> _closeTargets = new HashMap<>();

    private CustomBaritoneGoalTask _huntTask;


    private float _cachedLastPriority;


    public HuntMobChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {

    }

    @Override
    public float getPriority(AltoClef mod) {
        _cachedLastPriority = getPriorityInner(mod);
        return _cachedLastPriority;
    }

    private float getPriorityInner(AltoClef mod) {
        if (!AltoClef.inGame()) {
            return Float.NEGATIVE_INFINITY;
        }

        if (mod.getModSettings().shouldHuntEnderman() && mod.getInventoryTracker().getItemCount(Items.ENDER_PEARL) < 16) {
            // Deal with hostiles because they are annoying.
            List<EndermanEntity> endermans;
            // TODO: I don't think this lock is necessary at all.
            synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                endermans =
                        mod.getEntityTracker().getTrackedEntities(EndermanEntity.class);//mod
                // .getEntityTracker()
                // .getHostiles();
                // mod
                // .getEntityTracker().getTrackedEntities(SkeletonEntity.class;
                List<Entity> toDealWith = new ArrayList<>();

                // TODO: I don't think this lock is necessary at all.
                synchronized (BaritoneHelper.MINECRAFT_LOCK) {
                    for (Entity enderman : endermans) {
                        int effectiveRange = 18;
                        boolean isClose = enderman.isInRange(mod.getPlayer(), effectiveRange);

                        if (isClose) {
                            isClose = LookHelper.seesPlayer(enderman,
                                    mod.getPlayer(), effectiveRange);
                        }

                        // Give each hostile a timer, if they're close for too long deal with them.
                        if (isClose) {
                            toDealWith.add(enderman);
                        }
//                            if (!_closeAnnoyingEntities.containsKey(hostile)) {
//                                _closeAnnoyingEntities.put(hostile, new TimerGame(mod.getModSettings().getKillHostileWhenCloseForSeconds()));
//                                _closeAnnoyingEntities.get(hostile).reset();
//                            }
//                            if (_closeAnnoyingEntities.get(hostile).elapsed()) {
//                                toDealWith.add(hostile);
//                            }
//                        } else {
//                            _closeAnnoyingEntities.remove(hostile);
//                        }
                    }

                    // Clear dead/non existing hostiles
                    List<Entity> toRemove = new ArrayList<>();
                    for (Entity check : _closeTargets.keySet()) {
                        if (!check.isAlive()) {
                            toRemove.add(check);
                        }
                    }
                    for (Entity remove : toRemove) _closeTargets.remove(remove);

                    int numberOfProblematicEntities = toDealWith.size();

                    if (numberOfProblematicEntities > 0) {

                        // Depending on our weapons/armor, we may chose to straight up kill hostiles if we're not dodging their arrows.

                        // wood 0 : 1 skeleton
                        // stone 1 : 1 skeleton
                        // iron 2 : 2 hostiles
                        // diamond 3 : 3 hostiles
                        // netherite 4 : 4 hostiles

                        // Armor: (do the math I'm not boutta calculate this)
                        // leather: ?1 skeleton
                        // iron: ?2 hostiles
                        // diamond: ?3 hostiles

                        // 7 is full set of leather
                        // 15 is full set of iron.
                        // 20 is full set of diamond.
                        // Diamond+netherite have bonus "toughness" parameter (we can simply add them I think, for now.)
                        // full diamond has 8 bonus toughness
                        // full netherite has 12 bonus toughness
//                        int armor = mod.getPlayer().getArmor();
//                        float damage = bestSword == null ? 0 : (1 + bestSword.getMaterial().getAttackDamage());
//
//                        int canDealWith = (int) Math.ceil((armor * 3.6 / 20.0) + (damage * 0.8));
//
//
                        if (numberOfProblematicEntities > 1) { // we want ot kill every
                            // enderman we find
                            // We can deal with it.
                            _huntTask = null;
                            setTask(new KillEntitiesTask(
                                    toDealWith::contains,
                                    // Oof
                                    TargetMob));
                            return 65;
                        } else {
                            return 80;
                        }
                    }
                }

                ToolItem bestSword = null;
                Item[] SWORDS = new Item[]{Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD};
                for (Item item : SWORDS) {
                    if (mod.getInventoryTracker().hasItem(item)) {
                        bestSword = (ToolItem) item;
                        break;
                    }
                }

                //hunt for enderman. Stop if we have 16 pearls
                if (_huntTask != null && !_huntTask.isFinished(mod)) {
                    setTask(_huntTask);
                    return _cachedLastPriority;
                }

            }
        }
        return 0;
    }
    @Override
    public String getName() {
        return "Hunt Mob";
    }

    @Override
    public boolean isActive() {
        // We're always checking for enderman slaughter chances
        return true;
    }
//    // MAY NEED THESE
//    // Pause if we're not loaded into a world.
//        if (!AltoClef.inGame()) return Float.NEGATIVE_INFINITY;
//
//    ToolItem bestSword = null;
//    Item[] SWORDS = new Item[]{Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.GOLDEN_SWORD, Items.STONE_SWORD, Items.WOODEN_SWORD};
//            for (Item item : SWORDS) {
//        if (mod.getInventoryTracker().hasItem(item)) {
//            bestSword = (ToolItem) item;
//            break;
//        }
//    }

}
