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

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Class that binds earthquake data from the USGS API service to the ListView
 */
public class QuakeAdapter extends ArrayAdapter<Quake> {

    /**
     * The filter definition String from the USGS service is to determine whether or not there is
     * a location offset present ("10km SSW of Basilisa, Philippines")
     */
    private static final String LOCATION_SEPARATOR = " of ";

    public QuakeAdapter(Activity context, ArrayList<Quake> earthquakes) {
        // Initialize the ArrayAdapter's internal storage
        super(context, 0, earthquakes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.earthquake_list_item, parent, false);
        }

        // Initiate user's shared preferences for customising the ListView item
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        // Get the Quake object at this position in the list
        Quake currentEarthquake = getItem(position);

        // Get the original location String from the Quake object
        String originalLocation = currentEarthquake.getQuakeLocation();

        // If the original location String (i.e. "10km SSW of Basilisa, Philippines") contains
        // a primary location (i.e. "Basilisa, Philippines") and an offset (i.e. "10km SSW of "),
        // store the primary location and the offset separately into 2 different Strings
        String primaryLocation;
        String locationOffset;

        // Check whether the originalLocation String contains the " of " text
        if (originalLocation.contains(LOCATION_SEPARATOR)) {
            // Split the String into different parts based on the " of " text;
            // String #1 shows as "10km SSW" and String #2 shows as "Basilisa, Philippines"
            String[] parts = originalLocation.split(LOCATION_SEPARATOR);
            // Location offset should be "10km SSW" + " of " --> "10km SSW of "
            locationOffset = parts[0] + LOCATION_SEPARATOR;
            // Primary location should be "Basilisa, Philippines"
            primaryLocation = parts[1];
        }
        else {
            // No " of " text detected in the originalLocation String, instead display "Near the"
            locationOffset = getContext().getString(R.string.near_the);
            // The primary location will be the full location String
            primaryLocation = originalLocation;
        }

        // Find a reference to the list_location_city TextView
        TextView primaryTextView =
                (TextView) listItemView.findViewById(R.id.list_location_city);
        // Populate the primaryTextView with the earthquake's location
        primaryTextView.setText(primaryLocation);

