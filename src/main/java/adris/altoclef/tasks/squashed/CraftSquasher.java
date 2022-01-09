package adris.altoclef.tasks.squashed;

import adris.altoclef.tasks.CraftInTableTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.util.RecipeTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CraftSquasher extends TypeSquasher<CraftInTableTask> {
    @Override
    protected List<ResourceTask> getSquashed(List<CraftInTableTask> tasks) {

        List<RecipeTarget> targetRecipies = new ArrayList<>();

        for (CraftInTableTask task : tasks) {
            targetRecipies.addAll(Arrays.asList(task.getRecipeTargets()));
        }

        return Collections.singletonList(new CraftInTableTask(targetRecipies.toArray(RecipeTarget[]::new)));
    }
}
