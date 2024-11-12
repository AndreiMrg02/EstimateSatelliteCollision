package com.ucv.implementation;

import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.PositionDifference;
import com.ucv.helper.DateExtractor;
import com.ucv.util.LoggerCustom;
import org.apache.log4j.Logger;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class CollisionTask implements Runnable {
    private final List<SpacecraftState> spacecraftStatesOne;
    private final List<SpacecraftState> spacecraftStatesTwo;
    private final String satelliteOneName;
    private final String satelliteTwoName;
    private final List<String> satelliteNames;
    private final Set<DisplaySatelliteModel> displaySatelliteModels;
    private final List<CollisionData> collisionData;
    private final int threshold;
    private static final Logger logger = Logger.getLogger(CollisionTask.class);

    public CollisionTask(List<SpacecraftState> spacecraftStatesOne, List<SpacecraftState> spacecraftStatesTwo, String satelliteOneName, String satelliteTwoName, Set<DisplaySatelliteModel> displaySatelliteModels, List<CollisionData> collisionData, int threshold) {
        this.spacecraftStatesOne = new ArrayList<>(spacecraftStatesOne);
        this.spacecraftStatesTwo = new ArrayList<>(spacecraftStatesTwo);
        this.satelliteOneName = satelliteOneName;
        this.satelliteTwoName = satelliteTwoName;
        this.collisionData = collisionData;
        this.displaySatelliteModels = displaySatelliteModels;
        this.threshold = threshold;
        this.satelliteNames = new ArrayList<>();
    }

    public static String formatAbsoluteDate(AbsoluteDate date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date javaDate = date.toDate(TimeScalesFactory.getUTC());
        return dateFormat.format(javaDate);
    }

    @Override
    public void run() {
        try {
            estimateCollision();
        }catch (Exception ex){
            logger.error(String.format("[run - CollisionTask]An unexpected exception occurred due to: %s", ex.getMessage()));
        }
    }

    private void estimateCollision() {
        try {
            DateExtractor dateExtractor = new DateExtractor(spacecraftStatesOne,spacecraftStatesTwo);
            AbsoluteDate startDate = dateExtractor.extractStartDate();
            AbsoluteDate endDate = dateExtractor.extractEndDate();
            if (verifySatellitesDate(startDate, endDate)) return;

            Ephemeris ephemerisSatelliteOne = new Ephemeris(spacecraftStatesOne, 4);
            Ephemeris ephemerisSatelliteTwo = new Ephemeris(spacecraftStatesTwo, 4);
            final PositionDifference closestApproach = new PositionDifference();
            propagateSatellites(ephemerisSatelliteOne, ephemerisSatelliteTwo, closestApproach);
            ephemerisSatelliteOne.propagate(startDate, endDate);
            ephemerisSatelliteOne.clearStepHandlers();
            addDisplaySatellites(startDate, endDate, ephemerisSatelliteOne, closestApproach, ephemerisSatelliteTwo);
            CollisionProbabilityCalculator calculator = new CollisionProbabilityCalculator();
            double collisionProbability = calculator.calculateProbability(closestApproach.getDifference(), threshold);
            calculator.logRiskLevel(collisionProbability, satelliteOneName, satelliteTwoName);
            String collisionFormat = String.format("%.10f%%", collisionProbability);
            addCollisionData(closestApproach, startDate, endDate, collisionFormat);

        } catch (Exception exception) {
            logger.error(String.format("[estimateCollision] An unexpected exception occurred due to: %s", exception.getMessage()));
        }
    }

    private void addDisplaySatellites(AbsoluteDate startDate, AbsoluteDate endDate, Ephemeris ephemerisSatelliteOne, PositionDifference closestApproach, Ephemeris ephemerisSatelliteTwo) {
        if (!satelliteNames.contains(satelliteOneName)) {
            displaySatelliteModels.add(new DisplaySatelliteModel(startDate, endDate, satelliteOneName, ephemerisSatelliteOne, spacecraftStatesOne, closestApproach.getDate()));
            satelliteNames.add(satelliteOneName);
        }
        if (!satelliteNames.contains(satelliteTwoName)) {
            displaySatelliteModels.add(new DisplaySatelliteModel(startDate, endDate, satelliteTwoName, ephemerisSatelliteTwo, spacecraftStatesTwo, closestApproach.getDate()));
            satelliteNames.add(satelliteTwoName);
        }
    }

    private void addCollisionData(PositionDifference closestApproach, AbsoluteDate startDate, AbsoluteDate endDate, String collisionFormat) {
        if (closestApproach.getDifference() > 0) {
            CollisionData currentCollisionData = new CollisionData(formatAbsoluteDate(startDate), formatAbsoluteDate(endDate), Double.toString(closestApproach.getDifference()), formatAbsoluteDate(closestApproach.getDate()), satelliteOneName, satelliteTwoName, collisionFormat);
            this.collisionData.add(currentCollisionData);
        }
    }


    private boolean verifySatellitesDate(AbsoluteDate startDate, AbsoluteDate endDate) {
        if (startDate == null || endDate == null) {
            LoggerCustom.getInstance().logMessage(String.format("There is no common date between satellite %s and satellite %s", satelliteOneName, satelliteTwoName));
            return true;
        }
        return false;
    }

    private void propagateSatellites(Ephemeris ephemerisSatelliteOne, Ephemeris ephemerisSatelliteTwo, PositionDifference closestApproach) {
        ephemerisSatelliteOne.setStepHandler(60, currentState -> {
            SpacecraftState stateTwo = ephemerisSatelliteTwo.propagate(currentState.getDate());
            Vector3D positionDifference = currentState.getPosition().subtract(stateTwo.getPosition());
            double distance = positionDifference.getNorm();
            if (closestApproach.getDifference() > distance) {
                closestApproach.setDifference(distance);
                closestApproach.setDate(currentState.getDate());
            }
        });
    }

}