        // Find a reference to the list_location_heading TextView
        TextView secondaryTextView =
                (TextView) listItemView.findViewById(R.id.list_location_heading);
        // Change the secondaryTextView color to textColorEarthquakeDetails
        secondaryTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.textColorEarthquakeDetails));
        // Populate the secondaryTextView with the earthquake's location offset
        secondaryTextView.setText(locationOffset);

        // Find a reference to the list_magnitude TextView
        TextView magnitudeView = (TextView) listItemView.findViewById(R.id.list_magnitude);
        // Format the magnitude to show 1 decimal place
        String formattedMagnitude = formatMagnitude(currentEarthquake.getQuakeMagnitude());
        // Populate the magnitudeView with the earthquake's magnitude
        magnitudeView.setText(formattedMagnitude);

        // Find a reference to the list_time TextView
        TextView thirdView = (TextView) listItemView.findViewById(R.id.list_time);
        // Find a reference to the list_date TextView
        TextView fourthView = (TextView) listItemView.findViewById(R.id.list_date);
        // Obtain user's preference on how to display the earthquake times
        String timePref = prefs.getString("time_preference", "ago");

        if (timePref.equals("ago")) {
            // Populate the thirdView TextView with the earthquake time in "Ago" format
            thirdView.setText(getTimeAgo(currentEarthquake.getQuakeUnixTime()));
            // Empty the fourthView TextView
            fourthView.setText("");
        }
        else {
            // Populate the thirdView TextView with the earthquake time
            thirdView.setText(getStandardTime(currentEarthquake.getQuakeUnixTime()));
            // Populate the fourthView TextView with the earthquake date
            fourthView.setText(getDate(currentEarthquake.getQuakeUnixTime()));
        }

        // Obtain user's preference on whether to display 'Tsunami Watch' theme
        String tsunamiPref = prefs.getString("tsunami_watch_theme", "enabled");
        // Find a reference to the drawable circle object
        GradientDrawable magnitudeCircle = (GradientDrawable) magnitudeView.getBackground();

        if (tsunamiPref.equals("enabled") && currentEarthquake.getTsunamiWarning() == 1) {
            // Display current Tsunami watches within the last 24 hours
            if (DateUtils.isToday(currentEarthquake.getQuakeUnixTime())) {
                // Change the circle color to black
                magnitudeCircle.setColor(Color.BLACK);
                // Change the secondaryTextView color to colorAccent
                secondaryTextView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
                // Populate the secondaryTextView to "Check for Tsunamis"
                secondaryTextView.setText(R.string.tsunami_header);
            }
        }
        else {
            // Reference the circle color with respect to the magnitude
            int magnitudeColor = getMagnitudeColor(currentEarthquake.getQuakeMagnitude());
            // Change the circle color
            magnitudeCircle.setColor(magnitudeColor);
        }
        // Return the whole list item layout so it can be displayed
        return listItemView;
    }

    /**
     * Helper method to convert double into String type for magnitude
     *
     * @param magnitude is the earthquake magnitude
     * @return          the magnitude as a String
     */
    private String formatMagnitude(double magnitude) {
        DecimalFormat magnitudeFormat = new DecimalFormat("0.0");
        return magnitudeFormat.format(magnitude);
    }

    /**
     * Helper method to intensify circle color with respect to magnitude
     *
     * @param magnitude is the earthquake magnitude
     * @return          the circle color as an integer
     */
    private int getMagnitudeColor(double magnitude) {
        int magnitudeColorResourceId;
        int magnitudeFloor = (int) Math.floor(magnitude);
        switch (magnitudeFloor) {
            case 0:
            case 1:
                magnitudeColorResourceId = R.color.magnitude1;
                break;
            case 2:
                magnitudeColorResourceId = R.color.magnitude2;
                break;
            case 3:
                magnitudeColorResourceId = R.color.magnitude3;
                break;
            case 4:
                magnitudeColorResourceId = R.color.magnitude4;
                break;
            case 5:
                magnitudeColorResourceId = R.color.magnitude5;
                break;
            case 6:
                magnitudeColorResourceId = R.color.magnitude6;
                break;
            case 7:
                magnitudeColorResourceId = R.color.magnitude7;
                break;
            case 8:
                magnitudeColorResourceId = R.color.magnitude8;
                break;
            case 9:
                magnitudeColorResourceId = R.color.magnitude9;
                break;
            default:
                magnitudeColorResourceId = R.color.magnitude10plus;
                break;
        }
        // Return the relevant circle color
        return ContextCompat.getColor(getContext(), magnitudeColorResourceId);
    }

    /**
     * Helper method for getting the earthquake time in "Ago" format
     *
     * @param time is the earthquake time as unix
     * @return     the time ago as a String
     */
    private static String getTimeAgo(long time) {
        // Internal resources for getTimeAgo method
        final int SECOND_MILLIS = 1000;
        final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
        final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
        final int DAY_MILLIS = 24 * HOUR_MILLIS;
        if (time < 1000000000000L) {
            // If timestamp is given in seconds,
            // convert it to milliseconds
            time *= 1000;
        }
        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        }
        else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        }
        else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        }
        else if (diff < 120 * MINUTE_MILLIS) {
            return "an hour ago";
        }
        else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        }
        else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        }
        else {
            return diff / DAY_MILLIS + " days ago";
        }
    }

    /**
     * Helper method for getting the earthquake date
     *
     * @param date is the earthquake time as unix
     * @return     the earthquake date as a String
     */
    private String getDate(long date) {
        // Our target date format is "Feb 11, 2017"
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        // Detect the system time zone
        sdf.setTimeZone(TimeZone.getDefault());
        // Return the local date
        return sdf.format(date);
    }

    /**
     * Helper method for getting the earthquake time
     *
     * @param time is the earthquake time as unix
     * @return     the earthquake time as a String
     */
    private String getStandardTime(long time) {
        if (DateFormat.is24HourFormat(getContext())) {
            // System is using the 24-hour time format
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            // Detect the system time zone
            sdf.setTimeZone(TimeZone.getDefault());
            // Return the local time in 24-hour format
            return sdf.format(time);
        }
        else {
            // System is using the 12-hour time format
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            // Detect the system time zone
            sdf.setTimeZone(TimeZone.getDefault());
            // Return the local time in 12-hour format
            return sdf.format(time);
        }
    }
}