package lucid.exceptions;

import lucid.network.ServerError;

public class ServerStartException extends Exception {
	public ServerStartException(ServerError message, Exception e) {
		super(message.getDescription(), e);
	}
	
	public ServerStartException(ServerError message) {
		super(message.getDescription());
	}
}
