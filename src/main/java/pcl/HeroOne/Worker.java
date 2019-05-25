package pcl.HeroOne;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Worker extends Thread {

    @Override
    public void run() {
        
        // Loop for ten iterations.
        
		final String host = "149.56.6.197";
		final int portNumber = 31415;
		System.out.println("Creating socket to '" + host + "' on port " + portNumber);

		while (true) {
			Socket socket = null;
			try {
				socket = new Socket(host, portNumber);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			BufferedReader br = null;
			try {
				br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			PrintWriter out = null;
			try {
				out = new PrintWriter(socket.getOutputStream(), true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				System.out.println("server says:" + br.readLine());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			BufferedReader userInputBR = new BufferedReader(new InputStreamReader(System.in));
			String userInput = null;
			try {
				userInput = userInputBR.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			out.println(userInput);

			try {
				System.out.println("server says:" + br.readLine());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if ("exit".equalsIgnoreCase(userInput)) {
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
    }

}