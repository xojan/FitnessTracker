package cat.xojan.fittracker.presentation.workout;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import javax.inject.Inject;

import butterknife.ButterKnife;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.domain.ActivityType;
import cat.xojan.fittracker.injection.HasComponent;
import cat.xojan.fittracker.injection.component.DaggerWorkoutComponent;
import cat.xojan.fittracker.injection.component.WorkoutComponent;
import cat.xojan.fittracker.injection.module.WorkoutModule;
import cat.xojan.fittracker.presentation.BaseActivity;
import cat.xojan.fittracker.util.LocationFetcher;

import static cat.xojan.fittracker.util.LocationFetcher.LocationChangedListener;


public class WorkoutActivity extends BaseActivity implements HasComponent,
        OnMapReadyCallback,
        LocationChangedListener {

    public static final String FITNESS_ACTIVITY = "fitness_activity";

    @Inject
    LocationFetcher mLocationFetcher;
    @Inject
    WorkoutPresenter mWorkoutPresenter;
    @Inject
    MapPresenter mMapPresenter;

    private WorkoutComponent mComponent;
    private AppBarLayout mAppBarLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        initializeInjector();
        ButterKnife.bind(this);

        ActivityType activityType = (ActivityType) getIntent().getExtras().get(FITNESS_ACTIVITY);
        setTitle(activityType.name().toLowerCase());

        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(this);
        mLocationFetcher.setLocationListener(this);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.appbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        mLocationFetcher.stop();
    }

    private void initializeInjector() {
        mComponent = DaggerWorkoutComponent.builder()
                .appComponent(getApplicationComponent())
                .baseActivityModule(getActivityModule())
                .workoutModule(new WorkoutModule())
                .build();
        mComponent.inject(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMapPresenter.setUp(map);
        Location location = mLocationFetcher.getLocation();
        if (location != null) {
            goToLocation(location);
        } else {
            mLocationFetcher.start();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        goToLocation(location);
    }

    @Override
    public Object getComponent() {
        return mComponent;
    }

    private void goToLocation(Location location) {
        mMapPresenter.goToLocation(location);
        startWorkout();
    }

    private void startWorkout() {
        //addFragment(R.id.fragment_container, new WorkoutFragment());
        mAppBarLayout.setExpanded(false, true);
    }
}