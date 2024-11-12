package com.ucv.helper;


import com.ucv.controller.SatelliteInformationUpdate;
import com.ucv.earth.SatelliteUpdaterOnEarth;
import gov.nasa.worldwind.layers.AirspaceLayer;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.airspaces.Airspace;
import gov.nasa.worldwind.render.airspaces.SphereAirspace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.bodies.OneAxisEllipsoid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SatelliteUpdaterHelper {
    private final OneAxisEllipsoid earth;
    private final Map<String, Ephemeris> ephemerisMap;
    private final Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap;
    private final AirspaceLayer satAirspaces;
    private final AnnotationLayer labelLayer;
    private final Map<String, List<Airspace>> sphereFragmentsMap;
    private final Logger logger = LogManager.getLogger(SatelliteUpdaterHelper.class);

    public SatelliteUpdaterHelper(OneAxisEllipsoid earth, Map<String, Ephemeris> ephemerisMap,
                                  Map<String, Map.Entry<SphereAirspace, GlobeAnnotation>> sphereMap,
                                  AirspaceLayer satAirspaces, AnnotationLayer labelLayer,
                                  Map<String, List<Airspace>> sphereFragmentsMap) {
        this.earth = earth;
        this.ephemerisMap = ephemerisMap;
        this.sphereMap = sphereMap;
        this.satAirspaces = satAirspaces;
        this.labelLayer = labelLayer;
        this.sphereFragmentsMap = sphereFragmentsMap;
    }

    public void updateSatellites(AbsoluteDate targetDate, AbsoluteDate closeApproachDate,
                                 SatelliteInformationUpdate updateSatellitesInformation, boolean isCollision) {
        Map<String, Vector3D> positions = new HashMap<>();
        if (closeApproachDate != null) {
            AbsoluteDate threeMinutesAfter = closeApproachDate.shiftedBy(-180);
            AbsoluteDate threeMinutesBefore = closeApproachDate.shiftedBy(180);

            ephemerisMap.forEach((name, ephemeris) -> {
                try {
                    SatelliteUpdaterOnEarth satelliteUpdaterOnEarth = new SatelliteUpdaterOnEarth(
                            earth, sphereMap, satAirspaces, updateSatellitesInformation, labelLayer, sphereFragmentsMap);
                    satelliteUpdaterOnEarth.processUpdateSatellite(targetDate, name, ephemeris,
                            threeMinutesAfter, threeMinutesBefore,
                            positions, isCollision);
                } catch (OrekitException e) {
                    logger.error(String.format("Error updating satellites for date: %s and satellite: %s", targetDate, name), e);
                }
            });
        }
    }
}
