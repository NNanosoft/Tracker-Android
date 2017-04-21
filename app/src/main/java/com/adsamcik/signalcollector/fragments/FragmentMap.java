package com.adsamcik.signalcollector.fragments;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.adsamcik.signalcollector.utility.Assist;
import com.adsamcik.signalcollector.utility.Failure;
import com.adsamcik.signalcollector.utility.MapLayer;
import com.adsamcik.signalcollector.utility.NetworkLoader;
import com.adsamcik.signalcollector.utility.Preferences;
import com.adsamcik.signalcollector.utility.FabMenu;
import com.adsamcik.signalcollector.utility.Network;
import com.adsamcik.signalcollector.R;
import com.adsamcik.signalcollector.interfaces.ITabFragment;
import com.adsamcik.signalcollector.utility.SnackMaker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.UrlTileProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class FragmentMap extends Fragment implements OnMapReadyCallback, ITabFragment {
	private static final int MAX_ZOOM = 17;

	private static final String TAG = "SignalsMap";
	private UpdateLocationListener locationListener;
	private String type = null;
	private GoogleMap map;
	private TileProvider tileProvider;
	private LocationManager locationManager;
	private TileOverlay activeOverlay;

	private EditText searchText;

	private View view;

	private Circle userRadius;
	private Marker userCenter;

	private FloatingActionButton fabTwo, fabOne;
	private FabMenu menu;

	@Override
	public void onPermissionResponse(int requestCode, boolean success) {

	}

	@Override
	public void onHomeAction() {

	}

	/**
	 * Check if permission to access fine location is granted
	 * If not and is android 6 or newer, than it prompts you to enable it
	 *
	 * @return is permission available atm
	 */
	private boolean checkLocationPermission(Context context, boolean request) {
		if (context == null)
			return false;
		if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
			return true;
		else if (request)
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
		return false;
	}

	/**
	 * This function should be called when fragment is left
	 */
	public void onLeave(@NonNull FragmentActivity activity) {
		if (locationManager != null && checkLocationPermission(activity, false))
			locationManager.removeUpdates(locationListener);
		locationListener.cleanup();

		if (menu != null)
			menu.hideAndDestroy(activity);
	}

	/**
	 * Initializes fabs for Map fragment
	 *
	 * @param fabOne fabOne (lower)
	 * @param fabTwo fabTwo (above fabOne)
	 */
	public Failure<String> onEnter(@NonNull FragmentActivity activity, @NonNull FloatingActionButton fabOne, @NonNull FloatingActionButton fabTwo) {
		if (!Assist.isPlayServiceAvailable(activity))
			return new Failure<>(activity.getString(R.string.error_play_services_not_available));
		if (!checkLocationPermission(activity, true))
			return new Failure<>(activity.getString(R.string.error_missing_permission));

		this.fabTwo = fabTwo;
		this.fabOne = fabOne;

		fabOne.show();
		fabOne.setImageResource(R.drawable.ic_gps_fixed_black_24dp);
		fabOne.setOnClickListener(v -> {
			if (checkLocationPermission(activity, true) && map != null)
				locationListener.onMyPositionFabClick();
		});


		fabTwo.setImageResource(R.drawable.ic_layers_black_24dp);
		//fabTwo.setOnClickListener(v -> changeMapOverlay(typeIndex + 1 == availableTypes.length ? 0 : typeIndex + 1, fabTwo));
		fabTwo.setOnClickListener(v -> menu.show(activity));

		return new Failure<>();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final FragmentActivity activity = getActivity();
		if (Assist.isPlayServiceAvailable(activity) && container != null)
			view = inflater.inflate(R.layout.fragment_map, container, false);
		else
			return view = inflater.inflate(R.layout.no_play_services, container, false);

		NetworkLoader.load(Network.URL_MAPS_AVAILABLE, Assist.DAY_IN_MINUTES, activity, Preferences.PREF_AVAILABLE_MAPS, MapLayer[].class, (state, layerArray) -> {
			if (fabTwo != null && layerArray != null) {
				menu = new FabMenu((ViewGroup) container.getParent(), fabTwo, activity);
				menu.setCallback(this::changeMapOverlay);
				String savedOverlay = Preferences.get(activity).getString(Preferences.PREF_DEFAULT_MAP_OVERLAY, layerArray[0].name);
				if (!MapLayer.contains(layerArray, savedOverlay)) {
					savedOverlay = layerArray[0].name;
					Preferences.get(activity).edit().putString(Preferences.PREF_DEFAULT_MAP_OVERLAY, savedOverlay).apply();
				}

				final String defaultOverlay = savedOverlay;
				activity.runOnUiThread(() -> {
					changeMapOverlay(defaultOverlay);
					if (layerArray.length > 0) {
						for (MapLayer layer : layerArray)
							menu.addItem(layer.name, activity);
					}
					fabTwo.show();
				});
			}
		});
		searchText = ((EditText) view.findViewById(R.id.map_search));
		searchText.setOnEditorActionListener((v, actionId, event) -> {
			Geocoder geocoder = new Geocoder(getContext());
			try {
				List<Address> addresses = geocoder.getFromLocationName(v.getText().toString(), 1);
				if (addresses != null && addresses.size() > 0) {
					if (map != null && locationListener != null) {
						Address address = addresses.get(0);
						locationListener.stopUsingUserPosition(true);
						locationListener.animateToPositionZoom(new LatLng(address.getLatitude(), address.getLongitude()), 13);
					}
				}

			} catch (IOException e) {
				new SnackMaker(view).showSnackbar(R.string.error_general);
			}
			return true;
		});

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SupportMapFragment mapFragment = SupportMapFragment.newInstance();
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.container_map, mapFragment);
		fragmentTransaction.commit();
		mapFragment.getMapAsync(this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		map = null;
		view = null;
		menu = null;
	}

	/**
	 * Changes overlay of the map
	 *
	 * @param type exact case-sensitive name of the overlay
	 */
	private void changeMapOverlay(@NonNull String type) {
		if (map != null) {
			if ((!type.equals(this.type) || activeOverlay == null)) {
				if (activeOverlay != null)
					activeOverlay.remove();
				activeOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
				this.type = type;
			}
		} else this.type = type;
	}

	@Override
	public void onMapReady(GoogleMap map) {
		this.map = map;
		userRadius = null;
		userCenter = null;
		Context c = getContext();
		if (c == null)
			return;
		map.setMapStyle(MapStyleOptions.loadRawResourceStyle(c, R.raw.map_style));

		//does not work well with bearing. Known bug in Google maps api since 2014.
		//map.setPadding(0, Assist.dpToPx(c, 48 + 40 + 8), 0, 0);

		tileProvider = new UrlTileProvider(256, 256) {
			@Override
			public URL getTileUrl(int x, int y, int zoom) {
				String s = String.format(Locale.ENGLISH, Network.URL_TILES, zoom, x, y, type);

				if (!checkTileExists(x, y, zoom))
					return null;

				try {
					URL u = new URL(s);
					HttpURLConnection huc = (HttpURLConnection) u.openConnection();
					huc.setRequestMethod("HEAD");
					return huc.getResponseCode() == 200 ? u : null;
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			}

			@SuppressWarnings("BooleanMethodIsAlwaysInverted")
			private boolean checkTileExists(int x, int y, int zoom) {
				return !(zoom < 10 || zoom > MAX_ZOOM);
			}
		};

		locationListener = new UpdateLocationListener((SensorManager) c.getSystemService(Context.SENSOR_SERVICE));

		map.setMaxZoomPreference(MAX_ZOOM);
		if (checkLocationPermission(c, false)) {
			locationListener.followMyPosition = true;
			if (locationManager == null)
				locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
			Location l = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			if (l != null) {
				CameraPosition cp = CameraPosition.builder().target(new LatLng(l.getLatitude(), l.getLongitude())).zoom(16).build();
				map.moveCamera(CameraUpdateFactory.newCameraPosition(cp));
				locationListener.targetPosition = cp.target;
				DrawUserPosition(cp.target, l.getAccuracy());
			}
		}
		activeOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));
		if (type != null)
			changeMapOverlay(type);

		map.setOnMapClickListener(latLng -> {
			if (searchText.hasFocus()) {
				Assist.hideSoftKeyboard(getActivity(), searchText);
				searchText.clearFocus();
			} else if (searchText.getVisibility() == View.VISIBLE) {
				searchText.setVisibility(View.INVISIBLE);
				fabTwo.hide();
				fabOne.hide();
			} else {
				searchText.setVisibility(View.VISIBLE);
				fabTwo.show();
				fabOne.show();
			}
		});

		UiSettings uiSettings = map.getUiSettings();
		uiSettings.setMapToolbarEnabled(false);
		uiSettings.setIndoorLevelPickerEnabled(false);
		uiSettings.setCompassEnabled(false);

		locationListener.RegisterMap(map);

		if (locationManager == null)
			locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(1, 5, new Criteria(), locationListener, Looper.myLooper());

	}

	/**
	 * Draws user accuracy radius and location
	 * Is automatically initialized if no circle exists
	 *
	 * @param latlng   Latitude and longitude
	 * @param accuracy Accuracy
	 */
	private void DrawUserPosition(LatLng latlng, float accuracy) {
		if (map == null)
			return;
		if (userRadius == null) {
			Context c = getContext();
			userRadius = map.addCircle(new CircleOptions()
					.fillColor(ContextCompat.getColor(c, R.color.colorUserAccuracy))
					.center(latlng)
					.radius(accuracy)
					.zIndex(100)
					.strokeWidth(0));

			userCenter = map.addMarker(new MarkerOptions()
					.flat(true)
					.position(latlng)
					.icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_user_location))
					.anchor(0.5f, 0.5f)
			);
		} else {
			userRadius.setCenter(latlng);
			userRadius.setRadius(accuracy);
			userCenter.setPosition(latlng);
		}
	}

	private class UpdateLocationListener implements LocationListener, SensorEventListener {
		boolean followMyPosition = false;
		boolean useGyroscope = false;

		private Sensor rotationVector;
		private SensorManager sensorManager;

		private LatLng lastUserPos;
		private LatLng targetPosition;
		private float targetTilt;
		private float targetBearing;
		private float targetZoom;

		public UpdateLocationListener(@NonNull SensorManager sensorManager) {
			rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
			this.sensorManager = sensorManager;
		}

		public void RegisterMap(GoogleMap map) {
			map.setOnCameraMoveStartedListener(cameraChangeListener);
			CameraPosition cameraPosition = map.getCameraPosition();
			targetPosition = cameraPosition.target == null ? new LatLng(0, 0) : cameraPosition.target;
			targetTilt = cameraPosition.tilt;
			targetBearing = cameraPosition.bearing;
			targetZoom = cameraPosition.zoom;
		}

		public void UnregisterMap(GoogleMap map) {

		}

		private void stopUsingGyroscope(boolean returnToDefault) {
			useGyroscope = false;
			sensorManager.unregisterListener(this, rotationVector);
			targetBearing = 0;
			targetTilt = 0;
			if (returnToDefault)
				animateTo(targetPosition, targetZoom, 0, 0, DURATION_SHORT);
		}

		public void stopUsingUserPosition(boolean returnToDefault) {
			if (useGyroscope)
				stopUsingGyroscope(returnToDefault);

			if (followMyPosition)
				followMyPosition = false;
		}

		private final GoogleMap.OnCameraMoveStartedListener cameraChangeListener = i -> {
			if (followMyPosition && i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
				stopUsingUserPosition(true);
		};

		private final int DURATION_STANDARD = 1000;
		private final int DURATION_SHORT = 200;

		@Override
		public void onLocationChanged(Location location) {
			lastUserPos = new LatLng(location.getLatitude(), location.getLongitude());
			DrawUserPosition(lastUserPos, location.getAccuracy());
			if (followMyPosition && map != null)
				moveTo(lastUserPos);
		}

		private void animateToPositionZoom(LatLng position, float zoom) {
			targetPosition = position;
			targetZoom = zoom;
			animateTo(position, zoom, targetTilt, targetBearing, DURATION_STANDARD);
		}

		private void animateToBearing(float bearing) {
			animateTo(targetPosition, targetZoom, targetTilt, bearing, DURATION_SHORT);
			targetBearing = bearing;
		}

		private void animateToTilt(float tilt) {
			targetTilt = tilt;
			animateTo(targetPosition, targetZoom, tilt, targetBearing, DURATION_SHORT);
		}

		private void animateTo(LatLng position, float zoom, float tilt, float bearing, int duration) {
			CameraPosition.Builder builder = new CameraPosition.Builder(map.getCameraPosition()).target(position).zoom(zoom).tilt(tilt).bearing(bearing);
			map.animateCamera(CameraUpdateFactory.newCameraPosition(builder.build()), duration, null);
		}

		private void onMyPositionFabClick() {
			if (followMyPosition) {
				if (useGyroscope) {
					stopUsingGyroscope(true);
				} else {
					useGyroscope = true;
					sensorManager.registerListener(this, rotationVector,
							SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
					animateToTilt(45);
				}
			} else
				followMyPosition = true;

			if (lastUserPos != null)
				moveTo(lastUserPos);
		}

		private void moveTo(@NonNull LatLng latlng) {
			float zoom = map.getCameraPosition().zoom;
			animateToPositionZoom(latlng, zoom < 16 ? 16 : zoom > 17 ? 17 : zoom);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {

		}

		@Override
		public void onProviderDisabled(String provider) {

		}

		private void cleanup() {
			if (map != null)
				map.setOnMyLocationButtonClickListener(null);
		}

		float prevRotation;

		private void updateRotation(int rotation) {
			if (map != null && targetPosition != null && prevRotation != rotation) {
				animateToBearing(rotation);
			}
		}

		float[] orientation = new float[3];
		float[] rMat = new float[9];

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
				// calculate th rotation matrix
				SensorManager.getRotationMatrixFromVector(rMat, event.values);
				// get the azimuth value (orientation[0]) in degree
				updateRotation((int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360);
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	}

}
