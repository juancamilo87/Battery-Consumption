package com.aware.plugin.batteryconsumption;

/**
 * Created by JuanCamilo on 2/5/2016.
 */
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {

    public static final int DATABASE_VERSION = 2;

    /**
     * Provider authority: com.aware.plugin.ambient_noise.provider.ambient_noise
     */
    public static String AUTHORITY = "com.aware.plugin.batteryconsumption.provider.batteryconsumption";

    private static final int BATTERY_CONSUMPTION = 1;
    private static final int BATTERY_CONSUMPTION_ID = 2;

    public static final String DATABASE_NAME = "plugin_battery_consumption.db";

    public static final String[] DATABASE_TABLES = {
            "plugin_battery_consumption"
    };

    public static final String[] TABLES_FIELDS = {
            BatteryConsumption_Data._ID + " integer primary key autoincrement," +
                    BatteryConsumption_Data.TIMESTAMP + " real default 0," +
                    BatteryConsumption_Data.DEVICE_ID + " text default ''," +
                    BatteryConsumption_Data.CURRENT + " real default 0," +
                    BatteryConsumption_Data.LEVEL + " integer default 0," +
                    BatteryConsumption_Data.TEMPERATURE + " real default 0," +
                    BatteryConsumption_Data.VOLTAGE + " real default 0," +
                    BatteryConsumption_Data.IS_CHARGING + " boolean default false," +
                    "UNIQUE("+ BatteryConsumption_Data.TIMESTAMP+","+ BatteryConsumption_Data.DEVICE_ID+")"
    };

    public static final class BatteryConsumption_Data implements BaseColumns {
        private BatteryConsumption_Data(){};

        public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/plugin_battery_consumption");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.batteryconsumption";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.batteryconsumption";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String CURRENT = "long_current";
        public static final String LEVEL = "int_level";
        public static final String TEMPERATURE = "float_temperature";
        public static final String VOLTAGE = "float_voltage";
        public static final String IS_CHARGING = "bool_charging";
    }

    private static UriMatcher URIMatcher;
    private static HashMap<String, String> databaseMap;
    private static DatabaseHelper databaseHelper;
    private static SQLiteDatabase database;

    @Override
    public boolean onCreate() {

        AUTHORITY = getContext().getPackageName() + ".provider.batteryconsumption";

        URIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], BATTERY_CONSUMPTION);
        URIMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", BATTERY_CONSUMPTION_ID);

        databaseMap = new HashMap<>();
        databaseMap.put(BatteryConsumption_Data._ID, BatteryConsumption_Data._ID);
        databaseMap.put(BatteryConsumption_Data.TIMESTAMP, BatteryConsumption_Data.TIMESTAMP);
        databaseMap.put(BatteryConsumption_Data.DEVICE_ID, BatteryConsumption_Data.DEVICE_ID);
        databaseMap.put(BatteryConsumption_Data.CURRENT, BatteryConsumption_Data.CURRENT);
        databaseMap.put(BatteryConsumption_Data.LEVEL, BatteryConsumption_Data.LEVEL);
        databaseMap.put(BatteryConsumption_Data.TEMPERATURE, BatteryConsumption_Data.TEMPERATURE);
        databaseMap.put(BatteryConsumption_Data.VOLTAGE, BatteryConsumption_Data.VOLTAGE);
        databaseMap.put(BatteryConsumption_Data.IS_CHARGING, BatteryConsumption_Data.IS_CHARGING);

        return true;
    }

    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case BATTERY_CONSUMPTION:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (URIMatcher.match(uri)) {
            case BATTERY_CONSUMPTION:
                return BatteryConsumption_Data.CONTENT_TYPE;
            case BATTERY_CONSUMPTION_ID:
                return BatteryConsumption_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (URIMatcher.match(uri)) {
            case BATTERY_CONSUMPTION:
                long weather_id = database.insert(DATABASE_TABLES[0], BatteryConsumption_Data.DEVICE_ID, values);

                if (weather_id > 0) {
                    Uri new_uri = ContentUris.withAppendedId(
                            BatteryConsumption_Data.CONTENT_URI,
                            weather_id);
                    getContext().getContentResolver().notifyChange(new_uri,
                            null);
                    return new_uri;
                }
                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (URIMatcher.match(uri)) {
            case BATTERY_CONSUMPTION:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(databaseMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());

            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (URIMatcher.match(uri)) {
            case BATTERY_CONSUMPTION:
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                break;
            default:

                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}