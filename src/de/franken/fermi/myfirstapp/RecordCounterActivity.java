package de.franken.fermi.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class RecordCounterActivity extends Activity {
	public final static String EXTRA_MESSAGE = "de.franken.myfirstapp.recordCounterWithName";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_counter);

		Intent intent = getIntent();
		String counterID = intent.getStringExtra(EXTRA_MESSAGE);

    	TextView name = (TextView) findViewById(R.id.MeterID);
    	name.setText(counterID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.record_counter, menu);
		return true;
	}

    /** Called when the user clicks the new Meter button */
    public void newMeter(View view) {     // Do something in response to button }
    	Intent intent = new Intent(this, NewMeterActivity.class);
/*
    	EditText editText = (EditText) findViewById(R.id.editText1);
    	String message = editText.getText().toString();
    	intent.putExtra(EXTRA_MESSAGE, message);
    	*/
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
}
