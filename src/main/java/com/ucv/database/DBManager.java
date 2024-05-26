package com.ucv.database;

import com.ucv.datamodel.database.State;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.*;

import static com.ucv.database.HibernateUtil.getCurrentSession;
import static com.ucv.Util.UtilConstant.MU;


public class DBManager {

    public static synchronized void addStateDB(SpacecraftState spacecraftState, String satName) {


        try {
            Session session = getCurrentSession();
            State newState = new State();
            newState.setSatName(satName);
            newState.setDate(new java.sql.Timestamp(spacecraftState.getDate().toDate(TimeScalesFactory.getUTC()).getTime()));

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

    public static synchronized List<SpacecraftState> getStatesBySatelliteName(String satelliteName) {
        try (Session session = getCurrentSession()) {

            String sqlStatement = "FROM com.ucv.datamodel.database.State s WHERE s.satName = :satelliteName ORDER BY s.date ASC";
            Query<State> query = session.createQuery(sqlStatement, State.class);
            query.setParameter("satelliteName", satelliteName);


            List<State> stateEntities = query.list();
            List<State> states = new ArrayList<>(stateEntities);
            // Convertiți entitățile în stări SpacecraftState
            return convertEntitiesToSpacecraftStates(states, MU);
        } catch (Exception ex) {
            System.out.println("Eroarea vine din functia de getByName: " + ex.getMessage());
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static synchronized void clearAllStates() {
        Session session = null;
        Transaction tx = null;
        try {
            session = getCurrentSession();
            tx = session.beginTransaction();  // Start a transaction
            String hqlStatement = "DELETE FROM States";  // Ensure 'States' is the entity name, not the table name
            // Use createMutationQuery for HQL mutation operations
            MutationQuery query = session.createMutationQuery(hqlStatement);
            query.executeUpdate();
            tx.commit();  // Commit the transaction
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();  // Rollback the transaction in case of an error
            }
            ex.printStackTrace();
        } finally {
            if (session != null) {
                session.close();  // Close the session to free up resources
            }
        }
    }



    public static synchronized List<String> getStatesAllSatelliteName() {
        try (Session session = getCurrentSession()) {

            String sqlStatement = "SELECT DISTINCT s.satName FROM com.ucv.datamodel.database.State s";
            Query<String> query = session.createQuery(sqlStatement, String.class);

            List<String> satelliteNameList = query.list();
            return new ArrayList<>(satelliteNameList);
        } catch (Exception ex) {
            System.out.println("Eroarea vine din functia de getByName: " + ex.getMessage());
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static List<SpacecraftState> convertEntitiesToSpacecraftStates(List<State> stateEntities, double mu) {
        List<SpacecraftState> spacecraftStates = new ArrayList<>();

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
