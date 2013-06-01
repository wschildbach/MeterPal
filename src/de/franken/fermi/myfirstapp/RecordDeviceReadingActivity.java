package de.franken.fermi.myfirstapp;

import java.util.HashMap;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class RecordDeviceReadingActivity extends Activity {
	/* the activity can have two states:
	 * ENTER_READING, enters a reading on an existing device.
	 * ENTER_DEVICE, enter a new device into the database.
	 */
	
	private static final int STATE_ENTER_READING = 0;
	private static final int STATE_ENTER_DEVICE = 1;
	private int mState;
	
	public final static String EXTRA_MESSAGE = "de.franken.myfirstapp.recordCounterWithName";
    private static final String TAG = "RecordCounterActivity";

	static final class dbc {
		private dbc() {}
		final static String DATABASE_NAME = "counterdatabase";
		final static int DATABASE_VERSION = 6;
		
		public static final class dev {

			public static HashMap<String,Integer> typeID ;
	        /**
	         * Column name for the unique ID
	         * <P>Type: INTEGER</P>
	         */
	        public static final String _ID = "_ID";
			public static final String TABLE_NAME = "devices";
	        /**
	         * Column name for the meter type
	         * <P>Type: INTEGER (0 -- EMeter, 1 -- gasMeter)</P>
	         */
	        public static final int METER_TYPE_ELECTRICITY = 0;
	        public static final int METER_TYPE_GAS = 1;

	        public static final String COLUMN_NAME_METER_TYPE = "type";
	        /**
	         * Column name for the meter ID -- freeform human readable identifier
	         * <P>Type: STRING</P>
	         */
	        public static final String COLUMN_NAME_METER_NAME = "ID";
	        /**
	         * Column name for the ordering column -- for cycling through meters.
	         * <P>Type: INTEGER</P>
	         */
	        public static final String COLUMN_NAME_METER_NEXT = "order";

	        private dev() {
				typeID = new HashMap<String, Integer>();
				typeID.put("ELECTRICITY",0);
				typeID.put("GAS",1);
	        }
		}

		public static final class entries {
			public static final String TABLE_NAME = "entries";
	        /**
	         * Column name for the unique ID
	         * <P>Type: INTEGER</P>
	         */
	        public static final String _ID = "_ID";
	        /**
	         * Column name for the meter reading timestamp
	         * <P>Type: INTEGER</P>
	         */
	        public static final String COLUMN_NAME_COUNTER_ID = "counterID";
	        /**
	         * Column name for the value
	         * <P>Type: FLOAT</P>
	         */
	        public static final String COLUMN_NAME_COUNTER_VALUE = "value";
	        /**
	         * Column name for the meter reading timestamp
	         * <P>Type: INTEGER (long from System.curentTimeMillis())</P>
	         */
	        public static final String COLUMN_NAME_COUNTER_READATTIME = "readAtTime";
		}
	}

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;
    private Long mDeviceID ;
    private String mDeviceName ;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_counter);

        mOpenHelper = new DatabaseHelper(getApplicationContext());

        // the only intent we have is to record a meter. See if we've been given
        // a meter ID that is in our database
		Intent intent = getIntent();
		String extra = intent.getStringExtra(EXTRA_MESSAGE);

		/* The intent can get the device ID as extra. If there is no extra, or the
		 * device id does not exist, pick the first available.
		 * if there is none available, enter a new device.
		 */

		mState = STATE_ENTER_READING;			

		// XXX I think we should be catching and handling the exception here
		if (extra != null) {
			mDeviceID = Long.parseLong(extra);
			mDeviceName = getCounterName(mDeviceID); // mis-use to see if we have this device
			if (mDeviceName == null) {
				extra = null; // discard invalid device ID
			}
		}

		if (extra == null) {
			mDeviceID = getFirstCounterID();			
			if (mDeviceID == null) {
		        Resources res = getResources();
				mDeviceName = new String(res.getString(R.string.newMeterName));
				mDeviceID = newDevice(mDeviceName);
				mState = STATE_ENTER_DEVICE;
			}
		}
	}

    /**
     * This method is called when the Activity is about to come to the foreground. This happens
     * when the Activity comes to the top of the task stack, OR when it is first starting.
     *
     * XXX Explain what we do here.
     */
    @Override
    protected void onResume() {
        super.onResume();

        switchContentView();
    }

    private void switchContentView() {
        setTitle(mDeviceName);
        switch (mState) {
        case STATE_ENTER_DEVICE:
            setContentView(R.layout.activity_new_meter);
        	break;
        case STATE_ENTER_READING:
            setContentView(R.layout.activity_record_counter);
            break;
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);

		return true;
	}

    /** Called when the user clicks the submit button after meter data has been entered. */
    public void newMeterDone(View view) {
    	SQLiteDatabase db = mOpenHelper.getWritableDatabase();

    	TextView t = (TextView)findViewById(R.id.counter_name);
    	mDeviceName = t.getText().toString();

    	AdapterView av = (AdapterView)findViewById(R.id.counter_type); // the spinner!
    	long type = av.getSelectedItemId ();

		ContentValues cv = new ContentValues();
		cv.put(dbc.dev.COLUMN_NAME_METER_NAME, mDeviceName);
		cv.put(dbc.dev.COLUMN_NAME_METER_TYPE, type);

		db.update(dbc.dev.TABLE_NAME, cv, dbc.dev._ID + "=" + mDeviceID, null);

		mState = STATE_ENTER_READING;
		switchContentView();
    }

    public void newReadingDone(View view) {
    	SQLiteDatabase db = mOpenHelper.getWritableDatabase();

    	TextView t ;
    	t = (TextView)findViewById(R.id.meterTakenValue);
    	double value = Double.parseDouble(t.getText().toString());

    	ContentValues cv = new ContentValues();
    	cv.put(dbc.entries.COLUMN_NAME_COUNTER_ID, mDeviceID);
    	cv.put(dbc.entries.COLUMN_NAME_COUNTER_READATTIME, System.currentTimeMillis());
    	cv.put(dbc.entries.COLUMN_NAME_COUNTER_VALUE, value);

    	db.insertOrThrow(dbc.entries.TABLE_NAME, null, cv);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_newmeter:
			// XXX refactor me
	        Resources res = getResources();
			mDeviceName = new String(res.getString(R.string.newMeterName));
			mDeviceID = newDevice(mDeviceName);
			mState = STATE_ENTER_DEVICE;
			switchContentView();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private final Long getFirstCounterID()
	{
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

        if (!c.moveToFirst()) return null;

        return c.getLong(c.getColumnIndexOrThrow(dbc.dev._ID));
	}

	private final String getCounterName(long ID)
	{
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.query(true,dbc.dev.TABLE_NAME, null, dbc.dev._ID+"="+ID, null,          null,     null,   null, null);
//      c = db.query      (true, table,            columns, selection, selectionArgs, groupBy, having, orderBy, limit)

        if (!c.moveToFirst()) return null;
        int pos = c.getColumnIndexOrThrow(dbc.dev.COLUMN_NAME_METER_NAME);
        String name = c.getString(pos);
        return name;
	}

	final void initHistoryView(int ID) {
		/*
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        // XXXX use dbc.dev._ID
        Cursor c = db.query(dbc.TABLE_NAME, null, "'"+dbc.COLUMN_NAME_COUNTER_ID + "'='"+ID+"'", null, null, dbc.COLUMN_NAME_COUNTER_READATTIME, null);
        ListView lv = (ListView) findViewById(R.id.listView1);

        if (!c.moveToFirst()) return ;

        int posVal = c.getColumnIndexOrThrow(dbc.COLUMN_NAME_COUNTER_VALUE);
        int posTime = c.getColumnIndexOrThrow(dbc.COLUMN_NAME_COUNTER_READATTIME);
        do {
        	long time = c.getLong(posTime);
        	float val = c.getFloat(posVal);
        } while (c.moveToNext());
        */
	}
	
	static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {

			// calls the super constructor, requesting the default cursor
			// factory.
			super(context, dbc.DATABASE_NAME, null, dbc.DATABASE_VERSION);
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
					+ dbc.dev.COLUMN_NAME_METER_TYPE + " INTEGER"
					+ ");");

			// XXX insert two devices for debug purposes
			ContentValues cv,cv2;
			cv = new ContentValues();
			cv.put(dbc.dev._ID, 1);
			cv.put(dbc.dev.COLUMN_NAME_METER_NAME, "GAS12345");
			cv.put(dbc.dev.COLUMN_NAME_METER_TYPE, dbc.dev.METER_TYPE_GAS);
			db.insertOrThrow(dbc.dev.TABLE_NAME, null, cv);

			cv = new ContentValues();
			cv.put(dbc.dev._ID, 2);
			cv.put(dbc.dev.COLUMN_NAME_METER_NAME, "ELE54321");
			cv.put(dbc.dev.COLUMN_NAME_METER_TYPE, dbc.dev.METER_TYPE_ELECTRICITY);
			db.insertOrThrow(dbc.dev.TABLE_NAME, null, cv);

			// XXX insert two bogus readings for debug purposes
			// XXX IDs are bogus!
			cv = new ContentValues();
			cv.put(dbc.entries.COLUMN_NAME_COUNTER_ID,1);
			cv.put(dbc.entries.COLUMN_NAME_COUNTER_VALUE,12345.6);
			cv2 = new ContentValues(cv);
			cv.put(dbc.entries.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis());
			cv2.put(dbc.entries.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis()+1000);

			db.insertOrThrow(dbc.entries.TABLE_NAME, null, cv);
			db.insertOrThrow(dbc.entries.TABLE_NAME, null, cv2);
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

	private long newDevice(String s) {
    	SQLiteDatabase db = mOpenHelper.getWritableDatabase();

    	ContentValues cv = new ContentValues();
		cv.put(dbc.dev.COLUMN_NAME_METER_NAME, s);
//		cv.put(dbc.dev.COLUMN_NAME_METER_TYPE, dbc.dev.METER_TYPE_GAS); // XXX should this be hardcoded?
		return db.insertOrThrow(dbc.dev.TABLE_NAME, null, cv);		
	}
}
