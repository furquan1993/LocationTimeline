package com.assassin.location;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Toast;

import com.assassin.location.database.LocationDBHelper;

public class MainActivity extends ActionBarActivity {
	private Context context;
	private LocationDBHelper dbHelper;
	private Calendar today;
	private TimelineCursorAdapter adapter;

	// Declaring Views.
	private ListView lvTimeline;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;
		dbHelper = new LocationDBHelper(context);
		today = Calendar.getInstance();

		// Initiating Views.
		lvTimeline = (ListView) findViewById(R.id.lvTimeline);

		startService(new Intent(context, GPSService.class));

	}

	@Override
	protected void onResume() {
		super.onResume();
		SQLiteDatabase locationDb = dbHelper.getReadableDatabase();
		/*
		 * StringBuilder lat = new StringBuilder("Latitude\n"); StringBuilder
		 * lon = new StringBuilder("Longitude\n");
		 */

		String tableName = "" + today.get(Calendar.YEAR) + "-"
				+ (today.get(Calendar.MONTH) + 1) + "-"
				+ today.get(Calendar.DAY_OF_MONTH);

		String sql = "SELECT * FROM \'" + tableName + "\'";

		Cursor cursor = null;
		try {
			cursor = (new CursorGenerator()).execute(sql).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (cursor == null) {
			cursor = locationDb.rawQuery(sql, null);
		}
		/*
		 * if (cursor.getCount() != 0) { while (cursor.moveToNext()) { int id =
		 * cursor.getInt(0); lat.append(id + " " + cursor.getDouble(2) + "\n");
		 * lon.append(id + " " + cursor.getDouble(3) + "\n"); } }
		 */

		adapter = new TimelineCursorAdapter(context, cursor, true);
		adapter.notifyDataSetChanged();
		locationDb.close();

		lvTimeline.setAdapter(adapter);
		lvTimeline.setSelection(lvTimeline.getAdapter().getCount() - 1);
		lvTimeline.setOnScrollListener(new OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {

			}
		});

		// Check if Internet is available
		if (!isNetworkAvailable()) {
			Toast.makeText(context,
					"Internet Connection not working, Can't load maps.",
					Toast.LENGTH_SHORT).show();
		}

	}

	class CursorGenerator extends AsyncTask<String, Void, Cursor> {

		@Override
		protected Cursor doInBackground(String... params) {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.rawQuery(params[0], null);
			return cursor;
		}

	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		switch (id) {
		case R.id.refresh:
			refreshView();
			break;
		case R.id.clear_record:

			/*
			 * String tableName = "\'" + today.get(Calendar.YEAR) + "-" +
			 * (today.get(Calendar.MONTH) + 1) + "-" +
			 * today.get(Calendar.DAY_OF_MONTH) + "\'";
			 */

			MyDatePickerListener dateListener = new MyDatePickerListener();

			DatePickerDialog dialog = new DatePickerDialog(context,
					dateListener, today.get(Calendar.YEAR),
					today.get(Calendar.MONTH), today.get(Calendar.DATE));

			
			
			dialog.show();

			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void refreshView() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String tableName = "" + today.get(Calendar.YEAR) + "-"
				+ (today.get(Calendar.MONTH) + 1) + "-"
				+ today.get(Calendar.DAY_OF_MONTH);

		String sql = "SELECT * FROM \'" + tableName + "\'";
		Cursor cursor = null;

		try {
			cursor = new CursorGenerator().execute(sql).get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (cursor == null)
			db.rawQuery(sql, null);

		adapter.changeCursor(cursor);

		Toast.makeText(context, "Refreshing", Toast.LENGTH_SHORT).show();

		adapter.notifyDataSetChanged();
	}

	class MyDatePickerListener implements OnDateSetListener {

		String date = "";

		private SQLiteDatabase db;

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			date = "\'" + year + "-" + (monthOfYear + 1) + "-" + dayOfMonth
					+ "\'";
			AlertDialog.Builder clearRecord = new AlertDialog.Builder(context);
			clearRecord
					.setTitle("Delete Record")
					.setMessage(
							"Are you sure you want to delete the record for date: " + date)
					.setPositiveButton("Yes", new OnClickListener() {

						@Override
						public void onClick(DialogInterface arg0, int arg1) {

							

							SQLiteDatabase db = dbHelper.getWritableDatabase();
							db.execSQL("DROP TABLE IF EXISTs " + date);
							Toast.makeText(context,
									"Logs cleared for " + date + "",
									Toast.LENGTH_SHORT).show();
							db.execSQL("CREATE TABLE IF NOT EXISTS "
									+ date
									+ "  ( _id integer primary key autoincrement, time TEXT, latitude REAL, longitude REAL, address TEXT);");
							db.close();
							refreshView();
						}
					}).setNegativeButton("Cancel", new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();

						}
					});
			
			AlertDialog alert = clearRecord.create();
			alert.show();
			
			
			//lvTimeline.setTag(date);

		}

		public String getDate() {
			return date;
		}

	}

}
