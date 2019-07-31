import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;

    private String nick;



    public ClientHandler(Socket socket, Server server) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.equals("/end")) {
                                out.writeUTF("/serverclosed");
                                break;
                            }
                            if(str.startsWith("/registration")){
                                out.writeUTF(DataBaseService.registration(str));
                                server.writeToAdminLogInfo("Client try to registration with data: "+str);
                            }
                            if(str.startsWith("/recovery")){
                                out.writeUTF(DataBaseService.recoveryPass(str));
                                server.writeToAdminLogInfo("Client try to recovery password with data: "+str);


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

                        while (nick!=null){
                            String str = in.readUTF();
                            if(str.startsWith("/")) {
                                if (str.equals("/end")) {
                                    out.writeUTF("/serverclosed");
                                    break;
                                }
                                if(str.startsWith("/w")){
                                    String[] tokens = str.split(" ",3);
                                    server.sendPersonalMsg(ClientHandler.this,tokens[1],tokens[2]);
                                }
                                if(str.startsWith("/blacklist")){
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
//                                            sendMsg("/systemmsg Вы добавили пользователя " + tokens[1] + " в черный список");
                                            server.broadcastSystemMsg(nick+" добавил "+tokens[1]+ " в черный список");
                                        }
                                    }else{
                                        sendMsg("/systemmsg Пользователь с ником "+tokens[1]+" не зарегистрирован");
                                    }
                                }
                                if(str.startsWith("/delblacklist")){
                                    String[] tokens = str.split(" ");
                                    if(tokens.length ==1){
                                        sendMsg("/systemmsg вы не ввели ник пользователя");

                                    }
                                    else if (DataBaseService.isInBlackList(nick, tokens[1])) {
                                        DataBaseService.deleteFromBlackList(nick, tokens[1]);
//                                        sendMsg("Вы удалили пользователя " + tokens[1] + " из черного списока");
                                        server.broadcastSystemMsg(tokens[1]+ " теперь модет общаться с "+nick);

                                    } else {
                                        sendMsg("/systemmsg Пользователя "+ tokens[1]+" нет в черном списке");

                                    }
                                }
                                if(str.startsWith("/clearblacklist")){
                                    DataBaseService.clearBlackList(nick);
                                    sendMsg("/systemmsg Черный список очищен.");
                                }
                            }else {
                                server.broadcastMsg(ClientHandler.this,nick+" "+str);
                                DataBaseService.writeMessageToSQLite(nick, str);

                            }
                    }
                } catch (IOException e) {
//                         e.printStackTrace();
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

                }
            }).start();

        } catch (IOException e) {
            server.writeToAdminLogError(server.stackTraceToString(e.getStackTrace()));
//            e.printStackTrace();
        }
    }
    public void sendMsg(String msg){
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public String getNick() {
        return nick;
    }

}
