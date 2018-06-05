package lucid.network;

public interface ServerListener {

    /** Handles what happens when the server starts */
    public void onServerStart();

    /** Handles what happens when the server stops */
    public void onServerStop();

    /** Handles what happens when a client connects */
    public void onConnection(Connection connection);

    /** Handles what happens when a client disconnects */
    public void onDisconnect(Connection connection);

    /** Handles what happens when a packet is received */
    public void onReceived(Connection connection, Packet packet);
}
