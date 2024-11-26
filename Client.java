import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.io.*;
import java.net.*;

public class Client extends Application {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private TextArea chatArea;
    private TextField messageField;
    private Button sendButton;
    private Button uploadButton;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Button downloadButton;

    public Client() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            downloadButton = new Button("Download File");
            downloadButton.setOnAction(e -> downloadFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Supported Files", "*.txt", "*.jpg", "*.png", "*.*"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                out.println("DOWNLOAD " + file.getName());
                long fileSize = Long.parseLong(in.readLine());

                if (fileSize == -1) {
                    System.out.println("File not found on server.");
                    return;
                }

                byte[] buffer = new byte[4096];
                try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(file))) {
                    int bytesRead;
                    while ((bytesRead = in.read()) != -1) {
                        fileOut.write(bytesRead);
                        fileOut.flush();
                    }
                }
                System.out.println("File downloaded successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
        new Client();
    }

    @Override
    public void start(Stage primaryStage) {
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(400);
        chatArea.setWrapText(true);

        messageField = new TextField();
        messageField.setPromptText("Type a message...");

        sendButton = new Button("Send");
        uploadButton = new Button("Upload File");

        HBox inputLayout = new HBox(10, messageField, sendButton, uploadButton);
        inputLayout.setAlignment(Pos.CENTER);
        inputLayout.setPadding(new Insets(10));

        VBox mainLayout = new VBox(10, new ScrollPane(chatArea), inputLayout);
        mainLayout.setPadding(new Insets(10));

        Scene scene = new Scene(mainLayout, 500, 500);
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        setupConnection();

        sendButton.setOnAction(e -> sendMessage());
        uploadButton.setOnAction(e -> uploadFile());

        new Thread(this::listenForMessages).start();
    }

    private void setupConnection() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to the chat server!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            chatArea.appendText("You: " + message + "\n");
            messageField.clear();
        }
    }

    private void uploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {
                out.println("FILE_UPLOAD");
                out.println(file.getName());
                out.println(file.length());
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    socket.getOutputStream().write(buffer, 0, bytesRead);
                }
                fileInputStream.close();
                chatArea.appendText("File uploaded: " + file.getName() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void listenForMessages() {
        try {
            String serverMessage;
            while ((serverMessage = in.readLine()) != null) {
                if (serverMessage.equals("FILE_UPLOAD")) {
                    receiveFile();
                } else {
                    chatArea.appendText(serverMessage + "\n");
                    showNotification("New Message", serverMessage);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile() {
        try {
            String fileName = in.readLine();
            long fileLength = Long.parseLong(in.readLine());
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileLength) {
                bytesRead = socket.getInputStream().read(buffer);
                totalBytesRead += bytesRead;
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
            chatArea.appendText("File received: " + fileName + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String title, String message) {
        System.out.println("Notification - " + title + ": " + message);
    }
}
