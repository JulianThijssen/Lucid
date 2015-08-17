package lucid.network;

public enum ServerError {
	SERVER_START_FAILURE ("Failed to initialize the server, shutting down.."),
	SERVER_CLOSE_FAILURE ("Failed to close the server elegantly, forcing shutdown.."),
	PORT_OUT_OF_RANGE ("The given server port is out of range [0-65535]."),
	SERVER_ALREADY_STARTED ("A request to start the server was given, but the server is already running"),
	CLIENT_CONNECTION_FAIL ("Client failed to connect to the server, discarding request..");
	
	private final String description;
	
	private ServerError(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
}
