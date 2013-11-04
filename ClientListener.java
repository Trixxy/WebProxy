import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

public class ClientListener implements Runnable {
	private Socket socket = null;

	ClientListener(Socket clientSocket) {
		this.socket = clientSocket;
	}

	@Override
	public void run() {
		String str = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintStream writer = new PrintStream(socket.getOutputStream());

			String address = "";
			boolean supported = false;
			
			// Extract the address
			// Currently, we only account for GET requests, anything else is discarded.
			// ...and we don't account for any of the header fields either!
			while ((str = reader.readLine()) != null && str.length() > 0) {
				if (str.contains("GET")) {
					System.out.println(str);
					String slices[] = str.split(" ");
					if (slices.length > 1) {
						address = slices[1];
						supported = true;
						break;
					}
				}
			}

			//Here's where we jump out if the request is not supported.
			if (!supported)
				return;

			//Construct a URL from the extracted address
			URL url = new URL(address);
			
			//Open connection
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			//Set the user-agent
			con.setRequestProperty("User-Agent", "Phoenix 1.1");

			con.connect();

			// Construct a BIS with 16Kb buffer
			BufferedInputStream bis = new BufferedInputStream(con.getInputStream(), 16384);

			//Construct a buffer (actually a StringBuilder) for the header to be sent later.
			StringBuilder headerBuffer = new StringBuilder();
			
			//Include the response code as the first line in the header
			//without this the browser will receive the header and body as plain text.
			headerBuffer.append("HTTP/1.0 " + con.getResponseCode() + "\n");

			
			//If page is not found (e.g. 404) return the header and exit.
			if (con.getResponseCode() == 404) {
				writer.write(headerBuffer.toString().getBytes());
				writer.flush();
				return;
			}

			
			//Put all the header fields into the headerBuffer except the ones that
			//tries to trick our poor client to keep an open connection, who would want that right? :P
			for (String key : con.getHeaderFields().keySet()) {
				if (key != null && !key.equals("Connection") && !key.equals("Keep-Alive")) {
					headerBuffer.append(key + ": " + con.getHeaderFields().get(key).get(0) + "\n");
				}
			}
			
			//When done with the header fields, add the header-body separating line-break
			//And write the whole header to the client. PS: it's buffered
			headerBuffer.append("\n");
			writer.write(headerBuffer.toString().getBytes());

			//Now write the body (unmodified) to the client.
			//This is where we should put our color injection.
			int b = -1;
			while ((b = bis.read()) != -1) {
				writer.write(b);
			}
			
			//We're done, disconnect!
			con.disconnect();

		} catch (IOException ioe) {
			System.out.println("Something did go wrong, don't crash anyway, things happen...");
			ioe.printStackTrace();
		} finally {
			try {
				socket.close(); //some good practice at the end ;p 
				//Ignoring all the other streams and shit
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}