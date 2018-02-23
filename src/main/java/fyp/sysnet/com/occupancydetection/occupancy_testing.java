package fyp.sysnet.com.occupancydetection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class occupancy_testing extends Activity {

    private int total_rooms = 0;
    private int total_points = 0;
    private int total_samples = 0;
    private int valueOfK = 0;
    private String[] roomNames;
    rooms house = new rooms();
    private int setting = 0;

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
    public class DataLoader extends AsyncTask<Void, Void, String> {
        String output = "Room:1 Point:1 Sample:1";
        private String currentRoom = new String();
        int numberOfTuple = 0;
        int numberOfSamples = 0;
        int numberOfPoints = 0;
        int roomNumber = 0;
        BufferedReader br;

        @Override
        protected void onPreExecute(){
        }

        @Override
        protected String doInBackground(Void... v) {
            try {
                InputStream fis = openFileInput("data.dat");
                br = new BufferedReader(new InputStreamReader(fis));
            } catch (Exception e) {
                e.printStackTrace();
            }
            while(roomNumber < total_rooms) {
                numberOfPoints = 0;
                points tempPoints = new points();
                while (numberOfPoints < total_points) {
                    samples tempSamples = new samples();
                    numberOfSamples = 0;
                    while (numberOfSamples < total_samples) {
                        sample tempSample = new sample();
                        tempSample.tuple = getTupleFromFile();
                        if(tempSample.tuple == null) {
                            return "Data Loading Complete";   //end of file reached
                        }
                        //if(!tempSamples.compare(tempSample)) {
                            tempSamples.all_samples.put(numberOfSamples, tempSample);
                            numberOfSamples++;
                        //}
                    }
                    tempPoints.all_points.put(numberOfPoints, tempSamples);
                    numberOfPoints++;
                }
                house.all_rooms.put(currentRoom, tempPoints);
                roomNumber++;
            }
            return "";
        }

        @Override
        protected  void onProgressUpdate(Void... s) {
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(getApplicationContext(), "Data Loading Complete", Toast.LENGTH_SHORT).show();
        }

        public Map<String,Integer> getTupleFromFile() {
            String line = new String();
            try {
                line = br.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(!line.equals("eof/eof/eof/eof/eof+\n")) {
                Map<String, Integer> tuple = new HashMap<String, Integer>();
                String[] tok = line.split("/");
                currentRoom = tok[0];
                String[] tupleString = tok[3].split("#");
                for (int i = 0; i < tupleString.length; i++) {
                    String[] bssid_ss = tupleString[i].split("@");
                    //System.out.println("ZXCV: "+tupleString[i]);
                    tuple.put(bssid_ss[0], Integer.parseInt(bssid_ss[1]));
                }
                return tuple;
            }
            return null;
        }
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
    public class Locator extends AsyncTask<Void, Void, ArrayList<Distance>> {

        String output = new String();
        Map<String, Integer> currentTuple = getCurrentTuple();

        @Override
        protected void onPreExecute(){
        }

        public String getProbableRoom(ArrayList<Distance> allDistances) {
            Map<String,Integer> finalRoom = getMapWithFrequency(allDistances);
            int max = 0;
            String roomName = "";
            //get room with most frequency among top k
            for (Map.Entry<String, Integer> room : finalRoom.entrySet()) {
                temp2 += room.getKey()+" "+room.getValue()+"\n";
                if(room.getValue() > max) {
                    max = room.getValue();
                    roomName = room.getKey();
                }
            }
            return roomName;
        }
        public Map<String,Integer> getMapWithFrequency(ArrayList<Distance> allDistances) {
            int frequency = 0;
            Map<String,Integer> finalRoom = new HashMap<String,Integer>();
            for(int i=0;i<allDistances.size();i++) {
                if(i < valueOfK) {
                    for (int j = 0; j < allDistances.size(); j++) {
                        if (j < valueOfK) {
                            if (allDistances.get(i).room.equals(allDistances.get(j).room)) {
                                frequency++;
                            }
                        }
                    }
                    finalRoom.put(allDistances.get(i).room, frequency);
                }
            }
            return finalRoom;
        }

        String temp2;

        @Override
        protected ArrayList<Distance> doInBackground(Void... v) {
            currentTuple = getCurrentTuple();
            /*ArrayList<Distance> allDistances = whereIsThis(setting);
            String room = getProbableRoom(allDistances);
            output = "CurrentRoom: "+room;*/
            //System.out.println("setting: "+setting);
            ArrayList<Distance> allDistances = whereIsThisUsingMissingTestAndMissingTraining(true,true);
            int frequency = 0;
            Map<String,Integer> finalRoom = new HashMap<String,Integer>();
            for(int i=0;i<allDistances.size();i++) {

                for(int j=0;j<allDistances.size();j++) {
                    if(j < valueOfK) {
                        if (allDistances.get(i).room.equals(allDistances.get(j).room)) {
                            frequency++;
                        }
                    }
                }
                finalRoom.put(allDistances.get(i).room,frequency);
            }
            int max = 0;
            String key = "";
            for (Map.Entry<String, Integer> room : finalRoom.entrySet()) {
                if(room.getValue() > max) {
                    max = room.getValue();
                    key = room.getKey();
                }
            }


            output = "Current Room: "+key;
/*            for(int i=0;i<allDistances.size();i++) {
                if(i<valueOfK) {
                    output += allDistances.get(i).room + "/" + allDistances.get(i).point + "/" + allDistances.get(i).distance + "\n";
                }
            }*/
            return allDistances;
        }

        @Override
        protected  void onProgressUpdate(Void... s) {
        }

        @Override
        protected void onPostExecute(ArrayList<Distance> result) {
            TextView textView = (TextView) findViewById(R.id.location_info);
            textView.setText(output);

            TextView temp = (TextView) findViewById(R.id.testTextView);
            temp.setText(temp2);
        }


        public ArrayList<Distance> whereIsThis(int setting) {
            switch(setting) {
                case 1: //missing AP from Test, use -100 in place of it
                    return whereIsThisUsingMissingTest(false);
                case 2: //missing AP from Test, ignore AP from training
                    return whereIsThisUsingMissingTest(true);
                case 3: //missing AP from Training, use -100 in place of it
                    return whereIsThisUsingMissingTraining(false);
                case 4: //missing AP from Training, ignore AP from test
                    return whereIsThisUsingMissingTraining(true);
                case 5: //replace both test and tuple
                    return whereIsThisUsingMissingTestAndMissingTraining(false,false);
                case 6: //replace, ignore
                    return whereIsThisUsingMissingTestAndMissingTraining(false,true);
                case 7: //ignore,replace
                    return whereIsThisUsingMissingTestAndMissingTraining(true,false);
                case 8: //ignore,ignore
                    return whereIsThisUsingMissingTestAndMissingTraining(true,true);
                default:
                    return null;
            }
        }

        public ArrayList<Distance> whereIsThisUsingMissingTest(boolean ignore) {    //if not ignore, use -100 in place of missing Test, else ignore from training
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
                            if(!ignore) {
                                if (!found) {
                                    double temp = (rssi_data.getValue() - (-100)) * (rssi_data.getValue() - (-100));
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

        public ArrayList<Distance> whereIsThisUsingMissingTestAndMissingTraining(boolean ignoreFromTraining, boolean ignoreFromTesting) {    //if not ignore, use -100 in place of missing Test, else ignore from training
            System.out.println("zxcvbn");
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

        public ArrayList<Distance> whereIsThisUsingMissingTraining(boolean ignore) {    //if not ignore, use -100 in place of missing Training, else ignore from Testing
            ArrayList<Distance> allDistances1 = new ArrayList<Distance>();
            ArrayList<Double> differencesSquared = new ArrayList<Double>();

            for (Map.Entry<String, points> room : house.all_rooms.entrySet()) {
                for (Map.Entry<Integer,samples> point : room.getValue().all_points.entrySet()) {
                    for (Map.Entry<Integer,sample> tuple : point.getValue().all_samples.entrySet()){
                        for (Map.Entry<String, Integer> tempCurrentTuple : currentTuple.entrySet()) {
                            boolean found = false;
                            for (Map.Entry<String, Integer> rssi_data : tuple.getValue().tuple.entrySet()) {
                                if (tempCurrentTuple.getKey().equals(rssi_data.getKey())) {
                                    double temp = (tempCurrentTuple.getValue() - rssi_data.getValue()) * (tempCurrentTuple.getValue() - rssi_data.getValue());
                                    differencesSquared.add(temp);
                                    found = true;
                                }
                            }
                            if(!ignore) {
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

            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testing_layout);
        total_rooms = getIntent().getIntExtra("total_rooms",total_rooms);
        total_points = getIntent().getIntExtra("total_points",total_points);
        total_samples = getIntent().getIntExtra("total_samples",total_samples);
        valueOfK = getIntent().getIntExtra("valueOfK",valueOfK);
        Toast.makeText(getApplicationContext(), total_rooms + "," + total_points + "," + total_samples+","+valueOfK, Toast.LENGTH_LONG).show();
    }

    public void onButtonClickLoadFiles(View v) {
        DataLoader dataLoader = new DataLoader();
        dataLoader.execute();
    }

    public void onButtonClickWhereButton(View v) {
        Locator locator = new Locator();
        locator.execute();
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.replaceTestRadioButton:
                if (checked)
                    setting = 1;
                    break;
            case R.id.ignoreFromTrainingRadioButton:
                if (checked)
                    setting = 2;
                    break;
            case R.id.replaceTrainingRadioButton:
                if (checked)
                    setting = 3;
                break;
            case R.id.ignoreFromTestingRadioButton:
                if (checked)
                    setting = 4;
                break;
            case R.id.replaceBothRadioButton:
                if (checked)
                    setting = 5;
                break;
            case R.id.replaceTestIgnoreTestRadioButton:
                if (checked)
                    setting = 6;
                break;
            case R.id.ignoreTrainingReplaceTrainingRadioButton:
                if (checked)
                    setting = 7;
                break;
            case R.id.ignoreBothTrainingRadioButton:
                if (checked)
                    setting = 8;
                break;
        }

    }
}
