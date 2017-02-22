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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Activity that displays a website within the QuakeBuddy app
 *
 * The launching of WebViewActivity expects a "url" intent extra provision
 * All callers of WebViewActivity should check the validity of the URL
 */
public class WebViewActivity extends AppCompatActivity {

    // URL intent extra String
    private String quakeUrl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview_activity);

        // Locate and load the "url" extra String
        Bundle extras = getIntent().getExtras();
        quakeUrl = extras.getString("url");

        // Find and setup the WebView object in the layout
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());

        // Set some behavioural parameters on the WebView
        WebSettings viewSettings = webView.getSettings();
        viewSettings.setJavaScriptEnabled(true);
        viewSettings.setBuiltInZoomControls(true);
        viewSettings.setDisplayZoomControls(false);
        viewSettings.setLoadWithOverviewMode(true);
        viewSettings.setUseWideViewPort(true);

        // Begin loading the website URL
        webView.loadUrl(quakeUrl);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // If "Share" in the menu is clicked on
            case R.id.action_share_url:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Get Quake Buddy on Google Play");
                shareIntent.putExtra(Intent.EXTRA_TEXT, quakeUrl);
                startActivity(Intent.createChooser(shareIntent, "Share"));
                return true;

            // If "Open in browser" in the menu is clicked on
            case R.id.action_open_web_browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setData(Uri.parse(quakeUrl));
                startActivity(browserIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview, menu);
        return true;
    }
}