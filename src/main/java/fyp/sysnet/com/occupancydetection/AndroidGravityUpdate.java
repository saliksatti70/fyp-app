package fyp.sysnet.com.occupancydetection;

/**
 * Created by Mazhar on 4/11/2015.
 */
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.ArrayList;
import java.util.List;

public class AndroidGravityUpdate implements SensorEventListener {
    private SensorManager sensorManager;
    Vector3 gravity;
    List<Float>[] rollingAverage = new List[3];

    private static final int MAX_SAMPLE_SIZE = 5;

    AndroidGravityUpdate( SensorManager sensorManager ) {
        this.gravity = new Vector3();
        this.sensorManager = sensorManager;

        if(sensorManager.getSensorList(Sensor.TYPE_GRAVITY).size() > 0){
            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    SensorManager.SENSOR_DELAY_GAME
            );
        } else if( sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).size() > 0) {
            rollingAverage[0] = new ArrayList<Float>();
            rollingAverage[1] = new ArrayList<Float>();
            rollingAverage[2] = new ArrayList<Float>();

            sensorManager.registerListener(
                    this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME
            );
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_GRAVITY){
            gravity.z = event.values[0];
            gravity.x = event.values[1];
            gravity.y = - event.values[2];
        }
        else if ( event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
            //For whatever reason, my Samsung only has "Accelerometer"
            // But it is incredibly rough, so attempting to smooth
            // it out with rolling averages.
            rollingAverage[0] = roll(rollingAverage[0], event.values[0]);
            rollingAverage[1] = roll(rollingAverage[1], event.values[1]);
            rollingAverage[2] = roll(rollingAverage[2], -event.values[2]);

            gravity.z = averageList(rollingAverage[0]);
            gravity.x = averageList(rollingAverage[1]);
            gravity.y = averageList(rollingAverage[2]);
        }
    }

    public List<Float> roll(List<Float> list, float newMember){
        if(list.size() == MAX_SAMPLE_SIZE){
            list.remove(0);
        }
        list.add(newMember);
        return list;
    }

    public float averageList(List<Float> tallyUp){

        float total=0;
        for(float item : tallyUp ){
            total+=item;
        }
        total = total/tallyUp.size();

        return total;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public Vector3 getVector() {
        return gravity;
    }
}

class Vector3 {
    float x;
    float y;
    float z;
}
