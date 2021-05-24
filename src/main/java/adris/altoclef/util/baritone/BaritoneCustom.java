package adris.altoclef.util.baritone;


import adris.altoclef.AltoClef;
import baritone.Baritone;


// Custom baritone processes.
public class BaritoneCustom {
    
    private final InteractWithBlockPositionProcess interactWithBlockPositionProcess;
    
    public BaritoneCustom(AltoClef mod, Baritone baritone) {
        baritone.getPathingControlManager().registerProcess(
                interactWithBlockPositionProcess = new InteractWithBlockPositionProcess(baritone, mod));
    }
    
    
    public InteractWithBlockPositionProcess getInteractWithBlockPositionProcess() {
        return interactWithBlockPositionProcess;
    }
}
