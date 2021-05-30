package adris.altoclef.util;


import adris.altoclef.util.csharpisbetter.Timer;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Type;


/**
 * What is this?
 */
public class CachedProjectile {
    private final Timer lastCache = new Timer(2);
    public Vec3d velocity;
    public Vec3d position;
    public double gravity;
    public Type projectileType;
    private Vec3d cachedHit;
    private boolean cacheHeld;

    public Vec3d getCachedHit() {
        return cachedHit;
    }

    public void setCacheHit(Vec3d cache) {
        cachedHit = cache;
        cacheHeld = true;
        lastCache.reset();
    }

    public boolean needsToReCache() {
        return !cacheHeld || lastCache.elapsed();
    }
}
