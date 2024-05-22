package com.ucv.satellite;


import com.ucv.database.DBManager;
import com.ucv.datamodel.satellite.SpatialObject;
import org.apache.log4j.Logger;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.conversion.DormandPrince54IntegratorBuilder;
import org.orekit.propagation.conversion.JacobianPropagatorConverter;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;

import java.util.LinkedList;
import java.util.List;

public class TlePropagator extends Thread {
    static final double CROSS_SECTION = 10.0;
    static final double SRP_COEF = 0.7;
    static final double DAYS = 0.1;
    private final SpatialObject spatialObject;
    private final Propagator propagator;
    Logger logger = Logger.getLogger(TlePropagator.class);
    public TlePropagator(SpatialObject spatialObject) {
        this.spatialObject = spatialObject;

        // create a TLE propagator
        Propagator tlePropagator = TLEPropagator.selectExtrapolator(spatialObject.getTle());
        // get the ITRF frame
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        // create the Earth
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        // create the geopotential force
        ForceModel gravity = new HolmesFeatherstoneAttractionModel(itrf, GravityFieldFactory.getNormalizedProvider(12, 12));
        // create the moon influence
        ForceModel moon = new ThirdBodyAttraction(CelestialBodyFactory.getMoon());
        // create the sun influence
        ForceModel sun = new ThirdBodyAttraction(CelestialBodyFactory.getSun());
        OneAxisEllipsoid oneAxisEllipsoid = earth;
        final IsotropicRadiationSingleCoefficient spacecraft = new IsotropicRadiationSingleCoefficient(CROSS_SECTION, SRP_COEF);
        ForceModel solarRadiationPressure = new SolarRadiationPressure(CelestialBodyFactory.getSun(), oneAxisEllipsoid, spacecraft);
        NumericalPropagatorBuilder propagatorBuilder = new NumericalPropagatorBuilder(tlePropagator.getInitialState().getOrbit(), new DormandPrince54IntegratorBuilder(0.001, 100, 1.0), PositionAngleType.MEAN, 1.0);
        propagatorBuilder.addForceModel(gravity);
        propagatorBuilder.addForceModel(moon);
        propagatorBuilder.addForceModel(sun);
        propagatorBuilder.addForceModel(solarRadiationPressure);

        for (ParameterDriver driver : propagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (driver.getName().equals(RadiationSensitive.REFLECTION_COEFFICIENT)) {
                driver.setSelected(true);
            }
        }
        JacobianPropagatorConverter fitter = new JacobianPropagatorConverter(propagatorBuilder, 1.0, 500);
        logger.info("Converting...");
        propagator = fitter.convert(tlePropagator, DAYS * Constants.JULIAN_DAY, 20, RadiationSensitive.REFLECTION_COEFFICIENT);
        logger.info("Done!");

    }

    @Override
    public void run() {

        final List<SpacecraftState> states = new LinkedList<>();
        propagator.setStepHandler(60, states::add); // 60 SECONDS
        AbsoluteDate startDate = new AbsoluteDate(spatialObject.getTca(), TimeScalesFactory.getUTC()).shiftedBy(Constants.JULIAN_DAY * (-2));
        AbsoluteDate endDate = new AbsoluteDate(spatialObject.getTca(), TimeScalesFactory.getUTC()).shiftedBy(Constants.JULIAN_DAY * 2);
        propagator.propagate(startDate, endDate);
        for (SpacecraftState state : states) {
            DBManager.addStateDB(state, spatialObject.getName());
        }
        logger.info(String.format("Data extraction for satelite finished: %s", spatialObject.getName()));
    }

}
