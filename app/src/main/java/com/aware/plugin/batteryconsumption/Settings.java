package com.aware.plugin.batteryconsumption;

/**
 * Created by JuanCamilo on 2/3/2016.
 */
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_BATTERY_CONSUMPTION = "status_plugin_battery_consumption";

    public static final String FREQUENCY_PLUGIN_BATTERY_CONSUMPTION = "frequency_plugin_battery_consumption";

    //Plugin settings UI elements
    private static CheckBoxPreference status;

    private static EditTextPreference frequency;

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

        frequency = (EditTextPreference) findPreference(FREQUENCY_PLUGIN_BATTERY_CONSUMPTION);
        if( Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_BATTERY_CONSUMPTION).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), FREQUENCY_PLUGIN_BATTERY_CONSUMPTION, 1000);
        }
        frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_BATTERY_CONSUMPTION) + " milliseconds");
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

        if( setting.getKey().equals(FREQUENCY_PLUGIN_BATTERY_CONSUMPTION)) {
            if(Integer.valueOf(sharedPreferences.getString(key, "1000"))<100)
            {

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key,"100");
                editor.commit();
                Toast.makeText(getApplicationContext(),"The minimum frequency value is 100ms",Toast.LENGTH_SHORT).show();
                frequency.setText("100");
            }
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "1000"));
            frequency.setSummary("Every " + Aware.getSetting(getApplicationContext(), FREQUENCY_PLUGIN_BATTERY_CONSUMPTION) + " milliseconds");
        }

    }
}