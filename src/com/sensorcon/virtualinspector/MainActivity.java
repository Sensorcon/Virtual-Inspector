package com.sensorcon.virtualinspector;

import java.util.EventObject;

import com.sensorcon.sdhelper.ConnectionBlinker;
import com.sensorcon.sdhelper.SDHelper;
import com.sensorcon.sdhelper.SDStreamer;
import com.sensorcon.sensordrone.Drone;
import com.sensorcon.sensordrone.Drone.DroneEventListener;
import com.sensorcon.sensordrone.Drone.DroneStatusListener;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import android.view.WindowManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.util.Log;
import android.view.Display;
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
@SuppressLint("NewApi")
public class MainActivity extends Activity {
	
	/*
	 * Constants
	 */
	private final String TAG = "chris";
	private final String NORMAL_MODE = "NORMAL";
	private final String BASELINE_MODE = "BASELINE";
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
	private PreferencesStream pStream;
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
	private Handler ledHandler = new Handler();
	private Handler countdownHandler = new Handler();
	private Handler baselineCalcHandler = new Handler();
	private Handler arrowHandler = new Handler();
	private Handler btCountHandler = new Handler();
	private Handler powerDownHandler = new Handler();
	private Handler displayConcentrationHandler = new Handler();
	private Handler cancelCalHandler = new Handler();
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
	public boolean inPowerDownMode;
	public boolean inAlarmMode;
	public boolean inCancelCalMode;
	public boolean lowAlarmActivated;
	public boolean highAlarmActivated;
	public boolean ledsActivated;
	public boolean leftArrowOn;
	public boolean rightArrowOn;
	public boolean poweredOn;
	public boolean btHoldActivated;
	public boolean showMax;
	public boolean showIntro;
	private String previousMode;
	/*
	 * Timing variables
	 */
	public int countdown;
	private int btCount;
	private int powerDownCount;
	private int baselineCount;
	private int cancelCalCount;
	/*
	 * Accessable view variables from GUI
	 */
	public ImageView ledTopLeft_on;
	public ImageView ledTopRight_on;
	public ImageView ledBottomLeft_on;
	public ImageView ledBottomRight_on;
	private ImageView logoSensorconGray;
	private ImageView labelInspectorGray;
	private TextView ppmValue0;
	private TextView ppmValue1;
	private TextView ppmValue2;
	private TextView ppmValue3;
	private TextView countdownValue;
	private TextView labelPPM;
	private ImageButton leftButton;
	private ImageButton rightButton;
	private ImageButton leftButtonPressed;
	private ImageButton rightButtonPressed;
	private TextView labelCal;
	private TextView labelNo;
	private TextView labelZero;
	private TextView labelHold;
	private TextView labelOL;
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
		ppmValue0 = (TextView)findViewById(R.id.ppmValue0);
		ppmValue1 = (TextView)findViewById(R.id.ppmValue1);
		ppmValue2 = (TextView)findViewById(R.id.ppmValue2);
		ppmValue3 = (TextView)findViewById(R.id.ppmValue3);
		labelPPM = (TextView)findViewById(R.id.labelPPM);
		labelZero = (TextView)findViewById(R.id.labelZero);
		labelHold = (TextView)findViewById(R.id.labelHold);
		labelOL = (TextView)findViewById(R.id.labelOL);
		countdownValue = (TextView)findViewById(R.id.countdownValue);
		labelCal = (TextView)findViewById(R.id.labelCal);
		labelNo = (TextView)findViewById(R.id.labelNo);
		labelDone = (TextView)findViewById(R.id.labelDone);
		
		// Set LED font
		lcdFont = Typeface.createFromAsset(this.getAssets(), "DS-DIGI.TTF");	
		ppmValue0.setTypeface(lcdFont);
		ppmValue1.setTypeface(lcdFont);	
		ppmValue2.setTypeface(lcdFont);	
		ppmValue3.setTypeface(lcdFont);	
		countdownValue.setTypeface(lcdFont);	
		labelCal.setTypeface(lcdFont);
		labelNo.setTypeface(lcdFont);
		labelDone.setTypeface(lcdFont);
		labelOL.setTypeface(lcdFont);

