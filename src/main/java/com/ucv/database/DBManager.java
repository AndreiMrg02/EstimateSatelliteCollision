package com.ucv.database;

import com.ucv.database.model.State;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import static com.ucv.Util.HibernateUtil.getCurrentSession;
import static com.ucv.Util.UtilConstant.MU;


public class DBManager {

    public static synchronized void addStateDB(SpacecraftState spacecraftState, String satName) {


        try {
            Session session = getCurrentSession();
            State newState = new State();
            newState.setSatName(satName);
            // Convert AbsoluteDate to Date
            newState.setDate(spacecraftState.getDate().toDate(TimeScalesFactory.getUTC()));
            newState.setPosX(spacecraftState.getPVCoordinates().getPosition().getX());
            newState.setPosY(spacecraftState.getPVCoordinates().getPosition().getY());
            newState.setPosZ(spacecraftState.getPVCoordinates().getPosition().getZ());
            newState.setvX(spacecraftState.getPVCoordinates().getVelocity().getX());
            newState.setvY(spacecraftState.getPVCoordinates().getVelocity().getY());
            newState.setvZ(spacecraftState.getPVCoordinates().getVelocity().getZ());

            session.beginTransaction();
            session.persist(newState);
            session.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static synchronized LinkedHashSet<SpacecraftState> getStatesBySatelliteName(String satelliteName) {
        try (Session session = getCurrentSession()) {

            String sqlStatement = "FROM com.ucv.database.model.State s WHERE s.satName = :satelliteName";
            Query<State> query = session.createQuery(sqlStatement, State.class);
            query.setParameter("satelliteName", satelliteName);


            List<State> stateEntities = query.list();
            LinkedHashSet<State> states = new LinkedHashSet<>(stateEntities);
            // Convertiți entitățile în stări SpacecraftState
            return convertEntitiesToSpacecraftStates(states, MU);
        } catch (Exception ex) {
            System.out.println("Eroarea vine din functia de getByName: " + ex.getMessage());
            ex.printStackTrace();
            return new LinkedHashSet<>();
        }
    }

    private static LinkedHashSet<SpacecraftState> convertEntitiesToSpacecraftStates(LinkedHashSet<State> stateEntities, double mu) {
        LinkedHashSet<SpacecraftState> spacecraftStates = new LinkedHashSet<>();

        for (State stateEntity : stateEntities) {
            SpacecraftState spacecraftState = convertEntityToSpacecraftState(stateEntity, mu);
            spacecraftStates.add(spacecraftState);
        }

        return spacecraftStates;
    }

    private static SpacecraftState convertEntityToSpacecraftState(State stateEntity, double mu) {
        AbsoluteDate absoluteDate = new AbsoluteDate(stateEntity.getDate(), TimeScalesFactory.getUTC());
        final Vector3D position = new Vector3D(
                stateEntity.getPosX(),
                stateEntity.getPosY(),
                stateEntity.getPosZ()
        );
        final Vector3D velocity = new Vector3D(
                stateEntity.getvX(),
                stateEntity.getvY(),
                stateEntity.getvZ()
        );

        // create a state and add it to the map, in the states list for the satellite.
        final TimeStampedPVCoordinates coords = new TimeStampedPVCoordinates(absoluteDate, position, velocity);
        CartesianOrbit co = new CartesianOrbit(coords, FramesFactory.getEME2000(), mu);
        return new SpacecraftState(co);
    }
}
