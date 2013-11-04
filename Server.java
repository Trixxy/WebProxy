import java.io.IOException;
import java.net.ServerSocket;

public class Server {
	public static void main(String args[]) throws IOException {
		ServerSocket serverSocket = new ServerSocket(8080);
		
		try {
			System.out.println("LOG: Server up");
			
			while (!serverSocket.isClosed()) {				
				(new Thread(new ClientListener(serverSocket.accept()))).start();
			}
			
			System.out.println("LOG: Server down");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}