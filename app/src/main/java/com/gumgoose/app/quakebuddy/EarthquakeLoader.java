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

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.List;

/**
 * Class that obtains a list of earthquakes from the server using an AsyncTask
 */
public class EarthquakeLoader extends AsyncTaskLoader<List<Quake>> {

    // Initialize local URL String
    private String mUrl;

    /**
     * Construct a new {@link EarthquakeLoader}
     *
     * @param context of the activity
     * @param url     to load data from
     */
    public EarthquakeLoader(Context context, String url) {
        super(context);
        mUrl = url;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    /**
     * This task runs on a background thread
     */
    public List<Quake> loadInBackground() {
        // If the URL is null, then return early
        if (mUrl == null) {
            return null;
        }

        // Perform the network request for fetching and parsing
        List<Quake> earthquakes = QueryUtils.fetchEarthquakeData(mUrl);
        // Return a list of earthquakes
        return earthquakes;
    }
}