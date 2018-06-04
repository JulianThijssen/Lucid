package lucid.network;

public class AbstractConnection {
	/** A unique number identifying this connection from others on the same IP */
	private long unique = -1;
	
	/** Whether the connection is still alive */
	protected boolean connected = false;
	
	public long getUnique() {
		return unique;
	}
	
	public void setUnique(long unique) {
		this.unique = unique;
	}
}
