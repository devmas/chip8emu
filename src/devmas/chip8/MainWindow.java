package devmas.chip8;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JPanel;

/** The emulator's screen is here. */
public class MainWindow extends JPanel {

	/** This is the data that we will draw to the screen. */
	byte[] screen = new byte[256];
	/** This is how much the screen will be scaled (magnification). */
	int mag;
	
	public MainWindow(int magnification) {
		super();
		this.mag = magnification;
		this.setBounds(100, 100, 64*magnification, 32*magnification);
		
		
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 64*mag, 32*mag);
		g.setColor(Color.WHITE);
		for (int i = 0; i < 256; i++) {
			byte eightPixels = screen[i];
			//imagine 0x10 -- 00010000
			for (int j = 0; j < 8; j++) {
				byte px = (byte) ((eightPixels >> 7-j) & 0x1);
				int x = i%8*8+j;
				int y = i/8;

				if (px == 1) g.fillRect(x*mag, y*mag, mag, mag);
			}
		}
	}

}
