package devmas.chip8;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileNameExtensionFilter;


public class Main {
	
	static volatile char buttonsDown;
	
	public static final int sleepTime = 1;
	
	public static final String filePath = "C:\\Users\\Colin\\Desktop\\Emulation\\chip8\\Mazed.ch8";
	
	public static void main(String[] args) throws Exception {
		
		//Get the file pointed to by filePath
		File romFile = new File(filePath);
		
		//If the file doesn't exist, then ask the user which file to load with a file chooser.
		if (!romFile.exists()) romFile = askForFile();
		
		//Create the emulator window
		MainWindow win = createWindow(8);
		
		//Create the emulator
		Emulator emu = new Emulator(romFile);
		
		//Set the screen data in the window to the screen data in the emulator
		// so that when the emulator draws to the screen, the window will see it.
		win.screen = emu.screen;
		
		//Print the memory in the emulator to the screen.
		//Since we didn't run the program yet, it will only contain the ROM.
		emu.printMemory();
		
		//Infinite loop (until an exception happens)
		while (true) {
			//Print the data relating to the emulated CPU
			emu.printProcessorData();
			//Execute a CPU instruction
			emu.step(buttonsDown, sleepTime);
			//Tell the window to repaint itself
			win.repaint();
		}
	}
	
	
	/** Creates a window and returns a component which will allow you to draw data. */
	public static MainWindow createWindow(int magnification) {
		JFrame frame = new JFrame();
		frame.setBounds(100, 100, 64*magnification + 16, 32*magnification + 39);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		MainWindow win = new MainWindow(magnification);
		frame.add(win);
		frame.setVisible(true);
		
		
		frame.addKeyListener(new EmulatorKeyListener());
		
		
		
		return win;
	}
	
	
	/** Asks the user to open a file. */
	static File askForFile() throws Exception {
		JFileChooser chooser = new JFileChooser();
		
		//This next line sets the starting directory of the file chooser to the directory
		// where the program is running from.
		chooser.setCurrentDirectory(new File("."));
		
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new FileNameExtensionFilter("Chip-8 ROM Images", "ch8"));
		
		int result = chooser.showOpenDialog(null);
		
		if (result == JFileChooser.APPROVE_OPTION) {
			File ret = chooser.getSelectedFile();
			System.out.println("Opening "+ret.getAbsolutePath());
			return ret;
		}
		else throw new Exception("No file chosen.");
	}
	
	
	static class EmulatorKeyListener implements KeyListener {
		@Override
		public void keyPressed(KeyEvent ke) {
			switch (ke.getKeyCode()) {
			case KeyEvent.VK_X:
				buttonsDown = (char) (buttonsDown | 0b0000000000000001);
				break;
			case KeyEvent.VK_1:
				buttonsDown = (char) (buttonsDown | 0b0000000000000010);
				break;
			case KeyEvent.VK_2:
				buttonsDown = (char) (buttonsDown | 0b0000000000000100);
				break;
			case KeyEvent.VK_3:
				buttonsDown = (char) (buttonsDown | 0b0000000000001000);
				break;
			case KeyEvent.VK_Q:
				buttonsDown = (char) (buttonsDown | 0b0000000000010000);
				break;
			case KeyEvent.VK_W:
				buttonsDown = (char) (buttonsDown | 0b0000000000100000);
				break;
			case KeyEvent.VK_E:
				buttonsDown = (char) (buttonsDown | 0b0000000001000000);
				break;
			case KeyEvent.VK_A:
				buttonsDown = (char) (buttonsDown | 0b0000000010000000);
				break;
			case KeyEvent.VK_S:
				buttonsDown = (char) (buttonsDown | 0b0000000100000000);
				break;
			case KeyEvent.VK_D:
				buttonsDown = (char) (buttonsDown | 0b0000001000000000);
				break;
			case KeyEvent.VK_Z:
				buttonsDown = (char) (buttonsDown | 0b0000010000000000);
				break;
			case KeyEvent.VK_C:
				buttonsDown = (char) (buttonsDown | 0b0000100000000000);
				break;
			case KeyEvent.VK_4:
				buttonsDown = (char) (buttonsDown | 0b0001000000000000);
				break;
			case KeyEvent.VK_R:
				buttonsDown = (char) (buttonsDown | 0b0010000000000000);
				break;
			case KeyEvent.VK_F:
				buttonsDown = (char) (buttonsDown | 0b0100000000000000);
				break;
			case KeyEvent.VK_V:
				buttonsDown = (char) (buttonsDown | 0b1000000000000000);
				break;
			}
		}

		@Override
		public void keyReleased(KeyEvent ke) {
			switch (ke.getKeyCode()) {
			case KeyEvent.VK_X:
				buttonsDown = (char) (buttonsDown & 0b1111111111111110);
				break;
			case KeyEvent.VK_1:
				buttonsDown = (char) (buttonsDown & 0b1111111111111101);
				break;
			case KeyEvent.VK_2:
				buttonsDown = (char) (buttonsDown & 0b1111111111111011);
				break;
			case KeyEvent.VK_3:
				buttonsDown = (char) (buttonsDown & 0b1111111111110111);
				break;
			case KeyEvent.VK_Q:
				buttonsDown = (char) (buttonsDown & 0b1111111111101111);
				break;
			case KeyEvent.VK_W:
				buttonsDown = (char) (buttonsDown & 0b1111111111011111);
				break;
			case KeyEvent.VK_E:
				buttonsDown = (char) (buttonsDown & 0b1111111110111111);
				break;
			case KeyEvent.VK_A:
				buttonsDown = (char) (buttonsDown & 0b1111111101111111);
				break;
			case KeyEvent.VK_S:
				buttonsDown = (char) (buttonsDown & 0b1111111011111111);
				break;
			case KeyEvent.VK_D:
				buttonsDown = (char) (buttonsDown & 0b1111110111111111);
				break;
			case KeyEvent.VK_Z:
				buttonsDown = (char) (buttonsDown & 0b1111101111111111);
				break;
			case KeyEvent.VK_C:
				buttonsDown = (char) (buttonsDown & 0b1111011111111111);
				break;
			case KeyEvent.VK_4:
				buttonsDown = (char) (buttonsDown & 0b1110111111111111);
				break;
			case KeyEvent.VK_R:
				buttonsDown = (char) (buttonsDown & 0b1101111111111111);
				break;
			case KeyEvent.VK_F:
				buttonsDown = (char) (buttonsDown & 0b1011111111111111);
				break;
			case KeyEvent.VK_V:
				buttonsDown = (char) (buttonsDown & 0b0111111111111111);
				break;
			}
		}

		@Override
		public void keyTyped(KeyEvent arg0) {}
		
	}
	
}
