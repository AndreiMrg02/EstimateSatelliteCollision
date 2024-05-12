package com.ucv.Util;

import org.orekit.forces.gravity.potential.GravityFieldFactory;

public class UtilConstant {
    public static String QUERY_GET_SATELLITE_BY_DATE="FROM State WHERE satName = :satelliteName AND date BETWEEN :startDate AND :endDate";
    public static double MU = GravityFieldFactory.getNormalizedProvider(12, 12).getMu();
}
