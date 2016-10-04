/*
 * Copyright (c) 2016. Horizon Video Technologies. All rights reserved.
 */

package com.hvt.petrakeas.simpleapp;

import android.app.Application;

import com.hvt.horizonSDK.HorizonSDK;

public class SimpleApp extends Application {

    // You need to contact us to get a development or a purchased key for your app
    private static final String APIKey = "1GG8kZTcnP/xNDg2PtVfCNzyVgTO+Q9MJTbiFoA0Yx8fbHAGjODNPrPAxTsrErHIqN7xg+61FOXFlGyhQgSGD2NvbS5odnQucGV0cmFrZWFzLnNpbXBsZWFwcHxOTw==";

    @Override
    public void onCreate() {
        super.onCreate();
        HorizonSDK.init(getApplicationContext(), APIKey);
    }
}
