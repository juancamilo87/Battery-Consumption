package com.aware.plugin.batteryconsumption;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = "BATTERY_READER";

    private TextView txt_counter;
    private TextView txt_battery_level;
    private TextView txt_voltage;
    private TextView txt_temperature;
    private TextView txt_is_charging;
    private TextView txt_current;

    private Handler handler;

    private String batteryLevelText;
    private String voltageText;
    private String temperatureText;
    private boolean isCharging;
    private String currentText;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_counter = (TextView) findViewById(R.id.counter);
        txt_battery_level = (TextView) findViewById(R.id.battery_level);
        txt_voltage = (TextView) findViewById(R.id.voltage);
        txt_temperature = (TextView) findViewById(R.id.temperature);
        txt_is_charging = (TextView) findViewById(R.id.is_charging);
        txt_current = (TextView) findViewById(R.id.current);
        //TODO: Put it in a service and make this a plugin
        handler = new Handler();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
//                    Log.d(TAG,"Runnable");
                    Intent batteryIntent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    if (batteryIntent != null) {
//                        Log.d(TAG,"Intent not null");
                        int scale = batteryIntent.getIntExtra("scale", 100);
                        int batteryLevel = (int)((float)batteryIntent.getIntExtra("level", 0)*100/scale);
                        batteryLevelText = String.valueOf(batteryLevel) + "%";
                        float currentVoltage = (float)batteryIntent.getIntExtra("voltage", 0) / 1000;
                        voltageText = Float.toString(currentVoltage) + "V";
                        int temperature = batteryIntent.getIntExtra("temperature", 0);
                        temperatureText = String.format(Locale.ENGLISH, "%.1f\u00B0C", ((float)temperature/10));
                        isCharging = batteryIntent.getIntExtra("status", 1) == BatteryManager.BATTERY_STATUS_CHARGING;

                        txt_battery_level.setText(batteryLevelText);
                        txt_voltage.setText(voltageText);
                        txt_temperature.setText(temperatureText);
                        txt_is_charging.setText(String.valueOf(isCharging));


                    }
                }
                catch (Exception ex) {
                    Log.e("CurrentWidget", ex.getMessage());
                    ex.printStackTrace();
                }

                Long current = CurrentReaderFactory.getValue();
                if(current <0)
                    current = current * (-1);
                currentText = Long.toString(current);

                txt_current.setText(currentText);

                txt_counter.setText(Integer.toString(Integer.parseInt(txt_counter.getText().toString())+1));

                handler.postDelayed(this,2000);
            }


        };

        handler.postDelayed(runnable,2000);
    }
}
