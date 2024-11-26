import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 12345; // Port for server-client communication

    // Thread-safe lists and maps for managing clients, credentials, and chat rooms
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, String> userCredentials = new ConcurrentHashMap<>(); // Username-password storage
    private static final Map<String, List<ClientHandler>> chatRooms = new HashMap<>(); // Chat room management

    public static void main(String[] args) {
        // Load existing user credentials (if available)
        loadUserCredentials();

        // Start the server and wait for client connections
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and waiting for connections...");

            while (true) {
                // Accept a new client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create a handler for the connected client and start its thread
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients, userCredentials, chatRooms);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all clients in a specific chat room, excluding the sender.
     */
    public static void broadcast(String message, ClientHandler sender, String chatRoom) {
        synchronized (chatRooms) {
            List<ClientHandler> roomClients = chatRooms.get(chatRoom);
            if (roomClients != null) {
                for (ClientHandler client : roomClients) {
                    if (client != sender) {
                        client.sendMessage(message);
                    }
                }
            }
        }
    }

    /**
     * Saves user credentials to a file for persistence.
     */
    public static void saveUserCredentials() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("credentials.txt"))) {
            for (Map.Entry<String, String> entry : userCredentials.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads user credentials from a file, if available.
     */
    private static void loadUserCredentials() {
        try (BufferedReader reader = new BufferedReader(new FileReader("credentials.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    userCredentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.out.println("No credentials file found. Starting fresh.");
        }
    }

    /**
     * Handles file uploads from a client.
     */
    public static void handleFileUpload(Socket clientSocket) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream())) {
            String command = in.readUTF();
            if ("FILE_UPLOAD".equals(command)) {
                String fileName = in.readUTF();
                long fileSize = in.readLong();

                // Save the uploaded file on the server
                try (FileOutputStream fileOut = new FileOutputStream("server_files/" + fileName)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (fileSize > 0 && (bytesRead = in.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                        fileSize -= bytesRead;
                    }
                }
                System.out.println("Received file: " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles file download requests from clients.
     */
    public static void handleFileDownload(String fileName, PrintWriter out, Socket clientSocket) {
        File file = new File("server_files/" + fileName);

        try (BufferedOutputStream fileOut = new BufferedOutputStream(clientSocket.getOutputStream());
             FileInputStream fileIn = new FileInputStream(file)) {

            if (file.exists()) {
                // Notify the client about the file size
                out.println(file.length());

                // Send the file content in chunks
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.flush();
                System.out.println("File sent: " + fileName);
            } else {
                out.println("File not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
