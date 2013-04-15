package com.sensorcon.virtualinspector;

import java.util.EventObject;

import com.sensorcon.sdhelper.ConnectionBlinker;
import com.sensorcon.sdhelper.SDHelper;
import com.sensorcon.sdhelper.SDStreamer;
import com.sensorcon.sensordrone.Drone;
import com.sensorcon.sensordrone.Drone.DroneEventListener;
import com.sensorcon.sensordrone.Drone.DroneStatusListener;

import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main class for the Sensorcon Inspector. This app emulates an actual carbon monoxide detector sold
 * by Sensorcon, Inc.
 * 
 * Build using SDAndroidLib 1.1.1
 * 
 * @author Sensorcon, Inc.
 * @version 1.0.0
 */
public class MainActivity extends Activity {
	
	/*
	 * Constants
	 */
	private final String TAG = "chris";
	private final int MAX_MEASUREMENTS = 10;
	private final int LOW_ALARM_THRESHOLD = 35;
	private final int HIGH_ALARM_THRESHOLD = 200;
	private final int LOW_ALARM_TIMING = 1000;
	private final int HIGH_ALARM_TIMING = 250;
	/*
	 * Measurement variables
	 */
	public int concentration;
	public int offset;
	public int shownConcentration;
	public int numMeasurements;
	public int[] values = new int[10];
	public int[] blValues = new int[15];
	public int sum;
	public int average;
	public int blSum;
	public int blAverage;
	public int max;
	private BaselineStream blStream;
	/*
	 * Alarm variables
	 */
	private int ledTiming;
	private SoundPool alarmSound;
	private int soundId;
	private boolean loaded;
	private AudioManager am;
	/*
	 * Runs the sensordrone functions
	 */
	protected Drone myDrone;
	public Storage box;
	private Handler myHandler = new Handler();
	/*
	 * Contains functions to simplify connectivity
	 */
	public SDHelper myHelper;
	/*
	 * LED sequence variables
	 */
	private int cycles;
	private int count;
	private boolean startUpLEDSequence;
	/*
	 * Typeface variables
	 */
	Typeface lcdFont;
	/*
	 * Program flow flags
	 */
	public boolean leftPressed;
	public boolean rightPressed;
	public boolean inNormalMode;
	public boolean inCountdownMode;
	public boolean inBaselineMode;
	public boolean inBaselineCalcMode;
	public boolean lowAlarmActivated;
	public boolean highAlarmActivated;
	public boolean ledsActivated;
	public boolean leftArrowOn;
	public boolean rightArrowOn;
	public boolean poweredOn;
	public boolean btHoldActivated;
	public boolean showMax;
	/*
	 * Timing variables
	 */
	public int countdown;
	private int btCount;
	private int baselineCount;
	/*
	 * Accessable view variables from GUI
	 */
	public ImageView ledTopLeft_on;
	public ImageView ledTopRight_on;
	public ImageView ledBottomLeft_on;
	public ImageView ledBottomRight_on;
	private ImageView logoSensorconGray;
	private ImageView labelInspectorGray;
	private TextView ppmValue;
	private TextView countdownValue;
	private TextView labelPPM;
	private ImageButton leftButton;
	private ImageButton rightButton;
	private ImageButton leftButtonPressed;
	private ImageButton rightButtonPressed;
	private TextView labelCal;
	private TextView labelZero;
	private TextView labelHold;
	private ImageView arrowLeft;
	private ImageView arrowRight;
	private TextView labelDone;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize views
		ledTopLeft_on = (ImageView)findViewById(R.id.ledTopLeft_on);
		ledTopRight_on = (ImageView)findViewById(R.id.ledTopRight_on);
		ledBottomLeft_on = (ImageView)findViewById(R.id.ledBottomLeft_on);
		ledBottomRight_on = (ImageView)findViewById(R.id.ledBottomRight_on);
		logoSensorconGray = (ImageView)findViewById(R.id.logoSensorconGray);
		labelInspectorGray = (ImageView)findViewById(R.id.labelInspectorGray);
		leftButton = (ImageButton)findViewById(R.id.buttonLeft_unpressed);
		rightButton = (ImageButton)findViewById(R.id.buttonRight_unpressed);
		leftButtonPressed = (ImageButton)findViewById(R.id.buttonLeft_pressed);
		rightButtonPressed = (ImageButton)findViewById(R.id.buttonRight_pressed);
		arrowLeft = (ImageView)findViewById(R.id.arrowLeft);
		arrowRight = (ImageView)findViewById(R.id.arrowRight);
		ppmValue = (TextView)findViewById(R.id.ppmValue);
		labelPPM = (TextView)findViewById(R.id.labelPPM);
		labelZero = (TextView)findViewById(R.id.labelZero);
		labelHold = (TextView)findViewById(R.id.labelHold);
		countdownValue = (TextView)findViewById(R.id.countdownValue);
		labelCal = (TextView)findViewById(R.id.labelCal);
		labelDone = (TextView)findViewById(R.id.labelDone);
		
