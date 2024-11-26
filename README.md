Chat Application (Server-Client)
Description:
This is a simple, multi-user chat application developed in Java. It allows multiple users to log in, join different chat rooms, and engage in real-time messaging. The app also supports file sharing and private messaging. Users can authenticate using a username and password, participate in various chat rooms, send messages, and share files.

Features:

User Authentication: Secure login with a username and password.
Multiple Chat Rooms: Users can create, join, and leave chat rooms as they wish.
Real-Time Messaging: Chat messages are sent and received instantly in chat rooms.
File Sharing: Upload and download files between clients.
Private Messaging: Send private messages to other users within the same chat room.
Session Management: User sessions remain active until logout.


Requirements:
Java 8 or higher
Basic understanding of networking in Java
Instructions:

Compile the Java Files: Ensure all Java files are compiled. You can do this with the following command:
javac Server.java Client.java ClientHandler.java
Run the Server: To start the server, run the command below. The server will listen for client connections on port 12345 by default:
java Server
Run the Client: On the client side, you can connect to the server by running:
java Client
The client will prompt you to enter a username and password for login.

Usage:
User Login:
first run the client and enter your username and password. If you don't have an account, it will register a one.

Joining a Chat Room:
After logging in, you can join an existing chat room or create a new one with the following commands.

Sending Messages:
Type your message in the input field and press Enter to send it to the chat room. All users in the room will see the message in real-time.

Uploading/Downloading Files:

To upload a file, use the /upload <file_path> command. Or can be done using UI.
To download a file, use the /download <file_name> command.
Available Commands:
Create a new room: /create <room_name>
Join an existing room: /join <room_name>
Send a message: Type your message and hit Enter.
Upload a file: /upload <file_path>
Download a file: /download <file_name>
