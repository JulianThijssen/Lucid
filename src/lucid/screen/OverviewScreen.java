package lucid.screen;

import java.awt.Color;
import java.awt.Graphics2D;

import lucid.Frame;
import lucid.network.Server;

public class OverviewScreen extends Screen {
	
	public OverviewScreen(Server server) {
		super(server);
	}
	
	public void update() {
		
	}
	
	public void render(Graphics2D g) {
		//Draw calls go here
		g.setColor(Color.DARK_GRAY);
		g.fillRect(0, 0, Frame.WIDTH, Frame.HEIGHT);

		if(server != null) {
			g.setColor(Color.RED);
			if(server.isRunning()) {g.setColor(Color.GREEN);}
			//Draw game server
			g.fillRect(0, 0, Frame.WIDTH, Frame.HEIGHT/3);
//			//Draw all the zones
//			for(int i = 0; i < zones.size(); i++) {
//				g.setColor(Color.ORANGE);
//				g.fillRect(((Frame.WIDTH/4) * i) % Frame.WIDTH, (Frame.HEIGHT/6) * (i / 4) + Frame.HEIGHT/3, Frame.WIDTH/4, Frame.HEIGHT/6);
//				g.setColor(Color.BLUE);
//				g.drawRect(((Frame.WIDTH/4) * i) % Frame.WIDTH, (Frame.HEIGHT/6) * (i / 4) + Frame.HEIGHT/3, Frame.WIDTH/4, Frame.HEIGHT/6);
//				g.drawString(zones.get(i).name + ": " + zones.get(i).getPlayerCount() + " players", ((Frame.WIDTH/4) * i) % Frame.WIDTH, (Frame.HEIGHT/6) * (i / 4) + Frame.HEIGHT/3 + Frame.HEIGHT/12);
//			}
		}
	}

	@Override
	public void keyPressed(int keyCode) {

	}

	@Override
	public void keyReleased(int keyCode) {

	}
}
