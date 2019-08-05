import java.sql.*;

class DataBaseService {

    private static Connection connection;
    private static Statement stmt;

    static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:./src/main/resources/MyUsers.db");
            stmt = connection.createStatement();
            clearMessageTable();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    static synchronized String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname FROM main where login = '%s' and password = '%s'", login, pass);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static synchronized void addToBlackList(String holderBlackList, String userToBlackList){
        try {
            String sql = String.format("INSERT INTO blacklist (id_nick,id_black) VALUES (%s,%s)",getId(holderBlackList),getId(userToBlackList));
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static synchronized boolean isInBlackList (String holderBlackList, String userForCheck) {
        boolean res = false;

        try {
            String sql = String.format("SELECT  main.nickname as 'black' FROM main\n" +
                    "INNER JOIN blacklist ON main.id = blacklist.id_black\n" +
                    "WHERE blacklist.id_nick = %s and main.nickname ='%s'",getId(holderBlackList),userForCheck);
            ResultSet rs = stmt.executeQuery(sql);
            res = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;

    }

    static synchronized boolean isUserWithLogin(String login){
        boolean isUser = false;
        String sql = String.format("SELECT id FROM main where login = '%s'", login );
        try {
            ResultSet rs = stmt.executeQuery(sql);
            isUser = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isUser;
    }

    synchronized static boolean isUserWithNick(String nick){
        boolean isUser = false;
        String sql = String.format("SELECT id FROM main where nickname = '%s'", nick );
        try {
            ResultSet rs = stmt.executeQuery(sql);
            isUser = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isUser;

    }


    static synchronized void deleteFromBlackList (String holderBlackList, String userDeleteFromBlackList){
        try {
            String sql = String.format("DELETE FROM blacklist WHERE id_nick = %s and id_black = %s"
                    ,getId(holderBlackList),getId(userDeleteFromBlackList));
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static int getId(String nickname) throws SQLException {
        int res = -1;
        String sql = String.format("SELECT id FROM main WHERE nickname = '%s'",nickname);
        ResultSet rs = stmt.executeQuery(sql);
        if(rs.next()){
            res = rs.getInt(1);
        }
        return res;
    }

    static void writeRegDataToSQLite(String login, String password, String nickname, String controlword) {
        String sql = String.format("INSERT INTO main (login, password, nickname, controlword)\n" +
                "VALUES ('%s', '%s','%s','%s');", login, password, nickname, controlword);
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static synchronized String recoveryPass(String recoveryData){
        String resMsg = "/recovery ";
        String[] recoveryDataArr = recoveryData.split(" ");
        String sql = String.format("SELECT password FROM main where login = '%s' and  controlword = '%s';"
                ,recoveryDataArr[1],recoveryDataArr[2]);
        try {
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()){
                resMsg+=rs.getString(1);
            }
            else {
                resMsg+="User not found";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resMsg;
    }



    static void clearBlackList(String holderBlackList){
        try {
            String sql = String.format("DELETE FROM blacklist WHERE id_nick = %s",getId(holderBlackList));
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void writeMessageToSQLite(String nickName, String message){
        String sql = String.format("INSERT INTO messages (nickname, message) VALUES ('%s','%s');",nickName, message);
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void clearMessageTable(){
        String sql = "DELETE FROM messages";
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    static synchronized void sendAllMessage(ClientHandler clientHandler) {
        String sql = "SELECT nickname, message FROM messages;";
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while(rs.next()){
                clientHandler.sendMsg(rs.getString(1)+" "+rs.getString(2));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
