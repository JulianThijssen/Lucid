package lucid;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import lucid.network.Server;
import lucid.screen.OverviewScreen;
import lucid.screen.Screen;

public class Frame extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;

	/** The title of the server, this will be shown on the frame. */
	public static final String TITLE = "Lucid Server";
	
	public static final int OVERVIEWSCREEN = 1;
	
	/** The width of the screen */
	public static final int WIDTH = 600;
	
	/** The height of the screen */
	public static final int HEIGHT = 600;
	
	/** The server to monitor */
	private Server server = null;
	
	/** The current screen being shown */
	private Screen screen;
	
	/** The buffered graphics image on which everything is drawn */
	protected BufferedImage bufferimg;
	
	/** The Graphics object on which the buffered image is drawn */
	protected Graphics g;
	
	/** The Graphics2D object which takes care of all the rendering */
	protected Graphics2D bg;
	
	/** Whether the frame is running */
	protected boolean running = true;

	public Frame(Server server) {
		this.server = server;
		
		setTitle(TITLE);
		setSize(WIDTH, HEIGHT);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void init() {
		setScreen(OVERVIEWSCREEN);
		
		bufferimg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		g = getGraphics();
		bg = bufferimg.createGraphics();
		
		//Start the thread
		new Thread(this).start();
	}
	
	public void run() {
		while(running) {
			render();

			try {
				Thread.sleep(1000);
			} catch(InterruptedException e) {}
		}
	}
	
	public void render() {
		screen.render(bg);
		g.drawImage(bufferimg, 0, 0, null);
	}

	public void setScreen(int screenid){
		switch(screenid){
			case 1: screen = new OverviewScreen(server); break;
		}
	}
}
