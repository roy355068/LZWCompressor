package lzw;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class LZWCompression {
	
	final static int MAX = 4096;
	
	/**
	 * LZW compression algorithm implementation
	 * @param inputString which will be compressed
	 * @return compressed message in ArrayList<Integer> form
	 */
	private ArrayList<Integer> compress(String inputString) {
		int tableSize = 256;

		ArrayList<Integer> result = new ArrayList<Integer>();
		HashMap<String, Integer> symTable = new HashMap<String, Integer>();
		String s = "";
		char [] charArray = inputString.toCharArray();
		
		// Initialize the pattern mapping table
		for (int i = 0 ; i < tableSize ; i++) {
			String symbol = Character.toString((char) i);
			symTable.put(symbol, i);
		}
		
		for (char c : charArray) {
			
			/*
			 *  if the map exceeds the maximum size (2 ^ 12),
			 *  clear and reset the map
			 */
			if (tableSize >= MAX) {
				tableSize = 256;
				symTable = new HashMap<String, Integer>();
				for (int i = 0 ; i < tableSize ; i++) {
					String symbol = Character.toString((char) i);
					symTable.put(symbol, i);
				}
			}
			
			/*
			 *  if key (s + c) is in the mapping table
			 *  use the value of it as start point of 
			 *  the next round of compression
			 */
			if (symTable.containsKey(s + c)) {
				s = s + c;
			}
			/*
			 * Otherwise, add the value of s to the result
			 * and update the mapping table at the index of 
			 * current maximum table size
			 */
			else {
				result.add(symTable.get(s));
				symTable.put(s + c, tableSize);
				tableSize++;
				s = Character.toString(c);
			}
		
		}
		// add the not processed last data to the array
		if (s.equals("") == false) {
			result.add(symTable.get(s));
		}
		return result;
	}
	
	/**
	 * Compressing I/O routine, for encapsulation, this function 
	 * will call "compress" function to do the compression work.
	 * @param iFile input file
	 * @param oFile output file
	 * @throws IOException if the files not found
	 */
	public void compressor(String iFile, String oFile) throws IOException  {
		DataInputStream in;
		DataOutputStream out;
		
		in =
			new DataInputStream(
				new BufferedInputStream(
					new FileInputStream(iFile)));
		out =	
			new DataOutputStream(
				new BufferedOutputStream(
					new FileOutputStream(oFile)));
			
		ArrayList<Character> input = new ArrayList<Character>();
		ArrayList<Integer> comp;
		
		// read bytes from the input file, do masking to prevent sign extension
		byte byteIn;
		try {
			while (true) {
				byteIn = in.readByte();
				int temp = ((int)byteIn) & 0xff;
				input.add((char) temp);
			}
		} catch (EOFException e) {
			in.close();
		}
		
		/*
		 *  Since the program is going to modify the builder string so
		 *  many times, use StringBuilder to store the string would 
		 *  be a better and more efficient choice than String
		 */
		StringBuilder builder = new StringBuilder(input.size());
		for (Character c : input) {
			builder.append(c);
		}

		comp = compress(builder.toString());
		
		for (int i = 0 ; i < comp.size() ; i++) {
			// 12-bit mask
			int mask = 0xfff;
			int word = comp.get(i) & mask;
			int next = (i < (comp.size() - 1)) ? comp.get(i + 1) & mask : 0;
			
			// bit manipulations to construct the data that is 
			// going to be written out 
			byte[] bytes = ByteBuffer.allocate(3).array();
			if (i >= (comp.size() - 1)) {
				bytes[0] = (byte)((word & 0xff0) >>> 4);
				bytes[1] = (byte)((word & 0x00f) << 4);
				bytes[2] = (byte) 0;
			}
			else {
				bytes[0] = (byte)((word & 0xff0) >>> 4);
				bytes[1] = (byte)(((word & 0x00f) << 4) | ((next & 0xf00) >> 8));
				bytes[2] = (byte) (next & 0xff);
				i++;
			}
			
			// write data in bytes
			for (byte b : bytes) {
				out.writeByte(b);
			}

		}
		out.close();
	}
	
	/**
	 * LZW decompression algorithm implementation
	 * @param compressedWord is the compressed word and will be 
	 * 		  				 compressed to original message
	 * @return original message string
	 */
	private String decompress (ArrayList<Integer> compressedWord) {
		
		int tableSize = 256;
		
		/*
		 *  Since the program is going to modify the originalMsg so
		 *  many times, use StringBuilder to store the string would 
		 *  be a better and more efficient choice than String
		 */
		StringBuilder originalMsg = new StringBuilder();
		String pcw = Character.toString(((char)(int)compressedWord.remove(0)));
		String [] symTable = new String [MAX];
		String symbol;
		
		// initialize the array to NULL, and the originalMsg to the first char
		originalMsg.append(pcw);
		for (int i = 0 ; i < MAX ; i++) {
			symTable[i] = null;
		}
		
		// enter the first-256 symbols into the array map
		for (int i = 0 ; i < tableSize ; i++) {
			symbol = Character.toString((char)i);
			symTable[i] = symbol;
		}
		
		// while codewords(cw) are still left to be input
		// read it and parse it to set up output
		for (int cw : compressedWord) {
			String temp;
			
			// if-else set up the output string
			temp = (symTable[cw] == null) ? pcw + pcw.charAt(0) : symTable[cw];
			
			// enter string(pcw) + firstChar(string(cw)) into array
			if (tableSize < MAX)
				symTable[tableSize++] = pcw + temp.charAt(0);
			
			/*
			 *  if the array exceeds the maximum size (2 ^ 12),
			 *  clear and reset the String array
			 */
			else {
				for (int i = 256 ; i < MAX ; i++) {
					symTable[i] = null;
				}
				tableSize = 256;
				symTable[tableSize++] = pcw + temp.charAt(0);
			}
			
			pcw = temp;
			originalMsg.append(temp);
		}
		return originalMsg.toString();
	}
	
	/**
	 * Decompressing I/O routine, for encapsulation, this function 
	 * will call "decompress" function to do the decompression work.
	 * @param iFile input file
	 * @param oFile output file
	 * @throws IOException if the files not found
	 */
	public void decompressor(String inFile, String outFile) throws IOException {
		DataInputStream in = 
				new DataInputStream(
					new BufferedInputStream(
						new FileInputStream(inFile)));
		DataOutputStream out =
				new DataOutputStream(
					new BufferedOutputStream(
						new FileOutputStream(outFile)));
	
		ArrayList<Integer> CompressedString = new ArrayList<Integer>();
		
		// read data from file
		try {
			while (true) {
				int first;
				int second;
				int [] t = new int [3];
				// read 3 bytes at once
				t[0] = ((int)in.readByte()) & 0xff;
				t[1] = ((int)in.readByte()) & 0xff;
				t[2] = ((int)in.readByte()) & 0xff;
				
				// bit manipulation, parse data back into 2 integer data
				first = (t[0] << 4) | ((t[1] >>> 4) & 0xf);
				second = ((t[1] & 0xf) << 8) | t[2];
				
				CompressedString.add(first);
				CompressedString.add(second);
	
			}
		} catch (EOFException e) {
			in.close();
		}
		
		// write decompressed content into unzipped file
		String decompressedWord = decompress(CompressedString);
		for (int i : decompressedWord.toCharArray()) {
			
			// do masking to prevent sign bit extension of 
			// the data from binary file
			out.writeByte(i & 0xff);
		}
		out.close();
	}
	
	/**
	 * 
	 * Main routine of the LZWCompression.
	 * @param args - command line arguments
	 *  
	 * ** INTRO ** 
	 * 
	 * To let the program works on both ASCII and Binary files, we need to be 
	 * aware of the sign bit extension problems that arise from data type casting.
	 * When type casting from a smaller type (such as byte) to a larger byte (like int)
	 * , the sign bit of the data will be extended all the way to the leftmost bit,
	 * which as known as "most significant bit", and change every bit it passed to the 
	 * value of the sign bit (that is, change all bits from original sign bit to MSB to
	 * the value of the original sign bit).
	 * 
	 * To avoid that, when reading data from the file, I use masking (0xff) to ensure 
	 * that every byte data the program read contains only the lowest 8-bit pattern, 
	 * regardless of casting.
	 * 
	 * 
	 * ** TESTING **
	 * 
	 * I use both HashMap and TreeMap to store the mapping table in 
	 * compression routine, and following is the testing outcome on three big files.
	 * 
	 * Compress words.html using HashMap takes around 1574ms.
	 * Compress words.html using TreeMap takes around 2335ms.
	 * The compression rate of words.html is 2.5MB/1.1MB, around 2.272
	 * 
	 * Compress CrimeLatLonXY1990.csv using HashMap takes around 379ms.
	 * Compress CrimeLatLonXY1990.csv using TreeMap takes around 647ms.
	 * The compression rate of CrimeLatLonXY1990.csv is 279KB/139KB, around 2.007
	 * 
	 * Compress 01_Overview (1).mp4 using HashMap takes around 28897 ms.
	 * Compress 01_Overview (1).mp4 using TreeMap takes around 47394 ms.
	 * The compression rate of 01_Overview (1).mp4 is 25MB/33.8MB, around 0.740,
	 * which indicates that the file size grows after compression.
	 * 
	 * From the three testings we can tell that it takes longer using TreeMap than HashMap
	 * when compressing the file, as the result I keep the HashMap implementation.
	 * 
	 * Because when building the Map, TreeMap would have the order while HashMap 
	 * would not. Making this order takes more time hence has higher time complexity.
	 * In this case, we do not need the ordered elements to compress the data, 
	 * so the HashMap would be a faster solution.
	 * 
	 */
	public static void main(String[] args) {
		
		LZWCompression lzw = new LZWCompression();
		
		// check the command line arguments
		if (args.length != 3) {
			throw new IllegalArgumentException("Wrong input format"
					+ "must have a opreation character, one input file"
					+ "and a output file");
		}
		String operand = args[0];
		String in = args[1];
		String out = args[2];
		long start = 0;
		long time = 0;
		try {
			switch (operand) {
			case "c":
				start = System.currentTimeMillis();
				lzw.compressor(in, out);
				time = System.currentTimeMillis() - start;
				System.out.println("Compression time: " + time);
				break;
			case "d":
				start = System.currentTimeMillis();
				lzw.decompressor(in, out);
				time = System.currentTimeMillis() - start;
				System.out.println("Decompression time: " + time);
				break;
			default:
				throw new IllegalArgumentException("Wrong operation"
						+ ", must be either c for compressiom or d for"
						+ "decompression");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
