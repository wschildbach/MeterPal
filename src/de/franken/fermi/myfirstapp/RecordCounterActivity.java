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

public class RecordCounterActivity extends Activity {
	public final static String EXTRA_MESSAGE = "de.franken.myfirstapp.recordCounterWithName";
    private static final String TAG = "RecordCounterActivity";

	static final class dbc {
		private dbc() {}
		final static String DATABASE_NAME = "counterdatabase";
		final static int DATABASE_VERSION = 0;
        public static final String TABLE_NAME = "entries";
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
        public static final String COLUMN_NAME_COUNTER_READATTIME = "read";
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
		 * taken from the NotePad class.
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			ContentValues cv = new ContentValues();
			
			cv.put(dbc.COLUMN_NAME_COUNTER_ID,1234);
			cv.put(dbc.COLUMN_NAME_COUNTER_VALUE,12345.6);
			
			ContentValues cv2 = new ContentValues(cv);
			cv.put(dbc.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis());
			cv2.put(dbc.COLUMN_NAME_COUNTER_READATTIME,System.currentTimeMillis()+1000);
			
			db.execSQL("CREATE TABLE " + dbc.TABLE_NAME + " ("
					+ "ID" + " INTEGER PRIMARY KEY,"
					+ dbc.COLUMN_NAME_COUNTER_ID + " INTEGER,"
					+ dbc.COLUMN_NAME_COUNTER_VALUE + " FLOAT"
					+ dbc.COLUMN_NAME_COUNTER_READATTIME + " INTEGER,"
					+ ");");
		
			db.insertOrThrow(dbc.TABLE_NAME, null, cv);
			db.insertOrThrow(dbc.TABLE_NAME, null, cv2);
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
			db.execSQL("DROP TABLE IF EXISTS " + dbc.TABLE_NAME);

			// Recreates the database with a new version
			onCreate(db);
		}
	}

    // Handle to a new DatabaseHelper.
    private DatabaseHelper mOpenHelper;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_counter);

        // Creates a new helper object. Note that the database itself isn't opened until
        // something tries to access it, and it's only created if it doesn't already exist.
        mOpenHelper = new DatabaseHelper(getApplicationContext());

        // we should not be doing this as it calls the database in the UI thread
        fillHistoryView("1234");
        
        // the only intent we have is to record a meter. See if we've been given
        // a meter ID that is in our database
		Intent intent = getIntent();
		// I think we should be catching and handling the exception here
		Integer counterID = Integer.parseInt(intent.getStringExtra(EXTRA_MESSAGE));
		
		StringBuilder title = new StringBuilder("Gaszähler").append(counterID);

		setTitle(title);
		// TextView name = (TextView) findViewById(R.id.MeterID);
		// name.setText(counterID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);

		return true;
	}

	/** Called when the user clicks the new Meter button */
	public void newMeter(View view) { // Do something in response to button }
		Intent intent = new Intent(this, NewMeterActivity.class);

		startActivity(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_newmeter:
			Intent intent = new Intent(this, NewMeterActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	final void fillHistoryView(String ID) {
		/*
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
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
