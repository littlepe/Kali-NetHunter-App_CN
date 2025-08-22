package com.offsec.nethunter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.LinkedList;
import java.util.List;

/* SearchSploit 数据库帮助类 */
class SearchSploitSQL extends SQLiteOpenHelper {
    private final ShellExecuter exe = new ShellExecuter();
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SearchSploit";
    SearchSploitSQL(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        /* 创建 exploits 表（若不存在） */
        String CREATE_SEARCHSPLOIT_TABLE = "CREATE TABLE  IF NOT EXISTS " + SearchSploit.TABLE +
                " (" + SearchSploit.ID + " INTEGER PRIMARY KEY, " +
                SearchSploit.FILE + " TEXT," +
                SearchSploit.DESCRIPTION + " TEXT," +
                SearchSploit.DATE + " TEXT," +
                SearchSploit.AUTHOR + " TEXT," +
                SearchSploit.TYPE + " TEXT," +
                SearchSploit.PLATFORM + " TEXT," +
                SearchSploit.PORT + " INTEGER DEFAULT 0)";

        database.execSQL(CREATE_SEARCHSPLOIT_TABLE);
        /* 禁用 WAL 日志, 兼容旧系统 */
        database.disableWriteAheadLogging();
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        /* 删除旧表并重建 */
        database.execSQL("DROP TABLE IF EXISTS " + SearchSploit.TABLE);
        onCreate(database);
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        onUpgrade(database, oldVersion, newVersion);
    }

    public void doDrop() {
        /* 手动删除表 */
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + SearchSploit.TABLE);
    }

    Boolean doDbFeed() {
        /* 1. 在 Kali chroot 里生成 CSV → SQLite 到 /tmp/SearchSploit */
        String _cmd1 = "su -c " + NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd 'usr/bin/python3 /sdcard/nh_files/modules/csv2sqlite.py /usr/share/exploitdb/files_exploits.csv /tmp/SearchSploit " + SearchSploit.TABLE + "'";
        exe.RunAsRootOutput(_cmd1);
        /* 2. 把数据库文件移动到应用可见目录 */
        String _cmd2 = "mv " + NhPaths.CHROOT_SYMLINK_PATH + "/tmp/SearchSploit /sdcard/nh_files/SearchSploit";
        exe.RunAsRootOutput(_cmd2);

        return true;
    }

    /* 获取总记录数 */
    long getCount() {
        String sql = "SELECT COUNT(*) FROM " + SearchSploit.TABLE;
        SQLiteDatabase db = this.getWritableDatabase();
        SQLiteStatement statement = db.compileStatement(sql);
        return statement.simpleQueryForLong();
    }

    /* 获取前100条 exploit 记录 */
    public List<SearchSploit> getAllExploits() {
        String query = "SELECT  * FROM " + SearchSploit.TABLE + " LIMIT 100";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<SearchSploit> _List = createExploitList(cursor);
        db.close();
        return _List;
    }

    /* 按描述、类型、平台过滤 */
    List<SearchSploit> getAllExploitsFiltered(String filter, String type, String platform) {
        String wildcard = "%" + filter + "%";
        String query = "SELECT * FROM " + SearchSploit.TABLE
                + " WHERE " + SearchSploit.DESCRIPTION + " like ?" +
                " and " + SearchSploit.TYPE + "='" + type + "'" +
                " and " + SearchSploit.PLATFORM + "='" + platform + "'" +
                " GROUP BY " + SearchSploit.ID;
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("查询语句", query);
        Cursor cursor = db.rawQuery(query, new String[]{wildcard});
        List<SearchSploit> _List = createExploitList(cursor);
        db.close();
        return _List;
    }

    /* 无过滤搜索（描述/作者/类型/平台） */
    List<SearchSploit> getAllExploitsRaw(String filter) {
        String wildcard = "%" + filter + "%";
        String query = "SELECT * FROM " + SearchSploit.TABLE
                + " WHERE ( " + SearchSploit.DESCRIPTION + " like ? or " + SearchSploit.AUTHOR + " like ? or " + SearchSploit.TYPE + " like ? or " + SearchSploit.PLATFORM + " like ? ) GROUP BY " + SearchSploit.ID;
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("EXPLOIT_QUERY", query);
        Cursor cursor = db.rawQuery(query, new String[]{wildcard, wildcard, wildcard, wildcard});
        List<SearchSploit> _List = createExploitList(cursor);
        db.close();
        return _List;
    }

    /* 根据游标创建 exploit 列表 */
    private List<SearchSploit> createExploitList(Cursor cursor) {
        List<SearchSploit> commandList = new LinkedList<>();
        if (cursor.moveToFirst()) {
            do {
                SearchSploit _exploit = new SearchSploit();
                _exploit.setId(cursor.getInt(0));                  // id
                _exploit.setFile(cursor.getString(1));             // 文件
                _exploit.setDescription(cursor.getString(2));      // 描述
                _exploit.setDate(cursor.getString(3));             // 日期
                _exploit.setAuthor(cursor.getString(4));           // 作者
                _exploit.setPlatform(cursor.getString(5));         // 平台
                _exploit.setType(cursor.getString(6));             // 类型
                _exploit.setPort(cursor.getInt(7));                // 端口
                commandList.add(_exploit);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return commandList;
    }

    /* 获取所有不重复的类型 */
    List<String> getTypes() {
        String query = "SELECT DISTINCT " + SearchSploit.TYPE +
                " FROM " + SearchSploit.TABLE +
                " ORDER BY " + SearchSploit.TYPE + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<String> _List = createStringList(cursor);
        db.close();
        return _List;
    }

    /* 获取所有不重复的平台 */
    List<String> getPlatforms() {
        String query = "SELECT DISTINCT " + SearchSploit.PLATFORM + " FROM " + SearchSploit.TABLE + " ORDER BY " + SearchSploit.PLATFORM + " ASC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        List<String> _List = createStringList(cursor);
        db.close();
        return _List;
    }

    /* 根据游标创建字符串列表 */
    private List<String> createStringList(Cursor cursor) {
        List<String> strList = new LinkedList<>();
        if (cursor.moveToFirst()) {
            do {
                strList.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return strList;
    }
}