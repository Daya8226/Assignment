import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class Server {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <configFilePath>");
            return;
        }
        // main method is accepting an argument specifying a config file path.
        String configFilePath = args[0].trim();
        // Properties class represents a persistent set of properties.
        Properties properties = new Properties();
        readConfigFile(configFilePath, properties);

        String outputDirectory = properties.getProperty("outputDirectory");
        int port = Integer.parseInt(properties.getProperty("port"));

        // Start the server
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ServerThread(clientSocket, outputDirectory).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void readConfigFile(String configFilePath, Properties properties) {
        try {
            FileInputStream fileInputStream = new FileInputStream(configFilePath);
            // load the property file and its contents in key-value pairs.
            properties.load(fileInputStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("config file not found at config file path " + configFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}