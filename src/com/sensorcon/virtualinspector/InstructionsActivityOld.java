package com.sensorcon.virtualinspector;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * For phones that do not support swipe views, there will be a simple
 * button layout to switch between instruction pages.
 * 
 * @author Sensorcon, Inc.
 */
public class InstructionsActivityOld extends Activity {
	
	private ImageButton button1;
	private ImageButton button2;
	private ImageView screen;
	private TextView text;
	private TextView inst;
	private int count;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_instructions_old);
		
		// Instantiate views
		button1 = (ImageButton)findViewById(R.id.button1);
		button2 = (ImageButton)findViewById(R.id.button2);
		screen = (ImageView)findViewById(R.id.imageView1);
		text = (TextView)findViewById(R.id.textView1);
		inst = (TextView)findViewById(R.id.labelInst);
		
		count = 0;
		
		button1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				leftClick();
			}
		});
		
		button2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				rightClick();
			}
		});
	}

	/**
	 * When left navigation button is clicked
	 */
	public void leftClick() {
		count--;
		if(count < 0) {
			count = 4;
		}
		
		// Switch based on count
		switch(count) {
		case 0:
			view1();
			break;
		case 1:
			view2();
			break;
		case 2:
			view3();
			break;
		case 3:
			view4();
			break;
		case 4:
			view5();
			break;
		default:
			view1();
		}
	}
	
	/**
	 * When right navigation button is clicked
	 */
	public void rightClick() {
		count++;
		if(count > 4) {
			count = 0;
		}
		
		// Switch based on count
		switch(count) {
		case 0:
			view1();
			break;
		case 1:
			view2();
			break;
		case 2:
			view3();
			break;
		case 3:
			view4();
			break;
		case 4:
			view5();
			break;
		default:
			view1();
		}
	}
	
	/**
	 * First page
	 */
	public void view1() {
		screen.setVisibility(View.VISIBLE);
		screen.setImageDrawable(getResources().getDrawable(R.drawable.inst_left_button));
		text.setText(R.string.tab1_string);
		inst.setText("Connect");
	}
	
	/**
	 * Second page
	 */
	public void view2() {
		screen.setVisibility(View.VISIBLE);
		screen.setImageDrawable(getResources().getDrawable(R.drawable.inst_left_button));
		text.setText(R.string.tab2_string);
		inst.setText("Mute");
	}
	
	/**
	 * Third page
	 */
	public void view3() {
		screen.setVisibility(View.VISIBLE);
		screen.setImageDrawable(getResources().getDrawable(R.drawable.inst_right_button));
		text.setText(R.string.tab3_string);
		inst.setText("Max");
	}
	
	/**
	 * Fourth page
	 */
	public void view4() {
		screen.setVisibility(View.GONE);
		text.setText(R.string.cal_old);
		inst.setText("Calibrate");
	}
	
	/**
	 * Fifth page
	 */
	public void view5() {
		screen.setVisibility(View.VISIBLE);
		screen.setImageDrawable(getResources().getDrawable(R.drawable.inst_left_button));
		text.setText(R.string.tab5_string);
		inst.setText("Disconnect");
	}
}
