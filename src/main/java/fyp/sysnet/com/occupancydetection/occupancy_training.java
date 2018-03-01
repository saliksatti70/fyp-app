package fyp.sysnet.com.occupancydetection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class occupancy_training extends Activity {
    private int total_rooms = 0;
    private int total_points = 0;
    private int total_samples = 0;
    private String[] roomNames;
    rooms house = new rooms();
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
    public class DataRecorder extends AsyncTask<Void, Void, String> {
        String output = "Room:1 Point:1 Sample:1";
        int numberOfSamples = 0;
        int numberOfPoints = 0;
        int roomNumber = 0;

        @Override
        protected void onPreExecute(){
            TextView textView = (TextView) findViewById(R.id.update_editText);
            textView.setText(output);
        }

        @Override
        protected String doInBackground(Void... v) {
            while(roomNumber < total_rooms) {
                System.out.println("In rooms");
                numberOfPoints = 0;
                points tempPoints = new points();
                Map<String,Integer> lastTuple = new HashMap<String,Integer>();  //to ensure unique tuple is stored when point changes
                while (numberOfPoints < total_points) {
                    samples tempSamples = new samples();
                    numberOfSamples = 0;
                    sample tempSample = new sample();
                    while (numberOfSamples < total_samples) {
                        tempSample = new sample();
                        tempSample.tuple = getCurrentTuple();
                        if(lastTuple.equals(tempSample.tuple)) {
                           //ignore - dont add to tempSamples
                        }
                        else if(!tempSamples.compare(tempSample)) {
                            tempSamples.all_samples.put(numberOfSamples, tempSample);
                            output = "Room:"+(roomNumber+1)+" Point:"+(numberOfPoints+1)+" Sample:"+(numberOfSamples+1);
                            publishProgress();
                            numberOfSamples++;
                        }
                    }
                    output = "Point complete. Go to other point";
                    publishProgress();
                    try {
                        Thread.sleep(15000);
                    } catch (Exception e) {

                    }
                    lastTuple = tempSample.tuple;
                    tempPoints.all_points.put(numberOfPoints, tempSamples);
                    numberOfPoints++;
                    //output = "Room:"+roomNumber+1+" Point:"+numberOfPoints+1+" Sample:"+numberOfSamples;
                    //publishProgress();
                }
                house.all_rooms.put(roomNames[roomNumber], tempPoints);
//                output = "Room:"+(roomNumber+1)+" Point:"+(numberOfPoints)+" Sample:"+numberOfSamples;
//                publishProgress();
                roomNumber++;
            }
            return output;
        }

        @Override
        protected  void onProgressUpdate(Void... s) {
            TextView textView1 = (TextView) findViewById(R.id.update_editText);
            textView1.setText(output);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView textView = (TextView) findViewById(R.id.update_editText);
            textView.setText(result);
            Toast.makeText(getApplicationContext(), "Recording complete", Toast.LENGTH_SHORT).show();
        }

        public Map<String, Integer> getCurrentTuple() {

            Map <String,Integer> returnSamples = new HashMap<String,Integer>();

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
            }
            catch (Exception e) {
                //return e.toString();
            }
            return null;
        }
    }

    public void onButtonClickSave(View v) {
        writeRecordedDataToFile();
        Toast.makeText(getApplicationContext(), "Data written to file", Toast.LENGTH_LONG).show();
    }

    public void writeRecordedDataToFile() {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput("data.dat", Context.MODE_PRIVATE);
        }catch (Exception e) {
            e.printStackTrace();
        }
        for (Map.Entry<String, points> room : house.all_rooms.entrySet())
        {
            String output = new String();
            for (Map.Entry<Integer,samples> point : room.getValue().all_points.entrySet())
            {
                output = room.getKey().toString();
                for (Map.Entry<Integer,sample> tuple : point.getValue().all_samples.entrySet())
                {
                    output = room.getKey() + "/" + point.getKey() + "/" + tuple.getKey()+ "/";
                    for ( Map.Entry<String,Integer> rssi_data : tuple.getValue().tuple.entrySet())
                    {
                        output += rssi_data.getKey() + "@" + rssi_data.getValue() + "#";
                    }
                    output += "\n";
                    try {
                        fos.write(output.getBytes());
                        fos.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            String output = "eof/eof/eof/eof/eof\n";
            fos.write(output.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.training_layout);
        total_rooms = getIntent().getIntExtra("total_rooms",total_rooms);
        total_points = getIntent().getIntExtra("total_points",total_points);
        total_samples = getIntent().getIntExtra("total_samples",total_samples);
        Toast.makeText(getApplicationContext(), total_rooms + "," + total_points + "," + total_samples, Toast.LENGTH_LONG).show();

        TextView textView = (TextView) findViewById(R.id.textView9);
        String outputForTotalData = "Rooms:"+total_rooms+" Points:"+total_points+" Samples:"+total_samples;
        textView.setText(outputForTotalData);
    }

    public void onButtonClickStartTraining(View v) {
        EditText roomNamesField = (EditText) findViewById(R.id.editText);
        String roomNamesFromUser = roomNamesField.getText().toString();
        roomNames = roomNamesFromUser.split(",");
        if (roomNames.length != total_rooms) {
            Toast.makeText(getApplicationContext(), "Enter names of rooms equal to their number.", Toast.LENGTH_SHORT).show();
        }
        else {
            System.out.println("In On Button Click");
            DataRecorder dataRecorder = new DataRecorder();
            dataRecorder.execute();
        }
    }
    boolean mExternalStorageAvailable = false;
    boolean mExternalStorageWriteable = false;

    public void checkExternalMedia(){
        mExternalStorageAvailable = false;
        mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        /*tv.append("\n\nExternal Media: readable="
                +mExternalStorageAvailable+" writable="+mExternalStorageWriteable);*/
    }

    /** Method to write ascii text characters to file on SD card. Note that you must add a
     WRITE_EXTERNAL_STORAGE permission to the manifest file or this method will throw
     a FileNotFound Exception because you won't have write permission. */


    public void writeToSDFile(){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        //tv.append("\nExternal file system root: "+root);

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File dir = new File (root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir, "myData.txt");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("Hi , How are you");
            pw.println("Hello");
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        /*    Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        */} catch (IOException e) {
            e.printStackTrace();
        }
        //tv.append("\n\nFile written to "+file);
    }

    /** Method to read in a text file placed in the res/raw directory of the application. The
     method reads in all lines of the file sequentially. */

    private void readRaw(){
        //tv.append("\nData read from res/raw/textfile.txt:");
        /*InputStream is = this.getResources().openRawResource(R.raw.textfile);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr, 8192);    // 2nd arg is buffer size

        // More efficient (less readable) implementation of above is the composite expression
    *//*BufferedReader br = new BufferedReader(new InputStreamReader(
            this.getResources().openRawResource(R.raw.textfile)), 8192);*//*

        try {
            String test;
            while (true){
                test = br.readLine();
                // readLine() returns null if no more lines in the file
                if(test == null) break;
                //tv.append("\n"+"    "+test);
            }
            isr.close();
            is.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
}
/*
get all points
for (Map.Entry<String, points> room : house.all_rooms.entrySet())
        {
            for (Map.Entry<Integer,samples> point : room.getValue().all_points.entrySet())
            {
                for (Map.Entry<Integer,sample> tuple : point.getValue().all_samples.entrySet())
                {
                    for ( Map.Entry<String,Integer> rssi_data : tuple.getValue().tuple.entrySet())
                    {
                        System.out.println(room.getKey() + "/" + point.getKey() + "/" + tuple.getKey()+ "/" + rssi_data.getKey() + "/" + rssi_data.getValue());
                    }
                }
            }
        }
 */