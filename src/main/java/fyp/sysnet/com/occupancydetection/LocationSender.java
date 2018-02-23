package fyp.sysnet.com.occupancydetection;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mazhar on 4/11/2015.
 */
public class LocationSender extends Thread {

    Context context;
    rooms house = new rooms();
    int k = 0;

    public class sample{
        public Map<String,Integer> tuple;
    }
    public class samples{
        public Map<Integer,sample> all_samples = new HashMap<Integer, sample>();
        public boolean compare(sample tempSample) {
            for (Map.Entry<Integer, sample> temp1 : all_samples.entrySet()) {
                if(temp1.getValue().tuple.equals(tempSample.tuple)) {
                    return true;
                }
            }
            return false;
        }
    }
    public class points{
        public Map<Integer,samples> all_points = new HashMap<Integer, samples>();
    }
    public class rooms {
        public Map<String,points> all_rooms = new HashMap<String, points>();
    }
    public class Distance implements Comparable<Distance>{
        public String room;
        public int point;
        public double distance;

        Distance() {
            room = new String();
            point = 999;
            distance = 999;
        }

        Distance(String room, int point, double distance) {
            this.room = room;
            this.point = point;
            this.distance = distance;
        }

        @Override
        public int compareTo(Distance o) {
            return new Double(distance).compareTo( o.distance);
        }
    }
    public class finalRoom {
        public String room = new String();
        public int frequency = 0;
    }

    public LocationSender(Context context, int k) {
        this.context = context;
        this.k = k;
    }

    @Override
    public void run() {
        while (true) {
            /*DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet("");
            HttpResponse response = httpclient.execute(httpget);
            in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String result = sb.toString();
            Log.v("My Response :: ", result);*/
            /*int numberOfTimesToSend = AccelerometerService.getCounter();
            for(int i=0;i<numberOfTimesToSend;i++) {
                ArrayList<Distance> distances1 = whereIsThisUsingMissingTestAndMissingTraining(true,true);
                finalRoom[] fr = new finalRoom[k];
                for(int j=0;j<k;j++) {
                    fr[j].room = distances1.get(j).room;
                    int l = 0;
                    while(l<k) {

                    }
                }
                AccelerometerService.decrementCounter();
            }*/
        }
    }

    public ArrayList<Distance> whereIsThisUsingMissingTestAndMissingTraining(boolean ignoreFromTraining, boolean ignoreFromTesting) {    //if not ignore, use -100 in place of missing Test, else ignore from training
        Map<String, Integer> currentTuple = getCurrentTuple();

        ArrayList<Distance> allDistances1 = new ArrayList<Distance>();
        ArrayList<Double> differencesSquared = new ArrayList<Double>();

        for (Map.Entry<String, points> room : house.all_rooms.entrySet()) {
            for (Map.Entry<Integer,samples> point : room.getValue().all_points.entrySet()) {
                for (Map.Entry<Integer,sample> tuple : point.getValue().all_samples.entrySet()){
                    for (Map.Entry<String, Integer> rssi_data : tuple.getValue().tuple.entrySet()) {
                        boolean found = false;
                        for (Map.Entry<String, Integer> tempCurrentTuple : currentTuple.entrySet()) {
                            if (rssi_data.getKey().equals(tempCurrentTuple.getKey())) {
                                double temp = (tempCurrentTuple.getValue() - rssi_data.getValue()) * (tempCurrentTuple.getValue() - rssi_data.getValue());
                                differencesSquared.add(temp);
                                found = true;
                            }
                        }
                        if(!ignoreFromTraining) {
                            if (!found) {
                                double temp = (rssi_data.getValue() - (-100)) * (rssi_data.getValue() - (-100));
                                differencesSquared.add(temp);
                            }
                        }
                    }
                    for (Map.Entry<String, Integer> tempCurrentTuple : currentTuple.entrySet()) {
                        boolean found = false;
                        for (Map.Entry<String, Integer> rssi_data : tuple.getValue().tuple.entrySet()) {
                            if (rssi_data.getKey().equals(tempCurrentTuple.getKey())) {
                                found = true;
                            }
                        }
                        if(!ignoreFromTesting) {
                            if (!found) {
                                double temp = (tempCurrentTuple.getValue() - (-100)) * (tempCurrentTuple.getValue() - (-100));
                                differencesSquared.add(temp);
                            }
                        }
                    }
                    calculateDistance(room, point, differencesSquared, allDistances1);
                }
            }
        }
        Collections.sort(allDistances1);
        return allDistances1;
    }

    public void calculateDistance(Map.Entry<String,points> room, Map.Entry<Integer,samples> point, ArrayList<Double> differencesSquared, ArrayList<Distance> allDistances ) {
        double sumOfDifferencesSquared = 0;
        for (int i = 0; i < differencesSquared.size(); i++) {
            sumOfDifferencesSquared += differencesSquared.get(i);
        }
        double distance = Math.sqrt(sumOfDifferencesSquared);
        Distance d = new Distance(room.getKey(), point.getKey(), distance);
        allDistances.add(d);
        differencesSquared.clear();
    }

    public Map<String, Integer> getCurrentTuple() {

        Map <String,Integer> returnSamples = new HashMap<String,Integer>();

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            if (wifi != null) {
                if (!wifi.isWifiEnabled()) {
                    wifi.setWifiEnabled(true);
                }/*
                while(!wifi.startScan()) {

                }*/
                wifi.startScan();
                List<ScanResult> scanResults = wifi.getScanResults();
                for (int i = 0; i < scanResults.size(); i++) {
                    returnSamples.put(scanResults.get(i).BSSID, scanResults.get(i).level);
                }
                return returnSamples;
            }
        }
        catch (Exception e) {
            //return e.toString();
        }
        return null;
    }
}