		// Make certain views invisible 
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
		ppmValue0.setVisibility(View.GONE);
		ppmValue1.setVisibility(View.GONE);
		ppmValue2.setVisibility(View.GONE);
		ppmValue3.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		labelNo.setVisibility(View.GONE);
		labelOL.setVisibility(View.GONE);
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
		inPowerDownMode = false;
		inAlarmMode = false;
		inCancelCalMode = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
		ledsActivated = false;
		poweredOn = false;
		showMax = false;
		showIntro = true;
		previousMode = NORMAL_MODE;

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
		
		// Check for offset
		if(offset == -1) {
			offset = 0;
		}
		
		// Check user preferences
		pStream = new PreferencesStream();
		pStream.initFile(this);
		String[] preferences = new String[1];
		preferences = pStream.readPreferences();
		
		if(preferences[0].equals("DISABLE INTRO")){
			showIntro = false;
		}
		
//		Log.d(TAG, "Offset " + Integer.toString(offset));
//		Log.d(TAG, preferences[0]);
		
		// Initialize timing variables
		countdown = 6;
		btCount = 0;
		powerDownCount = 5;
		baselineCount = 31;
		ledTiming = 1000;
		cycles = 0;
		count = 0;
		cancelCalCount = 3;
		
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
					leftButtonPressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					leftButtonReleased();
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
					rightButtonPressed();
				}
				/*
				 * If button is released
				 */
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					rightButtonReleased();
				}
				return true;
			}
		});
		
		// Layout debug
