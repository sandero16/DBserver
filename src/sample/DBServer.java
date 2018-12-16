package sample;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
/*
table: logins(username,password)
create table logins(username text, password text);
table: onlineplayers(username,sessionToken, timeLogin)
create table onlinePlayers (username text, sessionToken text, timeLogin integer);
create table ranking (username text, points integer);

 */


public class DBServer {
    public int port;
    public Connection conn;
    public DBServerImpl obj;
    public DBServer(int port){
        this.port = port;
        this.conn = null;
        this.obj = null;
    }

    public void connect(){

        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:dbport" + port + ".db");
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    public void setupRMI(){
        try {
            // create remote object
            obj = new DBServerImpl(conn);
            // create on port (first argument)
            Registry registry = LocateRegistry.createRegistry(port);
            // create a new service named DBService
            registry.rebind("DBService", obj);


        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("system is ready");
    }

    private void setupRMIforOtherDBs(int ownPort, int port1, int port2) {
        try {
            obj.getRegistries(port1,port2);
            obj.db1.getRegistries(ownPort,port2);
            obj.db2.getRegistries(ownPort,port1);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DBServer db = new DBServer(Integer.parseInt(args[0]));
        db.connect();
        db.setupRMI();
        if(args.length>1){
            // laatste dbserver opgestart, andere argumenten zijn poortnummer van alle andere dbservers
            int ownPort = Integer.parseInt(args[0]);
            int port1 = Integer.parseInt(args[1]);
            int port2 = Integer.parseInt(args[2]);
            db.setupRMIforOtherDBs(ownPort,port1,port2);
        }

    }


}