package com.example.u_mote;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SocketSetup extends Activity implements OnClickListener{
	
	private Button mConnectButton;
	private EditText mIPAddressEditText;
	private EditText mPortEditText;
	private ProgressDialog mProgressDialog;
	
	private static Thread mClientTread;
	private static Handler mConnectionHandler;
	private static Handler getConnectionHandler() {
		return mConnectionHandler;
	}

	// Network requirements
	private static String mIP;
	private static String getIP() {
		return mIP;
	}

	private static int mPort;
	private static int getPort() {
		return mPort;
	}

	private static Socket mSocket;
	private static PrintWriter mOut;
	private static BufferedReader mIn;
	//private static long prev_invoke_time;
	
	public static void setConnection(Socket Socket) throws IOException {
		SocketSetup.mSocket = Socket;
		SocketSetup.mOut = new PrintWriter(SocketSetup.mSocket.getOutputStream(), true); 
		SocketSetup.mIn = new BufferedReader(new InputStreamReader(SocketSetup.mSocket.getInputStream()));
	}
	
	public static void close() {
		// Close output stream
		if(mOut != null)
			mOut.close();
		// Close input stream
		if(mIn != null) {
			try {
				mIn.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
		
		// Close Socket
		if(mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.init);
		
		mConnectButton = (Button) findViewById(R.id.button_connect);
		mConnectButton.setOnClickListener(this);
		
		mIPAddressEditText = (EditText) findViewById(R.id.editText_ip);
		mPortEditText = (EditText) findViewById(R.id.editText_port);
		
		mClientTread = null;
		mConnectionHandler = new ConnectionHandler(this);
		
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setMessage("Connecting ...");
		mProgressDialog.setCancelable(false);
		mProgressDialog.setInverseBackgroundForced(false);
		
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_connect:
			onConnectButtonClick();
			break;
		default:
			break;
		}
	}
	
	private void onConnectButtonClick() {
		// Create connection
		mIP = mIPAddressEditText.getText().toString();
		String port_string = mPortEditText.getText().toString();
		try {
			mPort = Integer.parseInt(port_string);

			mProgressDialog.show();
			// Establish connection
			establishConnection();
		} catch (NumberFormatException e) {
			Toast.makeText(this, "Can't Connect!! Invalid IP or Port", Toast.LENGTH_SHORT).show();
		}
	}
	
	public static void establishConnection() {
		final String IP = getIP();
		final int port = getPort();
		final Handler handler = getConnectionHandler();
		// Thread alive
		if(mClientTread != null && mClientTread.isAlive()) {
			Message msg = handler.obtainMessage();
			msg.what = 0;
			handler.sendMessage(msg);
			return;
		}
		// Start connection thread
		mClientTread = new Thread() {
			@Override
			public void run() {
				try {
					Message msg = new Message();
					msg.what = 1;
					Socket socket = new Socket(IP, port);
					msg.obj = socket;
					handler.sendMessage(msg);
				} catch (UnknownHostException e) {
					Message msg = new Message();
					msg.what = 2;
					handler.sendMessage(msg);
					msg.obj = e.getLocalizedMessage();
				} catch (IOException e) {
					Message msg = new Message();
					msg.what = 2;
					handler.sendMessage(msg);
					msg.obj = e.getLocalizedMessage();
				}
			}
		};
		mClientTread.start();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Destroy thread
		if(mClientTread != null) {
			mClientTread = null;
		}
		// Close all
		SocketSetup.close();
		// Destroy handler
		mConnectionHandler = null;
	}
	
	private static class ConnectionHandler extends Handler {
		private final WeakReference<Activity> mInitActivity;
		
		public ConnectionHandler(Activity activity) {
			super();
			mInitActivity = new WeakReference<Activity>(activity);
		}
		
		@Override
		public void handleMessage(Message msg) {
			SocketSetup initActivity = (SocketSetup) mInitActivity.get();
			
			if(initActivity != null) {
				initActivity.mProgressDialog.dismiss();
				switch (msg.what) {
					case 0:
						Toast.makeText(initActivity, "Previous connection still active!!", Toast.LENGTH_SHORT).show();
						break;
					case 1:
						try {
							SocketSetup.setConnection((Socket) msg.obj);
							// start new main activity
							Toast.makeText(initActivity, "Connection established!!", Toast.LENGTH_SHORT).show();
							Intent main_intent = new Intent(initActivity, MainActivity.class);
							initActivity.startActivity(main_intent);
						} catch (IOException e) {
							Toast.makeText(initActivity, "Can't Connect!! Invalid IP or Port", Toast.LENGTH_SHORT).show();
						}
						break;
					case 2:
						Toast.makeText(initActivity, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
						break;
					case 3:
						try {
							SocketSetup.setConnection((Socket) msg.obj);
							Toast.makeText(initActivity, "Connection established!!", Toast.LENGTH_SHORT).show();
						} catch (IOException e) {
							Toast.makeText(initActivity, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
						}
						break;
					default:
						Toast.makeText(initActivity, "WTF!!", Toast.LENGTH_SHORT).show();
						break;
				}
			}
		}
	}

	public static void print(String s) {
//		Calendar c = Calendar.getInstance();
//		long time = c.getTimeInMillis();
//		
//		if((time - prev_invoke_time) < 40 ) {
//			return;
//		}
//		
//		prev_invoke_time = time;
//		
		for (int i = s.length(); i < 39; i++) {
			s += " ";
		}
		mOut.println(s);	
	}

	public static void reconnect(Context context) {
		SocketSetup.print("bye");
		SocketSetup.close();
		
		final String IP = getIP();
		final int port = getPort();
		final Handler handler = getConnectionHandler();
		
		// Thread alive
		if(mClientTread != null && mClientTread.isAlive()) {
			Message msg = handler.obtainMessage();
			msg.what = 0;
			handler.sendMessage(msg);
			return;
		}
		// Start connection thread
		mClientTread = new Thread() {
			@Override
			public void run() {
				try {
					Message msg = new Message();
					msg.what = 3;
					Socket socket = new Socket(IP, port);
					msg.obj = socket;
					handler.sendMessage(msg);
				} catch (UnknownHostException e) {
					Message msg = new Message();
					msg.what = 2;
					handler.sendMessage(msg);
					Log.e("SocketConnectionError", e.getLocalizedMessage());
				} catch (IOException e) {
					Message msg = new Message();
					msg.what = 2;
					handler.sendMessage(msg);
					Log.e("SocketConnectionError", e.getLocalizedMessage());
				}
			}
		};
		mClientTread.start();
	}
}
