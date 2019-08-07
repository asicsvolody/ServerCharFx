import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientHandler {

    private Socket socket;
    private Server server;

    private DataInputStream in;
    private DataOutputStream out;

    private String nick;



    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(()-> {
                try {
                    loginAndRegCycle();

                    mainWorkCycle();

            } catch (IOException e) {
                    server.writeToAdminLogError(server.stackTraceToString(e.getStackTrace()));

                }finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    nick = (nick == null)? "Not log in user":nick;
                    server.writeToAdminLogInfo(nick+" - Disconnect ");

                    server.unsubscribe(ClientHandler.this);

                }
            }).start();

        } catch (IOException e) {
            server.writeToAdminLogError(server.stackTraceToString(e.getStackTrace()));
        }
    }

    private void loginAndRegCycle() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.equals("/end")) {
                out.writeUTF("/serverclosed");
                break;
            }
            if(str.startsWith("/registration")){
               tryToRegistration(str);
            }
            if(str.startsWith("/recovery")){
                tryToRecovery(str);
            }
            if(str.startsWith("/auth")) {
                String[] tokens = str.split(" ");
                String newNick = DataBaseService.getNickByLoginAndPass(tokens[1], tokens[2]);
                if (newNick != null) {
                    if(!server.isNickBusy(newNick)){
                        sendMsg("/authok " +newNick);
                        nick = newNick;
                        server.writeToAdminLogInfo(nick + "- log in is ok ");
                        server.subscribe(ClientHandler.this);
                        break;
                    } else {
                        sendMsg("Пользователь в сети.");
                        server.writeToAdminLogWarn("Attempt to log in with client in net with login:"+tokens[1]);
                    }
                }else {
                    sendMsg("Неверный логин/пароль");
                    server.writeToAdminLogWarn(String.format(
                            "Attempt to log in with wrong login:%s, password:%s",tokens[1],tokens[2]));
                }
            }
        }
    }

    private void tryToRegistration(String str) throws IOException {
        out.writeUTF(registration(str));
        server.writeToAdminLogInfo("Client try to registration with data: "+str);
    }

    private void tryToRecovery(String str) throws IOException {
        out.writeUTF(recoveryPass(str));
        server.writeToAdminLogInfo("Client try to recovery password with data: "+str);
    }

    private static synchronized String recoveryPass(String recoveryData){
        String resMsg = "/recovery ";
        String[] recoveryDataArr = recoveryData.split(" ");
        String password = DataBaseService.getPassword(recoveryDataArr[1],recoveryDataArr[2]);
        if(password!=null){
            resMsg+=password;
        }
        else {
            resMsg+="User not found";
        }
        return resMsg;
    }


    private void mainWorkCycle() throws IOException {
        while (nick!=null){
            String str = in.readUTF();
            if(str.startsWith("/")) {
                if (str.equals("/end")) {
                    out.writeUTF("/serverclosed");
                    break;
                }
                if(str.startsWith("/w")){
                   workWithPrivateMsg(str);
                }
                if(str.startsWith("/blacklist")){
                    addToBlacklist(str);                }
                if(str.startsWith("/delblacklist")){
                    deleteFromBlackList(str);
                }
                if(str.startsWith("/clearblacklist")){
                    clearBlackList();
                }
            }else {
                server.broadcastMsg(ClientHandler.this,nick+" "+str);
                DataBaseService.writeMessageToSQLite(nick, str);

            }
        }
    }

    private void workWithPrivateMsg(String str){
        String[] tokens = str.split(" ",3);
        server.sendPersonalMsg(ClientHandler.this,tokens[1],tokens[2]);
    }

    private void addToBlacklist(String str){
        String[] tokens = str.split(" ");
        if(tokens.length == 1){
            sendMsg("/systemmsg command not found");
        }
        else if(nick.equals(tokens[1])){
            sendMsg("/systemmsg Вы не можете добавить в черный список самого себя");
        }
        else if(DataBaseService.isUserWithNick(tokens[1])) {
            if (DataBaseService.isInBlackList(nick, tokens[1])) {
                sendMsg("/systemmsg пользователь уже в черном списке");
            } else {
                DataBaseService.addToBlackList(nick, tokens[1]);
                server.broadcastSystemMsg(nick+" добавил "+tokens[1]+ " в черный список");
            }
        }else{
            sendMsg("/systemmsg Пользователь с ником "+tokens[1]+" не зарегистрирован");
        }
    }

    private void deleteFromBlackList(String str){
        String[] tokens = str.split(" ");
        if(tokens.length ==1){
            sendMsg("/systemmsg вы не ввели ник пользователя");
        }
        else if (DataBaseService.isInBlackList(nick, tokens[1])) {
            DataBaseService.deleteFromBlackList(nick, tokens[1]);
            server.broadcastSystemMsg(tokens[1]+ " теперь может общаться с "+nick);

        } else {
            sendMsg("/systemmsg Пользователя "+ tokens[1]+" нет в черном списке");

        }
    }

    private void clearBlackList(){
        DataBaseService.clearBlackList(nick);
        sendMsg("/systemmsg Черный список очищен.");
    }

    private String registration(String regData){
        String msg="";
        String[] regDataArr = regData.split(" ");
        if(DataBaseService.isUserWithLogin(regDataArr[1])){
            msg = "Пользователь с логином "+regDataArr[1]+ " уже зарегистрирован";
        }else if(DataBaseService.isUserWithNick(regDataArr[3])){
            msg = "Пользователь с ником "+regDataArr[3]+ " уже зарегистрирован";
        }else {
            DataBaseService.writeRegDataToSQLite(regDataArr[1], regDataArr[2], regDataArr[3], regDataArr[4]);
            msg = "/regok";
        }
        return msg;
    }

    void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    String getNick() {
        return nick;
    }

}
