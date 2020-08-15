package com.example.indoorapp;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MyApplication extends Application {

    @Override
    public void onCreate(){
        super.onCreate();

       /* Realm.init(this);
        RealmConfiguration realmConfiguration = new RealmConfiguration
                .Builder()
                .deleteRealmIfMigrationNeeded()
                .name(Realm.DEFAULT_REALM_NAME)
                .schemaVersion(1)
                .build();
        Realm.setDefaultConfiguration(realmConfiguration);*/

        Realm.init(this);
        RealmConfiguration config = new RealmConfiguration
                .Builder()
                .schemaVersion(1)
                .build();
        Realm.setDefaultConfiguration(config);

    }



}
