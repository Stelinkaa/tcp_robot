package augusste.psi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
	// write your code here

        ServerSocket socket = null;

        try{
            socket = new ServerSocket(6868);
        } catch (IOException e) {
            System.out.println("Nedobry socket");
            return;
        }

        while(true){
            Socket client = null;
            try {
                client = socket.accept();
            } catch (IOException e) {
                System.out.println("Nedobra connection");
                return;
            }

            ThreadFun threadFun = null;
            try {
                threadFun = new ThreadFun(client);
            } catch (IOException e) {
                System.out.println("Daco zas zle");
                return;
            }

            new Thread(threadFun).start();

        }
    }
}
