import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private Logger adminLog;

    public Server() throws SQLException {
        clients = new Vector<>();
        ServerSocket server = null;

        Socket socket = null;

        adminLog = Logger.getLogger("adminChat");
        try {
            DataBaseService.connect();

            server = new ServerSocket(8189);
            writeToAdminLogInfo("Server is start");

            while (true){
                socket = server.accept();
                writeToAdminLogInfo("Client connect");

                new ClientHandler(socket,this);

            }
        }
        catch (IOException e) {
            writeToAdminLogFatal("Сервер упал!"+stackTraceToString(e.getStackTrace()));
//            e.printStackTrace();
        }
        finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            DataBaseService.disconnect();
        }
    }
    boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    void broadcastSystemMsg(String msg){
        for (ClientHandler o: clients) {
            o.sendMsg("/systemmsg "+msg);
        }
    }

    void broadcastMsg(ClientHandler from, String msg) {
        for (ClientHandler o : clients) {
            if (!DataBaseService.isInBlackList(from.getNick(),o.getNick())) {
                o.sendMsg(msg);
            }
        }
        writeToAdminLogDebug(msg);
    }

    void sendPersonalMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nickTo)) {
                if(DataBaseService.isInBlackList(o.getNick(),from.getNick())){
                    from.sendMsg("/wsystemmsg "+nickTo+" Вы находитесь в черном списке.");
                    return;
                }else {
                    o.sendMsg("/w " + from.getNick() +" "+ from.getNick()+" " + msg);
                    from.sendMsg("/w " + nickTo + " "+from.getNick()+ " " + msg);
                    writeToAdminLogDebug(String.format("%s private message to %s: %s",from.getNick(),nickTo,msg));
                    return;
                }
            }
        }
        from.sendMsg("/systemmsg Клиент с ником " + nickTo + " не в чате");
    }

    void subscribe (ClientHandler clientHandler){
        clients.add(clientHandler);
        broadcastSystemMsg(clientHandler.getNick()+" подключился");
        broadcastClientList();
        DataBaseService.sendAllMessage(clientHandler);


    }

    void unsubscribe (ClientHandler clientHandler){
        clients.remove(clientHandler);
        broadcastSystemMsg(clientHandler.getNick()+" отключился");
        broadcastClientList();
    }


    private void broadcastClientList(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/clientlist ");
        for (ClientHandler clientHandler : clients) {
           stringBuilder.append(clientHandler.getNick()) ;
           stringBuilder.append(" ");
        }
        String out = stringBuilder.toString();
        for (ClientHandler clientHandler: clients) {
            clientHandler.sendMsg(out);
        }

    }

    void writeToAdminLogInfo(String logMessage){
        adminLog.info(logMessage);
    }

    void writeToAdminLogWarn(String logMessage){
        adminLog.warn(logMessage);
    }

    private void writeToAdminLogDebug(String logMessage){
        adminLog.debug(logMessage);
    }

    private void writeToAdminLogFatal(String logMessage){
        adminLog.fatal(logMessage);
    }

    void writeToAdminLogError(String logMessage){
        adminLog.error(logMessage);
    }

    String stackTraceToString(StackTraceElement[] stackTracesArr){
        StringBuilder sb = new StringBuilder("\n");
        for (StackTraceElement st : stackTracesArr) {
            sb.append("\t");
            sb.append(st.toString());
            sb.append("\n");
        }
        return sb.toString();
    }


}
