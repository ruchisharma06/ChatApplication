import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

// Handles client interactions and manages chat functionality
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private List<ClientHandler> clients; // List of all connected clients
    private Map<String, String> userCredentials; // Stores usernames and passwords
    private Map<String, List<ClientHandler>> chatRooms; // Manages chat rooms
    private String currentRoom = "global"; // Default chat room

    // Constructor to initialize a client handler
    public ClientHandler(Socket socket, List<ClientHandler> clients, 
                         Map<String, String> userCredentials, 
                         Map<String, List<ClientHandler>> chatRooms) {
        this.clientSocket = socket;
        this.clients = clients;
        this.userCredentials = userCredentials;
        this.chatRooms = chatRooms;

        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Entry point when a new thread starts for a client
    @Override
    public void run() {
        try {
            // Authenticate the user before proceeding
            if (!authenticateUser()) {
                clientSocket.close();
                return;
            }

            out.println("Welcome to the chat, " + username + "! Type /help for commands.");
            joinRoom(currentRoom); // Automatically join the default room

            // Listen for client messages
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.startsWith("/")) {
                    processCommand(inputLine); // Handle commands
                } else {
                    logMessage(inputLine); // Log the message
                    Server.broadcast("[" + username + "]: " + inputLine, this, currentRoom); // Send to all in room
                }
            }

            // Cleanup when the client disconnects
            leaveRoom(currentRoom);
            clients.remove(this);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Authenticate the user or register a new account
    private boolean authenticateUser() throws IOException {
        out.println("Enter your username:");
        String inputUsername = in.readLine();
        out.println("Enter your password:");
        String inputPassword = in.readLine();

        if (userCredentials.containsKey(inputUsername)) {
            if (userCredentials.get(inputUsername).equals(inputPassword)) {
                username = inputUsername;
                return true;
            } else {
                out.println("Incorrect password. Disconnecting.");
                return false;
            }
        } else {
            // Register a new user
            out.println("Username not found. Registering new user.");
            userCredentials.put(inputUsername, inputPassword);
            Server.saveUserCredentials();
            username = inputUsername;
            return true;
        }
    }

    // Process chat commands from the user
    private void processCommand(String command) {
        if (command.startsWith("/pm")) {
            String[] parts = command.split(" ", 3);
            if (parts.length == 3) {
                sendPrivateMessage(parts[1], parts[2]);
            } else {
                out.println("Usage: /pm <username> <message>");
            }
        } else if (command.startsWith("/join")) {
            String[] parts = command.split(" ", 2);
            if (parts.length == 2) {
                leaveRoom(currentRoom);
                joinRoom(parts[1]);
            } else {
                out.println("Usage: /join <room>");
            }
        } else if (command.equals("/help")) {
            out.println("/pm <username> <message> - Send a private message.");
            out.println("/join <room> - Join a chat room.");
            out.println("/leave - Leave the chat.");
        } else if (command.equals("/leave")) {
            out.println("Goodbye!");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            out.println("Unknown command. Type /help for a list of commands.");
        }
    }

    // Send a private message to another user
    private void sendPrivateMessage(String targetUser, String message) {
        for (ClientHandler client : clients) {
            if (client.username.equals(targetUser)) {
                client.sendMessage("[Private from " + username + "]: " + message);
                return;
            }
        }
        out.println("User " + targetUser + " not found.");
    }

    // Join a specified chat room
    private void joinRoom(String roomName) {
        currentRoom = roomName;
        synchronized (chatRooms) {
            chatRooms.computeIfAbsent(roomName, k -> new ArrayList<>()).add(this);
        }
        Server.broadcast(username + " has joined the room.", this, currentRoom);
    }

    // Leave the current chat room
    private void leaveRoom(String roomName) {
        synchronized (chatRooms) {
            List<ClientHandler> roomClients = chatRooms.get(roomName);
            if (roomClients != null) {
                roomClients.remove(this);
                Server.broadcast(username + " has left the room.", this, roomName);
            }
        }
    }

    // Send a message to the current client
    public void sendMessage(String message) {
        out.println(message);
    }

    // Log messages to a file
    private void logMessage(String message) {
        try (FileWriter fw = new FileWriter("chat_history.txt", true);
             PrintWriter logWriter = new PrintWriter(fw)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            logWriter.println("[" + timestamp + "] [" + username + "]: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
