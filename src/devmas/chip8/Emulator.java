package devmas.chip8;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Emulator {
	
	/** The screen data */
	public byte[] screen = new byte[256];
	
	/** The system's RAM */
	public byte[] memory = new byte[4096];
	
	/** The CPU's registers. They are known as V0, V1, V2 ... VE, VF */
	public byte[] registers = new byte[16];
	
	/** The CPU's stack */
	public short[] stack = new short[16]; //The stack
	
	/** The CPU's Stack Pointer, that is, the size of the stack */
	public byte sp;
	
	/** The CPU's Program Counter, that is, the memory address of the currently executing instruction.
	 * Execution always starts at 0x200 */
	public short pc = 0x200;
	
	/** The CPU's special I register, usually used for storing memory addresses */
	public short iReg;
	
	/** The CPU's special timers. These get decremented every 1/60th of a second. */
	public byte delayTimer, soundTimer;
	
	/** The time when to decrement the CPU's timers */
	public long nextTime;
	
	/** 1/60th of a second */
	public static final long REFRESH_RATE = (int)(1000000000/60);
	
	/** For storing the last button press state so that we can see if a button down event happens */
	public char lastButtons;
	
	/** This is the data that is stored at the beginning of RAM. It is actually a font containing
	 * the numbers 0 - 9 and the letters A - F. */
	public static final byte[] textData = {
		(byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
		(byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
		(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
		(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
		(byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
		(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
		(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
		(byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
		(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
		(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
		(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
		(byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
		(byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
		(byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
		(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
		(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
	};
	
	/** Creates an emulator instance with the specified ROM file. */
	public Emulator(File romFile) throws IOException {
		
		//First, copy the text data to the beginning of RAM
		System.arraycopy(textData, 0, memory, 0, textData.length);
		
		//Then load the ROM file and copy it to the RAM
		FileInputStream in = new FileInputStream(romFile);
		in.read(memory, 0x200, (4096-0x200)); //The program must start at 0x200
		in.close();
		nextTime = System.nanoTime();
	}
	
	/** Execute one CPU cycle.
	 * 
	 * @param buttons The buttons pressed. Most significant bit: Button 0. Least: Button F
	 */
	public void step(char buttons, int sleepTime) {
		
		//Check to see if it is time to decrement the special timer registers
		if (System.nanoTime() > nextTime) {
			nextTime = System.nanoTime() + REFRESH_RATE;
			if (delayTimer != 0) delayTimer --;
			if (soundTimer != 0) {
				soundTimer --;
			}
		}
		
		//Get the opcode.
		//Note that the opcode is 16 bits, but our RAM is 8 bits.
		//Therefore, we must take the memory address of pc and pc+1 and OR them together.
		short opcode = (short) ((memory[pc] << 8) | (memory[pc+1] & 0xff));
		
		//Print some data on the screen. 
		System.out.print("******\nOpcode at 0x" + getHexString(pc) + ": " + getHexString(opcode));
		System.out.println();
		
		//"Category" is the type of instruction it is. For example, a jmp instruction might
		// be 4xxx and arithmetic might be 8xxx, etc... In order to isolate the category, we
		// AND with F000
		int category = opcode & 0xF000;
		
		//and now we decode and execute the instruction. Depending on what the instruction is,
		//we will execute different code.
		switch (category) {
		case 0x0000:
			if (opcode == 0x00E0) {
				//CLS
				System.out.println("CLS");
				for (int i = 0; i < screen.length; i++) screen[i] = 0;
			}
			else if (opcode == 0x00EE) {
				//RET return from subroutine
				System.out.println("RET");
				pc = stack[--sp];
			}
			//TODO: Remove this, because other older computer systems might use system calls here
			else unimplemented(opcode);
			break;
		case 0x1000:
			//JMP 1xxx
			System.out.println("JMP "+getHexString((short) (opcode & 0x0FFF)));
			if ((opcode & 0x0FFF) == pc) throw new RuntimeException("Infinite loop!");
			pc = (short) ((opcode & 0x0FFF) - 2); //-2 because we will add 2 later
			break;
		case 0x2000:
			//JSR 2xxx
			System.out.println("JSR "+getHexString((short) (opcode & 0x0FFF)));
			stack[sp++] = pc;
			pc = (short) ((opcode & 0x0FFF) - 2); //-2 because we will add 2 later
			break;
		case 0x3000:
			//SE skip next inst if equal 3xyy Register[x] == yy
			int reg = (opcode & 0x0F00) >> 8;
				System.out.println("SE V"+reg+","+getHexString((short) (opcode & 0x00FF)));
			if ((registers[reg] & 0xFF) == (opcode & 0xFF)) pc += 2;
			break;
		case 0x4000:
			//SNE skip next inst if not equal 4xyy Register[x] != yy
			reg = (opcode & 0x0F00) >> 8;
			System.out.println("SNE V"+reg+","+getHexString((short) (opcode & 0x00FF)));
			if ((registers[reg] & 0xFF) != (opcode & 0xFF)) pc += 2;
			break;
		case 0x5000:
			//SE skip next inst if equal 5xy0 Register[x] == Register[y]
			reg = (opcode & 0x0F00) >> 8;
			int reg2 = (opcode & 0x00F0) >> 4;
			System.out.println("SE V"+reg+", V"+reg2);
			if (registers[reg] == registers[reg2]) pc += 2;
			break;
		case 0x6000:
			//LD 6xyy Register[x] = yy
			reg = (opcode & 0x0F00) >> 8;
			System.out.println("LD V"+reg+","+getHexString((short) (opcode & 0x00FF)));
			registers[reg] = (byte) (opcode & 0xFF); //probably don't need to do the anding
			break;
		case 0x7000:
			//ADD 7xyy Register[x] += yy
			reg = (opcode & 0x0F00) >> 8;
			System.out.println("ADD V"+reg+","+getHexString((short) (opcode & 0x00FF)));
			registers[reg] += (byte) (opcode & 0xFF); //probably don't need to do the anding
			break;
		case 0x8000:
			//A series of opcodes dealing with math on two registers.
			
			//Get the two registers
			reg = (opcode & 0x0F00) >> 8;
			reg2 = (opcode & 0x00F0) >> 4;
			
			switch (opcode & 0x000F) {
			case 0:
				//ld 8xy0 Vx = Vy
				System.out.println("LD V"+reg+", V"+reg2);
				registers[reg] = registers[reg2];
				break;
			case 1:
				//or 8xy1 Vx | Vy
				System.out.println("OR V"+reg+", V"+reg2);
				registers[reg] = (byte) (registers[reg] | registers[reg2]);
				break;
			case 2:
				//and 8xy2 Vx & Vy
				System.out.println("AND V"+reg+", V"+reg2);
				registers[reg] = (byte) (registers[reg] & registers[reg2]);
				break;
			case 3:
				//xor 8xy3 Vx ^ Vy
				System.out.println("XOR V"+reg+", V"+reg2);
				registers[reg] = (byte) (registers[reg] ^ registers[reg2]);
				break;
			case 4:
				//add 8xy4 Vx + Vy
				System.out.println("ADD V"+reg+", V"+reg2);
				int result = (registers[reg] & 0xFF) + (registers[reg2] & 0xFF);
				if (result > 255) registers[0xF] = 1;
				else registers[0xF] = 0;
				registers[reg] = (byte) (result);
				break;
			case 5:
				//sub 8xy5 Vx - Vy
				System.out.println("SUB V"+reg+", V"+reg2);
				if ((registers[reg] & 0xFF) > (registers[reg2] & 0xFF)) registers[0xF] = 1;
				else registers[0xF] = 0;
				registers[reg] = (byte) ((registers[reg] & 0xFF) - (registers[reg2] & 0xFF));
				break;
			case 6:
				//shr 8xy6 Vx >>> 1 (and carry is set to VF)
				System.out.println("SHR V"+reg);
				registers[0xF] = (byte) (registers[reg] & 0x01);
				registers[reg] = (byte) (registers[reg] >>> 1);
				break;
			case 7:
				//sub 8xy7 Vy - Vx (store in Vx)
				System.out.println("SUBN V"+reg2+", V"+reg);
				if ((registers[reg2] & 0xFF) > (registers[reg] & 0xFF)) registers[0xF] = 1;
				else registers[0xF] = 0;
				registers[reg] = (byte) ((registers[reg2] & 0xFF) - (registers[reg] & 0xFF));
				break;
			case 0xE:
				//shl 8xy6 Vx << 1 (and carry is set to VF)
				System.out.println("SHL V"+reg);
				registers[0xF] = (byte) ((registers[reg] & 0x80) >> 7);
				registers[reg] = (byte) (registers[reg] << 1);
				break;
			default:
				unimplemented(opcode);
				break;
			}
			break;
		case 0x9000:
			//sne 9xy0 if vx != vy
			reg = (opcode & 0x0F00) >> 8;
			reg2 = (opcode & 0x00F0) >> 4;
			System.out.println("SNE V"+reg+", V"+reg2);
			if (registers[reg] != registers[reg2]) pc += 2;
			break;
		case 0xA000:
			//ld I, xxx
			System.out.println("LD I, "+(opcode&0x0FFF));
			iReg = (short) (opcode & 0x0FFF);
			break;
		case 0xB000:
			//jmp Bxxx v0 + xxx
			System.out.println("JMP v0 + "+(opcode&0x0FFF));
			pc = (short) ((registers[0] & 0xFF)+(opcode & 0xFFF) - 2);
		case 0xC000:
			//RAND Cxyy creates random number + stores in register x after ANDing with yy
			reg = (opcode & 0x0F00) >> 8;
			registers[reg] = (byte) ((int)(System.nanoTime()) & opcode & 0xFF);
			break;
		case 0xD000:
			//DRW Vx, Vy, z  Dxyz  Draws a sprite at Vx,Vy with the sprite data being z bytes.
			drawSprite(opcode);
			break;
		case 0xE000:
			//Two opcodes dealing with input.
			reg = (opcode & 0x0F00) >> 8;
			switch (opcode & 0x00FF) {
			case 0x9E:
				//SKP Vx Ex9E Skips next instruction if button in register is pressed
				int button = registers[reg];
				if (button > 15) throw new RuntimeException("Invalid button!");
				if ((buttons >>> button & 0x1) == 1) {
					System.out.println("Check for button "+button+" passed; skipping instruction");
					pc += 2;
				}
				break;
			case 0xA1:
				//SKNP Vx Ex9E Skips next instruction if button in register is NOT pressed
				button = registers[reg];
				if (button > 15) throw new RuntimeException("Invalid button!");
				if ((buttons >>> button & 0x1) == 0) {
					System.out.println("Check for no button "+button+" passed; skipping instruction");
					pc += 2;
				}
				break;
			default:
				unimplemented(opcode);
				break;
			}
			break;
		case 0xF000:
			//A series of opcodes mostly dealing with math relating to the I register.
			reg = (opcode & 0x0F00) >> 8;
			switch (opcode & 0x00FF) {
			case 0x07:
				//ld Vx, dt Fx07
				registers[reg] = delayTimer;
				break;
			case 0x0A:
				//LD Vx, K (Halts CPU until key press, then stores key in register) Fx0A
				int buttonsDown = (lastButtons ^ buttons) & buttons;
				if (buttonsDown != 0) {
					byte buttonPressed = 0;
					while ((buttonsDown & 1) == 0) {
						buttonsDown = (char) (buttonsDown >>> 1);
						buttonPressed++;
						if (buttonPressed > 15) throw new RuntimeException("Uh, buttons failure!");
					}
					System.out.println("Button pressed: "+buttonPressed);
					registers[reg] = buttonPressed;
				}
				else pc -= 2; //reset the PC to redo the instruction, effectively halting CPU
				break;
			case 0x15:
				//ld dt, Vx Fx15
				delayTimer = registers[reg];
				break;
			case 0x18:
				//ld st, Vx Fx18
				soundTimer = registers[reg];
				break;
			case 0x1E:
				//add I, Vx Fx1E
				iReg += (registers[reg] & 0xFF);
				break;
			case 0x29:
				//Sets I to the location of the sprite from built-in font containing the letter
				iReg = (short) ((registers[reg] & 0xFF)*5);
				break;
			case 0x33:
				// LD Fx33 LD B, Vx (Loads base 10 of Vx into (hundreds) I, (tens) I+1, and (ones) I+2
				int num = registers[reg] & 0xFF;
				int ones = num % 10;
				int tens = num / 10 % 10;
				int hundreds = num / 100 % 10; //mod 10 isn't necessary here but doin' it anyway
				memory[iReg] = (byte) hundreds;
				memory[iReg+1] = (byte) tens;
				memory[iReg+2] = (byte) ones;
				break;
			case 0x55:
				//LD Fx55 [I], Vx - Stores V0 to Vx in memory pointed to by I
				System.arraycopy(registers, 0, memory, iReg, reg+1);
				//printMemory();
				break;
			case 0x65:
				//LD Fx55 Vx, [I] - Stores memory pointed to by I to V0 through Vx
				System.arraycopy(memory, iReg, registers, 0, reg+1);
				//printMemory();
				break;
			default:
				unimplemented(opcode);
				break;
			}
			break;
		default:
			unimplemented(opcode);
			
		}
		
		//Sets the last buttons pressed to the current buttons
		lastButtons = buttons;
		
		//Set the program counter to point to the next instruction
		pc += 2;
		
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/** Called if an opcode is not defined, either because the program is invalid or because
	 * I was too lazy to code the opcode... */
	private void unimplemented(int opcode) {
		System.out.print("XXXXXXX Unimplemented: ");
		printHexString((short) opcode);
		System.out.println();
		throw new RuntimeException("unimpl");
	}
	
	/** Draws a sprite to the console for debug purposes. */
	private void drawSpriteToConsole(byte[] spriteData) {
		for (int i = 0; i < spriteData.length; i++) {
			for (int b = 0; b < 8; b++) {
				System.out.print((spriteData[i] >>> (7-b) & 0x1) == 0? ' ': '#');
			}
			System.out.println();
		}
	}
	
	/** An interpretation of the opcode 0xDxyz */
	private void drawSprite(int opcode) {
		
		//DRW Vx, Vy, z  Dxyz  Draws a sprite at Vx,Vy with the sprite data being z bytes.
		int reg = (opcode & 0x0F00) >> 8;
		int reg2 = (opcode & 0x00F0) >> 4;
		
		int x = registers[reg];
		int y = registers[reg2];
		int spriteLength = opcode & 0x000F;
		
		System.out.println("Drawing the following sprite at "+x+","+y+
				"\nSprite size: 8x"+spriteLength+"   Sprite location: "+getHexString(iReg));
		
		byte[] spriteData = new byte[spriteLength];
		System.arraycopy(memory, iReg, spriteData, 0, spriteLength);
		//System.arraycopy(memory, 5*0x0B, spriteData, 0, spriteLength); //make every sprite B
		
		//Watch out, this is a doosey! We need to keep track of not only both array indicies,
		// but also the current bit of both bytes since we're copying bitwise.
		int screenBit = x % 8;
		int screenByte = ((x / 8) % 8) + (y*8);
		int spriteBit = 0;
		int spriteByte = 0;
		registers[0xF] = 0; //reset to 0. Will be set to 1 if a pixel is flipped to off.
		
		while (true) {
			
			if (screenByte >= 0 && screenByte < screen.length) {
				//fetch sprite bit
				int sprBit = (spriteData[spriteByte] >>> (7-spriteBit)) & 0x1;
				
				//sprBit will either be 0 or 1. If 0, then we do not worry about drawing.
				//if 1, then...
				if (sprBit == 1) {
					//fetch screen bit
					int scrBit = (screen[screenByte] >>> (7-screenBit)) & 0x1;
					if (scrBit == 1) registers[0xF] = 1; //if the screen bit will be changed to 0
					
					screen[screenByte] = (byte) ((sprBit << (7-screenBit)) ^ screen[screenByte]);
				}
			}
			
			
			
			//increment the sprite read location by one bit
			spriteBit ++;
			if (spriteBit >= 8) { //meaning a new line in the sprite
				spriteBit = spriteBit % 8;
				spriteByte ++;
				screenByte += 7; //adds 7 instead of 8 because the extra 1 is taken care of below
			}
			//increment the screen write location by one bit
			screenBit ++;
			if (screenBit >= 8) {
				screenBit = screenBit % 8;
				screenByte ++;
			}
			if (spriteByte >= spriteData.length) break;
			if (screenByte >= screen.length) break;
		}
		
		
		drawSpriteToConsole(spriteData);
		
	}
	
	/** Prints to the console data relating to the processor. */
	public void printProcessorData() {
		
		
		System.out.print("Program Counter (PC): ");
		printHexString(pc);
		System.out.print("  Stack Pointer (SP): ");
		printHexString(sp);
		System.out.print("  I Register (I): ");
		printHexString(iReg);

		System.out.println();
		
		System.out.print("Registers: ");
		
		for (int i = 0; i < registers.length; i++) {
			System.out.print("V"+i+": ");
			int b = registers[i];
			System.out.print(getHexLetterFromBits(b >> 4));
			System.out.print(getHexLetterFromBits(b));
			System.out.print(' ');
		}
		System.out.println();
		
		System.out.print("Stack: ");
		
		for (int i = 0; i < stack.length; i++) {
			System.out.print("S"+i+": ");
			printHexString(stack[i], 3);
			System.out.print(' ');
		}
		System.out.println();
	}
	
	/** Prints the RAM. Each line of 16 bytes is prefixed by the address (i.e. "0x1F0: ") */
	public void printMemory() {
		for (int i = 0; i < memory.length; i++) {
			if (i%16 == 0) {
				System.out.print("0x");
				printHexString((short) (i));
				System.out.print(": ");
			}
			
			Byte b = memory[i];
			System.out.print(getHexLetterFromBits(b >> 4));
			System.out.print(getHexLetterFromBits(b));
			
			if (i % 16 == 15)
				System.out.print('\n');
			else 
				System.out.print(' ');
			
		}
	}

	
	/** Takes a short number (like 498) and prints out a string (like "01F2") */
	private static void printHexString(short num, int nibbles) {
		
		//For example, if we were to print the value 0x37BF (four nibbles), we would first:
		// num >> 12 (which would be 37BF >> 12, which equals 3 after ANDing with 0x000F)
		// num >> 8 (which would be 37BF >> 8, which equals 7 after ANDing with 0x000F)
		// num >> 4 (which would be 37BF >> 4, which equals B after ANDing with 0x000F)
		// num >> 0 (which would be 37BF no bit shifting, which equals F after ANDing with 0x000F)
		
		for (int i = nibbles*4-4; i >= 0; i-=4) {
			System.out.print(getHexLetterFromBits(num >> i));
		}
	}
	
	/** Takes a short number (like 498) and prints out a string (like "01F2") */
	private static String getHexString(short num, int nibbles) {
		
		//For example, if we were to print the value 0x37BF (four nibbles), we would first:
		// num >> 12 (which would be 37BF >> 12, which equals 3 after ANDing with 0x000F)
		// num >> 8 (which would be 37BF >> 8, which equals 7 after ANDing with 0x000F)
		// num >> 4 (which would be 37BF >> 4, which equals B after ANDing with 0x000F)
		// num >> 0 (which would be 37BF no bit shifting, which equals F after ANDing with 0x000F)
		String ret = "";
		for (int i = nibbles*4-4; i >= 0; i-=4) {
			ret += (getHexLetterFromBits(num >> i));
		}
		return ret;
	}
	
	/** Returns the character representing the hex value of the first four bits of the given number.
	 * For example, if the number you send is 14, then this will return 'E'. */
	private static char getHexLetterFromBits(int bits) {
		bits = bits & 0xF;
		if (bits < 10) return (char) ('0'+bits);
		else return (char)('A'+bits-10);
	}
	
	private static void printHexString(short num) {
		printHexString(num, 4);
	}
	private static String getHexString(short num) {
		return getHexString(num, 4);
	}
	
}
