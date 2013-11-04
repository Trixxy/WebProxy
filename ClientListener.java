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
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			PrintStream writer = new PrintStream(socket.getOutputStream());

			// Extract port, if any.
			String file = "";
			boolean supported = false;
			// Extract the host
			while ((str = reader.readLine()) != null && str.length() > 0) {
				if (str.contains("GET")) {
					System.out.println(str);
					String slices[] = str.split(" ");
					if (slices.length > 1) {
						file = slices[1];
						supported = true;
						break;
					}
				}
			}

			if (!supported)
				return;

			URL url = new URL(file);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestProperty("User-Agent", "Phoenix 1.1");

			con.connect();

			BufferedInputStream bis = new BufferedInputStream(
					con.getInputStream(), 8190);

			StringBuilder headerBuffer = new StringBuilder();
			headerBuffer.append("HTTP/1.0 " + con.getResponseCode() + "\n");

			if (con.getResponseCode() == 404) {
				writer.write(headerBuffer.toString().getBytes());
				writer.flush();
				return;
			}

			for (String key : con.getHeaderFields().keySet()) {
				if (key != null && !key.equals("Connection")
						&& !key.equals("Keep-Alive")) {
					headerBuffer.append(key + ": "
							+ con.getHeaderFields().get(key).get(0) + "\n");
				}
			}
			headerBuffer.append("\n");
			writer.write(headerBuffer.toString().getBytes());

			int b = -1;
			while ((b = bis.read()) != -1) {
				writer.write(b);
			}
			
			con.disconnect();

		} catch (IOException ioe) {
			System.out.println("I'm done 1 !");
			ioe.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}