package de.franken.fermi.myfirstapp;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class AddMeterDeviceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_meter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.new_meter, menu);
		return true;
	}

    /** Called when the user clicks the Done button */
    public void newMeterDone(View view) {
//    	RecordCounterActivity.mOpenHelper.getReadableDatabase();

    	Intent intent = new Intent(this, RecordDeviceReadingActivity.class); // Call the RecordCounter intent

    	EditText editText = (EditText) findViewById(R.id.counter_ID);
    	String message = editText.getText().toString();
    	intent.putExtra(RecordDeviceReadingActivity.EXTRA_MESSAGE, message);

    	startActivity(intent);
    }


}
