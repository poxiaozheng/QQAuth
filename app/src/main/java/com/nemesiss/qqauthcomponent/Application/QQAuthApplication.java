package com.nemesiss.qqauthcomponent.Application;

import android.app.Application;
import com.nemesiss.qqauthcomponent.Services.Auth.QQAuth;

public class QQAuthApplication extends Application {

    public QQAuth qqAuth;

    @Override
    public void onCreate() {
        super.onCreate();
        qqAuth = new QQAuth(getApplicationContext());
    }
}
