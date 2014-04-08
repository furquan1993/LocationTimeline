package com.assassin.location.database;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocationDBHelper extends SQLiteOpenHelper {

	public static final String DBNAME = "timeline";
	public static final int DBVERSION = 1;

	public LocationDBHelper(Context context) {
		super(context, DBNAME, null, DBVERSION);
		onCreate(this.getWritableDatabase());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		Calendar today = Calendar.getInstance();
		
		String tableName = "" + today.get(Calendar.YEAR) + "-"
				+ (today.get(Calendar.MONTH) + 1) + "-"
				+ today.get(Calendar.DAY_OF_MONTH);

		/*String time = "" + today.get(Calendar.HOUR_OF_DAY)
				+ today.get(Calendar.MINUTE) + today.get(Calendar.SECOND);*/

		String sql = "CREATE TABLE IF NOT EXISTS \'"
				+ tableName
				+ "\' (_id integer primary key autoincrement, time TEXT, latitude REAL, longitude REAL, address TEXT);";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
}
