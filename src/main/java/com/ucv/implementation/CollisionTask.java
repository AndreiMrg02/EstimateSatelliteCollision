package com.ucv.implementation;

import com.ucv.datamodel.satellite.DisplaySatelliteModel;
import com.ucv.datamodel.satellite.PositionDifference;
import com.ucv.datamodel.satellite.SpatialObjectTableModel;
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
    private final List<SpacecraftState> statesOne;
    private final List<SpacecraftState> statesTwo;
    private final String entryOne;
    private final String entryTwo;
    private final List<String> satName = new ArrayList<>();
    private final Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap;
    private final List<SpatialObjectTableModel> spatialObjectTableModels;

    public CollisionTask(List<SpacecraftState> statesOne, List<SpacecraftState> statesTwo, String entryOne, String entryTwo, Set<DisplaySatelliteModel> stringDisplaySatelliteModelMap, List<SpatialObjectTableModel> spatialObjectTableModels) {
        this.statesOne = new ArrayList<>(statesOne);
        this.statesTwo = new ArrayList<>(statesTwo);
        this.entryOne = entryOne;
        this.entryTwo = entryTwo;
        this.spatialObjectTableModels = spatialObjectTableModels;
        this.stringDisplaySatelliteModelMap = stringDisplaySatelliteModelMap;
    }

    @Override
    public void run() {
        estimateCollision(statesOne, statesTwo, entryOne, entryTwo);
    }

    public static String formatAbsoluteDate(AbsoluteDate date) {
        // Assuming AbsoluteDate can be directly converted to java.util.Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date javaDate = date.toDate(TimeScalesFactory.getUTC());
        return dateFormat.format(javaDate);
    }

    private void estimateCollision(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList, String entryOne, String entryTwo) {
        try {
            AbsoluteDate startDate = extractStartDate(stateOneList, stateTwoList);
            AbsoluteDate endDate = extractEndDate(stateOneList, stateTwoList);

            if (startDate == null || endDate == null) {
                System.out.println("Nu exista data comuna intre satelitii: " + entryOne + " si " + entryTwo);
                return;
            }

            Ephemeris ephemerisSatelliteOne = new Ephemeris(stateOneList, 4);
            Ephemeris ephemerisSatelliteTwo = new Ephemeris(stateTwoList, 4);

            final PositionDifference closestApproach = new PositionDifference();
            propagateSatellites(ephemerisSatelliteOne, ephemerisSatelliteTwo, closestApproach);
            ephemerisSatelliteOne.propagate(startDate, endDate);
            ephemerisSatelliteOne.clearStepHandlers();
            if (!satName.contains(entryOne)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryOne, ephemerisSatelliteOne, stateOneList, closestApproach.getDate()));
                satName.add(entryOne);
            }
            if (!satName.contains(entryTwo)) {
                stringDisplaySatelliteModelMap.add(new DisplaySatelliteModel(startDate, endDate, entryTwo, ephemerisSatelliteTwo, stateTwoList, closestApproach.getDate()));
                satName.add(entryTwo);
            }

            final double threshold = 10; // Set your threshold value here
            double collisionProbability = estimateCollisionProbability(closestApproach.getDifference(), threshold);

            System.out.println("Closest approach between Satellites: " + entryOne + " and " + entryTwo + " is: " + closestApproach.getDifference() + " meters at date: " + closestApproach.getDate().toDate(TimeScalesFactory.getUTC()));
            System.out.println("Collision probability is: " + String.format("%.10f%%", collisionProbability));
            String collisionFormat = String.format("%.10f%%", collisionProbability);
/*            List<SpacecraftState> stateOneListBetweenDate = stateOneList.stream()
                    .filter(state -> !state.getDate().isBefore(startDate) && !state.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
            List<SpacecraftState> stateTwoListBetweenDate = stateTwoList.stream()
                    .filter(state -> !state.getDate().isBefore(startDate) && !state.getDate().isAfter(endDate))
                    .collect(Collectors.toList());*/
            if (closestApproach.getDifference() > 0) {
                SpatialObjectTableModel spatialObjectTableModel = new SpatialObjectTableModel(formatAbsoluteDate(startDate), formatAbsoluteDate(endDate), Double.toString(closestApproach.getDifference()), formatAbsoluteDate(closestApproach.getDate()), entryOne, entryTwo, collisionFormat);

                spatialObjectTableModels.add(spatialObjectTableModel);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void propagateSatellites(Ephemeris ephemerisSatelliteOne, Ephemeris ephemerisSatelliteTwo, PositionDifference closestApproach) {
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


    /*
        private double estimateCollisionProbability(double closestApproachDistance, double threshold) {
            double scale = 0.16;  // Factorul de scalare pentru a ajusta sensibilitatea
            double ratio = (closestApproachDistance / threshold);
            double collisionProbability = Math.exp(-scale * ratio);  // Aplică factorul de scalare în exponent
            return collisionProbability * 100;  // Converteste în procente
        }
    */
/*
public static double estimateCollisionProbability(double closestApproachDistance, double targetDistance) {
    return Math.pow(targetDistance / closestApproachDistance, 2);
}
*/
    public static double estimateCollisionProbability(double closestApproachDistance, double targetDistance) {

        // Calculăm probabilitatea folosind formula simplificată și asigurându-ne că este bine scalată
        double probability = Math.pow(targetDistance / closestApproachDistance, 2);

        // Returnăm probabilitatea în procente, dar asigurându-ne că valoarea este scalată corect
        double scaledProbability = probability / Math.pow(targetDistance / 10, 2);

        return scaledProbability * 100;
    }



    private AbsoluteDate extractStartDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (stateOneList.isEmpty() || stateTwoList.isEmpty()) {
            return null;
        }
        for (SpacecraftState stateOne : stateOneList) {
            for (SpacecraftState stateTwo : stateTwoList) {
                if (stateOne.getDate().equals(stateTwo.getDate())) {
                    return stateOne.getDate();
                }
            }
        }

        return null;
    }

    private AbsoluteDate extractEndDate(List<SpacecraftState> stateOneList, List<SpacecraftState> stateTwoList) {
        if (stateOneList.isEmpty() || stateTwoList.isEmpty()) {
            return null;
        }
        for (int i = stateOneList.size() - 1; i >= 0; i--) {
            for (int j = stateTwoList.size() - 1; j >= 0; j--) {
                if (stateOneList.get(i).getDate().equals(stateTwoList.get(j).getDate())) {
                    return stateOneList.get(i).getDate();
                }
            }
        }
        return null;
    }

}
