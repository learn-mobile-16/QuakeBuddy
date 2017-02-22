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

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that tries an HTTP request, parses the data,
 * then returns the earthquake data for binding within the app
 */
public final class QueryUtils {

    /**
     * Class name String for Log messages
     */
    public static final String LOG_TAG = QueryUtils.class.getSimpleName();

    private QueryUtils() {
        // Unused, empty constructor
    }

    /**
     * Check the URL matches URL conventions
     *
     * @param stringUrl is the URL given by the Loader
     * @return          the validated URL object
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        }
        catch (MalformedURLException e) {
            // Error caught, print the exception to the logs
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request on the URL and return a String as the response
     *
     * @param url          is the URL object
     * @return             the formatted JSON response String
     * @throws IOException is an expected operation error, which is caught automatically
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            }
            else {
                Log.e(LOG_TAG, "HTTP error response code: " + urlConnection.getResponseCode());
            }
        }
        catch (IOException e) {
            // Error caught, print the exception to the logs
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results", e);
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies that an IOException
                // could be thrown
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Method for converting the InputStream into a String that contains the JSON response
     *
     * @param inputStream  is the InputStream object
     * @return             a JSON response String
     * @throws IOException is an expected operation error, which is caught automatically
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Parses the JSON response and returns a list of earthquakes
     *
     * @param earthquakeJSON is the JSON response String
     * @return               a list of earthquakes
     */
    public static List<Quake> extractFeatureFromJson(String earthquakeJSON) {
        // If the JSON String is empty or null, then return early
        if (TextUtils.isEmpty(earthquakeJSON)) {
            return null;
        }

        // Create an empty ArrayList
        ArrayList<Quake> earthquakes = new ArrayList<>();

        try {
            // Try to parse the earthquake JSON response
            JSONObject data = new JSONObject(earthquakeJSON);
            JSONArray earthQuakeArray = data.getJSONArray("features");

            for (int i = 0; i < earthQuakeArray.length(); i++) {
                String url = null;
                JSONObject currentEarthquake = earthQuakeArray.getJSONObject(i);
                JSONObject properties = currentEarthquake.getJSONObject("properties");
                double magnitude = properties.getDouble("mag");
                String location = properties.getString("place");
                long unix_time = properties.getLong("time");
                int warning = properties.getInt("tsunami");
                if(properties.has("url")) {
                    url = properties.getString("url");
                }
                Quake earthquake = new Quake(magnitude, location, unix_time, warning, url);
                earthquakes.add(earthquake);
            }
        }
        catch (JSONException e) {
            // Error caught, print the exception to the logs
            Log.e(LOG_TAG, "Problem with parsing the earthquake JSON results", e);
        }

        // Return a list of earthquakes
        return earthquakes;
    }

    /**
     * Caller method for interacting with other methods in this class
     *
     * @param requestUrl is the URL provided by the Loader
     * @return           the list of earthquakes
     */
    public static List<Quake> fetchEarthquakeData(String requestUrl) {
        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request on the URL in order to receive a JSON response
        String jsonResponse = null;

        try {
            jsonResponse = makeHttpRequest(url);
        }
        catch (IOException e) {
            // Error caught, print the exception to the logs
            Log.e(LOG_TAG, "Problem with making the HTTP request", e);
        }

        // Parse JSON response to get a list of earthquakes
        List<Quake> earthquakes = extractFeatureFromJson(jsonResponse);

        // Return the list of earthquakes
        return earthquakes;
    }
}