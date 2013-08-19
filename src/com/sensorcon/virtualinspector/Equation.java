package com.sensorcon.virtualinspector;

/**
 * NOTE: THIS CLASS WILL DO AVERAGING AND FILTERING. WILL BE COMPLETE IN NEXT UPDATE.
 *
 */
public class Equation {


    // I assume this is where chris intended to place this?

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
}
