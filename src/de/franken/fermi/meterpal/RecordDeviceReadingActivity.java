package de.franken.fermi.meterpal;

import java.text.DateFormat;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
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
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class RecordDeviceReadingActivity extends Activity {
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

	static final class dbc {
		private dbc() {
		}

		final static String DATABASE_NAME = "counterdatabase";
		final static int DATABASE_VERSION = 2;

		public static final class dev {

			public static HashMap<String, Integer> typeID;
			/**
			 * Column name for the unique ID
			 * <P>
			 * Type: INTEGER
			 * </P>
			 */
			public static final String _ID = "_id";
			public static final String TABLE_NAME = "devices";
			/**
			 * Column name for the meter type
			 * <P>
			 * Type: INTEGER (0 -- EMeter, 1 -- gasMeter)
			 * </P>
			 */
			public static final int METER_TYPE_ELECTRICITY = 0;
			public static final int METER_TYPE_GAS = 1;

			public static final String COLUMN_NAME_METER_TYPE = "type";
			/**
			 * Column name for the meter ID -- freeform human readable
			 * identifier
			 * <P>
			 * Type: STRING
			 * </P>
			 */
			public static final String COLUMN_NAME_METER_NAME = "ID";
			/**
			 * Column name for the ordering column -- for cycling through
			 * meters.
			 * <P>
			 * Type: INTEGER
			 * </P>
			 */
			public static final String COLUMN_NAME_METER_NEXT = "order";

			private dev() {
				typeID = new HashMap<String, Integer>();
				typeID.put("ELECTRICITY", 0);
				typeID.put("GAS", 1);
			}
		}

		public static final class entries {
			public static final String TABLE_NAME = "entries";
			/**
			 * Column name for the unique ID
			 * <P>
			 * Type: INTEGER
			 * </P>
			 */
			public static final String _ID = "_id";
			/**
			 * Column name for the meter reading timestamp
			 * <P>
			 * Type: INTEGER
			 * </P>
			 */
			public static final String COLUMN_NAME_COUNTER_ID = "counterID";
			/**
			 * Column name for the value
			 * <P>
			 * Type: FLOAT
			 * </P>
			 */
			public static final String COLUMN_NAME_COUNTER_VALUE = "value";
			/**
			 * Column name for the meter reading timestamp
			 * <P>
			 * Type: INTEGER (long from System.curentTimeMillis())
			 * </P>
			 */
			public static final String COLUMN_NAME_COUNTER_READATTIME = "readAtTime";
		}
	}

	// Handle to a new DatabaseHelper.
	public static DatabaseHelper mOpenHelper;
	private Long mDeviceID;
	private String mDeviceName;
	private SimpleCursorAdapter mEntryAdapter; // XXX needs version 11 or greater
	private SimpleCursorAdapter mDeviceAdapter;
	private Spinner mSpinner;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/*
		 * One-time inits
		 */
		mOpenHelper = new DatabaseHelper(getApplicationContext());

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
		setTitle(mDeviceName);
		// add the spinner to the actionBar

		/*
		ActionBar actionBar = getActionBar();
		mSpinner = (Spinner)getLayoutInflater().inflate(R.id.actionBarSpinner, null);
		actionBar.setCustomView(mSpinner); // add the spinner to the action bar
		*/

		/*
		 * to always fill the spinner with the availabel meters, set up a device adapter
		 * and connect it to the spinner. The cursor will be set up at resume() time.
		 */
		mDeviceAdapter = new SimpleCursorAdapter(
				getApplicationContext(),
				R.layout.device_spinner,
				null, // no cursor available yet
				new String[] { dbc.dev.COLUMN_NAME_METER_NAME }, // from
				new int[] {R.id.actionBarItem},// to
				0);

		mSpinner = (Spinner) findViewById(R.id.spinner1);
		mSpinner.setAdapter(mDeviceAdapter);

		/*
		 * to always have the log updated, create an "entry adapter" and connect it to the
		 * listview.
		 */
		mEntryAdapter = new SimpleCursorAdapter(
				getApplicationContext(),
				R.layout.reading_logview,
				null, // no cursor available yet
				new String[] {dbc.entries.COLUMN_NAME_COUNTER_READATTIME, dbc.entries.COLUMN_NAME_COUNTER_VALUE},// from
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

		mOpenHelper.close();
		mOpenHelper = null;
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
		Cursor c = db.query(false, // not unique
				dbc.entries.TABLE_NAME, // table name
				null, // all columns
				dbc.entries.COLUMN_NAME_COUNTER_ID + "=" + mDeviceID, // select
				null, // no selection args
				null, // no groupBy
				null, // no having
				dbc.entries.COLUMN_NAME_COUNTER_READATTIME + " DESC" , // order by time, latest first
				null); // no limit

		mEntryAdapter.changeCursor(c);

		c = db.query(false, // not unique
				dbc.dev.TABLE_NAME, // table name
				new String[] { dbc.dev._ID, dbc.dev.COLUMN_NAME_METER_NAME }, // column
				null, // select
				null, // no selection args
				null, // no groupBy
				null, // no having
				null, // no specific order
				null); // no limit

		mDeviceAdapter.changeCursor(c);

		db.close();
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

		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, getDatabaseDump());
		shareIntent.setType("text/plain");
		mShareActionProvider.setShareIntent(shareIntent); // without this, we
															// could stick to
															// API level 8

		return true;
	}

	public void newReadingDone(View view) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		TextView t;
		t = (TextView) findViewById(R.id.meterTakenValue);
		double value = Double.parseDouble(t.getText().toString());

		ContentValues cv = new ContentValues();
		cv.put(dbc.entries.COLUMN_NAME_COUNTER_ID, mDeviceID);
		cv.put(dbc.entries.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis());
		cv.put(dbc.entries.COLUMN_NAME_COUNTER_VALUE, value);

		db.insertOrThrow(dbc.entries.TABLE_NAME, // table
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
				dbc.dev.TABLE_NAME, // table name
				new String[] {dbc.dev._ID}, // only the id column
				null, // select all
				null, // no selection args
				null, // no groupBy
				null, // no having
				dbc.dev._ID, // order by _ID
				null); // no limit

		if (!c.moveToFirst())
			return null;

		return c.getLong(c.getColumnIndexOrThrow(dbc.dev._ID));
	}

	private final String dumpCursorToCSV(Cursor c) {
		StringBuilder sr = new StringBuilder();
		Character COLSEP = ';';
		
		if (c.moveToFirst()) {
			int i;
			for (i = 0; i < c.getColumnCount(); i++) {
				sr.append(c.getColumnName(i)).append(COLSEP);
			}
			sr.append("\n");

			do {
				for (i = 0; i < c.getColumnCount(); i++) {
					sr.append(c.getString(i)).append(COLSEP);
				}
				sr.append("\n");
			} while (c.moveToNext());
		}

		return sr.toString();
	}

	private final String getDatabaseDump() {
		StringBuilder sr = new StringBuilder();
		Character COLSEP = ';';

		DateFormat df = DateFormat.getDateTimeInstance();
		sr.append("database dumped on ")
				.append(df.format(System.currentTimeMillis())).append("\n\n");
		sr.append("database name: " + dbc.DATABASE_NAME + COLSEP
				+ "DATABASE_VERSION " + dbc.DATABASE_VERSION + COLSEP + "\n\n");

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query(true, // unique
				dbc.dev.TABLE_NAME, // table name
				null, // all columns
				null, // select all
				null, // no selection args
				null, // no groupBy
				null, // no having
				dbc.dev._ID, // order by _ID
				null); // no limit

		sr.append("device database\n");
		
		sr.append(DatabaseUtils.dumpCursorToString(c));

		sr.append(dumpCursorToCSV(c));

		c = db.query(true, // unique
				dbc.entries.TABLE_NAME, // table name
				null, // all columns
				null, // select all
				null, // no selection args
				null, // no groupBy
				null, // no having
				dbc.dev._ID, // order by _ID
				null); // no limit

		sr.append("\nentries database\n");
		sr.append(DatabaseUtils.dumpCursorToString(c));
		sr.append(dumpCursorToCSV(c));

		return sr.toString();
	}

	private final String getCounterName(long ID) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = db.query(true, dbc.dev.TABLE_NAME, null, dbc.dev._ID + "="
				+ ID, null, null, null, null, null);
		// c = db.query (true, table, columns, selection, selectionArgs,
		// groupBy, having, orderBy, limit)

		if (!c.moveToFirst())
			return null;
		int pos = c.getColumnIndexOrThrow(dbc.dev.COLUMN_NAME_METER_NAME);
		String name = c.getString(pos);
		return name;
	}

	static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {

			// calls the super constructor, requesting the default cursor
			// factory.
			super(context, dbc.DATABASE_NAME, null, dbc.DATABASE_VERSION);
		}

		public void deleteDB()
		{
			SQLiteDatabase db = getWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + dbc.entries.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + dbc.dev.TABLE_NAME);
			onCreate(db);
			db.close();
		}

		/**
		 * 
		 * Creates the underlying database with table name and column names
		 * taken from the dbc class.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			// create the table for meter readings
			// I should catch an exception here
			 db.execSQL("CREATE TABLE " + dbc.entries.TABLE_NAME + " ("
					+ dbc.entries._ID + " INTEGER PRIMARY KEY,"
					+ dbc.entries.COLUMN_NAME_COUNTER_ID + " INTEGER,"
					+ dbc.entries.COLUMN_NAME_COUNTER_VALUE + " FLOAT,"
					+ dbc.entries.COLUMN_NAME_COUNTER_READATTIME + " INTEGER"
					+ ");");

			// create the table for devices
			// I should catch an exception here
			db.execSQL("CREATE TABLE " + dbc.dev.TABLE_NAME + " ("
					+ dbc.dev._ID + " INTEGER PRIMARY KEY,"
					+ dbc.dev.COLUMN_NAME_METER_NAME + " STRING,"
					+ dbc.dev.COLUMN_NAME_METER_TYPE + " INTEGER" + ");");
		}

		/**
		 * 
		 * Called whenever the version changes.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			// Logs that the database is being upgraded
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");

			// Kills the table and existing data
			db.execSQL("DROP TABLE IF EXISTS " + dbc.entries.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + dbc.dev.TABLE_NAME);

			// Recreates the database with a new version
			onCreate(db);
		}
	}
}
