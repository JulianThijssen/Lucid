package tests;
import static org.junit.Assert.*;
import lucid.network.MacAddress;

import org.junit.Test;


public class MacAddressTest {

	@Test
	public void test() {
		long l = MacAddress.getAddress();
		if (l == -1) {
			fail();
		}
	}
}
