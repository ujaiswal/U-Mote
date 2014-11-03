package com.example.u_mote;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.u_mote.AddGestureShortcutFragment.AddShortcutFragmentCallbacks;
import com.example.u_mote.TrackpadFragment.TrackPadFragmentCallback;

public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, AddShortcutFragmentCallbacks, TrackPadFragmentCallback {

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	
	private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_CANCELLED = 1;
    private static final int STATUS_NO_STORAGE = 2;
    private static final int STATUS_NOT_LOADED = 3;

    private static final int DIALOG_RENAME_GESTURE = 1;
    private static final int DIALOG_EDIT_MAP_GESTURE = 2;
    private static final int DIALOG_SEND_COMMAND = 3;

    private static final int REQUEST_NEW_GESTURE = 1;
    
    // Type: long (id)
    private static final String GESTURES_INFO_ID = "gestures.info_id";
    public static final String MAPPING_FILE = "command_map";

	private static final String CURRENT_ACIVE_COMMAND = "gestures.command";

    private File mStoreFile;

    private final Comparator<NamedGesture> mSorter = new Comparator<NamedGesture>() {
        public int compare(NamedGesture object1, NamedGesture object2) {
            return object1.name.compareTo(object2.name);
        }
    };
    /**
     * Map from gesture Id to Command
     */
    HashMap<String, String> mCommandMap;
    public SharedPreferences mCommandStorage;
    SharedPreferences.Editor mCommandStorageEditor;

    private static GestureLibrary sStore;

    private GesturesAdapter mAdapter;
    private GesturesLoadTask mTask;

    private Dialog mActiveDialog;
    private EditText mInput;
    private TextView mLabel;
    private NamedGesture mCurrentActiveGesture;
    private String mCurrentCommand;
    
    public interface MainActivityCallbacks {

		void disableButtons();

		void enableButtons();

		void hideListView();

		void setEmpty();

		void setErrorLoading();
    	
    }
    
    private MainActivityCallbacks mCallbacks;
    
    @Override
	public void setFragmentCallback(MainActivityCallbacks callbacks) {
		mCallbacks = callbacks;
	}
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));
		
		mStoreFile = new File(getApplicationContext().getFilesDir(), "gestures");

		mAdapter = new GesturesAdapter(this);

        if (sStore == null) {
            sStore = GestureLibraries.fromFile(mStoreFile);
        }
        
        mCommandMap = new HashMap<String, String>();
		
		// Socket hello
		SocketSetup.print("hello");
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
    protected void onPause() {
    	super.onPause();
    	if (mCommandStorageEditor != null) {
			mCommandStorageEditor.commit();
		}
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	if (mCommandStorageEditor != null) {
			mCommandStorageEditor.commit();
		}
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
            mTask = null;
        }

        cleanupDialog();
    }
	
	@Override
	public void finish() {
		super.finish();

		SocketSetup.print("bye");
		SocketSetup.close();
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";
		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static Fragment newInstance(int sectionNumber) {
			Fragment fragment;
			Bundle args;
			switch(sectionNumber) {
			case 1:
				fragment = new TrackpadFragment();
				args = new Bundle();
				args.putInt(ARG_SECTION_NUMBER, sectionNumber);
				fragment.setArguments(args);
				return fragment;
			case 3:
				fragment = new AddGestureShortcutFragment();
				args = new Bundle();
				args.putInt(ARG_SECTION_NUMBER, sectionNumber);
				fragment.setArguments(args);
				return fragment;
			default:
				fragment = new PlaceholderFragment();
				args = new Bundle();
				args.putInt(ARG_SECTION_NUMBER, sectionNumber);
				fragment.setArguments(args);
				return fragment;
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.blank, container,
					false);
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
	}
	
	static GestureLibrary getStore() {
        return sStore;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void reloadGestures(View v) {
    	if (mCommandStorageEditor != null) {
			mCommandStorageEditor.commit();
		}
		loadGestures();
    }
    
    @SuppressWarnings({"UnusedDeclaration"})
    public void addGesture(View v) {
        Intent intent = new Intent(this, CreateGestureActivity.class);
        startActivityForResult(intent, REQUEST_NEW_GESTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_NEW_GESTURE:
                    loadGestures();
                    break;
            }
        }
    }

    public void loadGestures() {
        if (mTask != null && mTask.getStatus() != GesturesLoadTask.Status.FINISHED) {
            mTask.cancel(true);
        }        
        mTask = (GesturesLoadTask) new GesturesLoadTask().execute();
    }

    private void checkForEmpty() {
        if (mAdapter.getCount() == 0 && mCallbacks != null) {
            mCallbacks.setEmpty();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentActiveGesture != null) {
            outState.putLong(GESTURES_INFO_ID, mCurrentActiveGesture.gesture.getID());
        }
        
        if(mCurrentCommand != null) {
        	outState.putString(CURRENT_ACIVE_COMMAND, mCurrentCommand);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        long id = state.getLong(GESTURES_INFO_ID, -1);
        if (id != -1) {
            final Set<String> entries = sStore.getGestureEntries();
out:        for (String name : entries) {
                for (Gesture gesture : sStore.getGestures(name)) {
                    if (gesture.getID() == id) {
                        mCurrentActiveGesture = new NamedGesture();
                        mCurrentActiveGesture.name = name;
                        mCurrentActiveGesture.gesture = gesture;
                        break out;
                    }
                }
            }
        }
        
        String cmd = state.getString(CURRENT_ACIVE_COMMAND, null);
        if(cmd != null) {
        	mCurrentCommand = new String(cmd);
        }
    }
    
    public void renameGesture(NamedGesture gesture) {
        mCurrentActiveGesture = gesture;
        showDialog(DIALOG_RENAME_GESTURE);
    }

    @Override
	public void editGestureMapping(NamedGesture gesture) {
		mCurrentActiveGesture = gesture;
	    showDialog(DIALOG_EDIT_MAP_GESTURE);		
	}

	public void deleteGesture(NamedGesture gesture) {
	    sStore.removeGesture(gesture.name, gesture.gesture);
	    sStore.save();
	
	    final GesturesAdapter adapter = mAdapter;
	    adapter.setNotifyOnChange(false);
	    adapter.remove(gesture);
	    adapter.sort(mSorter);
	    mCommandMap.remove(gesture.gesture.getID());
	    mCommandStorageEditor.remove(gesture.name);
	    checkForEmpty();
	    adapter.notifyDataSetChanged();
	
	    Toast.makeText(this, R.string.gestures_delete_success, Toast.LENGTH_SHORT).show();
	}

	@Override
    protected Dialog onCreateDialog(int id) {
		switch(id) {
			case DIALOG_RENAME_GESTURE:
		            return createRenameDialog();
			case DIALOG_EDIT_MAP_GESTURE:
	        	return createEditMapDialog();
			case DIALOG_SEND_COMMAND:
				return createSendCommandDialog();
		}
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);
        mActiveDialog = dialog;
        if (id == DIALOG_RENAME_GESTURE) {
            mInput.setText(mCurrentActiveGesture.name);
        }
        else if(id == DIALOG_EDIT_MAP_GESTURE) {
        	mInput.setText(mCommandMap.get(mCurrentActiveGesture.name));
        }
        else if(id == DIALOG_SEND_COMMAND) {
            mLabel.setText(mCurrentCommand);
        }
    }
    
    private Dialog createSendCommandDialog() {
        final View layout = View.inflate(this, R.layout.dialog_detect, null);
        mLabel = (TextView) layout.findViewById(R.id.label_detected_gesture);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_detect_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                cleanupDialog();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cleanupDialog();
                }
            }
        );
        builder.setPositiveButton(getString(R.string.send_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	SocketSetup.print("key " + mCommandMap.get(mCurrentCommand));
                	((ToggleButton) findViewById(R.id.customGestureToggleButton)).toggle();
                	mCurrentCommand = null;
                }
            }
        );
        builder.setView(layout);
        return builder.create();
    }

    private Dialog createRenameDialog() {
        final View layout = View.inflate(this, R.layout.dialog_rename, null);
        mInput = (EditText) layout.findViewById(R.id.name);
        ((TextView) layout.findViewById(R.id.label)).setText(R.string.gestures_rename_label);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_rename_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                cleanupDialog();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cleanupDialog();
                }
            }
        );
        builder.setPositiveButton(getString(R.string.rename_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    changeGestureName();
                }
            }
        );
        builder.setView(layout);
        return builder.create();
    }
    
    private void changeGestureName() {
	    final String name = mInput.getText().toString();
	    if (!TextUtils.isEmpty(name)) {
	        final NamedGesture renameGesture = mCurrentActiveGesture;
	        final GesturesAdapter adapter = mAdapter;
	        final int count = adapter.getCount();
	
	        // Simple linear search, there should not be enough items to warrant
	        // a more sophisticated search
	        for (int i = 0; i < count; i++) {
	            final NamedGesture gesture = adapter.getItem(i);
	            if (gesture.gesture.getID() == renameGesture.gesture.getID()) {
	                sStore.removeGesture(gesture.name, gesture.gesture);
	                gesture.name = mInput.getText().toString();
	                sStore.addGesture(gesture.name, gesture.gesture);
	                break;
	            }
	        }
	
	        adapter.notifyDataSetChanged();
	    }
	    mCurrentActiveGesture = null;
	}

	private void cleanupDialog() {
	    if (mActiveDialog != null) {
	        mActiveDialog.dismiss();
	        mActiveDialog = null;
	    }
	    mCurrentActiveGesture = null;
	    mCurrentCommand = null;
	}

	private Dialog createEditMapDialog() {
    	final View layout = View.inflate(this, R.layout.dialog_rename, null);
        mInput = (EditText) layout.findViewById(R.id.name);
        ((TextView) layout.findViewById(R.id.label)).setText(R.string.gestures_edit_map_label);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(0);
        builder.setTitle(getString(R.string.gestures_edit_map_title));
        builder.setCancelable(true);
        builder.setOnCancelListener(new Dialog.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            	cleanupDialog();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    cleanupDialog();
                }
            }
        );
        builder.setPositiveButton(getString(R.string.edit_action),
            new Dialog.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	changeGestureCommandMap();
                }
            }
        );
        builder.setView(layout);
        return builder.create();
    }
    
    private void changeGestureCommandMap() {
		final String name = mInput.getText().toString();
	    if (!TextUtils.isEmpty(name)) {
	        final NamedGesture activeGesture = mCurrentActiveGesture;
	        mCommandMap.remove(activeGesture.gesture.getID());
	        mCommandMap.put(activeGesture.name, name);
	        mCommandStorageEditor.putString(activeGesture.name, name);
	    }
	    mCurrentActiveGesture = null;
	}

	public ListAdapter getAdapter() {
		return mAdapter;
	}

    @Override
	public void sendCommand(String name) {
		int count = mAdapter.getCount();
		// Linear Search
		for (int i = 0; i < count; i++) {
			NamedGesture gesture = mAdapter.getItem(i);
			if(gesture.name.equals(name)) {
				mCurrentCommand = name;
				showDialog(DIALOG_SEND_COMMAND);
				break;
			}
		}
	}

	private class GesturesLoadTask extends AsyncTask<Void, NamedGesture, Integer> {
        private int mThumbnailSize;
        private int mThumbnailInset;
        private int mPathColor;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final Resources resources = getResources();
            mPathColor = resources.getColor(R.color.gesture_color);
            mThumbnailInset = (int) resources.getDimension(R.dimen.gesture_thumbnail_inset);
            mThumbnailSize = (int) resources.getDimension(R.dimen.gesture_thumbnail_size);
            
            if(mCallbacks != null) {
            	mCallbacks.disableButtons();
            }
            mAdapter.setNotifyOnChange(false);            
            mAdapter.clear();
            
            mCommandMap.clear();
            mCommandStorage = getSharedPreferences(MAPPING_FILE, 0);
            mCommandStorageEditor = mCommandStorage.edit();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (isCancelled()) return STATUS_CANCELLED;

            final GestureLibrary store = sStore;

            if (store.load()) {
                for (String name : store.getGestureEntries()) {
                    if (isCancelled()) break;

                    for (Gesture gesture : store.getGestures(name)) {
                        final Bitmap bitmap = gesture.toBitmap(mThumbnailSize, mThumbnailSize,
                                mThumbnailInset, mPathColor);
                        final NamedGesture namedGesture = new NamedGesture();
                        namedGesture.gesture = gesture;
                        namedGesture.name = name;

                        mAdapter.addBitmap(namedGesture.gesture.getID(), bitmap);
                        mCommandMap.put(name, mCommandStorage.getString(name, null));
                        publishProgress(namedGesture);
                        
                    }
                }

                return STATUS_SUCCESS;
            }

            return STATUS_NOT_LOADED;
        }

        @Override
        protected void onProgressUpdate(NamedGesture... values) {
            super.onProgressUpdate(values);

            final GesturesAdapter adapter = mAdapter;
            adapter.setNotifyOnChange(false);

            for (NamedGesture gesture : values) {
                adapter.add(gesture);
            }

            adapter.sort(mSorter);
            adapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            if (result == STATUS_NO_STORAGE) {
            	if(mCallbacks != null) {
	            	mCallbacks.hideListView();
	            	mCallbacks.setErrorLoading();
            	}
            } else {
            	if(mCallbacks != null) {
            		mCallbacks.enableButtons();
            	}
                checkForEmpty();
            }
        }
    }
    
    static class NamedGesture {
        String name;
        Gesture gesture;
    }

    private class GesturesAdapter extends ArrayAdapter<NamedGesture> {
        private final LayoutInflater mInflater;
        private final Map<Long, Drawable> mThumbnails = Collections.synchronizedMap(
                new HashMap<Long, Drawable>());

        public GesturesAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        void addBitmap(Long id, Bitmap bitmap) {
            mThumbnails.put(id, new BitmapDrawable(bitmap));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.gestures_item, parent, false);
            }

            final NamedGesture gesture = getItem(position);
            final TextView label = (TextView) convertView;

            label.setTag(gesture);
            label.setText(gesture.name);
            label.setCompoundDrawablesWithIntrinsicBounds(mThumbnails.get(gesture.gesture.getID()),
                    null, null, null);

            return convertView;
        }
    }
}
