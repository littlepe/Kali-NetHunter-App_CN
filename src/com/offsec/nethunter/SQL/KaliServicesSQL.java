package com.offsec.nethunter.SQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;

import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.models.KaliServicesModel;
import com.offsec.nethunter.utils.NhPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;


/**
 * Kali 服务数据库操作类
 * 用于管理 Kali 服务的增删改查以及数据库备份恢复
 */
public class KaliServicesSQL extends SQLiteOpenHelper {
	private static KaliServicesSQL instance;
	private static final String DATABASE_NAME = "KaliServicesFragment"; // 数据库名称
	public static final String TAG = "KaliServicesSQL"; // 日志标签
	private static final String TABLE_NAME = DATABASE_NAME; // 数据表名称
	private static final ArrayList<String> COLUMNS = new ArrayList<>(); // 数据表列名
	private static final String[][] kaliserviceData = { // 默认 Kali 服务数据
			{"1", "APACHE2", "service apache2 start", "service apache2 stop", "apache2", "0"},
			{"2", "BLUETOOTH", "service bluetooth start", "service bluetooth stop", "bluetoothd", "0"},
			{"3", "DBUS", "service dbus start", "service dbus stop", "dbus-daemon", "0"},
			{"4", "DNSMASQ", "service dnsmasq start", "service dnsmasq stop", "dnsmasq", "0"},
			{"5", "GPSD", "service gpsd start", "service gpsd stop", "gpsd", "0"},
			{"6", "POSTGRESQL", "service postgresql start", "service postgresql stop", "postgres", "0"},
			{"7", "SSH", "service ssh start", "service ssh stop", "sshd", "0"},
			{"8", "NETWORKING", "service networking start", "service networking stop", "networking", "0"},
	};

	/**
	 * 获取单例实例
	 * @param context 应用上下文
	 * @return KaliServicesSQL 实例
	 */
	public static synchronized KaliServicesSQL getInstance(Context context){
		if (instance == null) {
			instance = new KaliServicesSQL(context.getApplicationContext());
		}
		return instance;
	}

	private KaliServicesSQL(Context context) {
		super(context, DATABASE_NAME, null, 1);
		// 添加数据表列名
		COLUMNS.add("id");
		COLUMNS.add("ServiceName");
		COLUMNS.add("CommandforStartService");
		COLUMNS.add("CommandforStopService");
		COLUMNS.add("CommandforCheckServiceStatus");
		COLUMNS.add("RunOnChrootStart");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// 创建数据表
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMNS.get(0) + " INTEGER, " +
				COLUMNS.get(1) + " TEXT, " + COLUMNS.get(2) +  " TEXT, " +
				COLUMNS.get(3) + " TEXT, " + COLUMNS.get(4) + " TEXT, " +
				COLUMNS.get(5) + " INTEGER)");
		ContentValues initialValues = new ContentValues();
		db.beginTransaction();
		for (String[] data : kaliserviceData) {
			initialValues.put(COLUMNS.get(0), data[0]);
			initialValues.put(COLUMNS.get(1), data[1]);
			initialValues.put(COLUMNS.get(2), data[2]);
			initialValues.put(COLUMNS.get(3), data[3]);
			initialValues.put(COLUMNS.get(4), data[4]);
			initialValues.put(COLUMNS.get(5), data[5]);
			db.insert(TABLE_NAME, null, initialValues);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// 升级数据库时重建表
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		this.onCreate(db);
	}

