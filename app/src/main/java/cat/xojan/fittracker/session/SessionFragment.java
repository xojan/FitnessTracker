package cat.xojan.fittracker.session;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cat.xojan.fittracker.Constant;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.Utils;
import cat.xojan.fittracker.googlefit.FitnessController;

public class SessionFragment extends Fragment {

    private static Handler handler;
    private Button mDeleteSessionButton;
    private int mNumSegments;
    private List<DataPoint> mDistanceDataPoints;
    private List<DataPoint> mSpeedDataPoints;

    public static Handler getHandler() {
        return handler;
    }
    private ProgressBar mProgressBar;
    private LinearLayout mSessionView;
    private Session mSession;
    private List<DataSet> mDataSets;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_session, container, false);

        ((ActionBarActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressBar = (ProgressBar) view.findViewById(R.id.fragment_loading_spinner);
        mSessionView = (LinearLayout) view.findViewById(R.id.fragment_session_container);
        mDeleteSessionButton = (Button) view.findViewById(R.id.fragment_button_delete_session);
        showProgressBar(true);
        /**
         * session contains:
         * name, description, identifier, package name, activity type, start time, end time
         */

        Bundle bundle = this.getArguments();
        String sessionId = bundle.getString(Constant.PARAMETER_SESSION_ID, "");
        long startTime = bundle.getLong(Constant.PARAMETER_START_TIME, 0);
        long endTime = bundle.getLong(Constant.PARAMETER_END_TIME, 0);

        FitnessController.getInstance().readSession(sessionId, startTime, endTime);

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Constant.MESSAGE_SINGLE_SESSION_READ:
                        mSession = FitnessController.getInstance().getSingleSession();
                        mDataSets = FitnessController.getInstance().getSingleSessionDataSets();
                        fillViewContent(view);
                        break;
                    case Constant.MESSAGE_SESSION_DELETED:
                        showProgressBar(false);
                        getActivity().getSharedPreferences(Constant.PACKAGE_SPECIFIC_PART, Context.MODE_PRIVATE)
                                .edit().putBoolean(Constant.PARAMETER_RELOAD_LIST, true).apply();
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new SessionListFragment())
                                .commit();
                }
            }
        };

        mDeleteSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            }
        });

        return view;
    }

    private void fillViewContent(View view) {
        if (mSession.getAppPackageName().equals(Constant.PACKAGE_SPECIFIC_PART))
            mDeleteSessionButton.setVisibility(View.VISIBLE);
        else
            mDeleteSessionButton.setVisibility(View.GONE);

        ((TextView) view.findViewById(R.id.fragment_session_name)).setText(mSession.getName());
        ((TextView)view.findViewById(R.id.fragment_session_description)).setText(mSession.getDescription());
        ((TextView)view.findViewById(R.id.fragment_session_date)).setText(Utils.millisToDate(mSession.getStartTime(TimeUnit.MILLISECONDS)));
        ((TextView)view.findViewById(R.id.fragment_session_start)).setText(Utils.millisToTime(mSession.getStartTime(TimeUnit.MILLISECONDS)));
        ((TextView)view.findViewById(R.id.fragment_session_end)).setText(Utils.millisToTime(mSession.getEndTime(TimeUnit.MILLISECONDS)));
        ((TextView)view.findViewById(R.id.fragment_session_total_time)).setText(Utils.getTimeDifference(mSession.getEndTime(TimeUnit.MILLISECONDS), mSession.getStartTime(TimeUnit.MILLISECONDS)));

        for (DataSet ds : mDataSets) {
            if (ds.getDataType().equals(DataType.AGGREGATE_ACTIVITY_SUMMARY)) {
                if (ds.getDataPoints() != null && ds.getDataPoints().size() > 0) {
                    mNumSegments = ds.getDataPoints().get(0).getValue(Field.FIELD_NUM_SEGMENTS).asInt();
                }
            } else if (ds.getDataType().equals(DataType.AGGREGATE_SPEED_SUMMARY)) {
                if (ds.getDataPoints() != null && ds.getDataPoints().size() > 0) {

                    String speed = Utils.getRightSpeed(ds.getDataPoints().get(0).getValue(Field.FIELD_AVERAGE).asFloat(), getActivity().getBaseContext());
                    ((TextView) view.findViewById(R.id.fragment_session_total_speed)).setText(speed);

                    String pace = Utils.getRightPace(ds.getDataPoints().get(0).getValue(Field.FIELD_AVERAGE).asFloat(), getActivity().getBaseContext());
                    ((TextView) view.findViewById(R.id.fragment_session_total_pace)).setText(pace);
                }
            } else if (ds.getDataType().equals(DataType.TYPE_DISTANCE_DELTA)) {
                mDistanceDataPoints = ds.getDataPoints();

            } else if (ds.getDataType().equals(DataType.TYPE_SPEED)) {
                mSpeedDataPoints = ds.getDataPoints();
            }
        }

        fillIntervalTable(view);

        showProgressBar(false);
    }

    private void fillIntervalTable(View view) {
        LinearLayout intervalView = (LinearLayout) view.findViewById(R.id.fragment_session_intervals);
        intervalView.removeAllViews();

        TableLayout intervalTable = new TableLayout(getActivity());
        intervalTable.setStretchAllColumns(true);

        for (int i = 0; i < mNumSegments; i++) {
            TableRow tableRow = new TableRow(getActivity());

            TextView title = new TextView(getActivity());
            title.setText("Interval " + (i + 1));
            tableRow.addView(title);

            TextView startTime = new TextView(getActivity());
            startTime.setText(Utils.millisToTime(mDistanceDataPoints.get(i).getStartTime(TimeUnit.MILLISECONDS)));
            tableRow.addView(startTime);

            TextView endTime = new TextView(getActivity());
            endTime.setText(Utils.millisToTime(mDistanceDataPoints.get(i).getEndTime(TimeUnit.MILLISECONDS)));
            tableRow.addView(endTime);

            TextView distance = new TextView(getActivity());
            distance.setText(Utils.getRightDistance(mDistanceDataPoints.get(i).getValue(Field.FIELD_DISTANCE).asFloat(), getActivity()));
            tableRow.addView(distance);

            TextView speed = new TextView(getActivity());
            speed.setText(Utils.getRightSpeed(mSpeedDataPoints.get(i).getValue(Field.FIELD_SPEED).asFloat(), getActivity()));
            tableRow.addView(speed);

            intervalTable.addView(tableRow);
        }
        intervalView.addView(intervalTable);
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
}
