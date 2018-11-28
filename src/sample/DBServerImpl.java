package sample;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public class DBServerImpl extends UnicastRemoteObject implements DBServerInterface {

    Connection conn;

    public DBServerImpl(Connection conn) throws RemoteException {
        this.conn = conn;
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
        }


        return sessionToken;
    }

    private boolean alreadyHasToken(String username) {
        Statement stmt = null;
        boolean alreadyHasToken = false;
        try{
            stmt = conn.createStatement();
            String query = "SELECT * FROM onlineplayers WHERE username='" + username + "';";
            ResultSet rs = stmt.executeQuery( query );
            if (rs.next() ) {
                // foute login, lege resultset
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

    @Override
    public void removeFromOnlinePlayers(String sessionToken) throws RemoteException {
        removeSessionToken(sessionToken);
    }

    private void insertNewLogin(String username, String password){
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
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
        if(usernameIsUnique)
            insertNewLogin(username,password);

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
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String query = "INSERT INTO onlinePlayers (username,sessionToken) " +
                    "VALUES ('" + username + "','" + sessionToken + "');";
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

}
