package com.offsec.nethunter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

class SQLPersistence extends SQLiteOpenHelper {
    private final static int DATABASE_VERSION = 1;
    private final static String DATABASE_NAME = "KaliLaunchers";

    /* 创建启动器表 */
    private final static String CREATE_LAUNCHER_TABLE = "CREATE TABLE " +
            LauncherApp.TABLE + " (" +
            LauncherApp.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            LauncherApp.BTN_LABEL + " TEXT, " +
            LauncherApp.CMD + " TEXT )";

    public SQLPersistence(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LAUNCHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /* 若表已存在则删除旧表再创建新表 */
        db.execSQL("DROP TABLE IF EXISTS " + LauncherApp.TABLE);
        this.onCreate(db);
    }

    /* 添加应用 */
    public long addApp(final String btn_name, final String command) {
        long id = 0;
        if (!btn_name.isEmpty() &&
                !command.isEmpty()) {

            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(LauncherApp.BTN_LABEL, btn_name);
            values.put(LauncherApp.CMD, command);

            id = db.insert(LauncherApp.TABLE, null, values);
            db.close();
        }
        return id;
    }

    /* 获取所有应用 */
    public List<LauncherApp> getAllApps() {
        List<LauncherApp> apps = new LinkedList<>();
        String query = "SELECT  * FROM " + LauncherApp.TABLE;

        SQLiteDatabase db = this.getWritableDatabase();
        try (Cursor cursor = db.rawQuery(query, null)) {
            LauncherApp app;
            if (cursor.moveToFirst()) {
                do {
                    app = new LauncherApp();
                    app.setId(Long.parseLong(cursor.getString(0)));
                    app.setBtn_label(cursor.getString(1));
                    app.setCommand(cursor.getString(2));
                    apps.add(app);
                } while (cursor.moveToNext());
            }
        }
        return apps;
    }

    /* 根据 ID 获取单个应用 */
    public LauncherApp getApp(final long id) {
        LauncherApp app = null;
        if (id != 0) {
            SQLiteDatabase db = this.getReadableDatabase();

            try (Cursor cursor = db.query(LauncherApp.TABLE,
                    LauncherApp.COLUMNS,
                    " id = ?",
                    new String[]{String.valueOf(id)},
                    null,
                    null,
                    null,
                    null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    app = new LauncherApp();
                    app.setId(Long.parseLong(cursor.getString(0)));
                    app.setBtn_label(cursor.getString(1));
                    app.setCommand(cursor.getString(2));
                }
            }
        }
        return app;
    }

    /* 更新应用 */
    public void updateApp(final LauncherApp app) {
        if (app != null) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(LauncherApp.BTN_LABEL, app.getBtn_label());
            values.put(LauncherApp.CMD, app.getCommand());

            db.update(LauncherApp.TABLE, values,
                    LauncherApp.ID + " = ?",
                    new String[]{String.valueOf(app.getId())});

            db.close();
        }
    }

    /* 删除应用 */
    public void deleteApp(final long id) {
        if (id != 0) {
            SQLiteDatabase db = this.getWritableDatabase();

            db.delete(LauncherApp.TABLE, LauncherApp.ID + " = ?",
                    new String[]{String.valueOf(id)});

            db.close();
        }
    }
}