		// Set LED font
		lcdFont = Typeface.createFromAsset(this.getAssets(), "DS-DIGI.TTF");	
		ppmValue.setTypeface(lcdFont);		
		countdownValue.setTypeface(lcdFont);	
		labelCal.setTypeface(lcdFont);
		labelDone.setTypeface(lcdFont);

		// Make certain view invisible 
		leftButtonPressed.setVisibility(View.GONE);
		rightButtonPressed.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		ledTopLeft_on.setVisibility(View.GONE);
		ledTopRight_on.setVisibility(View.GONE);
		ledBottomLeft_on.setVisibility(View.GONE);
		ledBottomRight_on.setVisibility(View.GONE);
		logoSensorconGray.setVisibility(View.GONE);
		labelInspectorGray.setVisibility(View.GONE);	
		ppmValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		countdownValue.setVisibility(View.GONE);
		labelDone.setVisibility(View.GONE);
		
		// Initialize alarm
		alarmSound = new SoundPool(10, AudioManager.STREAM_ALARM, 0);
		alarmSound.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				loaded = true;
			}
		});
		soundId = alarmSound.load(this, R.raw.beep, 1);
		am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		// Initialize program flow variables
		leftPressed = false;
		rightPressed = false;
		inNormalMode = false;
		inCountdownMode = false;
		inBaselineMode = false;
		inBaselineCalcMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
		ledsActivated = false;
		poweredOn = false;
		showMax = false;

		// Initialize equation variables
		concentration = 0;
		shownConcentration = 0;
		average = 0;
		blAverage = 0;
		numMeasurements = 0;
		sum = 0;
		blSum = 0;
		for(int i = 0; i < MAX_MEASUREMENTS; i++) {
			values[i] = 0;
		}
		for(int i = 0; i < 15; i++) {
			blValues[i] = 0;
		}
		max = 0;
		blStream = new BaselineStream();
		blStream.initFile(this);
		offset = blStream.readOffset();
		
		if(offset == -1) {
			offset = 0;
		}
		
		Log.d(TAG, Integer.toString(offset));
		
		// Initialize timing variables
		countdown = 4;
		btCount = 0;
		baselineCount = 30;
		ledTiming = 1000;
		cycles = 0;
		count = 0;
		
		/*
		 * Controls the program flow for when the left button is pressed/released
		 */
		leftButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					leftPressed = true;
					leftButton.setVisibility(View.GONE);
					leftButtonPressed.setVisibility(View.VISIBLE);
					
					// Do only if connected to drone
					if(poweredOn) {
						// If both buttons are pressed, go to countdown mode
						if(rightPressed == true) {
							countdownMode();
						}
						// Otherwise, toggle mute button and start bluetooth count
						else {
							toggleMute();
							bluetoothHoldMode();
						}
					}
					else {
						// If device is disconnected, only do bluetooth count
						bluetoothHoldMode();
					}
					//Log.d(TAG, "Left button pressed\n");
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					leftPressed = false;
					leftButton.setVisibility(View.VISIBLE);
					leftButtonPressed.setVisibility(View.GONE);
					
					// Disable the bluetooth 3 second count
					btCount = 0;
					btHoldActivated = false;
					
					// Do only if connected to drone
					if(poweredOn) {
						// If in middle of countdown mode, go back to previous mode
						if(inCountdownMode == true) {
							// Reset countdown
							countdown = 4;
							
							// Go back to previous mode
							if(inBaselineMode) {
								baselineMode();
							}
							else {
								normalMode();
							}
						}
					}
					//Log.d(TAG, "Left button not pressed\n");
				}
				return true;
			}
		});
		
		/*
		 * Controls the program flow for when the right button is pressed/released
		 */
		rightButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				/*
				 * If button is pressed down
				 */
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					rightPressed = true;
					rightButton.setVisibility(View.GONE);
					rightButtonPressed.setVisibility(View.VISIBLE);
					
					// Do only if connected to drone
					if(poweredOn) {
						// If both buttons are pressed, go to countdown mode
						if(leftPressed == true) {
							countdownMode();
						}
						// Otherwise, toggle max
						else {
							toggleMax();
						}
						
						// Calculate baseline
						if(inBaselineMode) {
							baselineCalcMode();
						}
					}
					//Log.d(TAG, "Right button pressed\n");
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					rightPressed = false;
					rightButton.setVisibility(View.VISIBLE);
					rightButtonPressed.setVisibility(View.GONE);
					
					// Do only if connected to drone
					if(poweredOn) {
						// If in middle of countdown mode, go back to previous mode
						if(inCountdownMode == true) {
							// Reset countdown
							countdown = 4;
							
							// Go back to previous mode
							if(inBaselineMode) {
								baselineMode();
							}
							else {
								normalMode();
							}
						}
					}			
					//Log.d(TAG, "Right button not pressed\n");
				}
				return true;
			}
		});
		
		// Initialize drone variables
		myDrone = new Drone();
		box = new Storage(this);
		myHelper = new SDHelper();
		
		// LED startup sequence
		startupLEDSequence();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (isFinishing()) {
			// Try and nicely shut down
			doOnDisconnect();
			// A brief delay
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			// Unregister the listener
			myDrone.unregisterDroneEventListener(box.droneEventListener);
			myDrone.unregisterDroneStatusListener(box.droneStatusListener);

		} else { 
			//It's an orientation change.
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.resetBaseline:
			offset = 0;
			blStream.reset();
			blStream.initFile(this);
			break;
		}
			
		return true;
	}
	
	/*
	 * We use this so we can restore our data. Note that this has been deprecated as of 
	 * Android API 13. The official Android Developer's recommendation is 
	 * if you are targeting HONEYCOMB or later, consider instead using a 
	 * Fragment with Fragment.setRetainInstance(boolean)
	 * (Also available via the android-support libraries for older versions)
	 */
	@Override
	public Storage onRetainNonConfigurationInstance() {
		
		// Make a new Storage object from our old data
		Storage bin = box;
		// Return our old data
		return bin;
	}
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * HELPFUL FUNCTIONS
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Scans for nearby sensordrones and brings up list
	 */
	public void scan() {
		myHelper.scanToConnect(myDrone, MainActivity.this , this, false);
	}
	
	/**
	 * Actions to do when drone is disconnected
	 */
	public void doOnDisconnect() {
		// Shut off any sensors that are on
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// Turn off myBlinker
				box.myBlinker.disable();
				
				// Make sure the LEDs go off
				if (myDrone.isConnected) {
					myDrone.setLEDs(0, 0, 0);
				}
				
				// Only try and disconnect if already connected
				if (myDrone.isConnected) {
					myDrone.disconnect();
				}
			}
		});
	}
		
	/**
	 * A function to display Toast Messages.
	 * 
	 * By having it run on the UI thread, we will be sure that the message
	 * is displays no matter what thread tries to use it.
	 * 
	 * @param msg	Message to be displayed
	 */
	public void quickMessage(final String msg) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * GUI INTERACTION FUNCTIONS
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Toggles the mute arrow and functionality
	 */
	public void toggleMute() {
		if(leftArrowOn == false) {
			arrowLeft.setVisibility(View.VISIBLE);
			leftArrowOn = true;
			
		}
		else {
			arrowLeft.setVisibility(View.GONE);
			leftArrowOn = false;
		}
	}
	
	/**
	 * Toggles the max arrow and functionality
	 */
	public void toggleMax() {
		if(rightArrowOn == false) {
			arrowRight.setVisibility(View.VISIBLE);
			rightArrowOn = true;
			showMax = true;
		}
		else {
			arrowRight.setVisibility(View.GONE);
			rightArrowOn = false;
			showMax = false;
			max = 0;
		}
	}
	
	/**
	 * Performs a single beep
	 * 
	 * @return	True if successful
	 */
	public boolean beep() {
		float volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		float max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volume = volume/max;
		
		if(loaded) {
			alarmSound.play(soundId, volume, volume, 1, 0, 1f);
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Sets all necessary parameters when device is "powered down"
	 */
	private void powerDown() {
		poweredOn = false;
		normalMode();
		clearScreenAndFlags(false);
	}
	
	/**
	 * Enables a single LED based on the index passed
	 * 
	 * @param index		0 for top left, 1 for top right, 2 for bottom left, 3 for bottom right
	 */
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
	
	/**
	 * Disables a single LED based on the passed index
	 * 
	 * @param index		0 for top left, 1 for top right, 2 for bottom left, 3 for bottom right
	 */
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
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * FUNCTIONS THAT DEFINE STATE MACHINE
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Starts the LED sequence at start up
	 */
	private void startupLEDSequence() {
		ledsActivated = true;
		startUpLEDSequence = true;
		myHandler.post(LEDRunnable);
	}
	
	/**
	 * Sends program to normal mode
	 */
	public void normalMode() {
		if(poweredOn) {
			initNormalMode();
			myHandler.post(displayConcentrationRunnable);
		}
		else {
			// Display connection message
			poweredOn = false;
			quickMessage("Not connected. Hold left button to scan for Sensordrone.");
		}
	}
	
	/**
	 * Sends program to countdown mode
	 */
	public void countdownMode() {		
		initCountdownMode();
		myHandler.post(countdownRunnable);
	}
	
	/**
	 * Sends program to baseline mode
	 */
	public void baselineMode() {
		initBaselineMode();
		myHandler.post(arrowRunnable);
	}
	
	/**
	 * Sends program to baseline count mode
	 */
	public void baselineCalcMode() {
		initBaselineCalcMode();
		myHandler.post(baselineCalcRunnable);
	}
	
	/**
	 * Activates bluetooth count to connect/disconnect
	 */
	public void bluetoothHoldMode() {	
		btHoldActivated = true;
		myHandler.post(btCountRunnable);
	}
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * FUNCTIONS THAT INITIALIZE THE DIFFERENT MODES
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Sets views and flags for normal mode
	 */
	private void initNormalMode() {
		clearScreenAndFlags(false);
		
		ppmValue.setVisibility(View.VISIBLE);
		labelPPM.setVisibility(View.VISIBLE);
		inNormalMode = true;
	}
	
	/**
	 * Sets views and flags for countdown mode
	 */
	private void initCountdownMode() {
		clearScreenAndFlags(true);
		
		countdownValue.setVisibility(View.VISIBLE);
		labelHold.setVisibility(View.VISIBLE);
		countdown = 4;
		inCountdownMode = true;
	}
	
	/**
	 * Sets views and flags for baseline mode
	 */
	private void initBaselineMode() {
		clearScreenAndFlags(false);
		
		labelZero.setVisibility(View.VISIBLE);
		labelCal.setVisibility(View.VISIBLE);
		inBaselineMode = true;
	}
	
	/**
	 * Sets views and flags for baseline count mode
	 */
	private void initBaselineCalcMode() {
		clearScreenAndFlags(false);
		
		countdownValue.setVisibility(View.VISIBLE);
		labelZero.setVisibility(View.VISIBLE);
		baselineCount = 30;
		inBaselineCalcMode = true;
	}
	
	/**
	 * Clears all views and flags
	 * 
	 * @param rememberLastMode	If this is set to true, it will not clear the normal and baseline mode flags
	 */
	private void clearScreenAndFlags(boolean rememberLastMode) {
		ppmValue.setVisibility(View.GONE);
		countdownValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		labelDone.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		leftArrowOn = false;
		rightArrowOn = false;
		if(rememberLastMode == false) { inNormalMode = false; }
		inCountdownMode = false;
		if(rememberLastMode == false) { inBaselineMode = false; }
		inBaselineCalcMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
		ledsActivated = false;
		showMax = false;
	}
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * RUNNABLES THAT UPDATE THE GUI
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/*
	 * Controls the timing thread for the LEDs
	 */
	final Runnable LEDRunnable = new Runnable() {
		
		@Override
		public void run() {

			if(ledsActivated) {
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
				case 4:
					disableLED(0);
					disableLED(1);
					disableLED(2);
					disableLED(3);
					break;
				}
				
				count++;
				
				/*
				 * If all four leds flashed, do a quick delay before they flash again
				 */
				if(count > 4) {
					count = 0;
					cycles++;
					
					if(lowAlarmActivated) {
						ledTiming = LOW_ALARM_TIMING;
					}
					else if(highAlarmActivated) {
						ledTiming = HIGH_ALARM_TIMING;
					}
					else if(startUpLEDSequence) {
						ledTiming = 125;
					}
					
					myHandler.postDelayed(this, ledTiming);
					
					if(leftArrowOn == false) {
						if(lowAlarmActivated || highAlarmActivated) {
							beep();
						}
					}
				}
				else {
					myHandler.postDelayed(this, 125);
				}
				
				/*
				 * If program is just started, do a special LED sequence
				 */
				if(startUpLEDSequence) {
					switch(cycles) {
					case 2:
						logoSensorconGray.setVisibility(View.VISIBLE);
						break;
					case 4:
						labelInspectorGray.setVisibility(View.VISIBLE);
						break;
					case 6:
						logoSensorconGray.setVisibility(View.GONE);
						labelInspectorGray.setVisibility(View.GONE);
						startUpLEDSequence = false;
						ledsActivated = false;
						normalMode();
						break;
					default:
						break;
					}
				}
			}
			else {
				disableLED(0);
				disableLED(1);
				disableLED(2);
				disableLED(3);
			}
		}
	};

	/*
	 * Controls timing thread for countdown
	 */
	public Runnable countdownRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(inCountdownMode) {
				countdown--;
				
				if(countdown == 0) {
					countdown = 4;
					
					if(inBaselineMode) {
						normalMode();
						inBaselineMode = false;
					}
					else {
						baselineMode();
					}
				}
				else if(inCountdownMode == false) {
					countdown = 4;
				}
				else {
					countdownValue.setText(Integer.toString(countdown));
					myHandler.postDelayed(this, 1000);
				}
			}
		}
	};
	
	/*
	 * Controls timing thread for countdown
	 */
	public Runnable baselineCalcRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(inBaselineCalcMode) {
				baselineCount--;
				
				if(baselineCount < 15) {
					blValues[baselineCount] = concentration;
				}
				
				if(baselineCount == 0) {
					clearScreenAndFlags(false);
					labelDone.setVisibility(View.VISIBLE);
					
					// Calculate offset and write to file
					for(int i = 0; i < 15; i++) {
						blSum += blValues[i];
					}
					blAverage = blSum/15;
					
					Log.d(TAG, "blAverage: " + Integer.toString(blAverage));
					
					blStream.writeOffset(blAverage);
					offset = blAverage;
					
					myHandler.postDelayed(this, 1000);
				}
				else if(inBaselineCalcMode == false) {
					baselineCount = 30;
				}
				else {
					countdownValue.setText(Integer.toString(baselineCount));
					myHandler.postDelayed(this, 1000);
				}
			}
			else {
				normalMode();
			}
		}
	};
	
	/*
	 * Controls timing thread for baseline blinking arrow
	 */
	public Runnable arrowRunnable = new Runnable() {

		@Override
		public void run() {
			if(!inCountdownMode) {
				if(inBaselineMode) {
					if(rightArrowOn == false) {
						arrowRight.setVisibility(View.VISIBLE);
						rightArrowOn = true;
					}
					else {
						arrowRight.setVisibility(View.GONE);
						rightArrowOn = false;
					}
					
					myHandler.postDelayed(this,500);
				}
			}
		}		
	};
	
	/*
	 * Controls timing thread for bluetooth count
	 */
	public Runnable btCountRunnable = new Runnable() {

		@Override
		public void run() {
			if(btHoldActivated) {
				btCount++;
				
				if(btCount == 3) {
					btCount = 0;
					btHoldActivated = false;
					
					if(poweredOn) {
						doOnDisconnect();
					}
					else {
						scan();
					}
				}
				else {
					myHandler.postDelayed(this, 1000);
				}
			}
			else {
				btCount = 0;
			}
		}
	};
	
	/*
	 * Controls timing thread for displaying the concentration on lcd
	 */
	public Runnable displayConcentrationRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(inNormalMode && poweredOn) {
				
				shownConcentration = average - offset;
				
				if(shownConcentration < 0) {
					shownConcentration = 0;
				}
				
				// Check for new max
				if(shownConcentration > max) {
					max = shownConcentration;
				}
				
				if(showMax == true) {
					ppmValue.setText(Integer.toString(max));
				}
				else {
					ppmValue.setText(Integer.toString(shownConcentration));
				}
					
				myHandler.postDelayed(this, 1000);
			}
		}
	};
	
	/*
	 * Because Android will destroy and re-create things on events like orientation changes,
	 * we will need a way to store our objects and return them in such a case. 
	 * 
	 * A simple and straightforward way to do this is to create a class which has all of the objects
	 * and values we want don't want to get lost. When our orientation changes, it will reload our
	 * class, and everything will behave as normal! See onRetainNonConfigurationInstance in the code
	 * below for more information.
	 * 
	 * A lot of the GUI set up will be here, and initialized via the Constructor
	 */
	public final class Storage {
		
		// A ConnectionBLinker from the SDHelper Library
		public ConnectionBlinker myBlinker;
		
		// Holds the sensor of interest - the CO precision sensor
		public int sensor;
		
		// Our Listeners
		public DroneEventListener droneEventListener;
		public DroneStatusListener droneStatusListener;
		public String MAC = "";
		
		// GUI variables
		public TextView statusView;
		public TextView tvConnectionStatus;
		public TextView tvConnectInfo;
		
		// Streams data from sensor
		public SDStreamer streamer;
		
		public Storage(Context context) {
			
			// Initialize sensor
			sensor = myDrone.QS_TYPE_PRECISION_GAS;
			
			// This will Blink our Drone, once a second, Blue
			myBlinker = new ConnectionBlinker(myDrone, 1000, 0, 0, 255);
			
			streamer = new SDStreamer(myDrone, sensor);
			
			/*
			 * Let's set up our Drone Event Listener.
			 * 
			 * See adcMeasured for the general flow for when a sensor is measured.
			 * 
			 */
			droneEventListener = new DroneEventListener() {
				
				@Override
				public void connectEvent(EventObject arg0) {

					quickMessage("Connected!");
					
					poweredOn = true;
					normalMode();
					
					streamer.enable();
					myDrone.quickEnable(sensor);
					
					// Flash teh LEDs green
					myHelper.flashLEDs(myDrone, 3, 100, 0, 255, 0);
					// Turn on our blinker
					myBlinker.enable();
					myBlinker.run();
				}

				
				@Override
				public void connectionLostEvent(EventObject arg0) {
					// Turn off the blinker
					myBlinker.disable();
				}

				@Override
				public void disconnectEvent(EventObject arg0) {
					// If drone is disconnected, "power down" the inspector
					powerDown();
				}

				@Override
				public void precisionGasMeasured(EventObject arg0) {
					concentration = (int)myDrone.precisionGas_ppmCarbonMonoxide;
					
					values[numMeasurements] = concentration;
					numMeasurements++;
					
					if(numMeasurements == MAX_MEASUREMENTS) {
						numMeasurements = 0;
					}
					
					sum = 0;
					for(int i = 0; i < MAX_MEASUREMENTS; i++) {
						sum += values[i];
					}
					
					average = sum/MAX_MEASUREMENTS;
					
					if(average >= LOW_ALARM_THRESHOLD) {
						if(lowAlarmActivated == false) {
							lowAlarmActivated = true;
							ledsActivated = true;
							myHandler.post(LEDRunnable);
						}
					}
					else if(average >= HIGH_ALARM_THRESHOLD) {
						if(highAlarmActivated == false) {
							highAlarmActivated = true;
							ledsActivated = true;
							myHandler.post(LEDRunnable);
						}
					}
					else {
						ledsActivated = false;
						lowAlarmActivated = false;
						highAlarmActivated = false;
					}
					
					streamer.streamHandler.postDelayed(streamer, 1);
				}

				/*
				 * Unused events
				 */
				@Override
				public void customEvent(EventObject arg0) {}
				@Override
				public void adcMeasured(EventObject arg0) {}
				@Override
				public void altitudeMeasured(EventObject arg0) {}
				@Override
				public void capacitanceMeasured(EventObject arg0) {}
				@Override
				public void humidityMeasured(EventObject arg0) {}
				@Override
				public void i2cRead(EventObject arg0) {}
				@Override
				public void irTemperatureMeasured(EventObject arg0) {}
				@Override
				public void oxidizingGasMeasured(EventObject arg0) {}
				@Override
				public void pressureMeasured(EventObject arg0) {}
				@Override
				public void reducingGasMeasured(EventObject arg0) {}
				@Override
				public void rgbcMeasured(EventObject arg0) {}
				@Override
				public void temperatureMeasured(EventObject arg0) {}
				@Override
				public void uartRead(EventObject arg0) {}
				@Override
				public void unknown(EventObject arg0) {}
				@Override
				public void usbUartRead(EventObject arg0) {}
			};
			
			/*
			 * Set up our status listener
			 * 
			 * see adcStatus for the general flow for sensors.
			 */
			droneStatusListener = new DroneStatusListener() {

				@Override
				public void precisionGasStatus(EventObject arg0) {
					streamer.run();
				}
				
				/*
				 * Unused statuses
				 */
				@Override
				public void adcStatus(EventObject arg0) {}
				@Override
				public void altitudeStatus(EventObject arg0) {}
				@Override
				public void batteryVoltageStatus(EventObject arg0) {}
				@Override
				public void capacitanceStatus(EventObject arg0) {}
				@Override
				public void chargingStatus(EventObject arg0) {}
				@Override
				public void customStatus(EventObject arg0) {}
				@Override
				public void humidityStatus(EventObject arg0) {}
				@Override
				public void irStatus(EventObject arg0) {}
				@Override
				public void lowBatteryStatus(EventObject arg0) {}
				@Override
				public void oxidizingGasStatus(EventObject arg0) {}
				@Override
				public void pressureStatus(EventObject arg0) {}
				@Override
				public void reducingGasStatus(EventObject arg0) {}
				@Override
				public void rgbcStatus(EventObject arg0) {}
				@Override
				public void temperatureStatus(EventObject arg0) {}
				@Override
				public void unknownStatus(EventObject arg0) {}
			};
			
			// Register the listeners
			myDrone.registerDroneEventListener(droneEventListener);
			myDrone.registerDroneStatusListener(droneStatusListener);
		}
	}
}