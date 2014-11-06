package com.example.u_mote;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TrackpadFragment extends Fragment implements OnTouchListener,
		OnCheckedChangeListener {

	public interface TrackPadFragmentCallback {
		void sendCommand(String name);
	}

	// private static final String DEBUG_TAG = "Gestures";
	private GestureLibrary gestureLibrary = null;
	private File mStoreFile;
	private GestureDetectorCompat mDetector;
	boolean isLongPressed = false;
	boolean isTwoFingerTapped = false;
	boolean isFourTapped = false;
	boolean isMinimized = false;
	boolean isMaximized = true;
	float old_distance = 0f;
	float new_distance = 0f;
	float[][] oldpos = new float[4][2];
	float[][] newpos = new float[4][2];

	/**
	 * The fragment argument representing the section number for this fragment.
	 */
	private static final String ARG_SECTION_NUMBER = "section_number";
	private GestureOverlayView mPadView;
	private ToggleButton mCustomGestureButton;

	TrackPadFragmentCallback mCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Handle orientation changes.
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_trackpad, container,
				false);
		mPadView = (GestureOverlayView) ((RelativeLayout) rootView)
				.getChildAt(0);
		mPadView.setOnTouchListener(this);

		mCustomGestureButton = (ToggleButton) ((RelativeLayout) rootView)
				.getChildAt(1);
		mCustomGestureButton.setOnCheckedChangeListener(this);

		mStoreFile = new File(getActivity().getApplicationContext()
				.getFilesDir(), "gestures");
		gestureLibrary = GestureLibraries.fromFile(mStoreFile);
		gestureLibrary.load();

		return rootView;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(getArguments().getInt(
				ARG_SECTION_NUMBER));
		mDetector = new GestureDetectorCompat(activity, new MyGestureListener());
		mCallback = (TrackPadFragmentCallback) activity;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		int action = MotionEventCompat.getActionMasked(event);

		if (action == MotionEvent.ACTION_UP) {
			isMinimized = false;
			isTwoFingerTapped = false;
			isFourTapped = false;
			if (isLongPressed) {
				isLongPressed = false;
				SocketSetup.print("mouse mouseup 1");
			}
		}
		if (event.getPointerCount() == 1) {

			this.mDetector.onTouchEvent(event);
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				oldpos[0][0] = event.getX();
				oldpos[0][1] = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				newpos[0][0] = event.getX();
				newpos[0][1] = event.getY();
				if (/* !isLongPressed && */Math
						.abs(newpos[0][0] - oldpos[0][0])
						+ Math.abs(newpos[0][1] - oldpos[0][1]) > 4)
					// Finger sliding by relative position
					// (newpos[0][0]-oldpos[0][0]) in X and
					// (newpos[0][1]-oldpos[0][1]) in Y
					// text2.setText("Movement by "+(newpos[0][0]-oldpos[0][0])+","+(newpos[0][1]-oldpos[0][1]));
					SocketSetup.print("mouse move "
							+ (newpos[0][0] - oldpos[0][0]) + " "
							+ (newpos[0][1] - oldpos[0][1]));
				oldpos[0][0] = newpos[0][0];
				oldpos[0][1] = newpos[0][1];
			case MotionEvent.ACTION_UP: 
				break;
			default:
				return false;
			}
		} else if (event.getPointerCount() == 2) {
			switch (action) {
			case MotionEvent.ACTION_POINTER_DOWN:

				isTwoFingerTapped = true;
				// Right click
				SocketSetup.print("mouse click 3");
				break;
			case MotionEvent.ACTION_POINTER_UP:
				break;
			case MotionEvent.ACTION_UP:
				break;
			default:
				return false;
			}
		} else if (event.getPointerCount() == 4) {

			switch (action) {

			case MotionEvent.ACTION_POINTER_DOWN:
				isFourTapped = true;
				old_distance = 0;
				for (int i = 0; i < 4; i++) {
					oldpos[i][0] = event.getX(i);
					oldpos[i][1] = event.getY(i);
				}
				for (int i = 0; i < 3; i++) {
					old_distance += Math.sqrt((oldpos[i + 1][0] - oldpos[i][0])
							* (oldpos[i + 1][0] - oldpos[i][0]))
							+ (oldpos[i + 1][1] - oldpos[i][1])
							* (oldpos[i + 1][1] - oldpos[i][1]);
				}
				// text2.setText("Four finger distance is " + old_distance);
				break;

			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < 4; i++) {
					newpos[i][0] = event.getX(i);
					newpos[i][1] = event.getY(i);
				}
				new_distance = 0;
				for (int i = 0; i < 3; i++) {
					new_distance += Math.sqrt((newpos[i + 1][0] - newpos[i][0])
							* (newpos[i + 1][0] - newpos[i][0]))
							+ (newpos[i + 1][1] - newpos[i][1])
							* (newpos[i + 1][1] - newpos[i][1]);
				}
				float dist_ratio = new_distance / old_distance;
				// text2.setText("Distance ratio is"+dist_ratio);
				if (dist_ratio < 0.6 && !isMinimized) {
					isMinimized = true;
					//text1.setText("Close window!");
					SocketSetup.print("key ctrl+super+d");
				}

			}
		}

		return true;
	}

	class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		// private static final String DEBUG_TAG = "Gestures";
		// TextView text = (TextView) findViewById(R.id.textView1);

		@Override
		public boolean onDown(MotionEvent event) {
			if (isLongPressed || isTwoFingerTapped)
				return false;
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			SocketSetup.print("mouse click 1");
			return true;
		}

		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,
				float velocityX, float velocityY) {
			// This is only imp if we want to know the x,y when sliding started
			// and when it ended
			// Need not map this to an action just yet
			// text.setText("onFling: " + event1.toString()+event2.toString());
			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			SocketSetup.print("mouse click --repeat 2 1");
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent event) {
			return true;
		}

		@Override
		public void onLongPress(MotionEvent event) {
			if (isTwoFingerTapped == false) {
				SocketSetup.print("mouse mousedown 1");
				isLongPressed = true;
			}
		}
	}

	void sendCommand(String name) {
		mCallback.sendCommand(name);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		if (isChecked) {
			mPadView.setOnTouchListener(null);
			mPadView.addOnGesturePerformedListener(new OnGesturePerformedListener() {

				@Override
				public void onGesturePerformed(GestureOverlayView view,
						Gesture gesture) {
					ArrayList<Prediction> prediction = gestureLibrary
							.recognize(gesture);
					if (prediction.size() > 0) {
						Prediction p = prediction.get(0);
						sendCommand(p.name);
					}

				}
			});
		} else {
			mPadView.setOnTouchListener(this);
			mPadView.removeAllOnGesturePerformedListeners();
		}

	}
}
