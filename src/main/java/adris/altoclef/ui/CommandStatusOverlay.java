package adris.altoclef.ui;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.List;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class CommandStatusOverlay {

    //For the ingame timer
    private long _timeRunning;
    private long _lastTime = 0;

    public void render(AltoClef mod, MatrixStack matrixstack) {
        if (mod.getModSettings().shouldShowTaskChain()) {
            List<Task> tasks = Collections.emptyList();
            if (mod.getTaskRunner().getCurrentTaskChain() != null) {
                tasks = mod.getTaskRunner().getCurrentTaskChain().getTasks();
            }

            int color = 0xFFFFFFFF;
            drawTaskChain(MinecraftClient.getInstance().textRenderer, matrixstack, 0, 0, color, 10, tasks, mod);
        }
    }
    private DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.from(ZoneOffset.of("+00:00"))); // The date formatter
    private void drawTaskChain(TextRenderer renderer, MatrixStack stack, float dx, float dy, int color, int maxLines, List<Task> tasks, AltoClef mod) {
        if (tasks.size() == 0) {
            renderer.draw(stack, " (no task running) ", dx, dy, color);
            if (_lastTime + 10000 < Instant.now().toEpochMilli() && mod.getModSettings().shouldShowTimer()) {
                _timeRunning = Instant.now().toEpochMilli(); // reset the timer if it's been more than 10 seconds
            }
        } else {
            float fontHeight = renderer.fontHeight;
            // Draw the timer
            if (mod.getModSettings().shouldShowTimer()) {
                _lastTime = Instant.now().toEpochMilli(); // keep the last time for the timer reset
                String _realTime = DATE_TIME_FORMATTER.format(Instant.now().minusMillis(_timeRunning)); // Format the running time to string
                renderer.draw(stack, "<" + _realTime + ">", dx, dy, color); // Draw the timer before drawing tasks list
                dx += 8; // Do the same thing to list the tasks
                dy += fontHeight + 2;
            }
            // Draw the item chain
            StringBuilder itemChain = new StringBuilder();
            for (Task task : tasks) {
                if (itemChain.length() > 0) {
                    itemChain.append(" <- ");
                }
                if (!task.getItemName().isBlank()) {
                    itemChain.append(task.getItemName());
                }
            }
            // Draw the tasks list
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
