package com.assassin.location;

import com.google.android.gms.maps.MapView;
import com.google.android.maps.MapActivity;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class MapViewActivity extends MapActivity {

	private MapView map;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map_view);

		map = (MapView) findViewById(R.id.mapView);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
