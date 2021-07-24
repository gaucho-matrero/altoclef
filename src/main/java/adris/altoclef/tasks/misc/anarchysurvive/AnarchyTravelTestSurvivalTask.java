package adris.altoclef.tasks.misc.anarchysurvive;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;

public class AnarchyTravelTestSurvivalTask extends AnarchyBaseSurvivalTask {
    public AnarchyTravelTestSurvivalTask() {
        super(5000, 300, 10000, true, 2000, 8, 5*30, 100, 250, 10000, 15, 30);
    }

    @Override
    protected Task inNetherWantToSetSpawnTask(AltoClef mod) {
        return new TravelAlongHighwayAxis(AnarchyUtil.getClosestAxis(mod), pos -> false);
    }

    @Override
    protected Task doFunStuff(AltoClef mod) {
        return new TravelAlongHighwayAxis(AnarchyUtil.getClosestAxis(mod), pos -> false);
    }

    @Override
    protected void runInBackground(AltoClef mod) {
        // TODO: This is where you detect players/signs and send logs+screenshots and stuff.
    }
}
