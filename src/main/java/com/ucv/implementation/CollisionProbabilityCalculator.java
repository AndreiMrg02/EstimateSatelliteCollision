package com.ucv.implementation;

import com.ucv.util.LoggerCustom;

public class CollisionProbabilityCalculator {
    public double calculateProbability(double closestApproachDistance, double targetDistance) {
        double probability = Math.pow(targetDistance / closestApproachDistance, 2);
        return Math.min(probability * 100, 100);
    }

    public void logRiskLevel(double collisionProbability, String satelliteOne, String satelliteTwo) {
        String message;
        if (collisionProbability >= 100) {
            message = String.format("INFO: Collision detected between %s and %s with a probability of %.3f%%", satelliteOne, satelliteTwo, collisionProbability);
        } else if (collisionProbability >= 80) {
            message = String.format("INFO: High collision risk between %s and %s with a probability of %.3f%%", satelliteOne, satelliteTwo, collisionProbability);
        } else if (collisionProbability > 50) {
            message = String.format("INFO: Medium collision risk between %s and %s with a probability of %.3f%%", satelliteOne, satelliteTwo, collisionProbability);
        } else {
            message = String.format("INFO: Low collision risk between %s and %s with a probability of %.3f%%", satelliteOne, satelliteTwo, collisionProbability);
        }
        LoggerCustom.getInstance().logMessage(message);
    }
}
