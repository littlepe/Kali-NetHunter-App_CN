package com.offsec.nethunter.SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;

import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.models.NethunterModel;
import com.offsec.nethunter.utils.NhPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class NethunterSQL extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "NethunterFragment";
    private static NethunterSQL instance;
    public static final String TAG = "NethunterSQL";
    private static final String TABLE_NAME = DATABASE_NAME;
    private static final ArrayList<String> COLUMNS = new ArrayList<>();
    private static final String[][] nethunterData = {
            {"1", "内核版本", "uname -a", "\\n", "1"},
            {"2", "Busybox 版本", Environment.getDataDirectory().getAbsolutePath() + "/data/com.offsec.nethunter/scripts/bin/busybox_nh | head -n1", "\\n", "1"},
            {"3", "Root 状态", "su -v", "\\n", "1"},
            {"4", "HID 状态", "[ -n \"$(ls /dev/hidg* 2>/dev/null)\" ] && ls /dev/hidg* || { echo \"未找到 HID 接口. \"; if [[ $(uname -r | cut -d. -f1) -ge 4 ]]; then echo \"请在 USB Arsenal 中启用. \"; fi }", "\\n", "1"},
            {"5", "CAN 状态", NhPaths.BUSYBOX + " ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$' || echo \"未找到 CAN 接口. \n请在 CAN Arsenal 中启用. \"", "\\n", "1"},
            {"6", "NetHunter 终端状态", "[ \"$(pm list packages | grep 'com.offsec.nhterm')\" ] && echo \"NetHunter 终端已安装. \" || echo \"NetHunter 终端尚未安装. \"", "\\n", "1"},
            {"7", "网络接口状态", " ip -o addr show | " + NhPaths.BUSYBOX + " awk '{print $2, $3, $4}'", "\\n", "1"},
            {"8", "外网 IP", NhPaths.BUSYBOX + " which wget > /dev/null 2>&1 && " + NhPaths.BUSYBOX + " wget -qO - icanhazip.com || " + NhPaths.BUSYBOX + " curl -s ipv4.icanhazip.com", "\\n", "0"}
    };

    public static synchronized NethunterSQL getInstance(Context context){
        if (instance == null) {
            instance = new NethunterSQL(context.getApplicationContext());
        }
        return instance;
    }

    private NethunterSQL(Context context) {
        super(context, DATABASE_NAME, null, 1);
        COLUMNS.add("id");
        COLUMNS.add("TitleName");
        COLUMNS.add("CommandforResult");
        COLUMNS.add("Delimiter");
        COLUMNS.add("RunOnCreate");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMNS.get(0) + " INTEGER, " +
                COLUMNS.get(1) + " TEXT, " + COLUMNS.get(2) +  " TEXT, " + COLUMNS.get(3) + " TEXT, " + COLUMNS.get(4) + " TEXT)");
        ContentValues initialValues = new ContentValues();
        db.beginTransaction();
        for (String[] data: nethunterData){
            initialValues.put(COLUMNS.get(0), data[0]);
            initialValues.put(COLUMNS.get(1), data[1]);
            initialValues.put(COLUMNS.get(2), data[2]);
            initialValues.put(COLUMNS.get(3), data[3]);
            initialValues.put(COLUMNS.get(4), data[4]);
            db.insert(TABLE_NAME, null, initialValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        this.onCreate(db);
    }

    public List<NethunterModel> bindData(List<NethunterModel> nethunterModelArrayList) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMNS.get(0) + ";", null);
        while (cursor.moveToNext()) {
            int columnIndex1 = cursor.getColumnIndex(COLUMNS.get(1));
            int columnIndex2 = cursor.getColumnIndex(COLUMNS.get(2));
            int columnIndex3 = cursor.getColumnIndex(COLUMNS.get(3));
            int columnIndex4 = cursor.getColumnIndex(COLUMNS.get(4));

            String columnValue1 = columnIndex1 != -1 ? cursor.getString(columnIndex1) : null;
            String columnValue2 = columnIndex2 != -1 ? cursor.getString(columnIndex2) : null;
            String columnValue3 = columnIndex3 != -1 ? cursor.getString(columnIndex3) : null;
            String columnValue4 = columnIndex4 != -1 ? cursor.getString(columnIndex4) : null;

            nethunterModelArrayList.add(new NethunterModel(
                    columnValue1,
                    columnValue2,
                    columnValue3,
                    columnValue4,
                    "".split("\\n")
            ));
        }
        cursor.close();
        db.close();
        return nethunterModelArrayList;
    }

    public void addData(int targetPositionId, List<String> addData){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = new ContentValues();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " + 1 WHERE " + COLUMNS.get(0) + " >= " + targetPositionId + ";");
        initialValues.put(COLUMNS.get(0), targetPositionId);
        initialValues.put(COLUMNS.get(1), addData.get(0));
        initialValues.put(COLUMNS.get(2), addData.get(1));
        initialValues.put(COLUMNS.get(3), addData.get(2));
        initialValues.put(COLUMNS.get(4), addData.get(3));
        db.beginTransaction();
        db.insert(TABLE_NAME, null, initialValues);
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public void deleteData(List<Integer> selectedTargetIds){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMNS.get(0) + " in (" + TextUtils.join(",", selectedTargetIds) + ");");
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMNS.get(0) + ";", null);

        while (cursor.moveToNext()) {
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + cursor.getPosition() + " + 1 WHERE " + COLUMNS.get(0) + " = " + cursor.getInt(0) + ";");
        }
        cursor.close();
        db.close();
    }

    public void moveData(Integer originalPosition, Integer targetPosition){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = -1 WHERE " + COLUMNS.get(0) + " = " + (originalPosition + 1) + ";");
        if (originalPosition < targetPosition) {
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " - 1 WHERE " + COLUMNS.get(0) + " > " +
                    (originalPosition + 1) + " AND " + COLUMNS.get(0) + " <= " + (targetPosition + 1) + ";");
        } else {
            db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " + 1 WHERE " + COLUMNS.get(0) + " > " +
                    targetPosition + " AND " + COLUMNS.get(0) + " <= " + (originalPosition + 1) + ";");
        }
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + (targetPosition + 1) + " WHERE " + COLUMNS.get(0) + " = -1;");
        db.close();
    }

    public void editData(Integer targetPosition, List<String> editData){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(1) + " = '" + editData.get(0).replace("'", "''") + "', " +
                COLUMNS.get(2) + " = '" + editData.get(1).replace("'", "''") + "', " +
                COLUMNS.get(3) + " = '" + editData.get(2).replace("'", "''") + "', " +
                COLUMNS.get(4) + " = '" + editData.get(3).replace("'", "''") + "'" +
                " WHERE " + COLUMNS.get(0) + " = " + (targetPosition + 1));
        db.close();
    }

    public void resetData(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMNS.get(0) + " INTEGER, " +
                COLUMNS.get(1) + " TEXT, " + COLUMNS.get(2) +  " TEXT, " + COLUMNS.get(3) + " TEXT, " + COLUMNS.get(4) + " TEXT)");
        ContentValues initialValues = new ContentValues();
        db.beginTransaction();
        for (String[] data: nethunterData){
            initialValues.put(COLUMNS.get(0), data[0]);
            initialValues.put(COLUMNS.get(1), data[1]);
            initialValues.put(COLUMNS.get(2), data[2]);
            initialValues.put(COLUMNS.get(3), data[3]);
            initialValues.put(COLUMNS.get(4), data[4]);
            db.insert(TABLE_NAME, null, initialValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
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
            return "db file not found.";
        }
        if (!verifyDB(storedDBpath)) {
            return "invalid columns format.";
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
            return e.toString();
        }
        return null;
    }

    private boolean verifyDB(String storedDBpath){
        SQLiteDatabase tempDB = SQLiteDatabase.openDatabase(storedDBpath, null, SQLiteDatabase.OPEN_READWRITE);
        Cursor c = tempDB.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NAME + "'", null);
        if (c.getCount()==1){
            c.close();
            c = tempDB.query(TABLE_NAME, null, null, null, null, null, null);
            String[] tempColumnNames = c.getColumnNames();
            c.close();
            if (tempColumnNames.length != COLUMNS.size()) {
                tempDB.close();
                return false;
            }
            for (int i = 0; i < tempColumnNames.length; i++){
                if (!tempColumnNames[i].equals(COLUMNS.get(i))){
                    tempDB.close();
                    return false;
                }
            }
            tempDB.close();
            return true;
        }
        tempDB.close();
        return false;
    }
}