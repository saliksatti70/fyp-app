package fyp.sysnet.com.occupancydetection;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileOutputStream;

/**
 * Created by Mazhar on 4/15/2015.
 */
public class ServerSenderThread extends Thread {

    Context context;
    Map<String,Integer> currentTuple;
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

    public void loadFromUserInformationFile() {
        try {
            InputStream fis = context.openFileInput("userInfo.dat");
            BufferedReader brr = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = brr.readLine()) != null) {
                String[] tok = line.split(":");
                if(tok[0].equals("UserName")) {
                    userName = tok[1];
                }
                else if(tok[0].equals("TotalRooms")) {
                    total_rooms = Integer.parseInt(tok[1]);
                }
                else if(tok[0].equals("TotalPoints")) {
                    total_points = Integer.parseInt(tok[1]);
                }
                else if(tok[0].equals("TotalSamples")) {
                    total_samples = Integer.parseInt(tok[1]);
                }
                else if(tok[0].equals("ValueOfK")) {
                    valueOfK = Integer.parseInt(tok[1]);
                }
                System.out.println("asd: "+total_rooms+" "+total_points+" "+total_samples+" "+valueOfK);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public String loadFromDataFile() {
        try {
            InputStream fis = context.openFileInput("data.dat");
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
    public String getProbableRoom(ArrayList<Distance> allDistances) {
        Map<String,Integer> finalRoom = getMapWithFrequency(allDistances);
        int max = 0;
        String roomName = "";
        //get room with most frequency among top k
        for (Map.Entry<String, Integer> room : finalRoom.entrySet()) {
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
            for(int j=0;j<allDistances.size();j++) {
                if(j < valueOfK) {
                    if (allDistances.get(i).room.equals(allDistances.get(j).room)) {
                        frequency++;
                    }
                }
            }
            finalRoom.put(allDistances.get(i).room,frequency);
        }
        return finalRoom;
    }
    public void sendToServer(String username, String room) {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            URI uri = new URI("http://google.com");
            HttpResponse response = httpclient.execute(new HttpGet(uri));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                response.getEntity().writeTo(out);
                String responseString = out.toString();
                out.close();
                //..more logic
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
            System.out.println(response.toString());
            Thread.sleep(10000);
        } catch(Exception e) {
            e.printStackTrace();
        }

    }
    ServerSenderThread(Context context) {
        this.context = context;
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
    public ArrayList<Distance> whereIsThisUsingMissingTestAndMissingTraining(boolean ignoreFromTraining, boolean ignoreFromTesting) {    //if not ignore, use -100 in place of missing Test, else ignore from training
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
    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

        loadFromUserInformationFile();
        loadFromDataFile();
        //while(true) {
        currentTuple = getCurrentTuple();
        ArrayList<Distance> allDistances = whereIsThisUsingMissingTestAndMissingTraining(false,false);
        String room = getProbableRoom(allDistances);
        System.out.println("CurrentRoom: "+room);
        sendToServer(userName,room);
        try {
            Thread.sleep(3000);
        } catch (Exception e) {

        }
        //}
    }
}
