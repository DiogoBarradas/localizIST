package com.example.indoorapp;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.indoorapp.model.Coordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    TextView display;
    Button save;
    EditText position;
    Realm realm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = (TextView) findViewById(R.id.display);
        save = (Button) findViewById(R.id.save);
        position = (EditText) findViewById(R.id.position);


        Log.d(TAG, "OnCreate: View Initialization done");

        realm = Realm.getDefaultInstance();



        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
                readData();
            }
        });


    }

    public void deleteDB(View view){
        realm.beginTransaction();
        realm.deleteAll();
        realm.commitTransaction();
    }


    public void saveData() {


        /*String displayMessage = display.getText().toString();
        String[] lines = displayMessage.split("\r\n|\r|\n");
        Toast.makeText(this, Arrays.toString(lines), Toast.LENGTH_LONG).show();*/

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {

                //check the coordinates in database to find position duplicates
                RealmResults<Coordinate> coordinates = bgRealm.where(Coordinate.class).findAll();
                int flag = 0;
                for (Coordinate coordinateConfirm : coordinates) {
                    if (coordinateConfirm.getPosition().equals(position.getText().toString())) {
                        flag = 1; //já existe aquele registo
                    }
                }
                if (flag == 0) {
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);

                   //sends to this maps the total of signal strenght/ bssid and number of the same bssid found
                    //goal to make the mean of 5 scans
                    Map<String, Integer> totalBSSID = new HashMap<String, Integer>();
                    Map<String, Integer> nrBSSID = new HashMap<String, Integer>();
                    for(int i=1;i<5;i++){
                        wifiManager.setWifiEnabled(false);
                        wifiManager.setWifiEnabled(true);
                        List<ScanResult> wifiList = wifiManager.getScanResults();
                        for (ScanResult scanResult : wifiList) {
                            if(scanResult.SSID.equals("eduroam")){

                                if(totalBSSID.containsKey(scanResult.BSSID)){
                                    totalBSSID.put(scanResult.BSSID,totalBSSID.get(scanResult.BSSID)+scanResult.level);
                                    nrBSSID.put(scanResult.BSSID,nrBSSID.get(scanResult.BSSID)+1);
                                }
                                else {
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
                    String chaveMax1 ="";
                    String chaveMax2 ="";
                    String chaveMax3 ="";
                    for(String chave:totalBSSID.keySet()){
                        if (nrBSSID.get(chave) > max1) {
                            max3 = max2; max2 = max1; max1 = nrBSSID.get(chave); chaveMax1 = chave;
                        }
                        else if (nrBSSID.get(chave) > max2) {
                            max3 = max2; max2 = nrBSSID.get(chave); chaveMax2 = chave;
                        }
                        else if (nrBSSID.get(chave) > max3) {
                            max3 = nrBSSID.get(chave); chaveMax3 = chave;
                        }
                    }

                    int signalS1=totalBSSID.get(chaveMax1)/nrBSSID.get(chaveMax1);
                    int signalS2=totalBSSID.get(chaveMax2)/nrBSSID.get(chaveMax2);
                    int signalS3=totalBSSID.get(chaveMax3)/nrBSSID.get(chaveMax3);

                    finalWifiList.put(chaveMax1,signalS1);
                    finalWifiList.put(chaveMax2,signalS2);
                    finalWifiList.put(chaveMax3,signalS3);


                    for (String bssid:finalWifiList.keySet()) {
                        if(!position.getText().toString().isEmpty()){ //not let insert empty position
                                Coordinate coordinate = bgRealm.createObject(Coordinate.class);
                                coordinate.setBssid(bssid);
                                coordinate.setSignal_Strenght(finalWifiList.get(bssid) + "");
                                coordinate.setPosition(position.getText().toString());
                        }
                    }
                }
            }



        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                // Transaction was a success.
                Log.d(TAG, "onSuccess: Data Written Successfully");
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                // Transaction failed and was automatically canceled.
                Log.d(TAG, "onError: Error Occured");
            }

        });

    }    /*-----------for-------------*/


    public void readData() {
        RealmResults<Coordinate> coordinates = realm.where(Coordinate.class).findAll();
        display.setText("");
        String data = "";


        for (Coordinate coordinate : coordinates) {
            try {
                Log.d(TAG, "readData: Reading Data");
                data = data + "\n" + coordinate.toString();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        display.setText(data);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


   public void locate(View view) {
        //scan the five strongest eduroam AP's and store in newwifilist
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(getApplicationContext().WIFI_SERVICE);

       Map<String, Integer> totalBSSID = new HashMap<String, Integer>();
       Map<String, Integer> nrBSSID = new HashMap<String, Integer>();
       for(int i=1;i<5;i++){
           wifiManager.setWifiEnabled(false);
           wifiManager.setWifiEnabled(true);
           List<ScanResult> wifiList = wifiManager.getScanResults();
           for (ScanResult scanResult : wifiList) {
               if(scanResult.SSID.equals("eduroam")){

                   if(totalBSSID.containsKey(scanResult.BSSID)){
                       totalBSSID.put(scanResult.BSSID,totalBSSID.get(scanResult.BSSID)+scanResult.level);
                       nrBSSID.put(scanResult.BSSID,nrBSSID.get(scanResult.BSSID)+1);
                   }
                   else {
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
       int min1 = Integer.MAX_VALUE;
       int min2 = Integer.MAX_VALUE;
       int min3 = Integer.MAX_VALUE;
       String chaveMax1 ="";
       String chaveMax2 ="";
       String chaveMax3 ="";
       for(String chave:totalBSSID.keySet()){
           if (nrBSSID.get(chave) > max1) {
               max3 = max2; max2 = max1; max1 = nrBSSID.get(chave); chaveMax1 = chave;
           }
           else if (nrBSSID.get(chave) > max2) {
               max3 = max2; max2 = nrBSSID.get(chave); chaveMax2 = chave;
           }
           else if (nrBSSID.get(chave) > max3) {
               max3 = nrBSSID.get(chave); chaveMax3 = chave;
           }
       }
       int signalS1=totalBSSID.get(chaveMax1)/nrBSSID.get(chaveMax1);
       int signalS2=totalBSSID.get(chaveMax2)/nrBSSID.get(chaveMax2);
       int signalS3=totalBSSID.get(chaveMax3)/nrBSSID.get(chaveMax3);

       finalWifiList.put(chaveMax1,signalS1);
       finalWifiList.put(chaveMax2,signalS2);
       finalWifiList.put(chaveMax3,signalS3);


       //get the coordinates stored on database with matching bssid's from scan
       RealmQuery<Coordinate> query = realm.where(Coordinate.class);
       query.equalTo("bssid", chaveMax1);
       query.or().equalTo("bssid", chaveMax2);
       query.or().equalTo("bssid", chaveMax3);


       RealmResults<Coordinate> result =query.findAll();
       /*for (Coordinate coordinateLocate : result) {
           Toast.makeText(this, coordinateLocate.getBssid(), Toast.LENGTH_SHORT).show();}*/

       Map<String, Integer> localizacaoDesvioTotal = new HashMap<String, Integer>();
       Map<String, Integer> localizacaoCounter = new HashMap<String, Integer>();


        //populate the two hashmaps, one with the total distance for each location of the SS in each equal bssid
       //the other with the number of bssid matches for each location
       for (Coordinate coordinateLocate : result) {
            //Toast.makeText(this, coordinateLocate.getBssid(), Toast.LENGTH_SHORT).show();
           for (String bssid:finalWifiList.keySet()) {
                if(coordinateLocate.getBssid().equals(bssid)){
                    int desvio=Math.abs(Integer.parseInt(coordinateLocate.getSignal_Strenght())-finalWifiList.get(bssid));
                    if(localizacaoDesvioTotal.containsKey(coordinateLocate.getPosition())){
                        localizacaoDesvioTotal.put(coordinateLocate.getPosition(), localizacaoDesvioTotal.get(coordinateLocate.getPosition()) + desvio);
                        localizacaoCounter.put(coordinateLocate.getPosition(),localizacaoCounter.get(coordinateLocate.getPosition())+1);
                    }else{
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

        int minValue=Integer.MAX_VALUE;
        String localizacao="";
        for(String chave:localizacaoDesvioTotal.keySet()){
            //Toast.makeText(this, chave, Toast.LENGTH_LONG).show();
            //Toast.makeText(this, String.valueOf(localizacaoDesvioTotal.get(chave)), Toast.LENGTH_LONG).show();
            int media=localizacaoDesvioTotal.get(chave)/localizacaoCounter.get(chave);
            if(media<minValue){
                minValue=media;
                //Toast.makeText(this,"minvalue:"+ String.valueOf(minValue), Toast.LENGTH_LONG).show();
                localizacao=chave;
           }
       }

       //Toast.makeText(this, "antes "+ localizacao, Toast.LENGTH_LONG).show();


        //Nova pagina com foto de localizacao
       Intent mapa = new Intent(this,ImageActivity.class);
       mapa.putExtra("localizacao", localizacao);
       startActivity(mapa);

    }

    public void scan(View view) {
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
        display.setText(message);
    }
}
