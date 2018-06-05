package lucid.database;

import java.util.ArrayList;

import lucid.exceptions.DatabaseException;

public interface Database {
    public void connect(String database, String user, String password) throws DatabaseException;

    public ArrayList<TableRow> query(String query);

    public void destroy();
}
