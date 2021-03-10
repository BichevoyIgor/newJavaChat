package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        //отключение
                        if (str.equals(Command.END)) {
                            out.writeUTF(Command.END);
                            break;
                        }
                        //аутентификация
                        if (str.startsWith(Command.AUTH)) {
                            String[] token = str.split("\\s");
                            if (token.length < 3) {
                                continue;
                            }
                            String newNick = server.getAuthService().getNicknameAndPassword(token[1], token[2]);
                            login = token[1];
                            if (newNick != null) {
                                if (!server.isLoginAutentification(login)) {
                                    nickname = newNick;
                                    sendMessage(Command.AUTH_OK + " " + nickname);
                                    server.subscribe(this);
                                    System.out.println("client connected with nick: " + nickname + " from " + socket.getRemoteSocketAddress());
                                    break;
                                } else {
                                    sendMessage("Логин занят");
                                }

                            } else {
                                sendMessage("Не верный логиин или пароль");
                                continue;
                            }
                        }
                        //регистрация
                        if (str.startsWith(Command.REG)) {
                            String[] token = str.split("\\s", 4);
                            if (token.length < 4) {
                                continue;
                            }
                            boolean regSuccess = server.getAuthService().registration(token[1], token[2], token[3]);
                            if (regSuccess) {
                                sendMessage(Command.REG_OK);
                            } else sendMessage(Command.REG_NO);
                        }
                    }
                    //цикл работы
                    while (true) {
                        socket.setSoTimeout(0);
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                System.out.println("Клиент отключен " + socket.getRemoteSocketAddress());
                                out.writeUTF(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.PRIVATE_MSG)) {
                                String[] token = str.split("\\s", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMSG(this, token[1], token[2]);
                            }
                        } else {
                            server.broadcastMSG(this, str);
                        }
                    }

                } catch (SocketTimeoutException e) {
                    try {
                        out.writeUTF(Command.END);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this, socket);
                    try {
                        in.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String str) {
        try {
            out.writeUTF(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
