package sample;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DBtoDBInterface extends Remote {

    void getRegistries(int port1, int port2) throws RemoteException;
    void insertLoginConsistent(String username, String password) throws RemoteException;
    void insertNewOnlinePlayerConsistent(String username, String sessionToken) throws RemoteException;
    void removeSessionTokenConsistent(String sessionToken) throws RemoteException;

}
