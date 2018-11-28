package sample;

import java.rmi.Remote;
import java.rmi.RemoteException;


// Remote interface databank server
public interface DBServerInterface extends Remote {

    boolean signIn(String username, String password) throws RemoteException;

    String logIn(String username, String password) throws RemoteException;

    void removeFromOnlinePlayers(String sessionToken) throws RemoteException;



}
