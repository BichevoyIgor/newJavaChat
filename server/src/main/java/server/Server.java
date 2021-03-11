package server;

import commands.Command;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 9999;
    private DataInputStream in;
    private DataOutputStream out;
    private List<ClientHandler> clientHandlerList;
    private AuthService authService;

    public Server() {
        clientHandlerList = new CopyOnWriteArrayList<>();
        //authService = new SimpleAuthService();
        if (!SQLHandler.connect()){
            throw new RuntimeException("Не удалось подключиться");
        }
        authService = new DBAuthService();
        try {
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен");
            while (true) {
                socket = server.accept();
                ClientHandler client = new ClientHandler(this, socket);
                if (client.getLogin() == null) {
                    System.out.println("Open socket: " + socket.getRemoteSocketAddress());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                SQLHandler.disconnect();
                socket.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcastMSG(ClientHandler sender, String msg) {
        String message = String.format("%s: %s", sender.getNickname(), msg);
        for (ClientHandler client : clientHandlerList) {
            client.sendMessage(message);
        }
    }

    public void privateMSG(ClientHandler sender, String reciever, String msg) {
        String message = String.format("%s to %s: %s", sender.getNickname(), reciever, msg);
        for (ClientHandler c : clientHandlerList) {
            if (c.getNickname().equals(reciever)) {
                c.sendMessage(message);
                if (!c.equals(sender)) {
                    sender.sendMessage(message);
                }
                return;
            }
        }
        sender.sendMessage("Получатель " + reciever + " не найден");
    }

    public void subscribe(ClientHandler clientHandler) {
        clientHandlerList.add(clientHandler);
        broadcastClientList();
    }

    public void unsubscribe(ClientHandler clientHandler, Socket socket) {
        clientHandlerList.remove(clientHandler);
        System.out.println("socked is closed " + socket.getRemoteSocketAddress());
        broadcastClientList();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAutentification(String login) {
        for (ClientHandler c : clientHandlerList) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList(){
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST);
        for (ClientHandler c: clientHandlerList){
            sb.append(" ").append(c.getNickname());
        }
        String msg = sb.toString();
        for (ClientHandler c: clientHandlerList){
            c.sendMessage(msg);
        }
    }
}