//		int layout = this.getResources().getConfiguration().screenLayout &  Configuration.SCREENLAYOUT_SIZE_MASK;
//		//Log.d(TAG, "Layout: " + Integer.toString(layout));
//		
//		String tag = (String)findViewById(R.id.my_activity_view).getTag();
//		//Log.d(TAG, "Tag: " + tag);
		
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
			showResetBaselineDialog();
			break;
		case R.id.instructions:
			Intent myIntent = new Intent(getApplicationContext(), InstructionsActivity.class);
			startActivity(myIntent);
			break;
		case R.id.factoryReset:
			showFactoryResetDialog();
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
	 * STATE MACHINE
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Program flow for left button being pressed down
	 */
	public void leftButtonPressed() {
		leftPressed = true;
		leftButton.setVisibility(View.GONE);
		leftButtonPressed.setVisibility(View.VISIBLE);
		
		// Do only if connected to drone
		if(poweredOn) {
			// If both buttons are pressed, go to countdown mode
			if(rightPressed == true) {
				countdownMode();
			}
			// Otherwise, decide what to do based on current mode
			else {
				if(inNormalMode) {
					// Toggle mute button and start power down count
					toggleMute();
					powerDownMode();
				}
				else if(inCountdownMode) {
					// N/A
				}
				else if(inBaselineMode) {
					// Go back to normal mode
					cancelCalMode();
				}
				else if(inCancelCalMode) {
					// N/A
				}
				else if(inBaselineCalcMode) {
					// Go back to normal mode
					cancelCalMode();
				}
				else if(inPowerDownMode) {
					// N/A
				}
			}
		}
		else {
			// If device is disconnected, only do bluetooth count
			bluetoothHoldMode();
		}
		//Log.d(TAG, "Left button pressed\n");
	}
	
	/**
	 * Program flow for left button being released
	 */
	public void leftButtonReleased() {
		leftPressed = false;
		leftButton.setVisibility(View.VISIBLE);
		leftButtonPressed.setVisibility(View.GONE);
		
		// Disable the bluetooth 3 second count
		btCount = 0;
		btHoldActivated = false;
		btCountHandler.removeCallbacksAndMessages(null);
		
		// Reset power down variables
		powerDownCount = 5;
		powerDownHandler.removeCallbacksAndMessages(null);
		
		// Do only if connected to drone
		if(poweredOn) {
			if(inNormalMode) {
				// N/A
			}
			else if(inCountdownMode) {
				// Reinitialize previous mode
				countdown = 6;
				
				// Go back to normal mode
				normalMode();
			}
			else if(inBaselineMode) {
				// N/A
			}
			else if(inBaselineCalcMode) {
				// N/A
			}
			else if(inCancelCalMode) {
				// N/A
			}
			else if(inPowerDownMode) {
				// Reinitialize previous mode
				powerDownCount = 5;
				
				// Go back to previous mode
				if(previousMode == NORMAL_MODE) {
					normalMode();
				}
				else {
					baselineMode();
				}
			}
		}
		//Log.d(TAG, "Left button not pressed\n");
	}
	
	/**
	 * Program flow for right button being pressed down
	 */
	public void rightButtonPressed() {
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
				if(inNormalMode) {
					// Toggle max
					toggleMax();
				}
				else if(inCountdownMode) {
					// N/A
				}
				else if(inBaselineMode) {
					baselineCalcMode();
				}
				else if(inCancelCalMode) {
					// N/A
				}
				else if(inBaselineCalcMode) {
					// N/A
				}
				else if(inPowerDownMode) {
					// N/A
				}
			}
		}
		//Log.d(TAG, "Right button pressed\n");
	}
	
	/**
	 * Program flow for right button being released
	 */
	public void rightButtonReleased() {
		rightPressed = false;
		rightButton.setVisibility(View.VISIBLE);
		rightButtonPressed.setVisibility(View.GONE);
		
		// Do only if connected to drone
		if(poweredOn) {
			if(inNormalMode) {
				// N/A
			}
			else if(inCountdownMode) {
				// Reinitialize previous mode
				countdown = 6;
				
				// Go back to normal mode
				normalMode();
			}
			else if(inBaselineMode) {
				// N/A
			}
			else if(inCancelCalMode) {
				// N/A
			}
			else if(inBaselineCalcMode) {
				// N/A
			}
			else if(inPowerDownMode) {
				// N/A
			}
		}			
		//Log.d(TAG, "Right button not pressed\n");
	}
	
	/*************************************************************************************************
	 *************************************************************************************************
	 * HELPFUL FUNCTIONS
	 *************************************************************************************************
	 *************************************************************************************************/
	
	/**
	 * Loads the dialog shown at startup
	 */
	public void showIntroDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(false);
		alert.setTitle("Introduction").setMessage("If you are new to the Inspector app, you should read through the instructions. To access them, go to the top right menu and select Instructions.");
		alert.setPositiveButton("Don't Show Again", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            pStream.disableIntroDialog();
		        }
		     })
		    .setNegativeButton("Okay", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            // do nothing
		        }
		     }).show();
	}
	
	/**
	 * Shows the dialog when user attempts to reset the baseline
	 */
	public void showResetBaselineDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Reset Baseline").setMessage("Are you sure you want to remove the baseline offset?");
		alert.setPositiveButton("No", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        }
		     })
		    .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	resetBaseline();
		        }
		     }).show();
	}
	
	/**
	 * Shows the dialog when user tries to erase all saved preferences
	 */
	public void showFactoryResetDialog() {

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Factory Reset").setMessage("Are you sure you would like to reset all saved user settings?");
		alert.setPositiveButton("No", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        }
		     })
		    .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            factoryReset();
		        }
		     }).show();
	}
	
	/**
	 * Resets baseline variables
	 */
	public void resetBaseline() {
		offset = 0;
		blStream.reset();
		blStream.initFile(this);
	}
	
	/**
	 * Resets all user saved variables
	 */
	public void factoryReset() {
		offset = 0;
		blStream.reset();
		blStream.initFile(this);
		
		pStream.reset();
	}
	
	/**
	 * Displays concentration value on virtual LCD
	 * 
	 * @param val	Value in PPM
	 */
	public void setDisplayValue(int val) {
		int d0 = 0;
		int d1 = 0;
		int d2 = 0;
		int d3 = 0;
		
		// Check the number of places, and adjust LCD characters accordingly
		if((val > 9) && (val < 100)) {
			d1 = val/10;
			d0 = val % 10;
			
			ppmValue0.setText(Integer.toString(d0));
			ppmValue1.setText(Integer.toString(d1));
			
			ppmValue0.setVisibility(View.VISIBLE);
			ppmValue1.setVisibility(View.VISIBLE);
			ppmValue2.setVisibility(View.GONE);
			ppmValue3.setVisibility(View.GONE);
			labelOL.setVisibility(View.GONE);
		}
		else if((val > 99) && (val < 999)) {
			d2 = val / 100;
			d1 = (val % 100)/10;
			d0 = val % 10;
			
			ppmValue0.setText(Integer.toString(d0));
			ppmValue1.setText(Integer.toString(d1));
			ppmValue2.setText(Integer.toString(d2));
			
			ppmValue0.setVisibility(View.VISIBLE);
			ppmValue1.setVisibility(View.VISIBLE);
			ppmValue2.setVisibility(View.VISIBLE);
			ppmValue3.setVisibility(View.GONE);
			labelOL.setVisibility(View.GONE);
			
		}
		else if((val > 999) && (val < 2000)) {
			d3 = val / 1000;
			d2 = (val % 1000)/100;
			d1 = (val % 100)/10;
			d0 = val % 10;
			
			ppmValue0.setText(Integer.toString(d0));
			ppmValue1.setText(Integer.toString(d1));
			ppmValue2.setText(Integer.toString(d2));
			ppmValue3.setText(Integer.toString(d3));
			
			ppmValue0.setVisibility(View.VISIBLE);
			ppmValue1.setVisibility(View.VISIBLE);
			ppmValue2.setVisibility(View.VISIBLE);
			ppmValue3.setVisibility(View.VISIBLE);
			labelOL.setVisibility(View.GONE);
		}
		else if(val > 1999) {
			ppmValue0.setVisibility(View.GONE);
			ppmValue1.setVisibility(View.GONE);
			ppmValue2.setVisibility(View.GONE);
			ppmValue3.setVisibility(View.GONE);
			labelOL.setVisibility(View.VISIBLE);
		}
		else {
			ppmValue0.setText(Integer.toString(val));
			
			ppmValue0.setVisibility(View.VISIBLE);
			ppmValue1.setVisibility(View.GONE);
			ppmValue2.setVisibility(View.GONE);
			ppmValue3.setVisibility(View.GONE);
			labelOL.setVisibility(View.GONE);
		}
	}
	
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
		clearScreenAndFlags();
		doOnDisconnect();
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
		ledHandler.post(LEDRunnable);
	}
	
	/**
	 * Sends program to normal mode
	 */
	public void normalMode() {
		Log.d(TAG, "In Normal Mode");
		
		if(poweredOn) {
			initNormalMode();
			displayConcentrationHandler.post(displayConcentrationRunnable);
			//myHandler.post(calculateAverageRunnable);
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
		Log.d(TAG, "In Countdown Mode");
		
		initCountdownMode();
		countdownHandler.post(countdownRunnable);
	}
	
	/**
	 * Sends program to baseline mode
	 */
	public void baselineMode() {
		Log.d(TAG, "In Baseline Mode");
		
		initBaselineMode();
		arrowHandler.post(arrowRunnable);
	}
	
	/**
	 * Sends program to baseline count mode
	 */
	public void baselineCalcMode() {
		Log.d(TAG, "In Calc Mode");
		
		initBaselineCalcMode();
		baselineCalcHandler.post(baselineCalcRunnable);
	}
	
	/**
	 * Stops calibration and shows "NO CAL"
	 */
	public void cancelCalMode() {
		Log.d(TAG, "Cancel cal mode");
		
		initCancelCalMode();
		cancelCalHandler.post(cancelCalRunnable);
	}
	
	/**
	 * Starts countdown to "power down" the device
	 */
	public void powerDownMode() {
		powerDownHandler.post(powerDownRunnable);
	}
	
	/**
	 * Activates bluetooth count to connect/disconnect
	 */
	public void bluetoothHoldMode() {	
		btHoldActivated = true;
		btCountHandler.post(btCountRunnable);
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
		clearScreenAndFlags();
		
		previousMode = NORMAL_MODE;
		
		ppmValue0.setVisibility(View.VISIBLE);
		labelPPM.setVisibility(View.VISIBLE);
		inNormalMode = true;
	}
	
	/**
	 * Sets views and flags for countdown mode
	 */
	private void initCountdownMode() {
		clearScreenAndFlags();
		
		countdownValue.setVisibility(View.VISIBLE);
		labelHold.setVisibility(View.VISIBLE);
		countdown = 6;
		inCountdownMode = true;
	}
	
	/**
	 * Sets views and flags for baseline mode
	 */
	private void initBaselineMode() {
		clearScreenAndFlags();
		
		previousMode = BASELINE_MODE;
		
		labelZero.setVisibility(View.VISIBLE);
		labelCal.setVisibility(View.VISIBLE);
		inBaselineMode = true;
	}
	
	/**
	 * Sets views and flags for baseline count mode
	 */
	private void initBaselineCalcMode() {
		clearScreenAndFlags();
		
		countdownValue.setVisibility(View.VISIBLE);
		labelZero.setVisibility(View.VISIBLE);
		baselineCount = 31;
		inBaselineCalcMode = true;
	}
	
	/**
	 * Sets views and flags for baseline count mode
	 */
	private void initPowerDownMode() {
		clearScreenAndFlags();
		
		inPowerDownMode = true;
		countdownValue.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Sets views and flags for cancel calibration mode
	 */
	private void initCancelCalMode() {
		clearScreenAndFlags();
		
		inCancelCalMode = true;
		labelZero.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Clears all views and flags
	 * 
	 * @param rememberLastMode	If this is set to true, it will not clear the normal and baseline mode flags
	 */
	private void clearScreenAndFlags() {
		ppmValue0.setVisibility(View.GONE);
		ppmValue1.setVisibility(View.GONE);
		ppmValue2.setVisibility(View.GONE);
		ppmValue3.setVisibility(View.GONE);
		countdownValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		labelDone.setVisibility(View.GONE);
		labelOL.setVisibility(View.GONE);
		ledTopLeft_on.setVisibility(View.GONE);
		ledTopRight_on.setVisibility(View.GONE);
		ledBottomLeft_on.setVisibility(View.GONE);
		ledBottomRight_on.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		leftArrowOn = false;
		rightArrowOn = false;
		inNormalMode = false;
		inCountdownMode = false;
		inBaselineMode = false;
		inBaselineCalcMode = false;
		inCancelCalMode = false;
		inAlarmMode = false;
		inPowerDownMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
		ledsActivated = false;
		showMax = false;
		ledHandler.removeCallbacksAndMessages(null);
		countdownHandler.removeCallbacksAndMessages(null);
		baselineCalcHandler.removeCallbacksAndMessages(null);
		cancelCalHandler.removeCallbacksAndMessages(null);
		arrowHandler.removeCallbacksAndMessages(null);
		btCountHandler.removeCallbacksAndMessages(null);
		powerDownHandler.removeCallbacksAndMessages(null);
		displayConcentrationHandler.removeCallbacksAndMessages(null);
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
				
				// Cycle through the four leds
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
					
					// Check for alarms, in which case you will adjust timing
					if(lowAlarmActivated) {
						ledTiming = LOW_ALARM_TIMING;
					}
					else if(highAlarmActivated) {
						ledTiming = HIGH_ALARM_TIMING;
					}
					else if(startUpLEDSequence) {
						ledTiming = 125;
					}
					
					// Make audible beep, and show leds on actual Sensordrone
					if(leftArrowOn == false) {
						if(lowAlarmActivated || highAlarmActivated) {
							beep();
							myHelper.flashLEDs(myDrone, 2, 125, 255, 0, 0);
						}
					}
				}
				else {
					ledTiming = 125;
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
						
						if(showIntro) {
							showIntroDialog();
						}
						
						normalMode();
						break;
					default:
						break;
					}
				}
				
				ledHandler.postDelayed(this, ledTiming);
			}
			else {
				// Make sure leds are off if they are not supposed to be activated
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
				
				// When countdown reaches 0, go to baseline mode
				if(countdown == 0) {
					countdown = 6;
					
					if(inBaselineMode) {
						normalMode();
						inBaselineMode = false;
					}
					else {
						baselineMode();
					}
				}
				else if(inCountdownMode == false) {
					countdown = 6;
				}
				else {
					countdownValue.setText(Integer.toString(countdown));
					countdownHandler.postDelayed(this, 1000);
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
				
				// Average for the last 15 seconds of count down
				if(baselineCount < 15) {
					blValues[baselineCount] = concentration;
				}
				
				if(baselineCount == 0) {
					// Show the DONE label
					clearScreenAndFlags();
					labelDone.setVisibility(View.VISIBLE);
					
					// Calculate offset and write to file
					for(int i = 0; i < 15; i++) {
						blSum += blValues[i];
					}
					blAverage = blSum/15;
					
					//Log.d(TAG, "blAverage: " + Integer.toString(blAverage));
					
					// Write offset to file
					blStream.writeOffset(blAverage);
					offset = blAverage;
					
					baselineCalcHandler.postDelayed(this, 1000);
				}
				else if(inBaselineCalcMode == false) {
					baselineCount = 31;
				}
				else {
					countdownValue.setText(Integer.toString(baselineCount));
					baselineCalcHandler.postDelayed(this, 1000);
				}
			}
			else {
				normalMode();
			}
		}
	};
	
	/*
	 * Controls timing thread for canceling calibration
	 */
	public Runnable cancelCalRunnable = new Runnable() {

		@Override
		public void run() {
			if(inCancelCalMode) {
				cancelCalCount--;
				
				// Show NO CAL
				if(cancelCalCount == 2) {
					labelNo.setVisibility(View.VISIBLE);
				}
				else if(cancelCalCount == 1) {
					labelNo.setVisibility(View.GONE);
					labelCal.setVisibility(View.VISIBLE);
				}
				else if(cancelCalCount == 0) {
					cancelCalCount = 3;
					normalMode();
				}
				
				cancelCalHandler.postDelayed(this, 1000);
			}
			else {
				cancelCalCount = 3;
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
					// Blink the right arrow every half second
					if(rightArrowOn == false) {
						arrowRight.setVisibility(View.VISIBLE);
						rightArrowOn = true;
					}
					else {
						arrowRight.setVisibility(View.GONE);
						rightArrowOn = false;
					}
					
					arrowHandler.postDelayed(this,500);
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
				
				// After three second, scan for Sensordrone
				if(btCount == 3) {
					btCount = 0;
					btHoldActivated = false;
					scan();
				}
				else {				
					btCountHandler.postDelayed(this, 1000);
				}
			}
			else {
				btCount = 0;
			}
		}
	};
	
	/*
	 * Controls timing thread for bluetooth count
	 */
	public Runnable powerDownRunnable = new Runnable() {

		@Override
		public void run() {
					
			//Log.d(TAG, "power count: " + Integer.toString(powerDownCount));
			
			if(inPowerDownMode) {
				powerDownCount--;
				
				// If countdown reaches 0, "power down" the device
				if(powerDownCount == 0) {
					powerDownCount = 5;
					inPowerDownMode = false;
					powerDown();		
				}
				else {
					countdownValue.setText(Integer.toString(powerDownCount));
					powerDownHandler.postDelayed(this, 1000);
				}
			}
			else {
				// Start countdown when left button is pressed
				if(leftPressed && !inPowerDownMode) {
					powerDownCount--;
						
					// After one second, actually show the countdown
					if(powerDownCount == 3) {
						//Log.d(TAG, "reached power mode");
						initPowerDownMode();
						countdownValue.setText(Integer.toString(powerDownCount));
					}
					
					powerDownHandler.postDelayed(this, 1000);
				}
				else {
					powerDownCount = 5;
				}
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
				
				// Check for low alarm
				if((concentration >= LOW_ALARM_THRESHOLD) && (concentration < HIGH_ALARM_THRESHOLD)  ) {
					
					// Initialize alarm 
					if(lowAlarmActivated == false) {
						lowAlarmActivated = true;
						highAlarmActivated = false;
						ledsActivated = true;

						// Leds on drone
						box.myBlinker.disable();
					
						if(inAlarmMode == false) {
							displayConcentrationHandler.post(LEDRunnable);
							inAlarmMode = true;
						}
					}
				}
				// Check for high alarm
				else if(concentration >= HIGH_ALARM_THRESHOLD) {
					
					// Initialize alarm
					if(highAlarmActivated == false) {
						lowAlarmActivated = false;
						highAlarmActivated = true;
						ledsActivated = true;
						
						// Leds on drone
						box.myBlinker.disable();
				
						if(inAlarmMode == false) {
							displayConcentrationHandler.post(LEDRunnable);
							inAlarmMode = true;
						}
					}
				}
				else {
					// Disable alarm if concentration goes below low alarm level
					if(inAlarmMode == true) {
						ledsActivated = false;
						lowAlarmActivated = false;
						highAlarmActivated = false;
					
						inAlarmMode = false;
						ledHandler.removeCallbacksAndMessages(null);
					
						// Make sure all LEDs off
						ledTopLeft_on.setVisibility(View.GONE);
						ledTopRight_on.setVisibility(View.GONE);
						ledBottomLeft_on.setVisibility(View.GONE);
						ledBottomRight_on.setVisibility(View.GONE);
						
						// Leds on drone
						box.myBlinker.enable();
						box.myBlinker.run();
					}
				}
				
				// Check for new max
				if(concentration > max) {
					max = concentration;
				}

				if(showMax == true) {
					setDisplayValue(max);
				}
				else {
					// Reset max
					max = 0;
					
					// Show current concentration
					setDisplayValue(concentration);
				}
					
				displayConcentrationHandler.postDelayed(this, 1000);
			}
		}
	};
	
	// NOTE: THIS WILL BE IMPLEMENTED IN SEPARATE FILE IN FUTURE UPDATE
