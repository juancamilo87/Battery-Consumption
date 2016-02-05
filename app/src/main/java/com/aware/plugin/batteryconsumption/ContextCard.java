package com.aware.plugin.batteryconsumption;

/**
 * Created by JuanCamilo on 2/3/2016.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aware.Aware;
import com.aware.ui.Stream_UI;
import com.aware.utils.IContextCard;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Calendar;

public class ContextCard implements IContextCard {

    //Set how often your card needs to refresh if the stream is visible (in milliseconds)
    private int refresh_interval = 1 * 1000; //1 second = 1000 milliseconds

    //DEMO: we are demo'ing a counter incrementing in real-time
    private int counter = 0;

    private Handler uiRefresher = new Handler(Looper.getMainLooper());
    private Runnable uiChanger = new Runnable() {
        @Override
        public void run() {


            //Modify card's content here once it's initialized
            if( card != null ) {
                //DEMO display the counter value
                Cursor latest = sContext.getContentResolver().query(Provider.BatteryConsumption_Data.CONTENT_URI, null, null, null, Provider.BatteryConsumption_Data.TIMESTAMP + " DESC LIMIT 1");
                if( latest != null && latest.moveToFirst() ) {
                    if(latest.getInt(latest.getColumnIndex(Provider.BatteryConsumption_Data.IS_CHARGING)) == 1)
                    {
                        current_txt.setText("- mA");
                    } else
                    {
                        current_txt.setText(latest.getLong(latest.getColumnIndex(Provider.BatteryConsumption_Data.CURRENT)) + " mA");
                    }
                    voltage_txt.setText(latest.getFloat(latest.getColumnIndex(Provider.BatteryConsumption_Data.VOLTAGE)) + " V");
                    is_charging_txt.setText(latest.getInt(latest.getColumnIndex(Provider.BatteryConsumption_Data.IS_CHARGING)) == 0 ? "Not Charging" : "Charging");
                    level_txt.setText("Level: " + latest.getInt(latest.getColumnIndex(Provider.BatteryConsumption_Data.LEVEL)) + "%");
                    temperature_txt.setText("Temperature: " + latest.getFloat(latest.getColumnIndex(Provider.BatteryConsumption_Data.TEMPERATURE)) + " \u2103");
                }
                if( latest != null && ! latest.isClosed() ) latest.close();
            }

            //Reset timer and schedule the next card refresh
            uiRefresher.postDelayed(uiChanger, refresh_interval);
        }
    };

    //Empty constructor used to instantiate this card
    public ContextCard(){}

    //You may use sContext on uiChanger to do queries to databases, etc.
    private Context sContext;

    //Declare here all the UI elements you'll be accessing
    private View card;
    private TextView current_txt, voltage_txt, is_charging_txt, level_txt, temperature_txt;

    @Override
    public View getContextCard(Context context) {
        sContext = context;
        //Tell Android that you'll monitor the stream statuses
        IntentFilter filter = new IntentFilter();
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_OPEN);
        filter.addAction(Stream_UI.ACTION_AWARE_STREAM_CLOSED);
        context.registerReceiver(streamObs, filter);

        //Load card information to memory
        LayoutInflater sInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        card = sInflater.inflate(R.layout.card, null);

        //Initialize UI elements from the card
        current_txt = (TextView) card.findViewById(R.id.current_txt);
        voltage_txt = (TextView) card.findViewById(R.id.voltage);
        is_charging_txt = (TextView) card.findViewById(R.id.is_charging);
        level_txt = (TextView) card.findViewById(R.id.level);
        temperature_txt = (TextView) card.findViewById(R.id.temperature_txt);

        LinearLayout current_plot_container = (LinearLayout) card.findViewById(R.id.current_plot);

        current_plot_container.removeAllViews();
        current_plot_container.addView(drawGraph(context));
        current_plot_container.invalidate();

        //Begin refresh cycle
        uiRefresher.postDelayed(uiChanger, refresh_interval);

        //Return the card to AWARE/apps
        return card;
    }

    private LineChart drawGraph( Context context ) {

        ArrayList<String> x_hours = new ArrayList<>();
        for(int i=0; i<24; i++) {
            x_hours.add(String.valueOf(i));
        }

        //Get today's time from the beginning in milliseconds
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        ArrayList<Entry> dbEntries = new ArrayList<>();
//        ArrayList<Entry> hzEntries = new ArrayList<>();

        Cursor latest = context.getContentResolver().query(
                Provider.BatteryConsumption_Data.CONTENT_URI,
                new String[]{ "AVG(" + Provider.BatteryConsumption_Data.CURRENT+") as average_mA",
                        "strftime('%H'," + Provider.BatteryConsumption_Data.TIMESTAMP +"/1000, 'unixepoch','localtime')+0 as time_of_day" },
                Provider.BatteryConsumption_Data.CURRENT + ">0 AND " + Provider.BatteryConsumption_Data.TIMESTAMP + " >= " + c.getTimeInMillis() + " ) GROUP BY ( time_of_day ",
                null,
                "time_of_day ASC");

        if( latest != null && latest.moveToFirst() ) {
            do {
                dbEntries.add(new Entry(latest.getFloat(0), latest.getInt(1)));
//                hzEntries.add(new Entry(latest.getFloat(0), latest.getInt(1)));
            } while(latest.moveToNext());
        }
        if( latest != null && ! latest.isClosed() ) latest.close();


        LineDataSet dbData = new LineDataSet(dbEntries, "Average mA");
        dbData.setColor(Color.parseColor("#33B5E5"));
        dbData.setDrawValues(false);

//        BarDataSet hzData = new BarDataSet(hzEntries, "Average Hz");
//        hzData.setColor(Color.parseColor("#009688"));
//        hzData.setDrawValues(false);

        ArrayList<LineDataSet> datasets = new ArrayList<>();
        datasets.add(dbData);
//        datasets.add(hzData);

        LineData data = new LineData(x_hours, datasets);

        LineChart mChart = new LineChart(context);
        mChart.setContentDescription("");
        mChart.setDescription("");
        mChart.setMinimumHeight(300);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBorders(false);

        YAxis left = mChart.getAxisLeft();
        left.setDrawLabels(true);
        left.setDrawGridLines(true);
        left.setDrawAxisLine(true);

        YAxis right = mChart.getAxisRight();
        right.setDrawAxisLine(false);
        right.setDrawLabels(false);
        right.setDrawGridLines(false);

        XAxis bottom = mChart.getXAxis();
        bottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottom.setSpaceBetweenLabels(0);
        bottom.setDrawGridLines(false);

        mChart.setData(data);
        mChart.invalidate();

        mChart.animateX(1000);

        return mChart;
    }

    //This is a BroadcastReceiver that keeps track of stream status. Used to stop the refresh when user leaves the stream and restart again otherwise
    private StreamObs streamObs = new StreamObs();
    public class StreamObs extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_OPEN) ) {
                //start refreshing when user enters the stream
                uiRefresher.postDelayed(uiChanger, refresh_interval);

                //DEMO only, reset the counter every time the user opens the stream
                counter = 0;
            }
            if( intent.getAction().equals(Stream_UI.ACTION_AWARE_STREAM_CLOSED) ) {
                //stop refreshing when user leaves the stream
                uiRefresher.removeCallbacks(uiChanger);
                uiRefresher.removeCallbacksAndMessages(null);
            }
        }
    }
}
