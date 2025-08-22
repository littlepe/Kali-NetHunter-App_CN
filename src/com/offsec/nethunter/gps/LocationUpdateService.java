package com.offsec.nethunter.gps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.offsec.nethunter.AppNavHomeActivity;
import com.offsec.nethunter.KaliGpsServiceFragment;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.NhPaths;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Date;

public class LocationUpdateService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private static LocationUpdateService instance = null;
    private KaliGPSUpdates.Receiver updateReceiver;
    public static final String CHANNEL_ID = "NethunterLocationUpdateChannel";
    public static final int NOTIFY_ID = 1004;
    private static final String TAG = "LocationUpdateService";
    private static final String notificationTitle = "GPS 提供程序正在运行";
    private static final String notificationText = "正在发送 GPS 数据到 udp://127.0.0.1:" + NhPaths.GPS_PORT;
    private String lastLocationSourceReceived = "无";
    private String lastLocationSourcePublished = "无";
    private String lastNotificationText = null;
    private double lastLocationLatitude = 0.0;
    private double lastLocationLongitude = 0.0;
    private int lastLocationSats = 0;
    private double lastLocationAccuracy = 0.0;
    private InetAddress udpDestAddr = null;
    private DatagramSocket dSock = null;
    private Date lastLocationTime = new Date();
    private Handler timerTaskHandler = null;
    private Handler resetListenersTimerTaskHandler = null;
    private final IBinder binder = new ServiceBinder();

    // 这允许我们检查是否已有 LocationUpdateService 在运行, 而无需实际附加到它
    public static boolean isInstanceCreated() {
        return (instance != null);
    }

    // 如果我们通过 Intent 或命令行启动, 而不是通过应用程序启动, 则会调用此方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        requestUpdates(null); // 请求更新, 但没有小部件接收它们, 因为应用程序没有运行
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initTimers();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nethunter 持久通道服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * 将 #Location 中的卫星数量格式化为字符串. 
     * 如果使用 #LocationManager.NETWORK_PROVIDER, 则返回伪造值 "1", 
     * 因为某些软件拒绝使用 "0" 或空值. 
     */
    public String formatSatellites(Location location) {
        int satellites = 0;
        Bundle bundle = location.getExtras();
        if (bundle != null) {
            satellites = bundle.getInt("satellites");
        }

        if (satellites > 4) {
            return String.valueOf(satellites);
        }

        if (LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            return satellites == 0 ? "" : String.valueOf(satellites);
        }

        // 非 GPS 提供程序的回退值
        return "4";
    }

    /**
     * 将 #Location 中的海拔高度格式化为字符串, 带有第二个单位字段（"M" 表示米）. 
     * 如果海拔高度未知, 则返回两个空字段. 
     */
    public String formatAltitude(Location location) {
        StringBuilder s = new StringBuilder();
        if (location.hasAltitude())
            s.append(String.format("%.4f,M", location.getAltitude()));
        else
            s.append(",");
        return s.toString();
    }

    /**
     * 计算指定字符串的 NMEA 校验和. 
     * 在此处传递 '$' 和 '*' 之间的行部分. 
     */
    private String checksum(String s) {
        int checksum = 0;

        for (int i = 0; i < s.length(); i++)
            checksum = checksum ^ s.charAt(i);

        String hex = Integer.toHexString(checksum);
        if (hex.length() == 1)
            hex = "0" + hex;
        return ("*" + hex.toUpperCase());
    }

    /**
     * 将 #Location 中的时间格式化为字符串. 
     */
    @SuppressLint("DefaultLocale")
    public static String formatTime(Location location) {
        DateTimeFormatter dtf = DateTimeFormat.forPattern("HHmmss");
        return dtf.print(new DateTime(location.getTime()));
    }

    /**
     * 将 #Location 中的表面位置（纬度和经度）格式化为字符串. 
     */
    public static String formatPosition(Location location) {
        double latitude = location.getLatitude();
        char nsSuffix = latitude < 0 ? 'S' : 'N';
        latitude = Math.abs(latitude);

        double longitude = location.getLongitude();
        char ewSuffix = longitude < 0 ? 'W' : 'E';
        longitude = Math.abs(longitude);
        @SuppressLint("DefaultLocale") String lat = String.format("%02d%02d.%04d,%c",
                (int) latitude,
                (int) (latitude * 60) % 60,
                (int) (latitude * 60 * 10000) % 10000,
                nsSuffix);
        @SuppressLint("DefaultLocale") String lon = String.format("%03d%02d.%04d,%c",
                (int) longitude,
                (int) (longitude * 60) % 60,
                (int) (longitude * 60 * 10000) % 10000,
                ewSuffix);
        return lat + "," + lon;
    }

    public class ServiceBinder extends Binder {
        public LocationUpdateService getService() {
            return LocationUpdateService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        this.startService(intent);
        return binder;
    }

    public void requestUpdates(KaliGPSUpdates.Receiver receiver) {
        Log.d(TAG, "在 requestUpdates 中");
        if (receiver != null)
            this.updateReceiver = receiver;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startLocationUpdates();
        }
    }

    public void stopUpdates() {
        Log.d(TAG, "在 stopUpdates 中");
        stopSelf();
    }

    private boolean locationUpdatesStarted = false;
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startLocationUpdates() {
        if (locationUpdatesStarted)
            return;
        locationUpdatesStarted = true;
        Log.d(TAG, "在 startLocationUpdates 中");

        final LocationRequest lr = new LocationRequest.Builder(LocationRequest.PRIORITY_HIGH_ACCURACY, 1000L / 2L)
                .setMinUpdateIntervalMillis(100L)
                .setMaxUpdateDelayMillis(600L)
                .setDurationMillis(1000 * 3600 * 2)
                .build();

        Log.d(TAG, "请求 Marshmallow 权限");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // 如果尚未初始化, 则初始化 fusedLocationClient
            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            }

            // 注册位置服务, 以便我们可以构造伪造的 NMEA 数据
            fusedLocationClient.requestLocationUpdates(lr, locationListener, null);

            // 尝试注册直接从 GPS 获取的实际 NMEA 数据
            LocationManager locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
            try {
                Method addNmeaListener =
                        LocationManager.class.getMethod("addNmeaListener", GpsStatus.NmeaListener.class);
                addNmeaListener.invoke(locationManager, nmeaListener);
                Log.d(TAG, "addNmeaListener 成功");
            } catch (Exception exception) {
                Log.d(TAG, "添加 NMEA 监听器失败: " + exception.getMessage());
            }
        }

        // 开启持久通知, 以便即使在后天也能继续获取位置更新
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        // TODO 使此结果意图打开 NH 应用程序
        Intent resultIntent = new Intent(this, AppNavHomeActivity.class);
        resultIntent.putExtra("menuFragment", R.id.gps_item);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentTitle(notificationTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(resultPendingIntent);
        Notification notification = builder.build();
        notificationManagerCompat.notify(NOTIFY_ID, notification);

        this.startForeground(NOTIFY_ID, notification);
        // 启动一个定时器, 每秒更新我们的通知
        Log.d(TAG, "启动通知更新定时器");
        startTimers();
    }

    private void initTimers() {
        timerTaskHandler = new Handler();
        resetListenersTimerTaskHandler = new Handler();
    }

    private void startTimers() {
        timerTask.run();
        // Android 将在请求后两小时停止向我们发送更新. 
        // 因此, 每小时我们将发出一个新请求
        resetListenersTimerTaskHandler.postDelayed(resetListenersTimerTask, 3600*1000);
        // resetListenersTimerTask.run();
    }

    private void stopTimers() {
        timerTaskHandler.removeCallbacks(timerTask);
        resetListenersTimerTaskHandler.removeCallbacks(resetListenersTimerTask);
    }

    private final Runnable resetListenersTimerTask = () -> {
        // 重置我们的监听器
        Log.d(TAG, "重新启动监听器");
        stopLocationUpdates();
        requestUpdates(null);
    };

    private final Runnable timerTask = new Runnable() {
        @Override
        public void run() {
            try {
                if (lastLocationLatitude != 0.0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        updateNotification();
                    }
                // Log.d(TAG, "TimerTask: " + lastLocationLatitude + ", " + lastLocationLongitude);
            } catch (Exception e) {
                Log.d(TAG, "TimerTask 异常: " + e);
                // e.printStackTrace();
            } finally {
                // 每秒一次
                timerTaskHandler.postDelayed(timerTask, 1000);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateNotification() {
        Date now = new Date();
        long age = (now.getTime() - lastLocationTime.getTime()) / 1000;
        String ageStr;
        if (age <= 10)
            ageStr = "当前";
        else if (age < 60)
            ageStr = age + "秒";
        else if (age < 3600)
            ageStr = (age / 60) + "分钟";
        else
            ageStr = (age / 3600) + "小时";
        String updatedText = String.format("纬度: %1.5f  经度: %1.5f  +/- %1.1f米  来源: %s  时间: %s  卫星: %d",
                lastLocationLatitude, lastLocationLongitude, lastLocationAccuracy,
                lastLocationSourcePublished, ageStr, lastLocationSats);

        if (updatedText.equals(lastNotificationText))
            return;
        lastNotificationText = updatedText;

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        Intent resultIntent = new Intent(this, AppNavHomeActivity.class);
        resultIntent.putExtra("menuFragment", R.id.gps_item);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.gps_notification);
        contentView.setTextViewText(R.id.gps_notification_latitude, String.format("%1.5f", lastLocationLatitude));
        contentView.setTextViewText(R.id.gps_notification_longitude, String.format("%1.5f", lastLocationLongitude));
        contentView.setTextViewText(R.id.gps_notification_accuracy, String.format("%1.1f米", lastLocationAccuracy));
        contentView.setTextViewText(R.id.gps_notification_source, lastLocationSourcePublished);
        contentView.setTextViewText(R.id.gps_notification_age, ageStr);
        if (lastLocationSourcePublished.equals("GPS"))
            contentView.setTextViewText(R.id.gps_notification_sats, String.format("%d", lastLocationSats));
        else
            contentView.setTextViewText(R.id.gps_notification_sats, "-");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setAutoCancel(false)
                .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                .setContent(contentView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(contentView)
                .setContentTitle(notificationTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(resultPendingIntent);
        Notification notification = builder.build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "POST_NOTIFICATIONS 权限未授予. 通知未发送. ");
            return;
        }
        notificationManagerCompat.notify(NOTIFY_ID, notification);
        Log.d(TAG, "通知已发送: " + updatedText);
    }

    private final GpsStatus.NmeaListener nmeaListener = (l, s) -> {
        if(!s.startsWith("$GPGGA")) {
            // 如果我们使用真实 GPS 作为来源, 则将这些额外信息字符串发送到 gpsd
            if("GPS".equals(lastLocationSourcePublished))
                sendUdpPacket(s);
            return;
        }
        String[] fields = s.split(",");
        int fixType = 0;
        // int sats = 0;
        try {
            fixType = Integer.parseInt(fields[6]);
            // sats = Integer.parseInt(fields[7]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }
        if (fixType == 0)
            return;
        // Log.d(TAG, "sats = " + sats);
        Log.d(TAG, "真实 NMEA: " + s);
        lastLocationSourceReceived = "NmeaListener";
        publishLocation(s, "GPS");
    };

    private boolean firstupdate = true;
    private final LocationListener locationListener = location -> {
        String nmeaSentence = nmeaSentenceFromLocation(location);

        Log.d(TAG, "构造的 NMEA: "+nmeaSentence);
        // 如果我们当前没有从 NmeaListener 获取真实的句子, 我们将只发布这些构造的句子
        if(lastLocationSourceReceived.equals("LocationListener"))
            publishLocation(nmeaSentence, "网络");
        lastLocationSourceReceived = "LocationListener";
    };

    private void publishLocation(String nmeaSentence, String source) {
        // 允许在主线程中进行网络操作的变通方法
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            String[] fields = nmeaSentence.split(",");
            String latStr = fields[2];
            String ns = fields[3];
            String lonStr = fields[4];
            String ew = fields[5];
            int sats = Integer.parseInt(fields[7]);
            double accuracy = Float.parseFloat(fields[8]) * 19.0; // 为什么是 19.0？参见 https://gitlab.com/gpsd/gpsd/-/blob/master/libgpsd_core.c, P_UERE_NO_DGPS

            int latDeg = Integer.parseInt(latStr.substring(0, 2));
            double latMin = Float.parseFloat(latStr.substring(2));
            double lat = latDeg + latMin/60.0;
            int lonDeg = Integer.parseInt(lonStr.substring(0, 3));
            double lonMin = Float.parseFloat(lonStr.substring(3));
            double lon = lonDeg + lonMin/60.0;
            if (ns.equalsIgnoreCase("S"))
                lat *= -1;
            if (ew.equalsIgnoreCase("W"))
                lon *= -1;

            synchronized (this) {
                lastLocationLatitude = lat;
                lastLocationLongitude = lon;
            }
            lastLocationSats = sats;
            lastLocationAccuracy = accuracy;
            lastLocationTime = new Date();
            lastLocationSourcePublished = source;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
        }

        if (updateReceiver != null) {
            if (firstupdate) {
                firstupdate = false;
                updateReceiver.onFirstPositionUpdate();
            }
            updateReceiver.onPositionUpdate(nmeaSentence);
        }

        sendUdpPacket(nmeaSentence);
    }

    private void initializeUdpComponents() {
        if (udpDestAddr == null) {
            try {
                udpDestAddr = Inet4Address.getByName("127.0.0.1");
            } catch (UnknownHostException e) {
                Log.d(TAG, "UnknownHostException: " + e);
            }
        }

        if (dSock == null) {
            try {
                dSock = new DatagramSocket();
            } catch (final java.net.SocketException e) {
                Log.d(TAG, "SocketException: " + e);
                dSock = null;
            }
        }
    }

    private void sendUdpPacket(String nmeaSentence) {
        initializeUdpComponents();

        if (udpDestAddr == null || dSock == null) {
            Log.d(TAG, "UDP 目标地址或套接字为空. 数据包未发送. ");
            return;
        }

        try {
            nmeaSentence += "\n";
            byte[] buf = nmeaSentence.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, udpDestAddr, NhPaths.GPS_PORT);
            dSock.send(packet);
        } catch (final IOException e) {
            Log.d(TAG, "IOException: " + e);
            dSock = null;
            udpDestAddr = null;
        }
    }

    private String nmeaSentenceFromLocation(Location location) {
        // 来自: https://github.com/ya-isakov/blue-nmea-mirror/blob/master/src/Source.java
        String time = formatTime(location);
        String position = formatPosition(location);
        String accuracy = String.format("%.4f", location.getAccuracy()/19.0); // 为什么是 19.0？参见 https://gitlab.com/gpsd/gpsd/-/blob/master/libgpsd_core.c, P_UERE_NO_DGPS
        String innerSentence = String.format("GPGGA,%s,%s,1,%s,%s,%s,,,,", time, position, formatSatellites(location),
                accuracy, formatAltitude(location));

        // 添加校验和和初始 $
        String checksum = checksum(innerSentence);
        return "$" + innerSentence + checksum;
    }

    private void stopLocationUpdates() {
        locationUpdatesStarted = false;
        LocationManager locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
        try {
            Method removeNmeaListener =
                    LocationManager.class.getMethod("removeNmeaListener", GpsStatus.NmeaListener.class);
            removeNmeaListener.invoke(locationManager, nmeaListener);
            Log.d(TAG, "removeNmeaListener 成功");
        } catch (Exception exception) {
            // 忽略
        }
        fusedLocationClient.removeLocationUpdates(locationListener);
    }

    private void requestPostNotificationsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context instanceof Activity) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1
            );
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "OnDestroy");
        instance = null;
        firstupdate = true;

        // 停止我们的通知更新定时器
        stopTimers();
        stopLocationUpdates();
        super.onDestroy();
    }
}