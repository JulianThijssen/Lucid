package lucid.util;

public class Log {
    public static LogLevel listenLevel = LogLevel.NONE;

    public static void debug(LogLevel level, String message) {
        if (level == listenLevel || listenLevel == LogLevel.ALL) {
            System.out.println(String.format("[%s] %s", level, message));
        }
    }
}
