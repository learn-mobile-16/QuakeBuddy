/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gumgoose.app.quakebuddy;

import android.app.Dialog;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EarthquakeActivity extends AppCompatActivity implements LoaderCallbacks<List<Quake>>,
        SwipeRefreshLayout.OnRefreshListener {

    /**
     * Resources for monitoring the QuakeBuddy app version
     */
    private static final String PRIVATE_PREF = "version_check";
    private static final String VERSION_KEY = "version_number";

    /**
     * Base URL String for obtaining earthquake data from USGS dataset
     */
    private static final String USGS_BASE_URL =
            "https://earthquake.usgs.gov/fdsnws/event/1/query";

    /**
     * Unique ID for app Loader
     */
    private static final int EARTHQUAKE_LOADER_ID = 1;

    /**
     * Swipe to refresh enabler
     */
    SwipeRefreshLayout swipe;

    private QuakeAdapter mAdapter;
    private TextView mEmptyTextView;
    private View mEmptyStateView;
    private View mLoadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.earthquake_activity);

        // Monitor upgrades in QuakeBuddy and trigger the changes dialog
        detectVersionChange();

        // Find a reference to the ListView in the layout
        ListView earthquakeListView = (ListView) findViewById(R.id.list);

        // Find a reference to the loading indicator in the layout
        mLoadingIndicator = findViewById(R.id.loading_indicator);

        // Find a reference to the empty list View placeholder
        mEmptyStateView = findViewById(R.id.empty_view);

        // Find a reference to the empty list TextView placeholder
        mEmptyTextView = (TextView) findViewById(R.id.empty_message);

        // Set the empty state View to display when the ListView is empty
        earthquakeListView.setEmptyView(mEmptyStateView);

        // Make a new adapter and enable it on the earthquake ListView
        mAdapter = new QuakeAdapter(this, new ArrayList<Quake>());
        earthquakeListView.setAdapter(mAdapter);

        // Find a reference to the swipe to refresh feature
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipe.setOnRefreshListener(EarthquakeActivity.this);
        swipe.setColorSchemeColors(getResources().getColor(R.color.colorAccent));

        // Set an OnItemClick listener on the ListView
        earthquakeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Fetch the USGS earthquake URL and check whether the URL is valid
                String quakeUrl = mAdapter.getItem(position).getQuakeURL();

                if (URLUtil.isValidUrl(quakeUrl)) {
                    // URL is valid, start WebViewActivity with URL intent
                    Intent websiteIntent =
                            new Intent(EarthquakeActivity.this, WebViewActivity.class);
                    websiteIntent.putExtra("url", quakeUrl);
                    startActivity(websiteIntent);
                }
                else {
                    // Inform the user by Toast message and exit gracefully
                    Toast.makeText(EarthquakeActivity.this, R.string.error_invalid_url,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Connect to the ConnectivityManager system service
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check whether there is an active network connection
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // Network detected, initialize the Loader
            LoaderManager loaderManager = getLoaderManager();
            loaderManager.initLoader(EARTHQUAKE_LOADER_ID, null, this);
        } else {
            // Display the empty View on ListView with no internet message
            mLoadingIndicator.setVisibility(View.GONE);
            swipe.setRefreshing(false);
            mEmptyTextView.setText(R.string.no_internet_connection);
            mEmptyStateView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRefresh() {
        // Refresh the earthquake ListView with current data
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check whether there is an active network connection
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            // Network detected, restart the Loader
            getLoaderManager().restartLoader(EARTHQUAKE_LOADER_ID, null, this);
        }
        else {
            // Display the empty View on ListView with no internet message
            swipe.setRefreshing(false);
            mEmptyTextView.setText(R.string.no_internet_connection);
            mEmptyStateView.setVisibility(View.VISIBLE);
        }
    }

    private void showWhatsNewDialog() {
        // Display the change log for QuakeBuddy on-screen
        Dialog dialog = new Dialog(this, android.R.style.Theme_Dialog);
        dialog.setCanceledOnTouchOutside(true);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setContentView(R.layout.dialog_change_log);
        dialog.show();
    }

    private static void openGooglePlay(Context context) {
        // Open Google Play app or browser equivalent to QuakeBuddy's page
        Intent rateIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + context.getPackageName()));
        boolean marketFound = false;

        // Find all apps able to handle the rating intent
        final List<ResolveInfo> otherApps = context.getPackageManager()
                .queryIntentActivities(rateIntent, 0);
        for (ResolveInfo otherApp: otherApps) {
            // Locate the Google Play app
            if (otherApp.activityInfo.applicationInfo.packageName
                    .equals("com.android.vending")) {
                ActivityInfo otherAppActivity = otherApp.activityInfo;
                ComponentName componentName = new ComponentName(
                        otherAppActivity.applicationInfo.packageName,
                        otherAppActivity.name
                );
                // Ensure Google Play opens on a new task
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                // Reparent the task if needed
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                // Handle if Google Play is already open
                rateIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                // Ensure that only Google Play catches the intent
                rateIntent.setComponent(componentName);
                context.startActivity(rateIntent);
                marketFound = true;
                break;
            }
        }
        if (!marketFound) {
            // Google Play was not found, open web browser instead
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + context.getPackageName()));
            context.startActivity(webIntent);
        }
    }

    /**
     * Calculate the starting date for earthquake results based on user's preference setting
     *
     * @param timePeriod is the user's String preference for the search time period
     * @return           the period's starting date as a String
     */
    private static String startDateCalculator(String timePeriod) {
        //
        SimpleDateFormat startDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Calendar calendar = Calendar.getInstance();
        switch(timePeriod) {
            case "24": // User has chosen "up to 24 hours"
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                break;
            case "48": // User has chosen "up to 48 hours"
                calendar.add(Calendar.DAY_OF_YEAR, -2);
                break;
            case "7": // User has chosen "within this Week"
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case "14": // User has chosen "within this Fortnight"
                calendar.add(Calendar.DAY_OF_YEAR, -14);
                break;
        }
        // Return the starting date as a String
        return startDate.format(new Date(calendar.getTimeInMillis()));
    }

    /**
     * @param loader      is the Loader that has finished
     * @param earthquakes is the earthquake data generated by the Loader
     */
    @Override
    public void onLoadFinished(Loader<List<Quake>> loader, List<Quake> earthquakes) {
        // Loader finished, hide all loading indicators from the screen
        mLoadingIndicator.setVisibility(View.GONE);
        swipe.setRefreshing(false);

        // Populate the mEmptyTextView to display no earthquakes message
        mEmptyTextView.setText(R.string.no_earthquakes);

        // Clear out existing data from the Adapter
        mAdapter.clear();

        // If there are earthquakes to be displayed,
        // trigger the Adapter to add, and hence update the ListView
        if (earthquakes != null && !earthquakes.isEmpty()) {
            mAdapter.addAll(earthquakes);
        }
        else {
            // Set the empty View on ListView to be visible
            mEmptyStateView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param id   is the ID of the Loader to be created
     * @param args are arguments supplied by the caller
     * @return     a new instance of the Loader to the LoaderManager
     */
    @Override
    public Loader<List<Quake>> onCreateLoader(int id, Bundle args) {
        // Loader created, obtain user's shared preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String timePeriod = sharedPrefs.getString(
                getString(R.string.settings_time_period_key),
                getString(R.string.settings_time_period_default));

        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));

        String minMagnitude = sharedPrefs.getString(
                getString(R.string.settings_min_magnitude_key),
                getString(R.string.settings_min_magnitude_default));

        // Create a URL builder object for querying the server
        Uri baseUri = Uri.parse(USGS_BASE_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query parameters to the URL builder
        uriBuilder.appendQueryParameter("format", "geojson");
        uriBuilder.appendQueryParameter("starttime", startDateCalculator(timePeriod));
        uriBuilder.appendQueryParameter("limit", getString(R.string.display_in_view_quantity));
        uriBuilder.appendQueryParameter("minmagnitude", minMagnitude);
        uriBuilder.appendQueryParameter("orderby", orderBy);

        // Hide the empty state View while Loader is running
        mEmptyStateView.setVisibility(View.INVISIBLE);

        // Start Loader and pass in the complete query URL String
        return new EarthquakeLoader(this, uriBuilder.toString());
    }

    /**
     * @param loader is the Loader that is being reset
     */
    @Override
    public void onLoaderReset(Loader<List<Quake>> loader) {
        // Clear out existing data from the Adapter
        mAdapter.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // If "Settings" in the menu is clicked on
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;

            // If "Tsunami watch" in the menu is clicked on
            case R.id.action_tsunami_watch:
                Intent websiteIntent =
                        new Intent(EarthquakeActivity.this, WebViewActivity.class);
                websiteIntent.putExtra("url", "http://ptwc.weather.gov");
                startActivity(websiteIntent);
                return true;

            // If "Version log" in the menu is clicked on
            case R.id.action_version_log:
                showWhatsNewDialog();
                return true;

            // If "Rate this app" in the menu is clicked on
            case R.id.action_rate_app:
                openGooglePlay(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void detectVersionChange() {
        // Monitor QuakeBuddy app and display the change log dialog if this version is newer
        SharedPreferences sharedPref = getSharedPreferences(PRIVATE_PREF, Context.MODE_PRIVATE);
        int thisVersionNumber = 0;
        int savedVersionNumber = sharedPref.getInt(VERSION_KEY, 0);

        // Get the current package info; check whether QuakeBuddy is a newer version
        try {
            PackageInfo app = getPackageManager().getPackageInfo(getPackageName(), 0);
            thisVersionNumber = app.versionCode;
        }
        catch (Exception e) {
            // Error caught, do nothing with it
        }
        if (thisVersionNumber > savedVersionNumber) {
            // QuakeBuddy is running a new version, display the change log dialog only once
            showWhatsNewDialog();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(VERSION_KEY, thisVersionNumber);
            editor.commit();
        }
        // Nothing new in this version of QuakeBuddy, exit gracefully
    }
}