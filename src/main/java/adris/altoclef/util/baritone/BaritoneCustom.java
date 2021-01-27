package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import baritone.Baritone;

// Custom baritone processes.
public class BaritoneCustom {

    private final InteractWithBlockPositionProcess _interactWithBlockPositionProcess;
    private final PlaceBlockNearbyProcess _placeBlockNearbyProcess;

    public BaritoneCustom(AltoClef mod, Baritone baritone) {
        baritone.getPathingControlManager().registerProcess(_interactWithBlockPositionProcess = new InteractWithBlockPositionProcess(baritone, mod) );
        baritone.getPathingControlManager().registerProcess(_placeBlockNearbyProcess = new PlaceBlockNearbyProcess(baritone, mod));
    }


    public InteractWithBlockPositionProcess getInteractWithBlockPositionProcess() {
        return _interactWithBlockPositionProcess;
    }
    public PlaceBlockNearbyProcess getPlaceBlockNearbyProcess() {
        return _placeBlockNearbyProcess;
    }
}
