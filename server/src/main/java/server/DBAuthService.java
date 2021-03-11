package server;

public class DBAuthService implements AuthService {
    @Override
    public String getNicknameAndPassword(String login, String password) {

        return SQLHandler.getNicknameAndPassword(login, password);
    }

    @Override
    public boolean registration(String login, String password, String nickName) {
        return SQLHandler.registration(login, password, nickName);
    }

    @Override
    public boolean changeNick(String oldNickName, String newNickName) {
        return SQLHandler.changeNick(oldNickName, newNickName);
    }
}
