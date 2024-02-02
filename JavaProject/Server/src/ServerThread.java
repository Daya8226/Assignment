import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public class ServerThread extends Thread {
    private final Socket socket;
    private final String outputDirectory;

    public ServerThread(Socket clientSocket, String outputDirectory) {
        this.socket = clientSocket;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public void run() {
        try (ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream())) {
            Properties properties = (Properties) inputStream.readObject();
            processProperties(properties);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void processProperties(Properties properties) {
        try {
            String fileName = properties.getProperty("transferFileName");
            // removing file name from properties because it was added explicitly in client side.
            properties.remove("transferFileName");
            Path outputPath = Paths.get(outputDirectory, fileName);

            // Write properties to the file
            properties.store(Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE),
                    "Processed properties from client");

            System.out.println("Received and processed properties saved to: " + outputPath);
        } catch (IOException e) {
            System.err.println("Error processing properties: " + e.getMessage());
        }
    }
}
