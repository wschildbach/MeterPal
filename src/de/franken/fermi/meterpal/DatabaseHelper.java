/**
 * 
 */
package de.franken.fermi.meterpal;

import java.text.DateFormat;
import java.util.HashMap;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends Application {
    private mySQLiteOpenHelper databaseHelper;

    /**
     * Called when the application is starting, before any other 
     * application objects have been created. Implementations 
     * should be as quick as possible...
     */
    @Override
    public void onCreate() {
        super.onCreate();
        databaseHelper = new mySQLiteOpenHelper(this);
    }

    /**
     * Called when the application is stopping. There are no more 
     * application objects running and the process will exit. 
     * Note: never depend on this method being called; in many 
     * cases an unneeded application process will simply be killed 
     * by the kernel without executing any application code...
     */
    @Override
    public void onTerminate() {
        super.onTerminate();
        databaseHelper.close();
    }
    
    public SQLiteOpenHelper getSQLiteOpenHelper() {
        return databaseHelper;
    }

	class mySQLiteOpenHelper extends SQLiteOpenHelper {

		private static final String TAG = "mySQLiteOpenHelper";

		mySQLiteOpenHelper(Context context) {

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

		public String getDatabaseDump() {
			StringBuilder sr = new StringBuilder();
			Character COLSEP = ';';

			DateFormat df = DateFormat.getDateTimeInstance();
			sr.append("database dumped on ")
					.append(df.format(System.currentTimeMillis())).append("\n\n");
			sr.append("database name: " + DatabaseHelper.dbc.DATABASE_NAME + COLSEP
					+ "DATABASE_VERSION " + DatabaseHelper.dbc.DATABASE_VERSION + COLSEP + "\n\n");

			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(false, // unique
					DatabaseHelper.dbc.dev.TABLE_NAME, // table name
					null, // all columns
					null, // select all
					null, // no selection args
					null, // no groupBy
					null, // no having
					DatabaseHelper.dbc.dev._ID, // order by _ID
					null); // no limit

			sr.append("device database\n");
			
//			sr.append(DatabaseUtils.dumpCursorToString(c));

			sr.append(dumpCursorToCSV(c));

			c = db.query(false, // unique
					DatabaseHelper.dbc.entries.TABLE_NAME, // table name
					null, // all columns
					null, // select all
					null, // no selection args
					null, // no groupBy
					null, // no having
					DatabaseHelper.dbc.entries._ID, // order by _ID
					null); // no limit

			sr.append("\nentries database\n");
	//		sr.append(DatabaseUtils.dumpCursorToString(c));
			sr.append(dumpCursorToCSV(c));

			return sr.toString();
		}

		private String dumpCursorToCSV(Cursor c) {
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
				} while(c.moveToNext());
			}

			return sr.toString();
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

	public static final class dbc {
		private dbc() { } // make sure we cannot be instantiated

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
}
