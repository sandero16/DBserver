package sample;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
/*
table: logins(username,password)
create table logins(username text, password text);
table: onlineplayers(username,sessionToken)
create table onlinePlayers (username text, sessionToken text);
 */


public class DBServer {

    public static Connection connect(){
        Connection conn = null;

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:test.db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return conn;
    }


    public static void setupRMI(Connection conn){
        try {
            // create remote object
            DBServerImpl obj = new DBServerImpl(conn);
            // create on port 1098
            Registry registry = LocateRegistry.createRegistry(1098);
            // create a new service named CounterService
            registry.rebind("DBService", obj);




        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("system is ready");
    }


    public static void main(String[] args) {

        Connection conn = connect();
        setupRMI(conn);


    }

}