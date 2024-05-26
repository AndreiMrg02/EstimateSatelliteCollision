package com.ucv.Util;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.Frame;

public class UtilConstant {
    public static String QUERY_GET_SATELLITE_BY_DATE = "FROM State WHERE satName = :satelliteName AND date BETWEEN :startDate AND :endDate";
    public static double MU = GravityFieldFactory.getNormalizedProvider(12, 12).getMu();
    public static Double ALTITUDIE_VALUE = 100d;
    public static Double LONGITUDE_VALUE = 100d;
    public static Double LATITUDE_VALUE = 100d;
    public static Double ELEVATION_VALUE = 500d;
    public static Frame ITRF;
    public static OneAxisEllipsoid EARTH;
    private static final String LOGOUT_PATH = "/ajaxauth/logout";
    private static final String POST_METHOD = "POST";
    private static final String REPLACE_TARGET = "&lt;";
    private static final String REPLACE_REPLACEMENT = "<";

}
