package fyp.sysnet.com.occupancydetection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;


import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class main_menu extends ActionBarActivity {


    private boolean sendToServer = true;
    private int SETTING_REQUEST_CODE = 8;
    private int total_rooms = 0;
    private int total_points = 0;
    private int total_samples = 0;
    private int valueOfK = 0;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        //receiver for monitoring screen turned off/on
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    System.out.println("ASD: Screen Turned off");
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    System.out.println("ASD: Screen Turned on");
                }
            }
        }, intentFilter);

        //service for accelerometer
        //AccelerometerService as = new AccelerometerService();
        //AccelerometerService.start(getApplicationContext());
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onButtonClick_occupancy_view (View view)
    {
       Intent occupancy_view_intent =new Intent(main_menu.this,occupancy_view.class);
       startActivity(occupancy_view_intent);
    }

    public void onButtonClick_training_button(View v)
    {
        Intent intent = new Intent(getApplicationContext(),occupancy_training.class);
        intent.putExtra("total_rooms",total_rooms);
        intent.putExtra("total_points",total_points);
        intent.putExtra("total_samples",total_samples);
        startActivity(intent);
    }

    public void onButtonClick_testing_button(View v)
    {
        Intent testingIntent = new Intent(getApplicationContext(),occupancy_testing.class);
        testingIntent.putExtra("total_rooms",total_rooms);
        testingIntent.putExtra("total_points",total_points);
        testingIntent.putExtra("total_samples",total_samples);
        testingIntent.putExtra("valueOfK",valueOfK);
        startActivity(testingIntent);
    }

    public void onButtonClick_settingButton(View v)
    {
        Intent settings_intent = new Intent(getApplicationContext(),settings.class);
        startActivityForResult(settings_intent, SETTING_REQUEST_CODE);
    }

    public void onButtonClick_exit(View v)
    {
        sendToServer = false;
        finish();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTING_REQUEST_CODE)
        {
            if(resultCode == RESULT_OK)
            {
                total_rooms = data.getIntExtra("total_rooms",total_rooms);
                total_points = data.getIntExtra("total_points",total_points);
                total_samples = data.getIntExtra("total_samples",total_samples);
                valueOfK = data.getIntExtra("valueOfK",valueOfK);
                Toast.makeText(getApplicationContext(),total_rooms+","+total_points+","+total_samples+","+valueOfK,Toast.LENGTH_LONG).show();
            }
            if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(getApplicationContext(),"No data returned",Toast.LENGTH_SHORT).show();
            }
        }
        AccelerometerService.stop();
    }

    public void onClickEnableServer(View v) {
       /* ServerSenderThread s = new ServerSenderThread(this);
        s.run();*/
        ServerSender serverSender = new ServerSender();
        serverSender.execute();
        Toast.makeText(getApplicationContext(),"Server enabled",Toast.LENGTH_SHORT).show();
    }

    public class ServerSender extends AsyncTask<Void,Void,Void> {

        String curr = new String();
        @Override
        protected Void doInBackground(Void... params) {
            loadFromUserInformationFile();
            loadFromDataFile();
            String oldRoom = "";
            while(true) {
                //while(true) {
                currentTuple = getCurrentTuple();

                ArrayList<Distance> allDistances = whereIsThisUsingMissingTestAndMissingTraining(true, true);
                String room = getProbableRoom(allDistances);
                System.out.println("CurrentRoom: " + room);
                if(!room.equals(oldRoom)) {
                    curr = room;
                    publishProgress();
                    sendToServer(userName, room);

                    oldRoom = room;
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {

                }
            }
            //return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // might want to change "executed" for the returned string passed
            // into onPostExecute() but that is upto you
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {
            TextView temp = (TextView) findViewById(R.id.currentRoomTextView);
            temp.setText(curr);

        }
        Map<String, Integer> currentTuple;
        rooms house = new rooms();

        private String currentRoom = new String();
        int numberOfSamples = 0;
        int numberOfPoints = 0;
        int roomNumber = 0;
        BufferedReader br;

        String userName = "";
        int total_rooms = 0;
        int total_samples = 0;
        int total_points = 0;
        int valueOfK = 0;

        public class sample {
            public Map<String, Integer> tuple;
        }
        public class samples {
            public Map<Integer, sample> all_samples = new HashMap<Integer, sample>();

            public boolean compare(sample tempSample) {
                for (Map.Entry<Integer, sample> temp1 : all_samples.entrySet()) {
                    if (temp1.getValue().tuple.equals(tempSample.tuple)) {
                        return true;
                    }
                }
                return false;
            }
        }
        public class points {
            public Map<Integer, samples> all_points = new HashMap<Integer, samples>();
        }
        public class rooms {
            public Map<String, points> all_rooms = new HashMap<String, points>();
        }
        public class Distance implements Comparable<Distance> {
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
                return new Double(distance).compareTo(o.distance);
            }
        }

        public void loadFromUserInformationFile() {
            try {
                InputStream fis = openFileInput("userInfo.dat");
                BufferedReader brr = new BufferedReader(new InputStreamReader(fis));
                String line;
                while ((line = brr.readLine()) != null) {
                    String[] tok = line.split(":");
                    if (tok[0].equals("UserName")) {
                        userName = tok[1];
                    } else if (tok[0].equals("TotalRooms")) {
                        total_rooms = Integer.parseInt(tok[1]);
                    } else if (tok[0].equals("TotalPoints")) {
                        total_points = Integer.parseInt(tok[1]);
                    } else if (tok[0].equals("TotalSamples")) {
                        total_samples = Integer.parseInt(tok[1]);
                    } else if (tok[0].equals("ValueOfK")) {
                        valueOfK = Integer.parseInt(tok[1]);
                    }
                    System.out.println("asd: " + total_rooms + " " + total_points + " " + total_samples + " " + valueOfK);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public Map<String, Integer> getTupleFromFile() {
            String line = new String();
            try {
                line = br.readLine();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!line.equals("eof/eof/eof/eof/eof+\n")) {
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
        public String loadFromDataFile() {
            try {
                InputStream fis = openFileInput("data.dat");
                br = new BufferedReader(new InputStreamReader(fis));
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (roomNumber < total_rooms) {
                numberOfPoints = 0;
                points tempPoints = new points();
                while (numberOfPoints < total_points) {
                    samples tempSamples = new samples();
                    numberOfSamples = 0;
                    while (numberOfSamples < total_samples) {
                        sample tempSample = new sample();
                        tempSample.tuple = getTupleFromFile();
                        if (tempSample.tuple == null) {
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
        public String getProbableRoom(ArrayList<Distance> allDistances) {
            Map<String, Integer> finalRoom = getMapWithFrequency(allDistances);
            int max = 0;
            String roomName = "";
            //get room with most frequency among top k
            for (Map.Entry<String, Integer> room : finalRoom.entrySet()) {
                if (room.getValue() > max) {
                    max = room.getValue();
                    roomName = room.getKey();
                }

            }
            return roomName;
        }
        public Map<String, Integer> getMapWithFrequency(ArrayList<Distance> allDistances) {
            int frequency = 0;
            Map<String, Integer> finalRoom = new HashMap<String, Integer>();
            for (int i = 0; i < allDistances.size(); i++) {
                if(i < valueOfK) {
                    for (int j = 0; j < allDistances.size(); j++) {
                        if (j < valueOfK) {
                            if (allDistances.get(i).room.equals(allDistances.get(j).room)) {
                                frequency++;
                            }
                        }
                    }
                    finalRoom.put(allDistances.get(i).room, frequency);
                    frequency = 0;
                }
            }
            return finalRoom;
        }
        public void sendToServer(String username, String room) {
            try {
                    username = "hamza";
                            room ="1";
                    //String sysnetURL = "http://sysnet.org.pk/occupecny_detection/addOccupancy.php?occupant="+username+"&room="+room;
                //String sysnetURL = "http://sysnet.org.pk/occupency_detection/addOccupancy.php?occupant=zaafar&room="+room;

                String sysnetURL = "http://192.168.10.2/Android_connect/addOccupancy.php?username=" + username +"&room="+ room;
                HttpClient httpclient = new DefaultHttpClient();
                URI uri = new URI(sysnetURL);
                HttpResponse response = httpclient.execute(new HttpPut(uri));

                /*StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    String responseString = out.toString();
                    System.out.println("ASD: "+responseString);
                    out.close();
                    //..more logic
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }*/
            } catch (Exception e) {
                e.printStackTrace();
            }

        }



        public Map<String, Integer> getCurrentTuple() {

            Map<String, Integer> returnSamples = new HashMap<String, Integer>();

            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
            } catch (Exception e) {
                //return e.toString();
            }
            return null;
        }
        public ArrayList<Distance> whereIsThisUsingMissingTestAndMissingTraining(boolean ignoreFromTraining, boolean ignoreFromTesting) {    //if not ignore, use -100 in place of missing Test, else ignore from training
            ArrayList<Distance> allDistances1 = new ArrayList<Distance>();
            ArrayList<Double> differencesSquared = new ArrayList<Double>();

            for (Map.Entry<String, points> room : house.all_rooms.entrySet()) {
                for (Map.Entry<Integer, samples> point : room.getValue().all_points.entrySet()) {
                    for (Map.Entry<Integer, sample> tuple : point.getValue().all_samples.entrySet()) {
                        for (Map.Entry<String, Integer> rssi_data : tuple.getValue().tuple.entrySet()) {
                            boolean found = false;
                            for (Map.Entry<String, Integer> tempCurrentTuple : currentTuple.entrySet()) {
                                if (rssi_data.getKey().equals(tempCurrentTuple.getKey())) {
                                    double temp = (tempCurrentTuple.getValue() - rssi_data.getValue()) * (tempCurrentTuple.getValue() - rssi_data.getValue());
                                    differencesSquared.add(temp);
                                    found = true;
                                }
                            }
                            if (!ignoreFromTraining) {
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
                            if (!ignoreFromTesting) {
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
        public void calculateDistance(Map.Entry<String, points> room, Map.Entry<Integer, samples> point, ArrayList<Double> differencesSquared, ArrayList<Distance> allDistances) {
            double sumOfDifferencesSquared = 0;
            for (int i = 0; i < differencesSquared.size(); i++) {
                sumOfDifferencesSquared += differencesSquared.get(i);
            }
            double distance = Math.sqrt(sumOfDifferencesSquared);
            Distance d = new Distance(room.getKey(), point.getKey(), distance);
            allDistances.add(d);
            differencesSquared.clear();
        }
    }
}

