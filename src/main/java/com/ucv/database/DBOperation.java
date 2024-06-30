package com.ucv.database;

import com.ucv.datamodel.database.ConnectionInformation;
import com.ucv.datamodel.database.State;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

import java.util.ArrayList;
import java.util.List;

import static com.ucv.database.HibernateUtil.getCurrentSession;
import static com.ucv.util.UtilConstant.MU;


public class DBOperation {
    private static final Logger logger = LogManager.getLogger(DBOperation.class);

    private DBOperation() {

    }

    public static void saveLastConnectionToDB(ConnectionInformation connectionInformation) {
        try {
            Session session = getCurrentSession();
            session.beginTransaction();
            Query<ConnectionInformation> query = session.createQuery(
                    "FROM ConnectionInformation WHERE username = :username", ConnectionInformation.class);
            query.setParameter("username", connectionInformation.getUsername());
            ConnectionInformation existingConnection = query.uniqueResult();

            if (existingConnection != null) {
                existingConnection.setLastConnectionDate(connectionInformation.getLastConnectionDate());
                session.merge(existingConnection);
            } else {
                session.persist(connectionInformation);
            }

            session.getTransaction().commit();
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred due to add/update connection in database for the user %s", connectionInformation.getUsername()), ex);
        }
    }

    public static ConnectionInformation getLastConnectionFromDB(String username) {
        ConnectionInformation connectionInformation = null;
        try {
            Session session = getCurrentSession();
            session.beginTransaction();
            Query<ConnectionInformation> query = session.createQuery(
                    "FROM ConnectionInformation WHERE username = :username", ConnectionInformation.class);
            query.setParameter("username", username);
            connectionInformation = query.uniqueResult();
            session.getTransaction().commit();
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred while fetching connection info for the user %s", username), ex);
        }
        return connectionInformation;
    }

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
            logger.error(String.format("Unexpected error occurred due to add state in database for the satellite %s", satName));
        }

    }

    public static synchronized List<SpacecraftState> getStatesBySatelliteName(String satelliteName) {
        try (Session session = getCurrentSession()) {

            String sqlStatement = "FROM com.ucv.datamodel.database.State s WHERE s.satName = :satelliteName ORDER BY s.date ASC";
            Query<State> query = session.createQuery(sqlStatement, State.class);
            query.setParameter("satelliteName", satelliteName);


            List<State> stateEntities = query.list();
            List<State> states = new ArrayList<>(stateEntities);

            return convertEntitiesToSpacecraftStates(states);
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred due to extract states for the satellite %s", satelliteName));
            return new ArrayList<>();
        }
    }

    public static synchronized void clearAllStates() {
        Session session = null;
        Transaction tx = null;
        try {
            session = getCurrentSession();
            tx = session.beginTransaction();
            String hqlStatement = "DELETE FROM States";

            MutationQuery query = session.createMutationQuery(hqlStatement);
            query.executeUpdate();
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            logger.error(String.format("Unexpected error occurred due to extract clear states for satellites: %s", ex.getMessage()));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    public static synchronized List<String> getSatellitesName() {
        try (Session session = getCurrentSession()) {

            String sqlStatement = "SELECT DISTINCT s.satName FROM com.ucv.datamodel.database.State s";
            Query<String> query = session.createQuery(sqlStatement, String.class);

            List<String> satelliteNameList = query.list();
            return new ArrayList<>(satelliteNameList);
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred due to extract the satellite name: %s", ex.getMessage()));
            return new ArrayList<>();
        }
    }

    private static List<SpacecraftState> convertEntitiesToSpacecraftStates(List<State> stateEntities) {
        try {
            List<SpacecraftState> spacecraftStates = new ArrayList<>();

            for (State stateEntity : stateEntities) {
                SpacecraftState spacecraftState = convertEntityToSpacecraftState(stateEntity);
                spacecraftStates.add(spacecraftState);
            }

            return spacecraftStates;
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred due to convert entities: %s", ex.getMessage()));
        }
        return new ArrayList<>();
    }

    private static SpacecraftState convertEntityToSpacecraftState(State stateEntity) {
        try {
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

            final TimeStampedPVCoordinates coords = new TimeStampedPVCoordinates(absoluteDate, position, velocity);
            CartesianOrbit co = new CartesianOrbit(coords, FramesFactory.getEME2000(), MU);
            return new SpacecraftState(co);
        } catch (Exception ex) {
            logger.error(String.format("Unexpected error occurred due to convert an entity to spacecraftState: %s", ex.getMessage()));
        }
        return null;
    }
}
