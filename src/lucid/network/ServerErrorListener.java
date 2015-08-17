package lucid.network;

public interface ServerErrorListener {
	
	/** Handles what happens when the server errors */
	public void onServerError(ServerError error);
}
