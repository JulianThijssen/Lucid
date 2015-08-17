package lucid.util;

import java.util.Random;

public class UniqueGenerator {
	private static Random random = new Random();
	
	public static long unique = getUnique();
	
	public static long getUnique() {
		return random.nextLong();
	}
}
