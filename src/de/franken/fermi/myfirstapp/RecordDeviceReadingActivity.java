package de.franken.fermi.myfirstapp;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class RecordDeviceReadingActivity extends Activity {
	public final static String EXTRA_MESSAGE = "de.franken.myfirstapp.recordCounterWithName";
    private static final String TAG = "RecordCounterActivity";

	static final class dbc {
		private dbc() {}
		final static String DATABASE_NAME = "counterdatabase";
		final static int DATABASE_VERSION = 6;
		
		public static final class dev {
		
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

    // Handle to a new DatabaseHelper.
    public DatabaseHelper mOpenHelper;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_counter);

        // Creates a new helper object. Note that the database itself isn't opened until
        // something tries to access it, and it's only created if it doesn't already exist.
        mOpenHelper = new DatabaseHelper(getApplicationContext());

        // the only intent we have is to record a meter. See if we've been given
        // a meter ID that is in our database
		Intent intent = getIntent();
		String extra = intent.getStringExtra(EXTRA_MESSAGE);
		int counterID ;
		
		// default
		if (extra == null) {
			// XXX I think we should be catching and handling the exception here
			counterID = getFirstCounterID();
		} else {
			// XXX I think we should be catching and handling the exception here
			counterID = Integer.parseInt(extra);
		}

		String name = getCounterName(counterID);
		if (name == null) {
			// create a new meter
			intent = new Intent(this, AddMeterDeviceActivity.class);
			startActivity(intent); // this may not return...
		}
		else
		{
			setTitle(name);
		}

		// XXX we should not be doing this as it calls the database in the UI thread
        initHistoryView(counterID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);

		return true;
	}

	/** Called when the user clicks the new Meter button */
	public void newMeter(View view) { // Do something in response to button }
		Intent intent = new Intent(this, AddMeterDeviceActivity.class);

		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_newmeter:
			Intent intent = new Intent(this, AddMeterDeviceActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	final int getFirstCounterID()
	{
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = db.query(true,dbc.dev.TABLE_NAME, null,    null,        null,     null,     null,   dbc.dev._ID, null);
//        c = db.query     (true, table,            columns, selection, selectionArgs, groupBy, having, orderBy, limit)

        // XXX not really safe as an error return
        if (!c.moveToFirst()) return -1;

        return c.getInt(c.getColumnIndexOrThrow(dbc.dev._ID));
	}

	final String getCounterName(int ID)
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
}
