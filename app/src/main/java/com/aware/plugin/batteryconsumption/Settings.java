package com.aware.plugin.batteryconsumption;

/**
 * Created by JuanCamilo on 2/3/2016.
 */
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_BATTERY_CONSUMPTION = "status_plugin_battery_consumption";

    //Plugin settings UI elements
    private static CheckBoxPreference status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_BATTERY_CONSUMPTION);
        if( Aware.getSetting(this, STATUS_PLUGIN_BATTERY_CONSUMPTION).length() == 0 ) {
            Aware.setSetting( this, STATUS_PLUGIN_BATTERY_CONSUMPTION, true ); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_BATTERY_CONSUMPTION).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);

        if( setting.getKey().equals(STATUS_PLUGIN_BATTERY_CONSUMPTION) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if( is_active ) {
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.batteryconsumption");
            } else {
                Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.batteryconsumption");
            }
            status.setChecked(is_active);
        }
    }
}