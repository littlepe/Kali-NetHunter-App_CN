package com.offsec.nethunter.SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.models.USBArsenalUSBNetworkModel;
import com.offsec.nethunter.models.USBArsenalUSBSwitchModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class USBArsenalSQL extends SQLiteOpenHelper {
    private static USBArsenalSQL instance;
    private static final String DATABASE_NAME = "USBArsenalFragment";
    private static final String TAG = "USBArsenalSQLSQL";
    private static final String USBSWITCH_TABLE_NAME = "USBSwitch";
    private static final String USBNETWORK_TABLE_NAME = "USBNetwork";
    private static final ArrayList<String> COLUMNS_USBSWITCH = new ArrayList<>();
    private static final ArrayList<String> COLUMNS_USBNETWORK = new ArrayList<>();

    public static synchronized USBArsenalSQL getInstance(Context context){
        if (instance == null) {
            instance = new USBArsenalSQL(context.getApplicationContext());
        }
        return instance;
    }

    private USBArsenalSQL(Context context) {
        super(context, DATABASE_NAME, null, 1);
        COLUMNS_USBSWITCH.add("target");
        COLUMNS_USBSWITCH.add("functions");
        COLUMNS_USBSWITCH.add("idVendor");
        COLUMNS_USBSWITCH.add("idProduct");
        COLUMNS_USBSWITCH.add("manufacturer");
        COLUMNS_USBSWITCH.add("product");
        COLUMNS_USBSWITCH.add("serialnumber");

        COLUMNS_USBNETWORK.add("attack_mode");
        COLUMNS_USBNETWORK.add("upstream_iface");
        COLUMNS_USBNETWORK.add("usb_iface");
        COLUMNS_USBNETWORK.add("ip_address_for_target");
        COLUMNS_USBNETWORK.add("ip_gateway");
        COLUMNS_USBNETWORK.add("ip_subnetmask");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String[][] USBSwitchData = {
                {"Windows","reset","0x1234","0x5678","","",""},
                {"Windows","hid","0x046d","0xc316","","",""},
                {"Windows","hid,adb","0x046d","0xc317","","",""},
                {"Windows","mass_storage","0x9051","0x168a","","",""},
                {"Windows","mass_storage,adb","0x9051","0x168b","","",""},
                {"Windows","rndis","0x0525","0xa4a2","","",""},
                {"Windows","rndis,adb","0x0525","0xa4a3","","",""},
                {"Windows","hid,mass_storage","0x046d","0xc318","","",""},
                {"Windows","hid,mass_storage,adb","0x046d","0xc319","","",""},
                {"Windows","rndis,hid","0x0525","0xa4a6","","",""},
                {"Windows","rndis,hid,adb","0x0525","0xa4a7","","",""},
                {"Windows","rndis,mass_storage","0x0525","0xa4a4","","",""},
                {"Windows","rndis,mass_storage,adb","0x0525","0xa4a5","","",""},
                {"Windows","rndis,hid,mass_storage","0x0525","0xa4a8","","",""},
                {"Windows","rndis,hid,mass_storage,adb","0x0525","0xa4a9","","",""},
                {"Linux","reset","0x1234","0x5678","","",""},
                {"Linux","hid","0x046d","0xc316","","",""},
                {"Linux","hid,adb","0x046d","0xc317","","",""},
                {"Linux","mass_storage","0x9051","0x168a","","",""},
                {"Linux","mass_storage,adb","0x9051","0x168b","","",""},
                {"Linux","rndis","0x0525","0xa4a2","","",""},
                {"Linux","rndis,adb","0x0525","0xa4a3","","",""},
                {"Linux","hid,mass_storage","0x046d","0xc318","","",""},
                {"Linux","hid,mass_storage,adb","0x046d","0xc319","","",""},
                {"Linux","rndis,hid","0x0525","0xa4a6","","",""},
                {"Linux","rndis,hid,adb","0x0525","0xa4a7","","",""},
                {"Linux","rndis,mass_storage","0x0525","0xa4a4","","",""},
                {"Linux","rndis,mass_storage,adb","0x0525","0xa4a5","","",""},
                {"Linux","rndis,hid,mass_storage","0x0525","0xa4a8","","",""},
                {"Linux","rndis,hid,mass_storage,adb","0x0525","0xa4a9","","",""},
                {"Mac OS", "reset","0x1234","0x5678","","",""},
                {"Mac OS", "hid","0x05ac","0x0201","","",""},
                {"Mac OS", "hid,adb","0x05ac","0x0201","","",""},
                {"Mac OS", "mass_storage","0x0930","0x6545","","",""},
                {"Mac OS", "mass_storage,adb","0x0930","0x6545","","",""},
                {"Mac OS", "acm,ecm","0x1d6b","0x0105","","",""},
                {"Mac OS", "acm,ecm,adb","0x1d6b","0x0105","","",""},
                {"Mac OS", "hid,mass_storage","0x05ac","0x0201","","",""},
                {"Mac OS", "hid,mass_storage,adb","0x05ac","0x0201","","",""},
                {"Mac OS", "acm,ecm,hid","0x05ac","0x0201","","",""},
                {"Mac OS", "acm,ecm,hid,adb","0x05ac","0x0201","","",""},
                {"Mac OS", "acm,ecm,mass_storage","0x1d6b","0x0105","","",""},
                {"Mac OS", "acm,ecm,mass_storage,adb","0x1d6b","0x0105","","",""},
                {"Mac OS", "acm,ecm,hid,mass_storage","0x05ac","0x0201","","",""},
                {"Mac OS", "acm,ecm,hid,mass_storage,adb","0x05ac","0x0201","","",""}
        };
        final String[][] USBNetworkData = {
                {"0", "wlan0", "rndis0", "192.168.192.100", "192.168.192.1", "255.255.255.0"}
        };
        db.execSQL("CREATE TABLE " + USBSWITCH_TABLE_NAME + " (" + COLUMNS_USBSWITCH.get(0) + " TEXT, " +
                COLUMNS_USBSWITCH.get(1) + " TEXT, " + COLUMNS_USBSWITCH.get(2) +  " TEXT, " +
                COLUMNS_USBSWITCH.get(3) + " TEXT, " + COLUMNS_USBSWITCH.get(4) + " TEXT, " +
                COLUMNS_USBSWITCH.get(5) + " TEXT, " + COLUMNS_USBSWITCH.get(6) + " TEXT)");
        db.execSQL("CREATE TABLE " + USBNETWORK_TABLE_NAME + " (" + COLUMNS_USBNETWORK.get(0) + " INTEGER, " +
                COLUMNS_USBNETWORK.get(1) + " TEXT, " + COLUMNS_USBNETWORK.get(2) +  " TEXT, " +
                COLUMNS_USBNETWORK.get(3) + " TEXT, " + COLUMNS_USBNETWORK.get(4) + " TEXT, " +
                COLUMNS_USBNETWORK.get(5) + " TEXT)");
        ContentValues initialValues = new ContentValues();
        db.beginTransaction();
        for (String[] data : USBSwitchData) {
            initialValues.put(COLUMNS_USBSWITCH.get(0), data[0]);
            initialValues.put(COLUMNS_USBSWITCH.get(1), data[1]);
            initialValues.put(COLUMNS_USBSWITCH.get(2), data[2]);
            initialValues.put(COLUMNS_USBSWITCH.get(3), data[3]);
            initialValues.put(COLUMNS_USBSWITCH.get(4), data[4]);
            initialValues.put(COLUMNS_USBSWITCH.get(5), data[5]);
            initialValues.put(COLUMNS_USBSWITCH.get(6), data[6]);
            db.insert(USBSWITCH_TABLE_NAME, null, initialValues);
            initialValues.clear();
        }
        for (String[] data : USBNetworkData) {
            initialValues.put(COLUMNS_USBNETWORK.get(0), data[0]);
            initialValues.put(COLUMNS_USBNETWORK.get(1), data[1]);
            initialValues.put(COLUMNS_USBNETWORK.get(2), data[2]);
            initialValues.put(COLUMNS_USBNETWORK.get(3), data[3]);
            initialValues.put(COLUMNS_USBNETWORK.get(4), data[4]);
            initialValues.put(COLUMNS_USBNETWORK.get(5), data[5]);
            db.insert(USBNETWORK_TABLE_NAME, null, initialValues);
            initialValues.clear();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + USBSWITCH_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + USBNETWORK_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public USBArsenalUSBSwitchModel getUSBSwitchColumnData(String targetOSName, String functionName){
        USBArsenalUSBSwitchModel usbArsenalUSBSwitchModel = new USBArsenalUSBSwitchModel();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + USBSWITCH_TABLE_NAME + " WHERE " + COLUMNS_USBSWITCH.get(0) + "='" + targetOSName + "'" + " AND " + COLUMNS_USBSWITCH.get(1) + "='" + functionName + "';", null);
        if (cursor.moveToFirst()) {
            int columnIndex2 = cursor.getColumnIndex(COLUMNS_USBSWITCH.get(2));
            int columnIndex3 = cursor.getColumnIndex(COLUMNS_USBSWITCH.get(3));
            int columnIndex4 = cursor.getColumnIndex(COLUMNS_USBSWITCH.get(4));
            int columnIndex5 = cursor.getColumnIndex(COLUMNS_USBSWITCH.get(5));
            int columnIndex6 = cursor.getColumnIndex(COLUMNS_USBSWITCH.get(6));

            String columnValue2 = columnIndex2 != -1 ? cursor.getString(columnIndex2) : null;
            String columnValue3 = columnIndex3 != -1 ? cursor.getString(columnIndex3) : null;
            String columnValue4 = columnIndex4 != -1 ? cursor.getString(columnIndex4) : null;
            String columnValue5 = columnIndex5 != -1 ? cursor.getString(columnIndex5) : null;
            String columnValue6 = columnIndex6 != -1 ? cursor.getString(columnIndex6) : null;

            usbArsenalUSBSwitchModel.setidVendor(columnValue2);
            usbArsenalUSBSwitchModel.setidProduct(columnValue3);
            usbArsenalUSBSwitchModel.setmanufacturer(columnValue4);
            usbArsenalUSBSwitchModel.setproduct(columnValue5);
            usbArsenalUSBSwitchModel.setserialnumber(columnValue6);
        }
        cursor.close();
        return usbArsenalUSBSwitchModel;
    }

    public USBArsenalUSBNetworkModel getUSBNetworkColumnData(int attackModePosition){
        USBArsenalUSBNetworkModel usbArsenalUSBNetworkModel = new USBArsenalUSBNetworkModel();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + USBNETWORK_TABLE_NAME + " WHERE " + COLUMNS_USBNETWORK.get(0) + "='" + attackModePosition + "';", null);
        if (cursor.moveToFirst()) {
            int columnIndex1 = cursor.getColumnIndex(COLUMNS_USBNETWORK.get(1));
            int columnIndex2 = cursor.getColumnIndex(COLUMNS_USBNETWORK.get(2));
            int columnIndex3 = cursor.getColumnIndex(COLUMNS_USBNETWORK.get(3));
            int columnIndex4 = cursor.getColumnIndex(COLUMNS_USBNETWORK.get(4));
            int columnIndex5 = cursor.getColumnIndex(COLUMNS_USBNETWORK.get(5));

            String columnValue1 = columnIndex1 != -1 ? cursor.getString(columnIndex1) : null;
            String columnValue2 = columnIndex2 != -1 ? cursor.getString(columnIndex2) : null;
            String columnValue3 = columnIndex3 != -1 ? cursor.getString(columnIndex3) : null;
            String columnValue4 = columnIndex4 != -1 ? cursor.getString(columnIndex4) : null;
            String columnValue5 = columnIndex5 != -1 ? cursor.getString(columnIndex5) : null;

            usbArsenalUSBNetworkModel.setupstream_iface(columnValue1);
            usbArsenalUSBNetworkModel.setusb_iface(columnValue2);
            usbArsenalUSBNetworkModel.setip_address_for_target(columnValue3);
            usbArsenalUSBNetworkModel.setip_gateway(columnValue4);
            usbArsenalUSBNetworkModel.setip_subnetmask(columnValue5);
        }
        cursor.close();
        return usbArsenalUSBNetworkModel;
    }

    public boolean setUSBSwitchColumnData(String functionName, int targetColumnIndex, String targetOSName, String content){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("UPDATE " + USBSWITCH_TABLE_NAME + " SET " +
                    COLUMNS_USBSWITCH.get(targetColumnIndex) + " = '" + content + "'" +
                    " WHERE " + COLUMNS_USBSWITCH.get(0) + " = '" + targetOSName + "'" + " AND " + COLUMNS_USBSWITCH.get(1) + " = '" + functionName + "';");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean setUSBNetworkColumnData(int usbNetworkAttackModePosition, USBArsenalUSBNetworkModel usbArsenalUSBNetworkModel){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("UPDATE " + USBNETWORK_TABLE_NAME + " SET " +
                    COLUMNS_USBNETWORK.get(1) + " = '" + usbArsenalUSBNetworkModel.getupstream_iface() + "'," +
                    COLUMNS_USBNETWORK.get(2) + " = '" + usbArsenalUSBNetworkModel.getusb_iface() + "'," +
                    COLUMNS_USBNETWORK.get(3) + " = '" + usbArsenalUSBNetworkModel.getip_address_for_target() + "'," +
                    COLUMNS_USBNETWORK.get(4) + " = '" + usbArsenalUSBNetworkModel.getip_gateway() + "'," +
                    COLUMNS_USBNETWORK.get(5) + " = '" + usbArsenalUSBNetworkModel.getip_subnetmask() + "'" +
                    " WHERE " + COLUMNS_USBNETWORK.get(0) + " = '" + usbNetworkAttackModePosition + "';");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean resetData(){
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS " + USBSWITCH_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + USBNETWORK_TABLE_NAME);
            onCreate(db);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String backupData(String storedDBpath) {
        try {
            File data = Environment.getDataDirectory();
            File sd = Environment.getExternalStorageDirectory();
            String currentDBPath = data.getAbsolutePath() + "/data/" + BuildConfig.APPLICATION_ID + "/databases/" + getDatabaseName();
            if (sd.canWrite()) {
                File currentDB = new File(currentDBPath);
                File backupDB = new File(storedDBpath);
                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
        return null;
    }

    public String restoreData(String storedDBpath) {
        if (!new File(storedDBpath).exists()){
            return "数据库文件未找到. ";
        }

        SQLiteDatabase tempDB = SQLiteDatabase.openDatabase(storedDBpath, null, SQLiteDatabase.OPEN_READONLY);
        if (tempDB.getVersion() > this.getReadableDatabase().getVersion()) {
            tempDB.close();
            return "无法恢复数据库. \n原因: 备份数据库版本高于当前数据库版本. ";
        }
        tempDB.close();

        if (!verifyDB(storedDBpath)) {
            return "数据库格式无效. ";
        }

        try {
            File data = Environment.getDataDirectory();
            File sd = Environment.getExternalStorageDirectory();
            String currentDBPath = data.getAbsolutePath() + "/data/" + BuildConfig.APPLICATION_ID + "/databases/" + getDatabaseName();
            if (sd.canWrite()) {
                File currentDB = new File(currentDBPath);
                File backupDB = new File(storedDBpath);
                if (backupDB.exists()) {
                    FileChannel src = new FileInputStream(backupDB).getChannel();
                    FileChannel dst = new FileOutputStream(currentDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return e.toString();
        }
        return null;
    }

    private boolean verifyDB(String storedDBpath){
        SQLiteDatabase tempDB = SQLiteDatabase.openDatabase(storedDBpath, null, SQLiteDatabase.OPEN_READWRITE);
        boolean isDBValid = true;
        if (ifTableExists(tempDB, USBSWITCH_TABLE_NAME)) {
            Cursor c = tempDB.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + USBSWITCH_TABLE_NAME + "'", null);
            if (c.getCount()==1){
                c.close();
                c = tempDB.query(USBSWITCH_TABLE_NAME, null, null, null, null, null, null);
                String[] tempColumnNames = c.getColumnNames();
                c.close();
                if (tempColumnNames.length == COLUMNS_USBSWITCH.size()) {
                    for (int i = 0; i < tempColumnNames.length; i++){
                        if (!tempColumnNames[i].equals(COLUMNS_USBSWITCH.get(i))){
                            isDBValid = false;
                            break;
                        }
                    }
                } else isDBValid = false;
            } else isDBValid = false;
        } else isDBValid = false;
        if (isDBValid) {
            if (ifTableExists(tempDB, USBNETWORK_TABLE_NAME)) {
                Cursor c = tempDB.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + USBNETWORK_TABLE_NAME + "'", null);
                if (c.getCount()==1){
                    c.close();
                    c = tempDB.query(USBNETWORK_TABLE_NAME, null, null, null, null, null, null);
                    String[] tempColumnNames = c.getColumnNames();
                    c.close();
                    if (tempColumnNames.length == COLUMNS_USBNETWORK.size()) {
                        for (int i = 0; i < tempColumnNames.length; i++){
                            if (!tempColumnNames[i].equals(COLUMNS_USBNETWORK.get(i))){
                                isDBValid = false;
                                break;
                            }
                        }
                    } else isDBValid = false;
                } else isDBValid = false;
            } else isDBValid = false;
        }
        tempDB.close();
        return isDBValid;
    }

    private boolean ifTableExists (SQLiteDatabase tempDB, String tableName) {
        boolean tableExists = false;
        try {
            Cursor c = tempDB.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'", null);
            if (c.getCount()==1) {
                tableExists = true;
            }
            c.close();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return tableExists;
    }
}
