package in.osc.carpool;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import in.osc.carpool.utils.PlaceProvider;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        handleIntent(getIntent());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        switch (position) {
            case 0 : getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, LocationChooserFragment.newInstance(position + 1))
                    .commit(); break;

            case 1 : getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, CarpoolSearchFragment.newInstance(position + 1))
                    .commit(); break;
        } 

    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = "Set home/destination";
                break;
            case 2:
                mTitle = "Search for carpool";
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        LocationChooserFragment mLocationChooserFragment;
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            mLocationChooserFragment = (LocationChooserFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            mLocationChooserFragment.performSearch(intent.getStringExtra(SearchManager.QUERY));
        } else if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            mLocationChooserFragment = (LocationChooserFragment) getSupportFragmentManager().findFragmentById(R.id.container);
            mLocationChooserFragment.getPlace(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
        }
    }


}
