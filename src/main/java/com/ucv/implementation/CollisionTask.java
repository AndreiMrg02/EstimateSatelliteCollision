package com.ucv.implementation;

import com.ucv.Util.LoggerCustom;
import com.ucv.datamodel.satellite.CollisionData;
import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.PositionDifference;
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

    @Override
    public void run() {
        estimateCollision();
    }

    public static String formatAbsoluteDate(AbsoluteDate date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date javaDate = date.toDate(TimeScalesFactory.getUTC());
        return dateFormat.format(javaDate);
    }

    private void estimateCollision() {
        try {
            AbsoluteDate startDate = extractStartDate();
            AbsoluteDate endDate = extractEndDate();
            if (verifySatellitesDate(startDate, endDate)) return;

            Ephemeris ephemerisSatelliteOne = new Ephemeris(spacecraftStatesOne, 4);
            Ephemeris ephemerisSatelliteTwo = new Ephemeris(spacecraftStatesTwo, 4);
            final PositionDifference closestApproach = new PositionDifference();
            propagateSatellites(ephemerisSatelliteOne, ephemerisSatelliteTwo, closestApproach);
            ephemerisSatelliteOne.propagate(startDate, endDate);
            ephemerisSatelliteOne.clearStepHandlers();
            addDisplaySatellites(startDate, endDate, ephemerisSatelliteOne, closestApproach, ephemerisSatelliteTwo);
            double collisionProbability = estimateCollisionProbability(closestApproach.getDifference(), threshold);
            printCollisionProbability(collisionProbability);
            String collisionFormat = String.format("%.10f%%", collisionProbability);
            addCollisionData(closestApproach, startDate, endDate, collisionFormat);

        } catch (Exception exception) {
            exception.printStackTrace();
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

    private void printCollisionProbability(double collisionProbability) {
        if (collisionProbability >= 100) {
            collisionProbability = 100;
            LoggerCustom.getInstance().logMessage(String.format("INFO: It was detected a collision between satellite %s and satellite %s  with a collision probability %.3f%%", satelliteOneName, satelliteTwoName, collisionProbability));
        }
        if (collisionProbability > 50 && collisionProbability < 80) {
            LoggerCustom.getInstance().logMessage(String.format("INFO: It was detected a MEDIUM collision risk between satellite %s and satellite %s with a collision probability %.3f%%", satelliteOneName, satelliteTwoName, collisionProbability));
        } else if (collisionProbability >= 80) {
            LoggerCustom.getInstance().logMessage(String.format("INFO: It was detected a HIGH collision risk between satellite %s and satellite %s  with a collision probability %.3f%%", satelliteOneName, satelliteTwoName, collisionProbability));
        } else if (collisionProbability <= 50 && collisionProbability > 10) {
            LoggerCustom.getInstance().logMessage(String.format("INFO: It was detected a LOW collision risk between satellite %s and satellite %s  with a collision probability %.3f%%", satelliteOneName, satelliteTwoName, collisionProbability));
        }
    }

    private boolean verifySatellitesDate(AbsoluteDate startDate, AbsoluteDate endDate) {
        if (startDate == null || endDate == null) {
            System.out.println("Nu exista data comuna intre satelitii: " + satelliteOneName + " si " + satelliteTwoName);
            return true;
        }
        return false;
    }

    private  void propagateSatellites(Ephemeris ephemerisSatelliteOne, Ephemeris ephemerisSatelliteTwo, PositionDifference closestApproach) {
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

    public  double estimateCollisionProbability(double closestApproachDistance, double targetDistance) {

        double probability = Math.pow(targetDistance / closestApproachDistance, 2);
        if (probability >= 100) {
            return 100;
        }
        return probability * 100;
    }

    private AbsoluteDate extractStartDate() {
        if (spacecraftStatesOne.isEmpty() || spacecraftStatesTwo.isEmpty()) {
            return null;
        }
        for (SpacecraftState stateOne : spacecraftStatesOne) {
            for (SpacecraftState stateTwo : spacecraftStatesTwo) {
                if (stateOne.getDate().equals(stateTwo.getDate())) {
                    return stateOne.getDate();
                }
            }
        }

        return null;
    }

    private AbsoluteDate extractEndDate() {
        if (spacecraftStatesOne.isEmpty() || spacecraftStatesTwo.isEmpty()) {
            return null;
        }
        for (int i = spacecraftStatesOne.size() - 1; i >= 0; i--) {
            for (int j = spacecraftStatesTwo.size() - 1; j >= 0; j--) {
                if (spacecraftStatesOne.get(i).getDate().equals(spacecraftStatesTwo.get(j).getDate())) {
                    return spacecraftStatesOne.get(i).getDate();
                }
            }
        }
        return null;
    }

}
