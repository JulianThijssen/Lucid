package lucid.database;

import java.util.HashMap;

public class TableRow {
	public HashMap<String, String> columns = new HashMap<String, String>();
	
	
	public String getString(String key) {
		String value = columns.get(key);
		return value;
	}
	
	public int getInt(String key) {
		String svalue = columns.get(key);
		int value = Integer.parseInt(svalue);
		return value;
	}
	
	@Override
	public String toString() {
		String s = "|";
		for(String value: columns.values()) {
			s += value + "|";
		}
		return s;
	}
}
