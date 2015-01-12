package cat.xojan.fittracker.workout;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import cat.xojan.fittracker.Constant;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.googlefit.FitnessController;
import cat.xojan.fittracker.result.ResultFragment;

public class MapController {

    private static GoogleMap mMap;
    private static LatLngBounds.Builder mBoundsBuilder;
    private static boolean isTracking;
    private static boolean isPaused;
    private static LatLng oldPosition;
    private static View mView;
    private FragmentActivity mFragmentActivity;
    private int mLapIndex;
    private List<MarkerOptions> mMarkerList;
    private static MapController instance = null;
    private static List<PolylineOptions> mPolylines;
    private LocationListener mFirstLocationListener;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private SensorManager mSensorManager;
    private Sensor mPressure;

    public MapController() {}

    public static MapController getInstance() {
        if(instance == null) {
            instance = new MapController();
        }
        return instance;
    }

    public void init(GoogleMap map, FragmentActivity activity, View view) {
        //init variables
        mFragmentActivity = activity;
        isPaused = false;
        isTracking = false;
        mView = view;
        mLapIndex = 0;
        mMarkerList = new ArrayList<>();
        mPolylines = new ArrayList<>();

        //init google map
        mMap = map;
        mMap.clear();
        mMap.setPadding(40, 280, 40, 120);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        //init buttons
        mView.findViewById(R.id.waiting_gps_bar).setVisibility(View.VISIBLE);

        // get SensorManager instance.
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        //register first location listener
        mLocationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        getFirstLocation();
    }

    private void getFirstLocation() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mFirstLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Log.i(Constant.TAG, "Got First Location");
                oldPosition = new LatLng(location.getLatitude(), location.getLongitude());
                updateMap(location.getLatitude(), location.getLongitude());
                mLocationManager.removeUpdates(mFirstLocationListener);
                getLocationUpdates();
                showStartButton();
                ElevationController.getInstance().setFirstAltitude(mAltitude);
                mSensorManager.registerListener(mSensorListener, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
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
        });
    }

    private void getLocationUpdates() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, mLocationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                updateTrack(location);
                updateMap(location.getLatitude(), location.getLongitude());
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
        });
    }

    private static void showStartButton() {
        mView.findViewById(R.id.start_bar).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.waiting_gps_bar).setVisibility(View.GONE);
    }

    private void updateTrack(Location location) {
        LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
        if (isTracking) {
            //create polyline with last location
            addMapPolyline(new PolylineOptions()
                    .geodesic(true)
                    .add(oldPosition)
                    .add(currentPosition)
                    .width(6)
                    .color(Color.BLACK));

            DistanceController.getInstance().updateDistance(oldPosition, currentPosition);
            ElevationController.getInstance().updateElevationGain(mAltitude);
            SpeedController.getInstance().updateSpeed();
            FitnessController.getInstance().storeLocation(location, mAltitude);
        }
        if (isTracking || isPaused) {
            mBoundsBuilder.include(currentPosition);
        }
        oldPosition = currentPosition;
    }

    private static void addMapPolyline(PolylineOptions polylineOptions) {
        mMap.addPolyline(polylineOptions);
        mPolylines.add(polylineOptions);
    }

    private static void updateMap(double latitude, double longitude) {
        if (isTracking || isPaused) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBoundsBuilder.build(), 0));
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
        }
    }

    public void start() {
        //change buttons visibility
        mView.findViewById(R.id.lap_pause_bar).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.start_bar).setVisibility(View.GONE);

        //init google maps settings
        mBoundsBuilder = new LatLngBounds.Builder();
        addStartMarker();
    }

    private void addStartMarker() {
        isTracking = true;
        LatLng position = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
        addMapMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .position(position)
                .title(String.valueOf(mFragmentActivity.getText(R.string.start))));

        mBoundsBuilder.include(position);
        oldPosition = position;

        FitnessController.getInstance().storeLocation(getCurrentLocation(), mAltitude);
    }

    private void addMapMarker(MarkerOptions markerOptions) {
        mMap.addMarker(markerOptions);
        mMarkerList.add(markerOptions);
    }

    public void lap() {
        addLapMarker();
        FitnessController.getInstance().storeLocation(getCurrentLocation(), mAltitude);
    }

    private void addLapMarker() {
        mLapIndex++;
        LatLng position = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
        addMapMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .position(position)
                .title(mFragmentActivity.getText(R.string.lap).toString() + " " + mLapIndex));
        mBoundsBuilder.include(position);
    }

    public void pause() {
        isPaused = true;
        isTracking = false;

        //change buttons visibility
        mView.findViewById(R.id.resume_finish_bar).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.lap_pause_bar).setVisibility(View.GONE);

        addFinishMarker();
    }

    private void addFinishMarker() {
        LatLng position = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
        addMapMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .position(position)
                .title(String.valueOf(mFragmentActivity.getText(R.string.finish))));
        mBoundsBuilder.include(position);
    }

    public void resume() {
        isPaused = false;
        isTracking = true;

        //change buttons visibility
        mView.findViewById(R.id.lap_pause_bar).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.resume_finish_bar).setVisibility(View.GONE);

        addStartMarker();
    }

    public void finish() {
        //remove location listener
        mLocationManager.removeUpdates(mLocationListener);

        //remove sensor listener
        mSensorManager.unregisterListener(mSensorListener);

        //change buttons visibility
        mView.findViewById(R.id.resume_finish_bar).setVisibility(View.GONE);

        //show results
        mFragmentActivity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ResultFragment(), Constant.RESULT_FRAGMENT_TAG)
                .commit();
    }

    public void exit() {
        if (mSensorListener != null) {
            mSensorManager.unregisterListener(mSensorListener);
        }
        if (mLocationListener != null)
            mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.removeUpdates(mFirstLocationListener);
    }

    public LatLngBounds getBounds() {
        return mBoundsBuilder.build();
    }

    public List<MarkerOptions> getMarkers() {
        return mMarkerList;
    }

    public List<PolylineOptions> getPolylines() {
        return mPolylines;
    }

    public void addKmMarker(String unitCounter) {
        addMapMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                .position(new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude()))
                .title(unitCounter));
    }

    private Location getCurrentLocation() {
        return mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    private float mAltitude;
    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
             mAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public LatLng getLastPosition() {
        return new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
    }
}
