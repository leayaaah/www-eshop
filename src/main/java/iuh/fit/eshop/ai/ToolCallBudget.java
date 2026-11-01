package iuh.fit.eshop.ai;

import java.util.concurrent.atomic.AtomicInteger;

final class ToolCallBudget {
    static final int MAX_CALLS = 2;
    private static final ThreadLocal<AtomicInteger> REMAINING = new ThreadLocal<>();

    private ToolCallBudget() {
    }

    static void start() {
        REMAINING.set(new AtomicInteger(MAX_CALLS));
    }

    static void consume(String toolName) {
        AtomicInteger remaining = REMAINING.get();
        if (remaining != null && remaining.decrementAndGet() < 0) {
            throw new ToolCallLimitExceededException(toolName);
        }
    }

    static void clear() {
        REMAINING.remove();
    }

    static final class ToolCallLimitExceededException extends RuntimeException {
        ToolCallLimitExceededException(String toolName) {
            super("Đã vượt giới hạn " + MAX_CALLS + " lần gọi tool trong một request: " + toolName);
        }
    }
}
