package server;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {

    private class UserData {
        String login;
        String password;
        String nickName;

        public UserData(String login, String password, String nickName) {
            this.login = login;
            this.password = password;
            this.nickName = nickName;
        }
    }

    private List<UserData> users;

    public SimpleAuthService() {
        users = new ArrayList<>();
        users.add(new UserData("igoreek", "123", "igoreek"));
        for (int i = 1; i <= 10; i++) {
            users.add(new UserData("u" + i, "123", "u" + i));
        }
    }

    @Override
    public String getNicknameAndPassword(String login, String password) {
        for (UserData user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.nickName;
            }
        }
        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickName) {
        for (UserData user : users) {
            if (user.login.equals(login) || user.nickName.equals(nickName)) {
                return false;
            }
        }
        users.add(new UserData(login, password, nickName));
        return true;
    }
}
