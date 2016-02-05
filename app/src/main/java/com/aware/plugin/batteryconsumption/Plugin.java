package com.aware.plugin.batteryconsumption;

/**
 * Created by JuanCamilo on 2/3/2016.
 */
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import java.util.Iterator;
import java.util.Set;

public class Plugin extends Aware_Plugin {

    /**
     * Broadcasted with battery current value <br/>
     * Extra: current, in mA
     */
    public static final String ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION = "ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION";

    /**
     * Extra for ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION
     * (long) battery current value in mA
     */
    public static final String EXTRA_CURRENT = "current";

    /**
     * Extra for ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION
     * (int) battery level in percentage
     */
    public static final String EXTRA_LEVEL= "level";

    /**
     * Extra for ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION
     * (float) battery voltage in V
     */
    public static final String EXTRA_VOLTAGE= "voltage";

    /**
     * Extra for ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION
     * (int) battery temperature in C
     */
    public static final String EXTRA_TEMPERATURE= "temperature";

    /**
     * Extra for ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION
     * (boolean) true if battery is charging
     */
    public static final String EXTRA_CHARGING= "charging";

    /**
     * Broadcasted to read the battery current values
     */
    public static final String ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION_READ = "ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION_READ";

    //AWARE context producer
    public static ContextProducer context_producer;

    private Handler handler;
    private Runnable batteryTask;
    private int frequency;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::"+getResources().getString(R.string.app_name);
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_BATTERY_CONSUMPTION).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_BATTERY_CONSUMPTION, true);
        }

        if( Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_BATTERY_CONSUMPTION).length() == 0 ) {
            Aware.setSetting(this, Settings.FREQUENCY_PLUGIN_BATTERY_CONSUMPTION, 1000);
        }

        frequency = Integer.parseInt(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_BATTERY_CONSUMPTION));

        //Activate programmatically any sensors/plugins you need here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER,true);
        //NOTE: if using plugin with dashboard, you can specify the sensors you'll use there.

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent context_battery_consumption = new Intent();
                context_battery_consumption.setAction(ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION);
                context_battery_consumption.putExtra(EXTRA_CURRENT, BatteryReader.current);
                context_battery_consumption.putExtra(EXTRA_LEVEL, BatteryReader.batteryLevel);
                context_battery_consumption.putExtra(EXTRA_TEMPERATURE, BatteryReader.temperature);
                context_battery_consumption.putExtra(EXTRA_VOLTAGE, BatteryReader.currentVoltage);
                context_battery_consumption.putExtra(EXTRA_CHARGING, BatteryReader.isCharging);
                sendBroadcast(context_battery_consumption);
                Log.d(TAG,"Value recorded: Current: " + BatteryReader.current +
                                " - Level: " + BatteryReader.batteryLevel +
                                " - Temperature: " + BatteryReader.temperature+
                                " - Voltage: " + BatteryReader.currentVoltage +
                                " - IsCharging: " + BatteryReader.isCharging);
//                dumpIntent(context_battery_consumption);
            }
        };
        context_producer = CONTEXT_PRODUCER;


        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ Provider.BatteryConsumption_Data.CONTENT_URI };

        handler = new Handler();



        batteryTask = new Runnable() {
            @Override
            public void run() {
                Intent batteryIntent = new Intent();
                batteryIntent.setAction(ACTION_AWARE_PLUGIN_BATTERY_CONSUMPTION_READ);
                sendBroadcast(batteryIntent);
                handler.removeCallbacks(batteryTask);
                handler.postDelayed(this,frequency);
            }
        };

        //Activate plugin
        Aware.startPlugin(this, "com.aware.plugin.batteryconsumption");
    }

    public static void dumpIntent(Intent i){

        Bundle bundle = i.getExtras();
        if (bundle != null) {
            Set<String> keys = bundle.keySet();
            Iterator<String> it = keys.iterator();
            Log.d(TAG,"Dumping Intent start");
            while (it.hasNext()) {
                String key = it.next();
                Log.e(TAG,"[" + key + "=" + bundle.get(key)+"]");
            }
            Log.e(TAG,"Dumping Intent end");
        }
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        frequency = Integer.parseInt(Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_BATTERY_CONSUMPTION));
        handler.removeCallbacks(batteryTask);
        handler.postDelayed(batteryTask,frequency);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_BATTERY_CONSUMPTION, false);
        handler.removeCallbacks(batteryTask);
        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.batteryconsumption");
    }

    public static class BatteryReader extends BroadcastReceiver {

        public static long current;
        public static int batteryLevel;
        public static float temperature;
        public static float currentVoltage;
        public static boolean isCharging;

        @Override
        public void onReceive(Context context, Intent intent) {
//            long startTime = System.currentTimeMillis();
            try {
//                    Log.d(TAG,"Runnable");
                Intent batteryIntent = context.getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryIntent != null) {
//                        Log.d(TAG,"Intent not null");
                    int scale = batteryIntent.getIntExtra("scale", 100);
                    batteryLevel = (int)((float)batteryIntent.getIntExtra("level", 0)*100/scale);
                    currentVoltage = (float)batteryIntent.getIntExtra("voltage", 0) / 1000;
                    temperature = (float)batteryIntent.getIntExtra("temperature", 0)/(float)10;
                    isCharging = batteryIntent.getIntExtra("status", 1) == BatteryManager.BATTERY_STATUS_CHARGING;
                }
            }
            catch (Exception ex) {
                Log.e("CurrentWidget", ex.getMessage());
                ex.printStackTrace();
            }

            current = CurrentReaderFactory.getValue();
            if(current <0)
                current = 0;

            //Write to database
            ContentValues data = new ContentValues();
            data.put(Provider.BatteryConsumption_Data.TIMESTAMP, System.currentTimeMillis());
            data.put(Provider.BatteryConsumption_Data.DEVICE_ID, Aware.getSetting(context.getApplicationContext(), Aware_Preferences.DEVICE_ID));
            data.put(Provider.BatteryConsumption_Data.CURRENT, current);
            data.put(Provider.BatteryConsumption_Data.LEVEL, batteryLevel);
            data.put(Provider.BatteryConsumption_Data.TEMPERATURE, temperature);
            data.put(Provider.BatteryConsumption_Data.VOLTAGE, currentVoltage);
            data.put(Provider.BatteryConsumption_Data.IS_CHARGING, isCharging);

            context.getContentResolver().insert(Provider.BatteryConsumption_Data.CONTENT_URI, data);

            //Share context
            try {
                context_producer.onContext();
            }catch (Exception e){
                Log.e(TAG,"Couldn't send broadcast");
                e.printStackTrace();
            }

//            Log.d(TAG,"TIME: " + (System.currentTimeMillis()-startTime));
        }
    }
}