//	public Runnable calculateAverageRunnable = new Runnable() {
//
//		@Override
//		public void run() {
//			
//			if(inNormalMode && poweredOn && !inCountdownMode) {
//				
//				values[numMeasurements] = concentration;
//				numMeasurements++;
//				
//				if(numMeasurements == MAX_MEASUREMENTS) {
//					numMeasurements = 0;
//				}
//				
//				sum = 0;
//				for(int i = 0; i < MAX_MEASUREMENTS; i++) {
//					sum += values[i];
//				}
//				
//				average = sum/MAX_MEASUREMENTS - offset;
//				
//				if((average >= LOW_ALARM_THRESHOLD) && (average < HIGH_ALARM_THRESHOLD)  ) {
//					if(lowAlarmActivated == false) {
//						lowAlarmActivated = true;
//						highAlarmActivated = false;
//						ledsActivated = true;
//						myHandler.post(LEDRunnable);
//					}
//				}
//				else if(average >= HIGH_ALARM_THRESHOLD) {
//					if(highAlarmActivated == false) {
//						lowAlarmActivated = false;
//						highAlarmActivated = true;
//						ledsActivated = true;
//						myHandler.post(LEDRunnable);
//					}
//				}
//				else {
//					ledsActivated = false;
//					lowAlarmActivated = false;
//					highAlarmActivated = false;
//				}
//				
//				if(average < 0) {
//					average = 0;
//				}
//			}
//				
//			myHandler.postDelayed(this, 125);
//		}
//	};
	
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
			myBlinker = new ConnectionBlinker(myDrone, 1000, 0, 255, 0);
			
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
					myHelper.flashLEDs(myDrone, 3, 100, 0, 0, 22);
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
					if(inNormalMode && poweredOn) {
						concentration = (int)myDrone.precisionGas_ppmCarbonMonoxide - offset;
						
						if(concentration < 0) {
							concentration = 0;
						}
					}
					
					streamer.streamHandler.postDelayed(streamer, 100);
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