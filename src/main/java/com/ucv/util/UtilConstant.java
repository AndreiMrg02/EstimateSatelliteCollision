package com.ucv.util;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.Frame;

public class UtilConstant {

    public static double MU = GravityFieldFactory.getNormalizedProvider(12, 12).getMu();
    public static final double CROSS_SECTION = 10.0;
    public static final double SRP_COEF = 0.7;
    public static final double DAYS = 0.1;

}