	/**
	 * 查询并绑定数据
	 * @param kaliServicesModelArrayList 数据列表
	 * @return 查询到的数据列表
	 */
	public List<KaliServicesModel> bindData(List<KaliServicesModel> kaliServicesModelArrayList) {
		SQLiteDatabase db = getWritableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMNS.get(0) + ";", null);
		while (cursor.moveToNext()) {
			int columnIndex1 = cursor.getColumnIndex(COLUMNS.get(1));
			int columnIndex2 = cursor.getColumnIndex(COLUMNS.get(2));
			int columnIndex3 = cursor.getColumnIndex(COLUMNS.get(3));
			int columnIndex4 = cursor.getColumnIndex(COLUMNS.get(4));
			int columnIndex5 = cursor.getColumnIndex(COLUMNS.get(5));

			String columnValue1 = columnIndex1 != -1 ? cursor.getString(columnIndex1) : null;
			String columnValue2 = columnIndex2 != -1 ? cursor.getString(columnIndex2) : null;
			String columnValue3 = columnIndex3 != -1 ? cursor.getString(columnIndex3) : null;
			String columnValue4 = columnIndex4 != -1 ? cursor.getString(columnIndex4) : null;
			String columnValue5 = columnIndex5 != -1 ? cursor.getString(columnIndex5) : null;

			kaliServicesModelArrayList.add(new KaliServicesModel(
					columnValue1,
					columnValue2,
					columnValue3,
					columnValue4,
					columnValue5,
					"[-] 服务未运行"
			));
		}
		cursor.close();
		db.close();
		return kaliServicesModelArrayList;
	}

	/**
	 * 添加数据
	 * @param targetPositionId 目标位置 ID
	 * @param Data 数据列表
	 */
	public void addData(int targetPositionId, List<String> Data) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues initialValues = new ContentValues();
		db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " + 1 WHERE " + COLUMNS.get(0) + " >= " + targetPositionId + ";");
		initialValues.put(COLUMNS.get(0), targetPositionId);
		initialValues.put(COLUMNS.get(1), Data.get(0));
		initialValues.put(COLUMNS.get(2), Data.get(1));
		initialValues.put(COLUMNS.get(3), Data.get(2));
		initialValues.put(COLUMNS.get(4), Data.get(3));
		initialValues.put(COLUMNS.get(5), Data.get(4));
		db.beginTransaction();
		db.insert(TABLE_NAME, null, initialValues);
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	/**
	 * 删除数据
	 * @param selectedTargetIds 选中的目标 ID 列表
	 */
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

	/**
	 * 移动数据
	 * @param originalPosition 原始位置
	 * @param targetPosition 目标位置
	 */
	public void moveData(Integer originalPosition, Integer targetPosition){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = 0 - 1 WHERE " + COLUMNS.get(0) + " = " + (originalPosition + 1) + ";");
		if (originalPosition < targetPosition){
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " - 1 WHERE " + COLUMNS.get(0) + " > " +
					(originalPosition + 1)  + " AND " + COLUMNS.get(0) + " < " + (targetPosition + 2) + ";");
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + (targetPosition + 1) + " WHERE " + COLUMNS.get(0) + " = -1;");
		} else {
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + COLUMNS.get(0) + " + 1 WHERE " + COLUMNS.get(0) + " > " +
					targetPosition  + " AND " + COLUMNS.get(0) + " < " + (originalPosition + 1) + ";");
			db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(0) + " = " + (targetPosition + 1) + " WHERE " + COLUMNS.get(0) + " = -1;");
		}
		db.close();
	}

	/**
	 * 编辑数据
	 * @param targetPosition 目标位置
	 * @param editData 编辑数据
	 */
	public void editData(Integer targetPosition, List<String> editData){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("UPDATE " + TABLE_NAME + " SET " + COLUMNS.get(1) + " = '" + editData.get(0).replace("'", "''") + "', " +
				COLUMNS.get(2) + " = '" + editData.get(1).replace("'", "''") + "', " +
				COLUMNS.get(3) + " = '" + editData.get(2).replace("'", "''") + "', " +
				COLUMNS.get(4) + " = '" + editData.get(3).replace("'", "''") + "', " +
				COLUMNS.get(5) + " = '" + editData.get(4).replace("'", "''") + "'" +
				" WHERE " + COLUMNS.get(0) + " = " + (targetPosition + 1));
		db.close();
	}

	/**
	 * 重置数据
	 */
	public void resetData(){
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMNS.get(0) + " INTEGER, " +
				COLUMNS.get(1) + " TEXT, " + COLUMNS.get(2) + " TEXT, " + COLUMNS.get(3) + " TEXT, " +
				COLUMNS.get(4) + " TEXT, " + COLUMNS.get(5) + " INTEGER)");
		ContentValues initialValues = new ContentValues();
		db.beginTransaction();
		for (String[] data : kaliserviceData){
			initialValues.put(COLUMNS.get(0), data[0]);
			initialValues.put(COLUMNS.get(1), data[1]);
			initialValues.put(COLUMNS.get(2), data[2]);
			initialValues.put(COLUMNS.get(3), data[3]);
			initialValues.put(COLUMNS.get(4), data[4]);
			initialValues.put(COLUMNS.get(5), data[5]);
			db.insert(TABLE_NAME, null, initialValues);
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}

	/**
	 * 备份数据
	 * @param storedDBpath 保存数据库路径
	 * @return 备份结果
	 */
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

	/**
	 * 恢复数据
	 * @param storedDBpath 恢复数据库路径
	 * @return 恢复结果
	 */
	public String restoreData(String storedDBpath) {
		if (!new File(storedDBpath).exists()){
			return "未找到数据库文件. ";
		}
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
			return e.toString();
		}
		return null;
	}

	/**
	 * 验证数据库
	 * @param storedDBpath 数据库路径
	 * @return 验证结果
	 */
	private boolean verifyDB(String storedDBpath) {
		SQLiteDatabase tempDB = SQLiteDatabase.openDatabase(storedDBpath, null, SQLiteDatabase.OPEN_READWRITE);
		Cursor c = tempDB.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='" + TABLE_NAME + "'", null);
		if (c.getCount() == 1) {
			c.close();
			c = tempDB.query(TABLE_NAME, null, null, null, null, null, null);
			String[] tempColumnNames = c.getColumnNames();
			c.close();
			if (tempColumnNames.length != COLUMNS.size()) {
				tempDB.close();
				return false;
			}
			for (int i = 0; i < tempColumnNames.length; i++) {
				if (!tempColumnNames[i].equals(COLUMNS.get(i))) {
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