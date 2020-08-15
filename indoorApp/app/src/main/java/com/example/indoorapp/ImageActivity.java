package com.example.indoorapp;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.indoorapp.model.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class ImageActivity extends AppCompatActivity {
    Realm realm;
    int flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        realm = Realm.getDefaultInstance();

        //Access Location from another activity

            String localizacao = getIntent().getStringExtra("localizacao");




            int local1 = 0;
            int local2 = 0;

            if (!localizacao.equals("")) {
                local1 = Integer.parseInt(localizacao.substring(0, 1));
                local2 = Integer.parseInt(localizacao.substring(2, 3));

                local1 = local1 - 1;
                local2 = local2 - 1;
            }

            //para aceder a casa certa da matriz


            //vmatriz de transformacao localizacao - margins
            int[][][] transform = { { {230, 620, 56, 10}, {477, 470, 56, 10}, {730, 620, 56, 10} },
                                    { {570, 200, 56, 10}, {820, 200, 56, 10} }
            };

            int left = transform[local1][local2][0];
            int top = transform[local1][local2][1];
            int right = transform[local1][local2][2];
            int bottom = transform[local1][local2][3];

        /*Toast.makeText(this,String.valueOf(left), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(top), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(right), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(bottom), Toast.LENGTH_LONG).show();*/


            //Set the location image in the right place
            ImageView movingCircle = (ImageView) findViewById(R.id.circle);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
            params.setMargins(left, top, right, bottom);
            movingCircle.setLayoutParams(params);


        }

        public void goBack (View view){
            Intent startNewActivity = new Intent(this, MainActivity.class);
            startActivity(startNewActivity);
        }

        public void locateAgain (View view){
            //scan the five strongest eduroam AP's and store in newwifilist
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);

            Map<String, Integer> totalBSSID = new HashMap<String, Integer>();
            Map<String, Integer> nrBSSID = new HashMap<String, Integer>();
            for (int i = 1; i < 5; i++) {
                wifiManager.setWifiEnabled(false);
                wifiManager.setWifiEnabled(true);
                List<ScanResult> wifiList = wifiManager.getScanResults();
                for (ScanResult scanResult : wifiList) {
                    if (scanResult.SSID.equals("eduroam")) {

                        if (totalBSSID.containsKey(scanResult.BSSID)) {
                            totalBSSID.put(scanResult.BSSID, totalBSSID.get(scanResult.BSSID) + scanResult.level);
                            nrBSSID.put(scanResult.BSSID, nrBSSID.get(scanResult.BSSID) + 1);
                        } else {
                            totalBSSID.put(scanResult.BSSID, scanResult.level);
                            nrBSSID.put(scanResult.BSSID, 1);
                        }
                    }
                }
            }
            //find the 3 most frequent APs and calculate the mean signal strenght
            Map<String, Integer> finalWifiList = new HashMap<String, Integer>();
            int max1 = Integer.MIN_VALUE;
            int max2 = Integer.MIN_VALUE;
            int max3 = Integer.MIN_VALUE;
            String chaveMax1 = "";
            String chaveMax2 = "";
            String chaveMax3 = "";
            for (String chave : totalBSSID.keySet()) {
                if (nrBSSID.get(chave) > max1) {
                    max3 = max2;
                    max2 = max1;
                    max1 = nrBSSID.get(chave);
                    chaveMax1 = chave;
                } else if (nrBSSID.get(chave) > max2) {
                    max3 = max2;
                    max2 = nrBSSID.get(chave);
                    chaveMax2 = chave;
                } else if (nrBSSID.get(chave) > max3) {
                    max3 = nrBSSID.get(chave);
                    chaveMax3 = chave;
                }
            }
            int signalS1 = totalBSSID.get(chaveMax1) / nrBSSID.get(chaveMax1);
            int signalS2 = totalBSSID.get(chaveMax2) / nrBSSID.get(chaveMax2);
            int signalS3 = totalBSSID.get(chaveMax3) / nrBSSID.get(chaveMax3);

            finalWifiList.put(chaveMax1, signalS1);
            finalWifiList.put(chaveMax2, signalS2);
            finalWifiList.put(chaveMax3, signalS3);


            //get the coordinates stored on database with matching bssid's from scan
            RealmQuery<Coordinate> query = realm.where(Coordinate.class);
            query.equalTo("bssid", chaveMax1);
            query.or().equalTo("bssid", chaveMax2);
            query.or().equalTo("bssid", chaveMax3);


            RealmResults<Coordinate> result = query.findAll();
       /*for (Coordinate coordinateLocate : result) {
           Toast.makeText(this, coordinateLocate.getBssid(), Toast.LENGTH_SHORT).show();}*/

            Map<String, Integer> localizacaoDesvioTotal = new HashMap<String, Integer>();
            Map<String, Integer> localizacaoCounter = new HashMap<String, Integer>();


            //populate the two hashmaps, one with the total distance for each location of the SS in each equal bssid
            //the other with the number of bssid matches for each location
            for (Coordinate coordinateLocate : result) {
                //Toast.makeText(this, coordinateLocate.getBssid(), Toast.LENGTH_SHORT).show();
                for (String bssid : finalWifiList.keySet()) {
                    if (coordinateLocate.getBssid().equals(bssid)) {
                        int desvio = Math.abs(Integer.parseInt(coordinateLocate.getSignal_Strenght()) - finalWifiList.get(bssid));
                        if (localizacaoDesvioTotal.containsKey(coordinateLocate.getPosition())) {
                            localizacaoDesvioTotal.put(coordinateLocate.getPosition(), localizacaoDesvioTotal.get(coordinateLocate.getPosition()) + desvio);
                            localizacaoCounter.put(coordinateLocate.getPosition(), localizacaoCounter.get(coordinateLocate.getPosition()) + 1);
                        } else {
                            localizacaoDesvioTotal.put(coordinateLocate.getPosition(), desvio);
                            localizacaoCounter.put(coordinateLocate.getPosition(), 1);
                        }
                    }
                }
            }

       /*for (String key : localizacaoCounter.keySet()) {
           Integer value = localizacaoCounter.get(key);
           Toast.makeText(this, String.valueOf(value), Toast.LENGTH_LONG).show();
           Integer valuelo = localizacaoDesvioTotal.get(key);
           Toast.makeText(this, String.valueOf(valuelo), Toast.LENGTH_LONG).show();
       }*/

            //calculates the minimum mean of deviation between SS , minimum 3 bssids equal
            //maybe add +((5-localizacaoCounter.get(chave))*5) to comparing min value

            int minValue = Integer.MAX_VALUE;
            String localizacao = "";
            for (String chave : localizacaoDesvioTotal.keySet()) {
                //Toast.makeText(this, chave, Toast.LENGTH_LONG).show();
                //Toast.makeText(this, String.valueOf(localizacaoDesvioTotal.get(chave)), Toast.LENGTH_LONG).show();
                int media = localizacaoDesvioTotal.get(chave) / localizacaoCounter.get(chave);
                if (media < minValue) {
                    minValue = media;
                    //Toast.makeText(this,"minvalue:"+ String.valueOf(minValue), Toast.LENGTH_LONG).show();
                    localizacao = chave;
                }
            }

            //Toast.makeText(this, "antes " + localizacao, Toast.LENGTH_LONG).show();
            int local1 = 0;
            int local2 = 0;

            if (!localizacao.equals("")) {
                local1 = Integer.parseInt(localizacao.substring(0, 1));
                local2 = Integer.parseInt(localizacao.substring(2, 3));

                local1 = local1 - 1;
                local2 = local2 - 1;
            }

            //para aceder a casa certa da matriz


            //matriz de transformacao localizacao - margins
            int[][][] transform = { { {230, 620, 56, 10}, {477, 470, 56, 10}, {730, 620, 56, 10} },
                                    { {570, 200, 56, 10}, {820, 200, 56, 10} }
            };

            int left = transform[local1][local2][0];
            int top = transform[local1][local2][1];
            int right = transform[local1][local2][2];
            int bottom = transform[local1][local2][3];

        /*Toast.makeText(this,String.valueOf(left), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(top), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(right), Toast.LENGTH_LONG).show();
        Toast.makeText(this,String.valueOf(bottom), Toast.LENGTH_LONG).show();*/


            //Set the location image in the right place
            ImageView movingCircle = (ImageView) findViewById(R.id.circle);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
            params.setMargins(left, top, right, bottom);
            movingCircle.setLayoutParams(params);
        }
    public void sync(View view) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);

        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);


        List<ScanResult> wifiList = wifiManager.getScanResults();




        String message = "";
        int counter =0;
        for (ScanResult scanResult : wifiList) {
            if (scanResult.SSID.equals("eduroam")) {
                counter=counter+1;
                String newmessage = "";
                int signal_Strenght = scanResult.level;
                String bssid = scanResult.BSSID;
                String ssid = scanResult.SSID;
                newmessage = "Signal Strenght of " + ssid + " : " + bssid + "  is  " + signal_Strenght + "\n";
                message = message + newmessage;
                if(counter==8){
                    break;
                }
            }
        }
    }
}


