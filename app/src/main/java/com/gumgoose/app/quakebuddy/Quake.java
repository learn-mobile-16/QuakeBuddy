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

public class Quake {

    // Magnitude of the earthquake
    private Double mQuakeMagnitude;

    // Location of the earthquake
    private String mQuakeLocation;

    // Time of the earthquake
    private Long mQuakeUnixTime;

    // Tsunami warning of the earthquake
    private int mWarnTsunami;

    // URL address of the earthquake
    private String mQuakeURL;

    /**
     * Create a new {@link Quake} object
     *
     * @param quakeMagnitude is a double with the magnitude of the earthquake
     * @param quakeLocation  is a String with the location of the earthquake
     * @param quakeUnixTime  is a long with the unix time of the earthquake
     * @param warnTsunami    is a binary integer that tells whether there is a tsunami warning
     * @param quakeURL       is a String with the URL address of the earthquake's details
     */
    public Quake(Double quakeMagnitude, String quakeLocation, Long quakeUnixTime,
                 int warnTsunami, String quakeURL) {
        mQuakeMagnitude = quakeMagnitude;
        mQuakeLocation = quakeLocation;
        mQuakeUnixTime = quakeUnixTime;
        mWarnTsunami = warnTsunami;
        mQuakeURL = quakeURL;
    }

    /** Getter method for magnitude of the earthquake */
    public double getQuakeMagnitude() {
        return mQuakeMagnitude;
    }

    /** Getter method for location of the earthquake */
    public String getQuakeLocation() {
        return mQuakeLocation;
    }

    /** Getter method for time of the earthquake */
    public Long getQuakeUnixTime() {
        return mQuakeUnixTime;
    }

    /** Getter method for earthquake tsunami warning */
    public int getTsunamiWarning() {
        return mWarnTsunami;
    }

    /** Getter method for URL address of the earthquake */
    public String getQuakeURL() {
        return mQuakeURL;
    }
}