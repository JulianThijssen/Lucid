package lucid.screen;

import java.awt.Graphics2D;

import lucid.network.Server;

public abstract class Screen {
	protected Server server = null;
	
	public Screen(Server server) {
		this.server = server;
	}
	
	/** Takes care of all logic updates of components */
	public abstract void update();
	
	/** Method which will render all drawable components on a double buffer,
	 *  after which they will be shown on screen.
	 */
	public abstract void render(Graphics2D g);
	
	/** Gets called when a key gets pressed */
	public abstract void keyPressed(int keyCode);
	
	/** Gets called when a key get released */
	public abstract void keyReleased(int keyCode);
}
