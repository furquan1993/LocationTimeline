package com.assassin.location;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.assassin.location.database.LocationDBHelper;

public class TimelineCursorAdapter extends CursorAdapter {

	// private double previousLat, previousLng;
	private Context context;
	private Cursor cursor;
	private LayoutInflater inflater;
	private String tableName;

	public TimelineCursorAdapter(Context context, Cursor cursor,
			boolean autoRequery, String tableName) {
		super(context, cursor, autoRequery);
		this.context = context;
		this.cursor = cursor;
		inflater = LayoutInflater.from(context);
		this.tableName = tableName;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ImageView imgMap = (ImageView) view.findViewById(R.id.imgMap);
		TextView txtLocation = (TextView) view.findViewById(R.id.txtLocation);
		TextView txtTime = (TextView) view.findViewById(R.id.txtTime);

		StringBuilder loc = new StringBuilder();
//		if (cursor.moveToNext()) {
//			loc.append(cursor.getString(1) + " - ");
//			cursor.moveToPrevious();
//		}
		
		loc.append(cursor.getString(1));

		txtTime.setText(loc);
		loc.delete(0, loc.length());

		String address = null;
		try {
			address = cursor.getString(4);
		} catch (Exception e) {
			e.printStackTrace();
		}

		double latitude = cursor.getDouble(2);
		double longitude = cursor.getDouble(3);

		if (address != null && !address.equals("")) {
			txtLocation.setText(address);
		} else {
			Location location = new Location("");
			location.setLatitude(latitude);
			location.setLongitude(longitude);
			int id = cursor.getInt(0);
			new SetAddressTask(txtLocation, id).execute(location);
		}

		// Toast.makeText(context, "Cursor: \n" + latitude + "\n" + longitude,
		// Toast.LENGTH_SHORT).show();

		String url = "http://maps.googleapis.com/maps/api/staticmap?center="
				+ latitude + "," + longitude + "&zoom=15&markers=" + latitude
				+ "," + longitude + "&size=240x160&sensor=false";

		// show The Image
		new DownloadImageTask(imgMap).execute(url);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		/* return null; */
		return inflater.inflate(R.layout.timeline_row, parent, false);
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			try {
				InputStream in = new java.net.URL(urldisplay).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {

				// Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			bmImage.setImageBitmap(result);
		}
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	class SetAddressTask extends AsyncTask<Location, Void, String> {
		TextView address;
		int id;

		public SetAddressTask(TextView tv, int id) {
			address = tv;
			this.id = id;

		}

		@Override
		protected String doInBackground(Location... params) {
			Location location = params[0];
			Geocoder reverse = new Geocoder(context);
			String loc = null;
			try {
				if (isNetworkAvailable()) {
					List<Address> addresses = reverse.getFromLocation(
							location.getLatitude(), location.getLongitude(), 1);
					if (!addresses.isEmpty()) {
						StringBuilder address = new StringBuilder();
						for (Address add : addresses) {
							address.append(add.getAddressLine(0) + "\n");
							address.append(add.getAddressLine(1) + "\n");
							address.append(add.getAddressLine(2));
						}
						loc = (address.toString());
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LocationDBHelper dbHelper = new LocationDBHelper(context);
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues cv = new ContentValues();
			cv.put("address", loc);
			String[] whereArgs = new String[] { String.valueOf(id) };
			db.update(tableName, cv, "_id=?", whereArgs);

			return loc;
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				address.setText(result);
			}
		}

	}

}
