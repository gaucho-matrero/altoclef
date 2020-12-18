package adris.altoclef.util.baritone;

import adris.altoclef.AltoClef;
import baritone.Baritone;

// Custom baritone processes.
public class BaritoneCustom {

    private InteractWithBlockPositionProcess _interactWithBlockPositionProcess;

    public BaritoneCustom(AltoClef mod, Baritone baritone) {
        baritone.getPathingControlManager().registerProcess(_interactWithBlockPositionProcess = new InteractWithBlockPositionProcess(baritone) );
    }


    public InteractWithBlockPositionProcess getInteractWithBlockPositionProcess() {
        return _interactWithBlockPositionProcess;
    }
}
