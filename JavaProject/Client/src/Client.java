import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Properties;
import java.util.regex.Pattern;

public class Client {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java Client <configFilePath>");
            return;
        }
        // main method is accepting an argument specifying a config file path.
        String configFilePath = args[0].trim();
        // Properties class represents a persistent set of properties.
        Properties properties = new Properties();
        readConfigFile(configFilePath, properties);

        String directoryPath = properties.getProperty("directoryPath");
        String filterPattern = properties.getProperty("filterPattern");
        String serverAddress = properties.getProperty("serverAddress");
        int serverPort = Integer.parseInt(properties.getProperty("serverPort"));

        // create a watch service
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path directory = Paths.get(directoryPath);
        //register directory with the watch service
        directory.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        // an infinite loop to wait for incoming events
        while (true) {
            WatchKey key;
            try {
                // When an event occurs, the key is signaled and placed into the watcher's queue.
                // Retrieve the key from the watcher's queue.
                key = watchService.take();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for directory events: " + e.getMessage());
                return;
            }
            // Retrieve each pending event for the key
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    // obtain the file name from the event
                    Path filePath = directory.resolve((Path) event.context());
                    // Process the file
                    processFile(filePath, filterPattern, serverAddress, serverPort);
                    // Delete the file
                    Files.delete(filePath);
                }
            }
            key.reset();
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

    private static void processFile(Path filePath, String filterPattern, String serverAddress, int serverPort) {
        try {
            Properties properties = readJavaPropertiesFile(filePath);
            if (properties != null) {
                applyFilterPattern(filterPattern, properties);
                relayToServer(serverAddress, serverPort, properties);
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }

    private static Properties readJavaPropertiesFile(Path filePath) {
        // Read the java properties file into a Map
        Properties properties = new Properties();
        try {
            InputStream inputStream = Files.newInputStream(filePath.toFile().toPath());
            properties.load(inputStream);
            // Adding file name explicitly because at server side, the file should be reconstructed with original file name.
            properties.setProperty("transferFileName", filePath.getFileName().toString());
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error processing file: " + e.getMessage());
            return null;
        }
        return properties;
    }

    private static void applyFilterPattern(String filterPattern, Properties properties) {
        // Apply a regular expression pattern filter for keys
        Pattern pattern = Pattern.compile(filterPattern);
        properties.entrySet().removeIf(entry -> !pattern.matcher(entry.getKey().toString()).matches());
    }

    private static void relayToServer(String serverAddress, int serverPort,
                                      Properties properties) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream())) {
            outputStream.writeObject(properties);
        }
    }
}