package com.offsec.nethunter.gps;


import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 该类是一个容器, 包含多个用于帮助生成 NMEA 数据的静态方法. 
 * <p>
 * NMEA 的一个很好的参考位于 https://www.gpsinformation.org/dale/nmea.htm
 */
final class NMEA {
    /**
     * 将 #Location 中的速度（节）格式化为字符串. 
     * 如果速度未知, 则返回空字符串. 
     */
    public static String formatSpeedKt(Location location) {
        String s = "";
        if (location.hasSpeed())
            // http://www.google.com/search?q=m%2Fs+to+kt
            s += (location.getSpeed() * 1.94384449);
        return s;
    }

    /**
     * 将 #Location 中的方位角格式化为字符串. 
     * 如果方位角未知, 则返回空字符串. 
     */
    public static String formatBearing(Location location) {
        String s = "";
        if (location.hasBearing())
            s += location.getBearing();
        return s;
    }

    public static String formatGpsGsa(GpsStatus gps) {
        String fix = "1";
        StringBuilder prn = new StringBuilder();
        int nbr_sat = 0;
        Iterator<GpsSatellite> satellites = gps.getSatellites().iterator();
        for (int i = 0; i < 12; i++) {
            if (satellites.hasNext()) {
                GpsSatellite sat = satellites.next();
                if (sat.usedInFix()) {
                    prn.append(sat.getPrn());
                    nbr_sat++;
                }
            }

            prn.append(",");
        }

        if (nbr_sat > 3)
            fix = "3";
        else if (nbr_sat > 0)
            fix = "2";

        //TODO: 计算 DOP 值
        return fix + "," + prn + ",,,";
    }

    public static List<String> formatGpsGsv(GpsStatus gps) {
        List<String> gsv = new ArrayList<>();
        int nbr_sat = 0;
        for (GpsSatellite sat : gps.getSatellites())
            nbr_sat++;

        Iterator<GpsSatellite> satellites = gps.getSatellites().iterator();
        for (int i = 0; i < 3; i++) {
            if (satellites.hasNext()) {
                StringBuilder g = new StringBuilder(Integer.toString(nbr_sat));
                for (int n = 0; n < 4; n++) {
                    if (satellites.hasNext()) {
                        GpsSatellite sat = satellites.next();
                        g.append(",").append(sat.getPrn()).append(",").append(sat.getElevation()).append(",").append(sat.getAzimuth()).append(",").append(sat.getSnr());
                    }
                }
                gsv.add(g.toString());
            }
        }
        return gsv;
    }
}