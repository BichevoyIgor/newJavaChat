package server;

public interface AuthService {
    String getNicknameAndPassword(String login, String password);
    boolean registration(String login, String password, String nickName);
    boolean changeNick(String oldNickName, String newNickName);

}
