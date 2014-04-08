package com.assassin.location;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.assassin.location.database.LocationDBHelper;

public class GPSService extends Service {

	public static boolean shouldUpdate = true;

	private final static int updateInterval = 1000 * 60 * 1; // minutes.
	private final static int updateDistance = 1; // meters

	private Location prevLocation;

	private Context context;
	private LocationManager locationManager;
	// private NotificationManager notificationManager;
	private MyLocationListener locationListener;
	private Handler uiHandler;
	private LocationDBHelper dbHelper;

	@Override
	public void onCreate() {
		this.context = getApplicationContext();
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		// notificationManager = (NotificationManager)
		// getSystemService(NOTIFICATION_SERVICE);
		locationListener = new MyLocationListener();
		uiHandler = new Handler(Looper.getMainLooper());
		dbHelper = new LocationDBHelper(context);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/*
		 * Thread LocationUpdater = new Thread(new Runnable() { public void
		 * run() { while(true){ updateDatabase(); sleep
		 * 
		 * } });
		 */
		prevLocation = getLocation();
		// uiHandler.post(new PostLocationRunnable(prevLocation, context));
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener);
	}

	/*
	 * private void updateDatabase() {
	 * 
	 * }
	 */
	public Location getLocation() {
		Location location = null;

		if (isNetworkEnabled()) {
			if (locationManager == null)
				locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, updateInterval,
					updateDistance, locationListener);
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		} else {
			notifyToEnableInternet();
		}

		if (isGPSEnabled()) {
			if (locationManager == null)
				locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, updateInterval,
					updateDistance, locationListener);
			location = locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else {
			notifyToEnableGPS();
		}

		/*
		 * Calendar today = Calendar.getInstance();
		 * 
		 * String tableName = "" + today.get(Calendar.YEAR) + "-" +
		 * (today.get(Calendar.MONTH) + 1) + "-" +
		 * today.get(Calendar.DAY_OF_MONTH);
		 * 
		 * String time = "" + today.get(Calendar.HOUR_OF_DAY) + ":" +
		 * today.get(Calendar.MINUTE) +":" + today.get(Calendar.SECOND);
		 * 
		 * String sql = "CREATE TABLE IF NOT EXISTS \'" + tableName +
		 * "\' ( _id integer primary key autoincrement, time TEXT, latitude REAL, longitude REAL);"
		 * ;
		 * 
		 * SQLiteDatabase locationDb = dbHelper.getWritableDatabase();
		 * locationDb.execSQL(sql);
		 * 
		 * ContentValues cv = new ContentValues(); cv.put("time", time);
		 * cv.put("latitude", location.getLatitude()); cv.put("longitude",
		 * location.getLongitude());
		 * 
		 * locationDb.insert(tableName, null, cv);
		 */
		if ((isGPSEnabled() || isNetworkEnabled()) == false) {
			uiHandler.post(new Runnable() {

				@Override
				public void run() {
					AlertDialog.Builder alertBuilder = new AlertDialog.Builder(
							context);
					alertBuilder.setTitle("Enable Location Service");
					alertBuilder
							.setMessage("You need to enable location service inorder for this application to work correctly. Please enable the network service by going to the settings. Don't forget to press back once done");
					alertBuilder.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).setPositiveButton("Settings",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									context.startActivity(new Intent(
											Settings.ACTION_LOCATION_SOURCE_SETTINGS));
									dialog.dismiss();
								}
							});
					alertBuilder.show();
				}
			});
		}

		return location;
	}

	// Notify to enable GPS
	private void notifyToEnableGPS() {
		NotificationManager notMan = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				context);

		NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();

		String notiTitle = "Enable GPS";
		String notiText = "Enable GPS satellites for better location data and accuracy";
		bigText.setBigContentTitle(notiTitle).bigText(notiText);

		notBuilder.setContentText(notiText).setContentTitle(notiTitle)
				.setSmallIcon(R.drawable.ic_launcher);

		PendingIntent gpsIntent = PendingIntent.getActivity(context, 0,
				new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
				PendingIntent.FLAG_UPDATE_CURRENT);

		notBuilder.setContentIntent(gpsIntent);
		notBuilder.setStyle(bigText);

		Notification gpsNotification = notBuilder.build();
		gpsNotification.flags |= Notification.FLAG_SHOW_LIGHTS
				| Notification.FLAG_AUTO_CANCEL;

		notMan.notify(0, gpsNotification);

	}

	// Notify to enable Internet

	private void notifyToEnableInternet() {
		NotificationManager notMan = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(
				context);

		String notiTitle = "Turn on Internet";
		String notiText = "Turn on internet connection to enable location access";

		NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
		bigText.setBigContentTitle(notiTitle).bigText(notiText);

		notBuilder.setContentText(notiText).setContentTitle(notiTitle)
				.setSmallIcon(R.drawable.ic_launcher);

		PendingIntent networkIntent = PendingIntent.getActivity(context, 0,
				new Intent(Settings.ACTION_WIRELESS_SETTINGS),
				PendingIntent.FLAG_UPDATE_CURRENT);

		notBuilder.setContentIntent(networkIntent);

		Notification networkNotification = notBuilder.build();
		networkNotification.flags |= Notification.FLAG_SHOW_LIGHTS
				| Notification.FLAG_AUTO_CANCEL;

		notMan.notify(0, networkNotification);

	}

	private boolean isGPSEnabled() {
		if (locationManager == null)
			locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isNetworkEnabled() {
		if (locationManager == null)
			locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			return true;
		else
			return false;
	}

	// Check if the location is changed.
	private boolean withinRadius(double newLat, double newLng, double radius) {

		double previousLat = prevLocation.getLatitude();
		double previousLng = prevLocation.getLongitude();

		Double latDistance = Math.toRadians(newLat - previousLat);
		Double lonDistance = Math.toRadians(newLng - previousLng);

		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(previousLat))
				* Math.cos(Math.toRadians(newLat)) * Math.sin(lonDistance / 2)
				* Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		Double distance = (6371.00 * c) * 1000;

		// Toast.makeText(context, distance.toString(),
		// Toast.LENGTH_SHORT).show();

		return (distance <= 100);

	}

	private String getAddress(Location loc) {
		Geocoder reverse = new Geocoder(context);
		StringBuilder finalAddress = new StringBuilder();
		try {
			if (isNetworkAvailable()) {
				List<Address> addresses = reverse.getFromLocation(
						loc.getLatitude(), loc.getLongitude(), 1);
				if (!addresses.isEmpty()) {
					StringBuilder address = new StringBuilder();
					for (Address add : addresses) {
						address.append(add.getAddressLine(0) + "\n");
						address.append(add.getAddressLine(1) + "\n");
						address.append(add.getAddressLine(2));
					}
					finalAddress.append(address.toString());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (finalAddress.length() > 0) {
			return finalAddress.toString();
		}

		return null;
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			

			Calendar today = Calendar.getInstance();

			// String tableName = "\'" + today.get(Calendar.YEAR) + "-"
			// + (today.get(Calendar.MONTH) + 1) + "-"
			// + today.get(Calendar.DAY_OF_MONTH) + "\'";

			String time = "" + today.get(Calendar.HOUR_OF_DAY) + ":"
					+ today.get(Calendar.MINUTE) + ":"
					+ today.get(Calendar.SECOND);

			if (!withinRadius(location.getLatitude(), location.getLongitude(),
					.01)) {

				String address = getAddress(location);

				ContentValues cv = new ContentValues();
				cv.put("time", time);
				cv.put("latitude", location.getLatitude());
				cv.put("longitude", location.getLongitude());
				cv.put("address", address);

				new WriteToDatabase().execute(cv);

				prevLocation = location;
				
				uiHandler.post(new PostLocationRunnable(location, context));

			}
			// locationDb.close();

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	}

	class PostLocationRunnable implements Runnable {

		private Location loc;
		private Context context;

		public PostLocationRunnable(Location loc, Context context) {
			this.loc = loc;
			this.context = context;
		}

		@Override
		public void run() {
			String message = "I Follow\nLatitude: " + loc.getLatitude()
					+ "\nLongitude: " + loc.getLongitude();
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
		}

	}

	class WriteToDatabase extends AsyncTask<ContentValues, Void, Boolean> {

		@Override
		protected Boolean doInBackground(ContentValues... params) {
			ContentValues cv = params[0];
			SQLiteDatabase db = dbHelper.getWritableDatabase();

			Calendar today = Calendar.getInstance();
			String tableName = "\'" + today.get(Calendar.YEAR) + "-"
					+ (today.get(Calendar.MONTH) + 1) + "-"
					+ today.get(Calendar.DAY_OF_MONTH) + "\'";

			String sql = "CREATE TABLE IF NOT EXISTS "
					+ tableName
					+ " ( _id integer primary key autoincrement, time TEXT, latitude REAL, longitude REAL, address TEXT);";

			db.execSQL(sql);

			long id = db.insert(tableName, null, cv);
			db.close();
			if (id < 0)
				return false;
			else
				return true;

			// return true;
		}

	}

}
