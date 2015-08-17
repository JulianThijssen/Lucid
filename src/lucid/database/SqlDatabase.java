package lucid.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import lucid.exceptions.DatabaseException;

public class SqlDatabase implements Database {	
	
	private Connection conn = null;

	public void connect(String database, String user, String password) throws DatabaseException {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch(ClassNotFoundException e) {
			//Log.debug("Could not find JDBC driver");
		}
		try {
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/"+database+"?user="+user+"&password="+password);
		} catch(SQLException e) {
			e.printStackTrace();
			throw new DatabaseException("Failed to connect to the database");
		}
	}

	public ArrayList<TableRow> query(String query) {
		ArrayList<TableRow> tabledata = new ArrayList<TableRow>();
		
		try {
			Statement stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(query);
			int columns = resultSet.getMetaData().getColumnCount();
			
			
			while(resultSet.next()) {
				TableRow tr = new TableRow();
				for(int i = 1; i <= columns; i++) {
					String key = resultSet.getMetaData().getColumnName(i);
					String value = resultSet.getString(i);
					tr.columns.put(key, value);
				}
				tabledata.add(tr);
			}
		} catch(SQLException e) {
			//Log.debug(e.toString());
		}
		
		destroy();
		
		return tabledata;
	}

	public void destroy() {
		try {
			conn.close();
		} catch(SQLException e) {
			//Log.debug(e.toString());
		}
	}
}
