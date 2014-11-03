package com.example.u_mote;

import java.io.File;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.example.u_mote.MainActivity.MainActivityCallbacks;
import com.example.u_mote.MainActivity.NamedGesture;

public class AddGestureShortcutFragment extends ListFragment implements MainActivityCallbacks {
	
	public interface AddShortcutFragmentCallbacks {
		
		void setFragmentCallback(MainActivityCallbacks callbacks);

		void renameGesture(NamedGesture gesture);

		void deleteGesture(NamedGesture gesture);

		ListAdapter getAdapter();

		void loadGestures();

		void editGestureMapping(NamedGesture gesture);
		
	}
	
	private static final String ARG_SECTION_NUMBER = "section_number";
	
	private AddShortcutFragmentCallbacks mCallbacks;
	private TextView mEmpty;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		((MainActivity) activity).onSectionAttached(getArguments().getInt(ARG_SECTION_NUMBER));
		mCallbacks = (AddShortcutFragmentCallbacks) activity;
		mCallbacks.setFragmentCallback(this);
        setListAdapter(mCallbacks.getAdapter());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.gestures_list, container, false);
		return rootView;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		registerForContextMenu(getListView());
		mEmpty = (TextView) getActivity().findViewById(android.R.id.empty);
		mCallbacks.loadGestures();
	}
	
	private static final int MENU_ID_RENAME = 1;
	private static final int MENU_ID_EDIT = 2;
    private static final int MENU_ID_REMOVE = 3;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.setHeaderTitle(((TextView) info.targetView).getText());

        menu.add(0, MENU_ID_RENAME, 0, R.string.gestures_rename);
        menu.add(0, MENU_ID_EDIT, 0, R.string.gestures_map);
        menu.add(0, MENU_ID_REMOVE, 0, R.string.gestures_delete);
    }
    
	@Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo();
        final NamedGesture gesture = (NamedGesture) menuInfo.targetView.getTag();

        switch (item.getItemId()) {
            case MENU_ID_RENAME:
                mCallbacks.renameGesture(gesture);
                return true;
            case MENU_ID_EDIT:
            	mCallbacks.editGestureMapping(gesture);
            	return true;
            case MENU_ID_REMOVE:
                mCallbacks.deleteGesture(gesture);
                return true;
        }

        return super.onContextItemSelected(item);
    }

	@Override
	public void disableButtons() {
		View addButton = getActivity().findViewById(R.id.addButton);
		addButton.setEnabled(false);
		View reloadButton = getActivity().findViewById(R.id.reloadButton);
		reloadButton.setEnabled(false);
	}
	
	@Override
	public void enableButtons() {
		View addButton = getActivity().findViewById(R.id.addButton);
		addButton.setEnabled(true);
		View reloadButton = getActivity().findViewById(R.id.reloadButton);
		reloadButton.setEnabled(true);
	}

	@Override
	public void hideListView() {
		getListView().setVisibility(View.GONE);
	}

	@Override
	public void setEmpty() {
		mEmpty.setText(R.string.gestures_empty);
	}

	@Override
	public void setErrorLoading() {
		 mEmpty.setVisibility(View.VISIBLE);
         mEmpty.setText(getString(R.string.gestures_error_loading,
        		 (new File(getActivity().getApplicationContext().getFilesDir(), "gestures")).getAbsolutePath()));
	}
}
