package cat.xojan.fittracker.session;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import java.util.List;
import java.util.concurrent.TimeUnit;

import cat.xojan.fittracker.Constant;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.googlefit.FitnessController;
import cat.xojan.fittracker.sessionlist.SessionListFragment;
import cat.xojan.fittracker.util.SessionDetailedDataLoader;
import cat.xojan.fittracker.util.Utils;

public class SessionActivity extends ActionBarActivity {

    private ProgressBar mProgressBar;
    private LinearLayout mSessionView;
    private static Handler handler;
    private Session mSession;
    private MenuItem mDeleteButton;
    private List<DataSet> mDataSets;
    private List<DataPoint> mLocationDataPoints;
    private List<DataPoint> mSpeedDataPoints;
    private List<DataPoint> mSegmentDataPoints;

    public static Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        Toolbar toolbar = (Toolbar) findViewById(R.id.fragment_session_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constant.MESSAGE_SINGLE_SESSION_READ:
                        mSession = FitnessController.getInstance().getSingleSessionResult();
                        if (mSession != null) {
                            if (mSession.getAppPackageName().equals(Constant.PACKAGE_SPECIFIC_PART))
                                mDeleteButton.setVisible(true);

                            mDataSets = FitnessController.getInstance().getSingleSessionDataSets();
                            fillViewContent();
                        }
                        break;
                    case Constant.MESSAGE_SESSION_DELETED:
                        showProgressBar(false);
                        finish();
                }
            }
        };

        mProgressBar = (ProgressBar) findViewById(R.id.fragment_loading_spinner);
        mSessionView = (LinearLayout) findViewById(R.id.fragment_session_container);
        showProgressBar(true);

        /**
         * session contains:
         * name, description, identifier, package name, activity type, start time, end time
         */

        Intent intent = getIntent();

        // Get the intent extras
        /*long startTime = Fitness.getStartTime(intent, TimeUnit.MILLISECONDS);
        long endTime = Fitness.getEndTime(intent, TimeUnit.MILLISECONDS);
        Session session = Session.extract(intent); TODO not working so far*/
        long startTime = intent.getLongExtra(Constant.EXTRA_START, 0);
        long endTime = intent.getLongExtra(Constant.EXTRA_END, 0);
        String sessionIdentifier = intent.getStringExtra(Constant.EXTRA_SESSION);

        // Show the session in your app
        FitnessController.getInstance().readSessionDataSets(startTime, endTime, sessionIdentifier);
    }

    private void showProgressBar(boolean b) {
        if (b) {
            mProgressBar.setVisibility(View.VISIBLE);
            mSessionView.setVisibility(View.GONE);
        }
        else {
            mProgressBar.setVisibility(View.GONE);
            mSessionView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.menu_main, menu);

        mDeleteButton = menu.findItem(R.id.action_delete);
        mDeleteButton.setVisible(false);
        MenuItem music = menu.findItem(R.id.action_music);
        music.setVisible(false);
        MenuItem settings = menu.findItem(R.id.action_settings);
        settings.setVisible(false);
        MenuItem attributions = menu.findItem(R.id.action_attributions);
        attributions.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.delete_session)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                FitnessController.getInstance().deleteSession(mSession);
                                showProgressBar(true);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();
                break;
            case android.R.id.home:
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void fillViewContent() {

        //name
        ((TextView) findViewById(R.id.fragment_session_name))
                .setText(mSession.getName());
        //description
        ((TextView)findViewById(R.id.fragment_session_description))
                .setText(mSession.getDescription());
        //date
        ((TextView)findViewById(R.id.fragment_session_date))
                .setText(Utils.getRightDate(mSession.getStartTime(TimeUnit.MILLISECONDS), this));
        //start time
        ((TextView)findViewById(R.id.fragment_session_start))
                .setText(Utils.millisToTime(mSession.getStartTime(TimeUnit.MILLISECONDS)));
        //end time
        ((TextView)findViewById(R.id.fragment_session_end))
                .setText(Utils.millisToTime(mSession.getEndTime(TimeUnit.MILLISECONDS)));
        //total/duration time
        long sessionTime = mSession.getEndTime(TimeUnit.MILLISECONDS) - mSession.getStartTime(TimeUnit.MILLISECONDS);
        ((TextView)findViewById(R.id.fragment_session_total_time))
                .setText(Utils.getTimeDifference(mSession.getEndTime(TimeUnit.MILLISECONDS), mSession.getStartTime(TimeUnit.MILLISECONDS)));
        //speed
        ((TextView) findViewById(R.id.fragment_session_total_speed)).setText(Utils.getRightSpeed(0, this));
        //pace
        ((TextView) findViewById(R.id.fragment_session_total_pace)).setText(Utils.getRightPace(0, this));

        List<DataPoint> mDistanceDataPoints = null;
        mLocationDataPoints = null;
        float speed = 0;

        for (DataSet ds : mDataSets) {
            if (ds.getDataType().equals(DataType.AGGREGATE_ACTIVITY_SUMMARY)) {
                if (ds.getDataPoints() != null && ds.getDataPoints().size() > 0) {
                    sessionTime = ds.getDataPoints().get(0).getValue(Field.FIELD_DURATION).asInt();
                    ((TextView) findViewById(R.id.fragment_session_total_time)).setText(Utils.getTimeDifference(sessionTime, 0));
                }
            } else if (ds.getDataType().equals(DataType.AGGREGATE_SPEED_SUMMARY)) {
                if (ds.getDataPoints() != null && ds.getDataPoints().size() > 0) {
                    speed = ds.getDataPoints().get(0).getValue(Field.FIELD_AVERAGE).asFloat();
                }

            } else if (ds.getDataType().equals(DataType.TYPE_DISTANCE_DELTA)) {
                mDistanceDataPoints = ds.getDataPoints();
            } else if (ds.getDataType().equals(DataType.TYPE_SPEED)) {
                mSpeedDataPoints = ds.getDataPoints();
            } else if (ds.getDataType().equals(DataType.TYPE_LOCATION_SAMPLE)) {
                mLocationDataPoints = ds.getDataPoints();
            } else if (ds.getDataType().equals(DataType.TYPE_ACTIVITY_SEGMENT)) {
                mSegmentDataPoints = ds.getDataPoints();
            }
        }

        float totalDistance = 0;
        for (DataPoint dp : mDistanceDataPoints) {
            totalDistance = totalDistance + dp.getValue(Field.FIELD_DISTANCE).asFloat();
        }

        //distance
        if (totalDistance == 0) {
            if (speed != 0) {
                totalDistance = speed * (sessionTime / 1000);
            }
        }
        ((TextView)findViewById(R.id.fragment_session_total_distance)).setText(Utils.getRightDistance(totalDistance, this));

        //speed/pace
        if (totalDistance != 0) {
            if (speed == 0) {
                speed = totalDistance / (sessionTime / 1000);
            }
            ((TextView)findViewById(R.id.fragment_session_total_speed)).setText(Utils.getRightSpeed(speed, this));
            ((TextView)findViewById(R.id.fragment_session_total_pace)).setText(Utils.getRightPace(speed, this));
        }

        showProgressBar(false);

        LinearLayout detailedView = (LinearLayout)findViewById(R.id.session_intervals);
        new SessionDetailedDataLoader(detailedView, this)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mLocationDataPoints, mSegmentDataPoints);

        if (mLocationDataPoints != null && mLocationDataPoints.size() > 0) {
            fillMap(true);
        } else {
            fillMap(false);
        }
    }

    private void fillMap(boolean fillMap) {
        MapFragment mapFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.fragment_session_map));
        if (fillMap) {
            if (mapFragment.getView() != null)
                mapFragment.getView().setVisibility(View.VISIBLE);
            final GoogleMap map = mapFragment.getMap();
            map.clear();
            map.setPadding(0, 0, 0, 0);
            map.setMyLocationEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(false);

            new MapLoader(map, this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    mLocationDataPoints, mSpeedDataPoints);

        } else {
            if (mapFragment.getView() != null)
                mapFragment.getView().setVisibility(View.GONE);
        }
    }
}