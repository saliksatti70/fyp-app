package fyp.sysnet.com.occupancydetection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Mazhar on 4/11/2015.
 */

public class AccelerometerService {
    private static SensorManager sensorManager;
    private static SensorEventListener sensorEventListener;
    private static boolean started = false;

    private static float[] accelerometer = new float[3];
    private static float[] magneticField = new float[3];

    private static float[] rotationMatrix = new float[9];
    private static float[] inclinationMatrix = new float[9];
    private static float[] attitude = new float[3];

    private final static double RAD2DEG = 180/Math.PI;

    private static int initialAzimuth = 0;
    private static int initialPitch = 0;
    private static int initialRoll = 0;
    private static int lastValue = 0;
    private static int newValue = 0;

    private static int[] attitudeInDegrees = new int[3];

    private static int counter = 0;

    public static synchronized void incrementCounter() {
        counter++;
    }

    public static synchronized void decrementCounter() {
        counter--;
    }

    public static int getCounter() {
        return counter;
    }


    public static void start(final Context applicationContext) {
        if(started) {
            return;
        }

        sensorManager = (SensorManager) applicationContext
                .getSystemService(Context.SENSOR_SERVICE);

        sensorEventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                int type = event.sensor.getType();
                if(type == Sensor.TYPE_MAGNETIC_FIELD) {
                    magneticField = event.values.clone();
                }
                if(type == Sensor.TYPE_ACCELEROMETER) {
                    if((newValue - lastValue) > 2 || (newValue - lastValue) < -2 ) {
                        System.out.println("ZXC: Sensor value: "+newValue+" , "+lastValue);
                        incrementCounter();
                    }
                    lastValue = (attitudeInDegrees[0]+attitudeInDegrees[1]+attitudeInDegrees[2])/3;
                    //System.out.println("ZXC: Sensor value:"+((attitudeInDegrees[0]+attitudeInDegrees[1]+attitudeInDegrees[2])/3));
                    accelerometer = event.values.clone();
                }

                SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometer, magneticField);
                SensorManager.getOrientation(rotationMatrix, attitude);

                attitudeInDegrees[0] =  (int) Math.round(attitude[0] * RAD2DEG);    //azimuth
                attitudeInDegrees[1] = (int) Math.round(attitude[1] * RAD2DEG);     //pitch
                attitudeInDegrees[2] = (int) Math.round(attitude[2] * RAD2DEG);     //roll
                newValue = (attitudeInDegrees[0]+attitudeInDegrees[1]+attitudeInDegrees[2])/3;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);

        started = true;
    }

    public static boolean getStarted() {
        return started;
    }

    public static void stop() {
        if(started) {
            sensorManager.unregisterListener(sensorEventListener);
            started = false;
        }
    }
}
