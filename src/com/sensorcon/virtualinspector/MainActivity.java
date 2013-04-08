package com.sensorcon.virtualinspector;

import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity {
	
	private int cycles;
	private int count;
	private boolean startUpLEDSequence;
	
	private Timer timer;
	private Handler myHandler = new Handler();
	
	/*
	 * Accessable view variables from GUI
	 */
	public ImageView ledTopLeft_on;
	public ImageView ledTopRight_on;
	public ImageView ledBottomLeft_on;
	public ImageView ledBottomRight_on;
	private ImageView logoSensorconGray;
	private ImageView labelInspectorGray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ledTopLeft_on = (ImageView)findViewById(R.id.ledTopLeft_on);
		ledTopRight_on = (ImageView)findViewById(R.id.ledTopRight_on);
		ledBottomLeft_on = (ImageView)findViewById(R.id.ledBottomLeft_on);
		ledBottomRight_on = (ImageView)findViewById(R.id.ledBottomRight_on);
		logoSensorconGray = (ImageView)findViewById(R.id.logoSensorconGray);
		labelInspectorGray = (ImageView)findViewById(R.id.labelInspectorGray);
		
		ledTopLeft_on.setVisibility(View.GONE);
		ledTopRight_on.setVisibility(View.GONE);
		ledBottomLeft_on.setVisibility(View.GONE);
		ledBottomRight_on.setVisibility(View.GONE);
		logoSensorconGray.setVisibility(View.GONE);
		labelInspectorGray.setVisibility(View.GONE);	

		cycles = 0;
		count = 0;
		startUpLEDSequence = false;
		timer = new Timer();
		timer.schedule(new TimerTask() {
			public void run() {startUpLEDSequence(); }
		}, 125, 125);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void enableLED(int index) {
		switch(index) {
		case 0:
			ledTopLeft_on.setVisibility(View.VISIBLE);
			break;
		case 1:
			ledTopRight_on.setVisibility(View.VISIBLE);
			break;
		case 2:
			ledBottomLeft_on.setVisibility(View.VISIBLE);
			break;
		case 3:
			ledBottomRight_on.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}		
	}
	
	private void disableLED(int index) {
		switch(index) {
		case 0:
			ledTopLeft_on.setVisibility(View.GONE);
			break;
		case 1:
			ledTopRight_on.setVisibility(View.GONE);
			break;
		case 2:
			ledBottomLeft_on.setVisibility(View.GONE);
			break;
		case 3:
			ledBottomRight_on.setVisibility(View.GONE);
			break;
		default:
			break;
		}		
	}
	
	private void startUpLEDSequence() {
		startUpLEDSequence = true;
		myHandler.post(LEDRunnable);
	}
	
	final Runnable LEDRunnable = new Runnable() {
		
		@Override
		public void run() {
			switch(count) {
			case 0:
				enableLED(0);
				disableLED(1);
				disableLED(2);
				disableLED(3);
				break;
			case 1:
				disableLED(0);
				enableLED(1);
				disableLED(2);
				disableLED(3);
				break;
			case 2:
				disableLED(0);
				disableLED(1);
				disableLED(2);
				enableLED(3);
				break;
			case 3:
				disableLED(0);
				disableLED(1);
				enableLED(2);
				disableLED(3);
				break;
			}
			
			count++;
			
			if(count > 3) {
				count = 0;
				cycles ++;
			}
			
			if(startUpLEDSequence) {
				switch(cycles) {
				case 2:
					logoSensorconGray.setVisibility(View.VISIBLE);
					break;
				case 4:
					labelInspectorGray.setVisibility(View.VISIBLE);
					break;
				case 6:
					timer.cancel();
					disableLED(0);
					disableLED(1);
					disableLED(2);
					disableLED(3);
					logoSensorconGray.setVisibility(View.GONE);
					labelInspectorGray.setVisibility(View.GONE);
					cycles = 0;
					count = 0;
					startUpLEDSequence = false;
					break;
				default:
					break;
				}
			}
		}
	};
}
