package adris.altoclef.movement;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ClientTickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class MovementSmoothing {

    private static final List<ScheduledAction> ACTIONS = new ArrayList<>();
    private static final Random RANDOM = new Random();
    private static int pendingJitterTicks;

    private MovementSmoothing() {
    }

    static {
        EventBus.subscribe(ClientTickEvent.class, evt -> tick());
    }

    public static void rotateSmooth(float yaw, float pitch, int durationMs) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (durationMs <= 0) {
            player.setYaw(yaw);
            player.setPitch(pitch);
            return;
        }
        int durationTicks = Math.max(1, (int) Math.round(durationMs / 50.0));
        float startYaw = player.getYaw();
        float startPitch = player.getPitch();
        for (int i = 1; i <= durationTicks; i++) {
            final int tickDelay = i - 1;
            final float t = i / (float) durationTicks;
            schedule(() -> {
                ClientPlayerEntity p = MinecraftClient.getInstance().player;
                if (p != null) {
                    float newYaw = MathHelper.lerp(t, startYaw, yaw);
                    float newPitch = MathHelper.lerp(t, startPitch, pitch);
                    p.setYaw(newYaw);
                    p.setPitch(newPitch);
                }
            }, tickDelay);
        }
    }

    public static void sleepJitter(int minMs, int maxMs) {
        if (maxMs < minMs) {
            maxMs = minMs;
        }
        if (minMs < 0) {
            minMs = 0;
        }
        int range = maxMs - minMs;
        int durationMs = minMs + (range <= 0 ? 0 : RANDOM.nextInt(range + 1));
        int ticks = Math.max(1, (int) Math.round(durationMs / 50.0));
        synchronized (MovementSmoothing.class) {
            pendingJitterTicks = Math.max(pendingJitterTicks, ticks);
        }
    }

    static int consumePendingJitterTicks() {
        synchronized (MovementSmoothing.class) {
            int result = pendingJitterTicks;
            pendingJitterTicks = 0;
            return result;
        }
    }

    static void schedule(Runnable runnable, int delayTicks) {
        if (delayTicks <= 0) {
            runnable.run();
            return;
        }
        synchronized (ACTIONS) {
            ACTIONS.add(new ScheduledAction(runnable, delayTicks));
        }
    }

    private static void tick() {
        synchronized (MovementSmoothing.class) {
            if (pendingJitterTicks > 0) {
                pendingJitterTicks--;
            }
        }
        synchronized (ACTIONS) {
            Iterator<ScheduledAction> iterator = ACTIONS.iterator();
            while (iterator.hasNext()) {
                ScheduledAction action = iterator.next();
                action.ticks--;
                if (action.ticks <= 0) {
                    action.runnable.run();
                    iterator.remove();
                }
            }
        }
    }

    private static final class ScheduledAction {
        private final Runnable runnable;
        private int ticks;

        private ScheduledAction(Runnable runnable, int ticks) {
            this.runnable = runnable;
            this.ticks = ticks;
        }
    }
}
