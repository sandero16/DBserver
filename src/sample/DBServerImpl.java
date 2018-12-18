package sample;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DBServerImpl extends UnicastRemoteObject implements DBServerInterface, DBtoDBInterface {

    Connection conn;
    DBtoDBInterface db1;
    DBtoDBInterface db2;

    public DBServerImpl(Connection conn) throws RemoteException {
        this.conn = conn;
        this.db1 = null;
        this.db2 = null;
    }

    @Override
    public boolean signIn(String username, String password) throws RemoteException {
        // returnt true als succesvol is geregistreerd, false als username al bestaat
        return signInAttempt(username,password);
    }

    @Override
    public String logIn(String username, String password) throws RemoteException {
        String sessionToken = null;
        boolean combinationCorrect = checkLogin(username,password);

        if(combinationCorrect) {

            if(alreadyHasToken(username)) return  null;

            // creation of unique token
            sessionToken = UUID.randomUUID().toString();
            insertNewOnlinePlayer(username, sessionToken);
            db1.insertNewOnlinePlayerConsistent(username, sessionToken);
            db2.insertNewOnlinePlayerConsistent(username, sessionToken);

        }


        return sessionToken;
    }
    @Override
    public void logOut(String sessionToken) throws RemoteException {
        removeSessionToken(sessionToken);
        db1.removeSessionTokenConsistent(sessionToken);
        db2.removeSessionTokenConsistent(sessionToken);
    }

    @Override
    public void updateRanking(String sessionToken, int points) throws RemoteException{
        updateRankingHulp(sessionToken, points);
        db1.updateRankingConsistent(sessionToken, points);
        db2.updateRankingConsistent(sessionToken, points);
    }

    public void updateRankingHulp(String sessionToken, int points){
        Statement stmt = null;
        try{
            stmt = conn.createStatement();
            String query = "SELECT username FROM onlineplayers WHERE sessionToken='" + sessionToken + "';";
            ResultSet rs = stmt.executeQuery( query );
            String username ="";
            if (rs.next() ) {
                username = rs.getString(1);
            }
            rs.close();
            stmt.close();

            stmt = conn.createStatement();
            String sql = "UPDATE ranking set points=points+" + points+ " where username='"+username+"';";
            stmt.executeUpdate(sql);
            rs.close();
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }
    public boolean sessionTokenValid(String sessionToken) throws RemoteException{
        long currentTimeMinutes = System.currentTimeMillis()/1000/60;
        boolean sessionTokenValid = false;
        Statement stmt = null;
        try{
            stmt = conn.createStatement();
            String query = "SELECT timeLogin FROM onlineplayers WHERE sessionToken='" + sessionToken + "';";
            ResultSet rs = stmt.executeQuery( query );
            if (rs.next() ) {
                long timeLoginMinutes = rs.getLong(1);
                if(currentTimeMinutes<(timeLoginMinutes+(24*60))){
                    sessionTokenValid = true;
                }
            }
            rs.close();
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return sessionTokenValid;

    }


    private boolean alreadyHasToken(String username) {
        Statement stmt = null;
        boolean alreadyHasToken = false;
        try{
            stmt = conn.createStatement();
            String query = "SELECT * FROM onlineplayers WHERE username='" + username + "';";
            ResultSet rs = stmt.executeQuery( query );
            if (rs.next() ) {
                alreadyHasToken = true;
            }
            rs.close();
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return alreadyHasToken;
    }
    private void insertNewRankedPlayer(String username){
        insertNewRankedPlayerHulp(username);
        try {
            db1.insertNewRankedPlayerConsistent(username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            db2.insertNewRankedPlayerConsistent(username);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void insertNewRankedPlayerHulp(String username) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            long currentMillis = System.currentTimeMillis();

            String query = "INSERT INTO ranking(username,points) " +
                    "VALUES ('" + username + "','" + 0 + "');";
            stmt.executeUpdate(query);
            stmt.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }


    private void insertNewLogin(String username, String password){
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            long currentMillis = System.currentTimeMillis();

            String query = "INSERT INTO logins (username,password) " +
                    "VALUES ('" + username + "','" + password + "');";
            stmt.executeUpdate(query);
            stmt.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    private boolean signInAttempt(String username, String password){
        Statement stmt = null;
        boolean usernameIsUnique = false;
        try{
            stmt = conn.createStatement();
            String query = "SELECT * FROM logins WHERE username='" + username+"';";
            ResultSet rs = stmt.executeQuery( query );
            if (!rs.next() ) {
                // username bestaat nog niet, login toevoegen
                usernameIsUnique = true;
            }
            rs.close();
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        if(usernameIsUnique) {
            insertNewLogin(username, password);
            insertNewRankedPlayer(username);
            try {
                db1.insertLoginConsistent(username, password);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                db2.insertLoginConsistent(username, password);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return usernameIsUnique;

    }

    private boolean checkLogin(String username, String password){
        Statement stmt = null;
        boolean loginSucceeded = true;
        try{
            stmt = conn.createStatement();
            String query = "SELECT * FROM logins WHERE username='" + username +
                    "' AND password ='" + password + "';";
            ResultSet rs = stmt.executeQuery( query );
            if (!rs.next() ) {
                // foute login, lege resultset
                loginSucceeded = false;
            }
            rs.close();
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return loginSucceeded;
    }

    private void insertNewOnlinePlayer(String username, String sessionToken){
        long timeLogin = System.currentTimeMillis()/1000/60; // in minuten
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String query = "INSERT INTO onlinePlayers (username,sessionToken,timeLogin) " +
                    "VALUES ('" + username + "','" + sessionToken + "','" + timeLogin + "');";
            stmt.executeUpdate(query);
            stmt.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }

    }

    private void removeSessionToken(String sessionToken) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String query = "DELETE from onlinePlayers where sessionToken = '"+ sessionToken + "';";
            stmt.executeUpdate(query);
            stmt.close();
        }
        catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    @Override
    public void getRegistries(int port1, int port2) throws RemoteException {
        DBtoDBInterface stub1 = null;
        DBtoDBInterface stub2 = null;
        try {
            // fire to localhost port 1099
            Registry myRegistry1 = LocateRegistry.getRegistry("localhost", port1);
            Registry myRegistry2 = LocateRegistry.getRegistry("localhost", port2);

            stub1 = (DBtoDBInterface) myRegistry1.lookup("DBService");
            stub2 = (DBtoDBInterface) myRegistry2.lookup("DBService");

            db1 = stub1;
            db2 = stub2;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void insertLoginConsistent(String username, String password) throws RemoteException {
        insertNewLogin(username,password);
    }

    @Override
    public void insertNewOnlinePlayerConsistent(String username, String sessionToken) throws RemoteException {
        insertNewOnlinePlayer(username,sessionToken);
    }

    @Override
    public void removeSessionTokenConsistent(String sessionToken) throws RemoteException {
        removeSessionToken(sessionToken);
    }

    @Override
    public void updateRankingConsistent(String sessionToken, int score) throws RemoteException {
        updateRankingHulp(sessionToken, score);
    }
    @Override
    public void insertNewRankedPlayerConsistent(String username) throws RemoteException{
        insertNewRankedPlayerHulp(username);
    }

}
