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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
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

    //?????????? initialize ???????????????????? ?????????? ?????????????????? ???????? ???????????????? ?????????? (implements Initializable)
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
                                System.out.println("???????????? ????????????????");
                                out.writeUTF(Command.END);
                                break;
                            }
                            if (str.startsWith(Command.AUTH_OK)) {
                                String[] token = str.split("\\s");
                                nickname = token[1];
                                setAuthentification(true);
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
                            if (str.equals(Command.REG_OK)){
                                regController.setResultTryToReg(Command.REG_OK);
                            }
                            if (str.equals(Command.REG_NO)){
                                regController.setResultTryToReg(Command.REG_NO);
                            }
                        } else textArea1.appendText(str + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
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
    public void CloseWindow(ActionEvent actionEvent) {
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

    //?????????????? ???????? ??????????????????????
    private void initRegWindow() {
        try {

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml")); // ???????????????????? ???????????????????? ????????
            Parent root = fxmlLoader.load();
            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("JavaChat registration");
            regStage.setScene(new Scene(root, 450, 235));
            regStage.initStyle(StageStyle.UTILITY); //?????????????? ???????????? ????????????????/????????????????????
            regStage.initModality(Modality.APPLICATION_MODAL);//?????????????? ?????????????????????? ?????????????????????????? ???? ???????????????? ????????
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registration(String login, String password, String nickName){
        //?????????????????? ?????????? ???????? ???? ????????????
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF(String.format("%s %s %s %s", Command.REG, login, password, nickName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
