package sample;

import commands.Command;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javax.swing.*;

public class RegController {
    @FXML
    public TextField nicknamefield;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextArea textArea;
    @FXML
    private Button reg;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void setResultTryToReg(String comnand) {
        if (comnand.equals(Command.REG_OK)) {
            textArea.appendText("Регистрация прошла успешно\n");
        }
        if (comnand.equals(Command.REG_NO)) {
            textArea.appendText("Логин или никнейм занят\n");
        }
    }


    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknamefield.getText().trim();
        if (login.length() * password.length() * nickname.length() == 0) {
            return;
        }
        controller.registration(login, password, nickname);
    }
}
