package sample;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea textArea1;
    @FXML
    public TextField textField1;
    @FXML
    public Button sendButton;
    @FXML
    public MenuBar menuBar;
    @FXML
    public Menu menuFile;
    @FXML
    public MenuItem menuItemExit;
    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public GridPane messagePanel;
    @FXML
    public HBox authPanel;
    @FXML
    public ListView<String> clientList;
    public MenuItem changeNick;

    private Socket socket;
    private final int PORT = 9999;
    private DataInputStream in;
    private DataOutputStream out;
    private final String HOST = "localhost";

    boolean authentification;
    private String nickname;
    private Stage stage;
    private Stage regStage;
    private RegController regController;
    private FileWriter writer;
    private String historyFileName;

    public void setAuthentification(boolean authentification) {
        this.authentification = authentification;
        messagePanel.setVisible(authentification);
        messagePanel.setManaged(authentification);
        authPanel.setVisible(!authentification);
        authPanel.setManaged(!authentification);
        clientList.setVisible(authentification);
        clientList.setManaged(authentification);
        textArea1.clear();
        setTitle(nickname);
    }

    //метод initialize запустится после прогрузки всех объектов формы (implements Initializable)
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) textArea1.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                stage.close();
                if (socket != null) {
                    try {
                        out.writeUTF(Command.END);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
    }

    private void connect() {
        try {
            socket = new Socket(HOST, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                System.out.println("Клиент отключен");
                                out.writeUTF(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.AUTH_OK)) {
                                String[] token = str.split("\\s");
                                nickname = token[1];
                                setAuthentification(true);
                                historyFileName =String.format("history_[%s].txt", this.nickname);

                                try {
                                    writer = new FileWriter(historyFileName, true);
                                    loadHistory(historyFileName);
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (str.startsWith(Command.CLIENT_LIST)) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                            if (str.equals(Command.REG_OK)) {
                                regController.setResultTryToReg(Command.REG_OK);
                            }
                            if (str.equals(Command.REG_NO)) {
                                regController.setResultTryToReg(Command.REG_NO);
                            }
                        } else {
                            textArea1.appendText(str + "\n");
                            if (writer!=null) {
                                writer.write(str + "\n");
                                writer.flush();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (writer!=null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    setAuthentification(false);
                    setTitle("");
                    loginField.clear();
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

    private void loadHistory(String historyFileName) {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> historyText = Files.readAllLines(Paths.get(historyFileName));
            int startPosition = 0;
            if (historyText.size() > 100){
                startPosition = historyText.size() - 100;
            }
            for (int i = startPosition; i < historyText.size(); i++) {
                //sb.append(historyText.get(i));
                textArea1.appendText((historyText.get(i)) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void send(ActionEvent actionEvent) {
        if (!textField1.getText().isEmpty()) {
            try {
                out.writeUTF(textField1.getText());
                textField1.clear();
                textField1.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else textField1.requestFocus();
    }

    @FXML
    public void sendTextFromTextField(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER) && !textField1.getText().isEmpty()) {
            try {
                out.writeUTF(textField1.getText());
                textField1.clear();
                textField1.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else textField1.requestFocus();
    }

    @FXML
    public void closeWindow(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            Stage stage = (Stage) sendButton.getScene().getWindow();
            stage.close();
            if (socket != null) {
                try {
                    out.writeUTF(Command.END);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void tryAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF(String.format("%s %s %s", Command.AUTH, loginField.getText().trim(), passwordField.getText().trim()));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            passwordField.clear();
         }
    }

    private void setTitle(String nickname) {

        Platform.runLater(() -> {
            if (nickname != null && nickname.equals("")) {
                stage.setTitle("JavaChat");
            } else {
                stage.setTitle("JavaChat - " + nickname);
            }
        });
    }

    public void clientListMouseReleased(MouseEvent mouseEvent) {
        String msg = String.format("%s %s ", Command.PRIVATE_MSG, clientList.getSelectionModel().getSelectedItem());
        textField1.setText(msg);
    }

    public void showRegWindow(ActionEvent actionEvent) {
        if (regStage == null) {
            initRegWindow();
        }
        regStage.show();
    }

    //создаем окно регистрации
    private void initRegWindow() {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml")); // прогружает содержимое окна
            Parent root = fxmlLoader.load();
            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("JavaChat registration");
            regStage.setScene(new Scene(root, 450, 235));
            regStage.initStyle(StageStyle.UTILITY); //убираем кнопки свернуть/развернуть
            regStage.initModality(Modality.APPLICATION_MODAL);//убираем возможность переключиться на основное окно
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registration(String login, String password, String nickName) {
        //открываем сокет если он закрыт
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF(String.format("%s %s %s %s", Command.REG, login, password, nickName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeNick(ActionEvent actionEvent) {
        textField1.appendText(Command.CHNG_NICK + " " + nickname + " to: ");
    }
}
