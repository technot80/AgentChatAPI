package dev.agentchat.util;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RateLimiter {

    private final boolean enabled;
    private final int maxGlobalPerMinute;
    private final int maxPerSessionPerMinute;
    private final Deque<Long> globalTimestamps;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> sessionTimestamps;

    public RateLimiter(boolean enabled, int maxGlobalPerMinute, int maxPerSessionPerMinute) {
        this.enabled = enabled;
        this.maxGlobalPerMinute = maxGlobalPerMinute;
        this.maxPerSessionPerMinute = maxPerSessionPerMinute;
        this.globalTimestamps = new ConcurrentLinkedDeque<>();
        this.sessionTimestamps = new ConcurrentHashMap<>();
    }

    public boolean tryAcquire(String sessionName) {
        if (!enabled) {
            return true;
        }

        long now = System.currentTimeMillis();
        long cutoff = now - 60_000L;

        prune(globalTimestamps, cutoff);
        if (maxGlobalPerMinute > 0 && globalTimestamps.size() >= maxGlobalPerMinute) {
            return false;
        }

        if (maxPerSessionPerMinute > 0 && sessionName != null) {
            ConcurrentLinkedDeque<Long> sessionQueue = sessionTimestamps.computeIfAbsent(sessionName, key -> new ConcurrentLinkedDeque<>());
            prune(sessionQueue, cutoff);
            if (sessionQueue.size() >= maxPerSessionPerMinute) {
                return false;
            }
            sessionQueue.add(now);
        }

        globalTimestamps.add(now);
        return true;
    }

    private void prune(Deque<Long> queue, long cutoff) {
        while (true) {
            Long head = queue.peekFirst();
            if (head == null || head >= cutoff) {
                return;
            }
            queue.pollFirst();
        }
    }
}
