package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.csharpisbetter.Util;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;

public class CataloguedResourceTask extends Task {


    private final TaskSquasher _squasher;
    private final ItemTarget[] _targets;
    private final List<ResourceTask> _tasksToComplete;

    public CataloguedResourceTask(boolean squash, ItemTarget... targets) {
        _squasher = new TaskSquasher();
        _targets = targets;
        _tasksToComplete = new ArrayList<>(targets.length);

        for (ItemTarget target : targets) {
            if (target != null) {
                _tasksToComplete.add(TaskCatalogue.getItemTask(target));
            }
        }

        if (squash) {
            squashTasks(_tasksToComplete);
        }
    }

    public CataloguedResourceTask(ItemTarget... targets) {
        this(true, targets);
    }

    @Override
    protected void onStart(AltoClef mod) {

    }

    @Override
    protected Task onTick(AltoClef mod) {
        for (ResourceTask task : _tasksToComplete) {
            for (ItemTarget target : task._itemTargets) {
                // If we failed to meet this task's targets, do the task.
                if (!mod.getInventoryTracker().targetMet(target)) return task;
            }
        }
        return null;
    }

    @Override
    public boolean isFinished(AltoClef mod) {
        for (ResourceTask task : _tasksToComplete) {
            for (ItemTarget target : task._itemTargets) {
                if (!mod.getInventoryTracker().targetMet(target)) return false;
            }
        }
        // All targets are met.
        return true;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {

    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof CataloguedResourceTask) {
            CataloguedResourceTask other = (CataloguedResourceTask) obj;
            return Util.arraysEqual(other._targets, _targets);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Get catalogued: " + ArrayUtils.toString(_targets);
    }

    private void squashTasks(List<ResourceTask> tasks) {
        _squasher.addTasks(tasks);
        tasks.clear();
        tasks.addAll(_squasher.getSquashed());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static class TaskSquasher {

        private final Map<Class, TypeSquasher> _squashMap = new HashMap<>();

        private final List<ResourceTask> _unSquashableTasks = new ArrayList<>();

        public TaskSquasher() {
            _squashMap.put(CraftInTableTask.class, new CraftSquasher());
            //_squashMap.put(MineAndCollectTask.class)
        }

        public void addTask(ResourceTask t) {
            Class type = t.getClass();
            if (_squashMap.containsKey(type)) {
                _squashMap.get(type).add(t);
            } else {
                //Debug.logMessage("Unsquashable: " + type + ": " + t);
                _unSquashableTasks.add(t);
            }
        }

        public void addTasks(List<ResourceTask> tasks) {
            for (ResourceTask task : tasks) {
                addTask(task);
            }
        }

        public List<ResourceTask> getSquashed() {
            List<ResourceTask> result = new ArrayList<>();

            for (Class type : _squashMap.keySet()) {
                result.addAll(_squashMap.get(type).getSquashed());
            }
            result.addAll(_unSquashableTasks);

            return result;
        }
    }

    static class CraftSquasher extends TypeSquasher<CraftInTableTask> {

        @Override
        protected List<CraftInTableTask> getSquashed(List<CraftInTableTask> tasks) {

            List<RecipeTarget> targetRecipies = new ArrayList<>();

            for (CraftInTableTask task : tasks) {
                targetRecipies.addAll(Arrays.asList(task.getRecipeTargets()));
            }

            //Debug.logMessage("Squashed " + targetRecipies.size());

            return Collections.singletonList(new CraftInTableTask(Util.toArray(RecipeTarget.class, targetRecipies)));
        }
    }

    static abstract class TypeSquasher<T extends ResourceTask> {

        private final List<T> _tasks = new ArrayList<>();

        void add(T task) {
            _tasks.add(task);
        }

        public List<T> getSquashed() {
            return getSquashed(_tasks);
        }

        protected abstract List<T> getSquashed(List<T> tasks);
    }

}
