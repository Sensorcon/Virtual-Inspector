package com.sensorcon.virtualinspector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Simple IO class to store baseline offset
 * 
 * @author Sensorcon, Inc.
 */
public class BaselineStream {
	
	private BufferedWriter out;
	private BufferedReader in;
	
	private final String fileName = "baseline.txt";
	
	private File file;
	
	/**
	 * Initializes file to save baseline
	 * 
	 * @param context
	 */
	public void initFile(Context context) {
		
		try {
			file = new File(context.getFilesDir(), fileName);
			 
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			
		} catch (IOException e) {
			Log.d("chris", e.getMessage());
		}
		
	}
	
	/**
	 * Writes offset to file
	 * 
	 * @param offset
	 */
	public void writeOffset(int offset) {
		try {
			out = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
			out.write(Integer.toString(offset));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reads offset from file
	 * 
	 * @return offset
	 */
	public int readOffset() {
		int offset = 0;
		
		try {
			in = new BufferedReader(new FileReader(file.getAbsoluteFile()));
			offset = Integer.parseInt(in.readLine());
			in.close();
			Log.d("chris", "OFFSET: " + Integer.toString(offset));

		} catch (NumberFormatException e) {
			Log.d("chris", e.getMessage());
		} catch (IOException e) {
			Log.d("chris", e.getMessage());
		}
		
		return offset;
	}
	
	/**
	 * Deletes the file where baseline is stored
	 */
	public void reset() {
		file.delete();
	}
}
