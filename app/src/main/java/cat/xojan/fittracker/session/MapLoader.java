package cat.xojan.fittracker.session;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapLoader extends AsyncTask<List<DataPoint>, Void, Boolean>{

    private final GoogleMap mMap;
    private final LatLngBounds.Builder mBoundsBuilder;
    private final Context mContext;
    private List<DataPoint> mSpeedDataPoints;
    private List<DataPoint> mLocationDataPoints;
    private List<DataPoint> mSegmentDataPoints;
    private List<PolylineOptions> mPolyList;
    private List<MarkerOptions> mMarkerList;

    public MapLoader(GoogleMap map, Context context) {
        mMap = map;
        mBoundsBuilder = new LatLngBounds.Builder();
        mContext = context;
        mPolyList = new ArrayList<>();
        mMarkerList = new ArrayList<>();
    }

    @Override
    protected Boolean doInBackground(List<DataPoint>... params) {
        if (params[0] == null || params[2] == null) {
            return false;
        }
        mLocationDataPoints = params[0];
        mSegmentDataPoints = params[2];

        if (params[1] != null && params[1].size() > 0) {
            mSpeedDataPoints = params[1];
        }

        for (DataPoint segment : mSegmentDataPoints) {
            PolylineOptions trackOptions = new PolylineOptions();
            boolean first = true;
            LatLng lastPosition = null;

            for (DataPoint dp : mLocationDataPoints) {
                if (dp.getStartTime(TimeUnit.MILLISECONDS) >= segment.getStartTime(TimeUnit.MILLISECONDS) &&
                        dp.getStartTime(TimeUnit.MILLISECONDS) <= segment.getEndTime(TimeUnit.MILLISECONDS)) {
                    LatLng currentPosition = new LatLng(dp.getValue(Field.FIELD_LATITUDE).asFloat(),
                            dp.getValue(Field.FIELD_LONGITUDE).asFloat());
                    mBoundsBuilder.include(currentPosition);
                    trackOptions.add(currentPosition);
                    if (first) {
                        mMarkerList.add(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .position(new LatLng(currentPosition.latitude, currentPosition.longitude)));
                        first = false;
                    }
                    lastPosition = currentPosition;
                }
            }
            if (lastPosition != null) {
                mMarkerList.add(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        .position(new LatLng(lastPosition.latitude, lastPosition.longitude)));
            }
            mPolyList.add(trackOptions);
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            for (PolylineOptions po : mPolyList) {
                mMap.addPolyline(po
                        .geodesic(true)
                        .width(6)
                        .color(Color.BLACK));
            }

            for (MarkerOptions mo : mMarkerList) {
                mMap.addMarker(mo);
            }
        }

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mBoundsBuilder.build(), 5));

                    if (mSpeedDataPoints != null) {
                        new SpeedMapLoader(mMap, mContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                mLocationDataPoints,
                                mSpeedDataPoints,
                                mSegmentDataPoints);
                    }
                }
            });
    }


}
