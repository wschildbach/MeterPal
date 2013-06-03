package de.franken.fermi.meterpal;

import de.franken.fermi.meterpal.DatabaseHelper.mySQLiteOpenHelper;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;

public class NewDeviceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_new_device);
	}

	/**
	 * Called when the user clicks the submit button after meter data has been
	 * entered.
	 */

	public void onSubmit(View v) {
		/*
		 * The DatabaseHelper is an object that is a singleton within this Application.
		 * We can thus refer to it from here.
		 */
	    DatabaseHelper dbh = ((DatabaseHelper)getApplicationContext());
	    mySQLiteOpenHelper openHelper = (mySQLiteOpenHelper)dbh.getSQLiteOpenHelper();
		SQLiteDatabase db = openHelper.getWritableDatabase();

		// retrieve the new meter name from the UI
		TextView t = (TextView) findViewById(R.id.counter_name);
		String name = t.getText().toString();

		// retrieve the type, as index into the spinner entries
		AdapterView av = (AdapterView) findViewById(R.id.counter_type); // the
		long type = av.getSelectedItemId();								// spinner!

		ContentValues cv = new ContentValues();
		cv.put(DatabaseHelper.dbc.dev.COLUMN_NAME_METER_NAME, name);
		cv.put(DatabaseHelper.dbc.dev.COLUMN_NAME_METER_TYPE, type);

		Long deviceID = db.insertOrThrow(DatabaseHelper.dbc.dev.TABLE_NAME, null, cv); // XXX catch me
		db.close(); // this closes all instances of the database (even in the calling Activity)

		// add new created device ID as extra
		Intent intent = new Intent(this, RecordDeviceReadingActivity.class);
		String message = deviceID.toString(); intent.putExtra(RecordDeviceReadingActivity.INTENT_ENTER_DEVICE_READING, message);
		startActivity(intent);
	}

	private void onCancel(View v) {
		Intent intent = new Intent(this, RecordDeviceReadingActivity.class);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.new_device, menu);
		return true;
	}
}
