package adris.altoclef.util;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class Blacklist {
    private final List<BlockPos> posList;
    private final List<CubeBounds> areaList;
    private final static List<Blacklist> BLACKLISTS = new ArrayList<>();

    public static final void blacklist(final BlockPos chestPos) {
        BLACKLISTS.add(new Blacklist(chestPos));
    }

    public static final void blacklist(final CubeBounds cubeBounds) { BLACKLISTS.add(new Blacklist(cubeBounds)); }

    public static final void clearBlacklist() {
        BLACKLISTS.clear();
    }

    public static final boolean removeBlacklisted(final BlockPos pos) {
        for (int i = 0; i < BLACKLISTS.size(); i++) {
            final Blacklist el = BLACKLISTS.get(i);
            if (el.posList.contains(pos)) {
                el.posList.remove(pos);
                return true;
            }
        }

        return false;
    }

    public static final boolean isBlacklisted(final BlockPos pos) {
        if (pos == null) {
            return true;
        }

        for (final Blacklist blacklist : BLACKLISTS) {
            for (final CubeBounds bound : blacklist.areaList) {
                if (bound.inside(pos)) {
                    return true;
                }
            }

            for(final BlockPos e : blacklist.posList) {
                if (pos.getX() == e.getX() &&
                        pos.getY() == e.getY() &&
                        pos.getZ() == e.getZ()) {
                    return true;
                }
            }
        }

        return false;
    }

    public Blacklist(final CubeBounds bound) {
        this.posList = new ArrayList<>();
        this.areaList = new ArrayList<>();

        this.areaList.add(bound);
    }

    public Blacklist(final BlockPos chestPos) {
        this.posList = new ArrayList<>();
        this.areaList = new ArrayList<>();

        /*   -
         * - + -
         *   -
         * */
        this.posList.add(chestPos);

        /*   -
         * + - -
         *   -
         * */
        this.posList.add(new BlockPos(chestPos.getX() - 1, chestPos.getY(), chestPos.getZ()));

        /*   +
         * - - -
         *   -
         * */
        this.posList.add(new BlockPos(chestPos.getX(), chestPos.getY(), chestPos.getZ() - 1));

        /*  -
         * - - +
         *   -
         * */
        this.posList.add(new BlockPos(chestPos.getX() + 1, chestPos.getY(), chestPos.getZ()));

        /*  -
         * - - -
         *   +
         * */
        this.posList.add(new BlockPos(chestPos.getX(), chestPos.getY(), chestPos.getZ() + 1));
    }

    public void clear() {
        this.posList.clear();
    }
}
