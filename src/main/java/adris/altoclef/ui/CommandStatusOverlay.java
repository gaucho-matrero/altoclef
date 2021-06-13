package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.List;

public class CommandStatusOverlay {

    public void render(AltoClef mod, MatrixStack matrixstack) {
        if (mod.getModSettings().shouldShowTaskChain()) {
            List<Task> tasks = Collections.emptyList();
            if (mod.getTaskRunner().getCurrentTaskChain() != null) {
                tasks = mod.getTaskRunner().getCurrentTaskChain().getTasks();
            }

            int color = 0xFFFFFFFF;
            drawTaskChain(MinecraftClient.getInstance().textRenderer, matrixstack, 0, 0, color, 10, tasks);
        }
    }

    private void drawTaskChain(TextRenderer renderer, MatrixStack stack, float dx, float dy, int color, int maxLines, List<Task> tasks) {
        if (tasks.size() == 0) {
            renderer.draw(stack, " (no task running) ", dx, dy, color);
        } else {
            float fontHeight = renderer.fontHeight;

            if (tasks.size() > maxLines) {
                for (int i = 0; i < tasks.size(); ++i) {
                    // Skip over the next tasks
                    if (i == 0 || i > tasks.size() - maxLines) {
                        renderer.draw(stack, tasks.get(i).toString(), dx, dy, color);
                    } else if (i == 1) {
                        renderer.draw(stack, " ... ", dx, dy, color);
                    } else {
                        continue;
                    }
                    dx += 8;
                    dy += fontHeight + 2;
                }
            } else {
                for (Task task : tasks) {
                    renderer.draw(stack, task.toString(), dx, dy, color);
                    dx += 8;
                    dy += fontHeight + 2;
                }
            }

        }
    }
}
