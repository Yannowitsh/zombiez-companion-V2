package io.github.keoz5.zombiezcompanion.log;

import io.github.keoz5.zombiezcompanion.log.LogCategory;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {
    private static final Logger SLF4J = LoggerFactory.getLogger((String)"ZombieZ Companion");
    private static BooleanSupplier debugFlag = () -> false;

    private Log() {
    }

    public static void bindDebugFlag(BooleanSupplier supplier) {
        debugFlag = supplier;
    }

    public static void info(String msg) {
        SLF4J.info(msg);
    }

    public static void warn(String msg) {
        SLF4J.warn(msg);
    }

    public static void warn(String msg, Throwable t) {
        SLF4J.warn(msg, t);
    }

    public static void error(String msg) {
        SLF4J.error(msg);
    }

    public static void error(String msg, Throwable t) {
        SLF4J.error(msg, t);
    }

    public static boolean isDebugEnabled() {
        return debugFlag.getAsBoolean();
    }

    public static void debug(LogCategory category, String msg) {
        if (!Log.isDebugEnabled()) {
            return;
        }
        SLF4J.info("[ZombieZ][DEBUG][{}] {}", (Object)category.tag(), (Object)msg);
    }
}

