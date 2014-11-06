package com.example.u_mote;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public class KeyboardFragment extends Fragment implements android.view.View.OnClickListener {
	
	private static final String ARG_SECTION_NUMBER = "section_number";
	private AutoCompleteTextView mInputTextView;
	private Button mSendButton;
	private Button mBackspaceButton;
	private Button mEnterButton;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_input, container, false);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mInputTextView = (AutoCompleteTextView) view.findViewById(R.id.input_text_view);
		mSendButton = (Button) view.findViewById(R.id.send_button);
		mSendButton.setOnClickListener(this);
		mBackspaceButton = (Button) view.findViewById(R.id.backspace_button);
		mBackspaceButton.setOnClickListener(this);
		mEnterButton = (Button) view.findViewById(R.id.enter_button);
		mEnterButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.send_button:
			String text;
			text = mInputTextView.getText().toString();
			mInputTextView.setText("");
			type(text);
			break;
		case R.id.backspace_button:
			SocketSetup.print("key BackSpace");
			break;
		case R.id.enter_button:
			SocketSetup.print("key Return");
			break;
		default:
			break;
		}
		
	}

	private void type(String text) {
		int l = text.length();
		int num_iter = l / 20;
		int num_extra = l % 20;
		
		for(int i = 0 ; i < num_iter ; i++) {
			SocketSetup.print("type \"" + text.substring(i*20, i*20 + 20) + "\"");
		}
		
		if(num_extra > 0) {
			SocketSetup.print("type \"" + text.substring(num_iter*20, l) + "\"");
		}
	}
	
	
	
}