package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import baritone.Baritone;

// Custom baritone processes.
public class BaritoneCustom {

    private final InteractWithBlockPositionProcess _interactWithBlockPositionProcess;

    public BaritoneCustom(AltoClef mod, Baritone baritone) {
        baritone.getPathingControlManager().registerProcess(_interactWithBlockPositionProcess = new InteractWithBlockPositionProcess(baritone, mod) );
    }


    public InteractWithBlockPositionProcess getInteractWithBlockPositionProcess() {
        return _interactWithBlockPositionProcess;
    }
}
