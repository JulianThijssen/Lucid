package lucid.util;

public class Log {
	public static void debug(String message) {
		System.out.println("[Server] " + message);
	}
	
	public static void error(String error) {
		System.err.println("[Error] " + error);
	}
}
