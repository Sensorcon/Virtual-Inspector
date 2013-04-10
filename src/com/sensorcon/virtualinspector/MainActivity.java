package com.sensorcon.virtualinspector;

import java.util.EventObject;
import java.util.Timer;
import java.util.TimerTask;

import com.sensorcon.sdhelper.ConnectionBlinker;
import com.sensorcon.sdhelper.OnOffRunnable;
import com.sensorcon.sdhelper.SDHelper;
import com.sensorcon.sdhelper.SDStreamer;
import com.sensorcon.sensordrone.Drone;
import com.sensorcon.sensordrone.Drone.DroneEventListener;
import com.sensorcon.sensordrone.Drone.DroneStatusListener;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	/*
	 * Constants
	 */
	private final String TAG = "chris";
	/*
	 * Runs the sensordrone functions
	 */
	protected Drone myDrone;
	public Storage box;
	/*
	 * Contains functions to simplify connectivity
	 */
	public SDHelper myHelper;
	/*
	 * LED sequence variables
	 */
	private int cycles;
	public int totalCycles;
	private int count;
	private int btCount;
	private boolean startUpLEDSequence;
	private boolean slowAlarmSequence;
	private boolean fastAlarmSequence;
	/*
	 * Handlers
	 */
	private Handler myHandler = new Handler();
	/*
	 * Typeface variables
	 */
	Typeface lcdFont;
	/*
	 * Program flow flags
	 */
	boolean leftPressed;
	boolean rightPressed;
	boolean inNormalMode;
	boolean inCountdownMode;
	boolean inBaselineMode;
	boolean btHoldActivated;
	boolean lowAlarmActivated;
	boolean highAlarmActivated;
	boolean leftArrowOn;
	boolean rightArrowOn;
	boolean poweredOn;
	int countdown;
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
		
		// Set LED font
		lcdFont = Typeface.createFromAsset(this.getAssets(), "DS-DIGI.TTF");	
		ppmValue.setTypeface(lcdFont);		
		countdownValue.setTypeface(lcdFont);	
		labelCal.setTypeface(lcdFont);

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
		
		leftPressed = false;
		rightPressed = false;
		inNormalMode = false;
		inCountdownMode = false;
		inBaselineMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
		poweredOn = false;

		countdown = 4;
		btCount = 0;
		
		leftButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					leftPressed = true;
					leftButton.setVisibility(View.GONE);
					leftButtonPressed.setVisibility(View.VISIBLE);
					
					// Only if other modes are enabled
					if(poweredOn) {
						// Check for both buttons pressed
						if(rightPressed == true) {
							countdownMode();
						}
						else {
							setMute();
						}
						
						bluetoothHoldMode();
					}
					else {
						bluetoothHoldMode();
					}
					Log.d(TAG, "Left button pressed\n");
				}
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					leftPressed = false;
					leftButton.setVisibility(View.VISIBLE);
					leftButtonPressed.setVisibility(View.GONE);
					btCount = 0;
					btHoldActivated = false;
					
					// Only if other modes are enabled
					if(poweredOn) {
						if(inCountdownMode == true) {
							countdown = 4;
							
							if(inBaselineMode) {
								baselineMode();
							}
							else {
								normalMode();
							}
						}
					}
					Log.d(TAG, "Left button not pressed\n");
				}
				return true;
			}
		});
		
		rightButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
					rightPressed = true;
					rightButton.setVisibility(View.GONE);
					rightButtonPressed.setVisibility(View.VISIBLE);
					
					// Only if other modes are enabled
					if(poweredOn) {
						// Check for both buttons pressed
						if(leftPressed == true) {
							countdownMode();
						}
						else {
							setMax();
						}
					}
					Log.d(TAG, "Right button pressed\n");
				}
				else if(event.getAction() == android.view.MotionEvent.ACTION_UP) {
					rightPressed = false;
					rightButton.setVisibility(View.VISIBLE);
					rightButtonPressed.setVisibility(View.GONE);
					
					// Only if other modes are enabled
					if(poweredOn) {
						if(inCountdownMode == true) {
							countdown = 4;
							
							if(inBaselineMode) {
								baselineMode();
							}
							else {
								normalMode();
							}
						}
					}
					
					Log.d(TAG, "Right button not pressed\n");
				}
				return true;
			}
		});

		cycles = 0;
		count = 0;
		totalCycles = 6;
		startUpLEDSequence = true;
		myHandler.post(LEDRunnable);
		
		myDrone = new Drone();
		box = new Storage(this);
		myHelper = new SDHelper();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/*
	 * 
	 * 
	 * Bluetooth functions
	 * 
	 * 
	 */
	public void scan() {
		myHelper.scanToConnect(myDrone, MainActivity.this , this, false);
	}
	
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
	
	
	public void setMute() {
		if(leftArrowOn == false) {
			arrowLeft.setVisibility(View.VISIBLE);
			leftArrowOn = true;
		}
		else {
			arrowLeft.setVisibility(View.GONE);
			leftArrowOn = false;
		}
	}
	
	public void setMax() {
		if(rightArrowOn == false) {
			arrowRight.setVisibility(View.VISIBLE);
			rightArrowOn = true;
		}
		else {
			arrowRight.setVisibility(View.GONE);
			rightArrowOn = false;
		}
	}
	
	/*
	 * 
	 * 
	 * These are the functions that define the different modes in the state machine
	 * 
	 * 
	 */
	
	/**
	 * Starts the LED sequence at start up
	 */
	private void startUpLEDSequence() {
		startUpLEDSequence = true;
		myHandler.post(LEDRunnable);
	}
	
	public void normalMode() {
		if(poweredOn) {
			initNormalMode();
		}
		else {
			// Display connection message
			poweredOn = false;
			quickMessage("Not connected. Hold left button to scan for Sensordrone.");
		}
	}
	
	public void countdownMode() {		
		initCountdownMode();
		myHandler.post(countdownRunnable);
	}
	
	public void baselineMode() {
		initBaselineMode();
		myHandler.post(arrowRunnable);
	}
	
	public void bluetoothHoldMode() {	
		btHoldActivated = true;
		myHandler.post(btCountRunnable);
	}
	
	public void lowAlarmMode() {
		
	}
	
	public void highAlarmMode() {
		
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
	
	public void quickMessage(final String msg) {
		this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
			}
		});

	}
	
	private void initNormalMode() {
		ppmValue.setVisibility(View.VISIBLE);
		countdownValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.VISIBLE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		inNormalMode = true;
		inCountdownMode = false;
		inBaselineMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
	}
	
	private void initCountdownMode() {
		ppmValue.setVisibility(View.GONE);
		countdownValue.setVisibility(View.VISIBLE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.VISIBLE);
		labelCal.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		//inNormalMode = false;
		inCountdownMode = true;
		//inBaselineMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
	}
	
	private void initBaselineMode() {
		ppmValue.setVisibility(View.GONE);
		countdownValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.VISIBLE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.VISIBLE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		inNormalMode = false;
		inCountdownMode = false;
		inBaselineMode = true;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
	}
	
	private void clearScreen() {
		ppmValue.setVisibility(View.GONE);
		countdownValue.setVisibility(View.GONE);
		labelPPM.setVisibility(View.GONE);
		labelZero.setVisibility(View.GONE);
		labelHold.setVisibility(View.GONE);
		labelCal.setVisibility(View.GONE);
		arrowLeft.setVisibility(View.GONE);
		arrowRight.setVisibility(View.GONE);
		leftPressed = false;
		rightPressed = false;
		inNormalMode = true;
		inCountdownMode = false;
		inBaselineMode = false;
		btHoldActivated = false;
		lowAlarmActivated = false;
		highAlarmActivated = false;
	}
	/**
	 * Controls the timing thread for the LEDs
	 */
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
					logoSensorconGray.setVisibility(View.GONE);
					labelInspectorGray.setVisibility(View.GONE);
					startUpLEDSequence = false;
					normalMode();
					break;
				default:
					break;
				}
			}
			
			if(cycles < totalCycles) {
				myHandler.postDelayed(this, 125);
			}
			else {
				disableLED(0);
				disableLED(1);
				disableLED(2);
				disableLED(3);
				cycles = 0;
				count = 0;
			}
		}
	};

	public Runnable countdownRunnable = new Runnable() {

		@Override
		public void run() {
			
			if(inCountdownMode) {
				countdown--;
				
				Log.d(TAG, Integer.toString(countdown));
				
				if(countdown == 0) {
					countdown = 4;
					if(inBaselineMode) {
						initNormalMode();
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
	
	public Runnable btCountRunnable = new Runnable() {

		@Override
		public void run() {
			Log.d(TAG, "In bt handler");
			if(btHoldActivated) {
				btCount++;
				
				Log.d(TAG, Integer.toString(btCount));
				
				if(btCount == 2) {
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
		}
	};
	
	public final class Storage {
		
		public ConnectionBlinker myBlinker;
		public int sensor;
		
		public DroneEventListener droneEventListener;
		public DroneStatusListener droneStatusListener;
		public String MAC = "";
		
		// GUI variables
		public TextView statusView;
		public TextView tvConnectionStatus;
		public TextView tvConnectInfo;
		
		public SDStreamer streamer;
		
		public Storage(Context context) {
			
			sensor = myDrone.QS_TYPE_PRECISION_GAS;
			myBlinker = new ConnectionBlinker(myDrone, 1000, 0, 0, 255);
			
			streamer = new SDStreamer(myDrone, sensor);
			
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
				public void adcMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void altitudeMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void capacitanceMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void connectionLostEvent(EventObject arg0) {
					// Turn off the blinker
					myBlinker.disable();
				}

				@Override
				public void customEvent(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void disconnectEvent(EventObject arg0) {
					
					poweredOn = false;
					clearScreen();
					normalMode();
					
				}

				@Override
				public void humidityMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void i2cRead(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void irTemperatureMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void oxidizingGasMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void precisionGasMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void pressureMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void reducingGasMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void rgbcMeasured(EventObject arg0) {
			
				}

				@Override
				public void temperatureMeasured(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void uartRead(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void unknown(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void usbUartRead(EventObject arg0) {
					// TODO Auto-generated method stub
					
				}
			};
			
			droneStatusListener = new DroneStatusListener() {

				@Override
				public void adcStatus(EventObject arg0) {
					
				}

				@Override
				public void altitudeStatus(EventObject arg0) {
				

				}

				@Override
				public void batteryVoltageStatus(EventObject arg0) {
					
				}

				@Override
				public void capacitanceStatus(EventObject arg0) {
					
				}

				@Override
				public void chargingStatus(EventObject arg0) {


				}

				@Override
				public void customStatus(EventObject arg0) {


				}

				@Override
				public void humidityStatus(EventObject arg0) {
					

				}

				@Override
				public void irStatus(EventObject arg0) {
					
				}

				@Override
				public void lowBatteryStatus(EventObject arg0) {
					
				}

				@Override
				public void oxidizingGasStatus(EventObject arg0) {


				}

				@Override
				public void precisionGasStatus(EventObject arg0) {
					

				}

				@Override
				public void pressureStatus(EventObject arg0) {
					
				}

				@Override
				public void reducingGasStatus(EventObject arg0) {


				}

				@Override
				public void rgbcStatus(EventObject arg0) {

				}

				@Override
				public void temperatureStatus(EventObject arg0) {
					
				}

				@Override
				public void unknownStatus(EventObject arg0) {


				}
			};
			
			myDrone.registerDroneEventListener(droneEventListener);
			myDrone.registerDroneStatusListener(droneStatusListener);
		}
	}
}