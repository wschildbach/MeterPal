package de.franken.fermi.myfirstapp;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class RecordCounterActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_counter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);
		return true;
	}

}
