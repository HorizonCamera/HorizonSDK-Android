/*
 * Copyright (c) 2016. Horizon Video Technologies. All rights reserved.
 */

package com.hvt.petrakeas.simpleapp;

import android.app.Application;

import com.hvt.horizonSDK.HorizonSDK;

public class SimpleApp extends Application {

    // You need to contact us to get a development or a purchased key for your app
    private static final String APIKey = "m8W7J+sSoGogrwibc/TCXX/M8h9XOFozFIQ1r03zhOmpOK9Bcfde8jUrEXthYmPfHn434/WNyjfF82i0Nv5cCmNvbS5odnQucGV0cmFrZWFzLnNpbXBsZWFwcHxOT3xhbmRyb2lk";

    @Override
    public void onCreate() {
        super.onCreate();
        HorizonSDK.init(getApplicationContext(), APIKey);
    }
}
