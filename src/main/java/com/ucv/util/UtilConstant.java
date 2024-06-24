package com.ucv.util;

import org.orekit.forces.gravity.potential.GravityFieldFactory;

public class UtilConstant {

    public static final double MU = GravityFieldFactory.getNormalizedProvider(12, 12).getMu();
    public static final double CROSS_SECTION = 10.0;
    public static final double SRP_COEF = 0.7;
    public static final double DAYS = 0.1;
    public static final String DEGREES = "%.2f degrees";
    public static final String UNKNOWN = "UNKNOWN";

}
