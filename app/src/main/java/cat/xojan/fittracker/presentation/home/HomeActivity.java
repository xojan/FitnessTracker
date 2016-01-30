package cat.xojan.fittracker.presentation.home;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import cat.xojan.fittracker.R;
import cat.xojan.fittracker.injection.component.AppComponent;
import cat.xojan.fittracker.injection.component.DaggerHomeComponent;
import cat.xojan.fittracker.injection.component.HomeComponent;
import cat.xojan.fittracker.injection.module.BaseActivityModule;
import cat.xojan.fittracker.injection.module.HomeModule;
import cat.xojan.fittracker.presentation.BaseActivity;

public class HomeActivity extends BaseActivity implements
        MenuAdapter.MenuClickListener {

    @Inject
    HomePresenter mPresenter;

    @Bind(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @Bind(R.id.left_drawer)
    RecyclerView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        mTitle = mDrawerTitle = getTitle();
        mPlanetTitles = new String[]{"test1", "test2"};

        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new MenuAdapter(mPlanetTitles, this));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(
                        this,
                        mDrawerLayout,
                        R.string.drawer_open,
                        R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
    }

    @Override
    protected void injectComponent(AppComponent appComponent,
                                   BaseActivityModule baseActivityModule) {
        HomeComponent component = DaggerHomeComponent.builder()
                .appComponent(appComponent)
                .baseActivityModule(baseActivityModule)
                .homeModule(new HomeModule())
                .build();

        component.inject(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.resume();
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new MainFragment();

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.content_frame, fragment);
        ft.commit();

        // update selected item title, then close the drawer
        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void onClick(View view, int position) {

    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        }
    }
}