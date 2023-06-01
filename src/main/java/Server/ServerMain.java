package Server;

import Server.Tables.CreateTables;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerMain {
    public static void main(String[] args) {
        final int serverPort = 3000;
        try {
        ServerSocket server = new ServerSocket(serverPort);
        CreateTables createTables = new CreateTables();

        while (true) {
            Socket socket = server.accept();
            System.out.println("Client has connected.");
            Manager service = new Manager(socket);
            Thread t = new Thread(service);
            t.start();
        }
        } catch (SocketException e){
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
