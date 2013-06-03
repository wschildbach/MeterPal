package de.franken.fermi.meterpal;

import java.text.DateFormat;
import java.util.HashMap;

import de.franken.fermi.meterpal.DatabaseHelper.mySQLiteOpenHelper;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class RecordDeviceReadingActivity extends Activity implements OnItemSelectedListener {
	/*
	 * the activity can have two states: ENTER_READING, enters a reading on an
	 * existing device. ENTER_DEVICE, enter a new device into the database.
	 */

	/*
	private static final int STATE_ENTER_READING = 0;
	private static final int STATE_ENTER_DEVICE = 1;
	private int mState;
*/
	
	public final static String INTENT_ENTER_DEVICE_READING = "de.franken.meterpal.intent_enter_device_reading";
	public final static String EXTRA_MESSAGE = "de.franken.meterpal.recordCounterWithName";
	private static final String TAG = "RecordCounterActivity";

	// Handle to a new DatabaseHelper.
	private mySQLiteOpenHelper mOpenHelper;
	private Long mDeviceID;
	private String mDeviceName;
	private SimpleCursorAdapter mEntryAdapter; // XXX needs version 11 or greater
	private SimpleCursorAdapter mDeviceAdapter;
	private Spinner mSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * The DatabaseHelper is an object that is a singleton within this Application.
		 * We can thus refer to it from here.
		 */
	    DatabaseHelper dbh = ((DatabaseHelper)getApplicationContext());
		mOpenHelper = (mySQLiteOpenHelper)dbh.getSQLiteOpenHelper();

		// the only intent we have is to record a meter. See if we've been given
		// a meter ID that is in our database
		Intent intent = getIntent();
		String extra = intent.getStringExtra(EXTRA_MESSAGE);

		/*
		 * The intent can get the device ID as extra. If there is no extra, or
		 * the device id does not exist, pick the first available. if there is
		 * none available, enter a new device.
		 */

		// XXX I think we should be catching and handling the exception here
		if (extra != null) {
			mDeviceID = Long.parseLong(extra);
			mDeviceName = getCounterName(mDeviceID); // mis-use to see if we
														// have this device
			if (mDeviceName == null) {
				extra = null; // discard invalid device ID
			}
		}

		if (extra == null) {
			mDeviceID = getFirstCounterID();
			if (mDeviceID == null) {
				intent = new Intent(this, NewDeviceActivity.class); startActivity(intent);
				// never return
			} else {
				mDeviceName = getCounterName(mDeviceID);
			}
		}
		setContentView(R.layout.activity_record_counter);

		/*
		 * to always fill the spinner with the availabel meters, set up a device adapter
		 * and connect it to the spinner. The cursor will be set up at resume() time.
		 */
		mDeviceAdapter = new SimpleCursorAdapter(
				getApplicationContext(),
				R.layout.device_spinner,
				null, // no cursor available yet
				new String[] { DatabaseHelper.dbc.dev.COLUMN_NAME_METER_NAME }, // from
				new int[] {R.id.actionBarItem},// to
				0);

		// add the spinner to the actionBar

		mSpinner = (Spinner)getLayoutInflater().inflate(R.layout.action_bar_spinner, null);
		ActionBar actionBar = getActionBar();
		actionBar.setCustomView(mSpinner); // add the spinner to the action bar
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM|ActionBar.DISPLAY_USE_LOGO);

		//mSpinner = (Spinner) findViewById(R.id.spinner1);
		mSpinner.setAdapter(mDeviceAdapter);
		mSpinner.setOnItemSelectedListener(this);

		/*
		 * to always have the log updated, create an "entry adapter" and connect it to the
		 * listview.
		 */
		mEntryAdapter = new SimpleCursorAdapter(
				getApplicationContext(),
				R.layout.reading_logview,
				null, // no cursor available yet
				new String[] {DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_READATTIME, DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_VALUE},// from
				new int[] {R.id.logEntryDatetime,R.id.logEntryValue},// to
				0);

		ListView lv = (ListView)findViewById(R.id.logView);
		lv.setAdapter(mEntryAdapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDeviceAdapter = null;
		mEntryAdapter = null;
	}

	/**
	 * This method is called when the Activity is about to come to the
	 * foreground. This happens when the Activity comes to the top of the task
	 * stack, OR when it is first starting.
	 * 
	 * XXX Explain what we do here.
	 */

	@Override
	protected void onResume() {
		super.onResume();

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		onDeviceIDChanged();

		Cursor c = db.query(false, // not unique
				DatabaseHelper.dbc.dev.TABLE_NAME, // table name
				new String[] { DatabaseHelper.dbc.dev._ID, DatabaseHelper.dbc.dev.COLUMN_NAME_METER_NAME }, // column
				null, // select
				null, // no selection args
				null, // no groupBy
				null, // no having
				null, // no specific order
				null); // no limit

		mDeviceAdapter.changeCursor(c);

		db.close();
	}

	private void onDeviceIDChanged() {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query(false, // not unique
				DatabaseHelper.dbc.entries.TABLE_NAME, // table name
				null, // all columns
				DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_ID + "=" + mDeviceID, // select
				null, // no selection args
				null, // no groupBy
				null, // no having
				DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_READATTIME + " DESC" , // order by time, latest first
				null); // no limit

		mEntryAdapter.changeCursor(c);
		// read new device name
		mDeviceName = getCounterName(mDeviceID);
//		setTitle(mDeviceName);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mEntryAdapter.changeCursor(null);
		mDeviceAdapter.changeCursor(null);
	}

	private ShareActionProvider mShareActionProvider;

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.menu_item_share);

		// Fetch and store ShareActionProvider
		mShareActionProvider = (ShareActionProvider) item.getActionProvider();

		/*
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, getDatabaseDump());
		shareIntent.setType("text/plain");
		mShareActionProvider.setShareIntent(shareIntent); // without this, we
															// could stick to
															// API level 8
		 */
		return true;
	}

	/*
	 * OnItemSelectedListener methods
	 */
    public void onItemSelected(AdapterView<?> parent, View view, 
            int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
    	mDeviceID = Long.valueOf(parent.getItemIdAtPosition(pos));
    	onDeviceIDChanged();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void newReadingDone(View view) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		TextView t;
		t = (TextView) findViewById(R.id.meterTakenValue);
		double value = Double.parseDouble(t.getText().toString());

		ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_ID, mDeviceID);
		cv.put(DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis());
		cv.put(DatabaseHelper.dbc.entries.COLUMN_NAME_COUNTER_VALUE, value);

		db.insertOrThrow(DatabaseHelper.dbc.entries.TABLE_NAME, // table
				null, // nullColumnHack
				cv);
		db.close();
		mEntryAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_newmeter:
			Intent intent = new Intent(this, NewDeviceActivity.class); startActivity(intent);
			// never return

			return true;
		case R.id.menu_deldb:
			mOpenHelper.deleteDB();
			intent = new Intent(this, NewDeviceActivity.class); startActivity(intent);

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private final Long getFirstCounterID() {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query(true, // unique
				DatabaseHelper.dbc.dev.TABLE_NAME, // table name
				new String[] {DatabaseHelper.dbc.dev._ID}, // only the id column
				null, // select all
				null, // no selection args
				null, // no groupBy
				null, // no having
				DatabaseHelper.dbc.dev._ID, // order by _ID
				null); // no limit

		if (!c.moveToFirst())
			return null;

		return c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.dbc.dev._ID));
	}

	private final String getCounterName(long ID) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query(true, DatabaseHelper.dbc.dev.TABLE_NAME, null, DatabaseHelper.dbc.dev._ID + "="
				+ ID, null, null, null, null, null);
		// c = db.query (true, table, columns, selection, selectionArgs,
		// groupBy, having, orderBy, limit)

		if (!c.moveToFirst())
			return null;
		int pos = c.getColumnIndexOrThrow(DatabaseHelper.dbc.dev.COLUMN_NAME_METER_NAME);
		String name = c.getString(pos);
		return name;
	}

}

