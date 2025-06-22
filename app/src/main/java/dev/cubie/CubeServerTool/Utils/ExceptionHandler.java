package dev.cubie.CubeServerTool.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandler {
    private static final Logger GLOBAL_LOGGER = LoggerUtility.getLogger(ExceptionHandler.class);

    public static void handleException(Exception e, String context) {
        GLOBAL_LOGGER.log(Level.SEVERE, "Fehler in " + context + ": " + e.getMessage(), e);
    }

    public static <T> T safeExecute(ExceptionSupplier<T> supplier, T defaultValue, String context) {
        try {
            return supplier.get();
        } catch (Exception e) {
            handleException(e, context);
            return defaultValue;
        }
    }

    @FunctionalInterface
    public interface ExceptionSupplier<T> {
        T get() throws Exception;
    }
}
