package fyp.sysnet.com.occupancydetection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileOutputStream;

public class settings extends Activity {
    private EditText total_rooms;
    private EditText total_points;
    private EditText total_unique_samples;
    private EditText valueOfK;
    private EditText userName;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        total_rooms = (EditText) findViewById(R.id.total_rooms);
        total_points =(EditText) findViewById(R.id.total_points);
        total_unique_samples = (EditText) findViewById(R.id.total_samples);
        valueOfK = (EditText) findViewById(R.id.valueOfKTextField);
        userName = (EditText) findViewById(R.id.editText2);
    }
    public void onButtonClick_save_button(View v)
    {
        try {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("total_rooms", Integer.parseInt(total_rooms.getText().toString()));
            returnIntent.putExtra("total_points", Integer.parseInt(total_points.getText().toString()));
            returnIntent.putExtra("total_samples", Integer.parseInt(total_unique_samples.getText().toString()));
            returnIntent.putExtra("valueOfK", Integer.parseInt(valueOfK.getText().toString()));
            //if (userName.getText().toString() != null) {
                String dataToBeWritten = "UserName:"+userName.getText().toString()+"\n";
                dataToBeWritten += "TotalRooms:"+total_rooms.getText().toString()+"\n";
                dataToBeWritten += "TotalPoints:"+total_points.getText().toString()+"\n";
                dataToBeWritten += "TotalSamples:"+total_unique_samples.getText().toString()+"\n";
                dataToBeWritten += "ValueOfK:"+valueOfK.getText().toString();
                writeUserInformation(dataToBeWritten);
            //}
            setResult(RESULT_OK,returnIntent);
            finish();

        } catch ( NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Incorrect Number", Toast.LENGTH_SHORT).show();
        }
    }

    public void writeUserInformation(String data) {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput("userInfo.dat", Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.flush();
            fos.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onButtonClick_cancel_button(View v)
    {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }
}
