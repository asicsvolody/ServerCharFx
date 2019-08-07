import java.io.RandomAccessFile;
import java.sql.*;
import java.util.ArrayList;

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
        try {
            PreparedStatement pStmt = connection.prepareStatement("SELECT nickname FROM main where login = ? and password = ?");
            pStmt.setString(1,login);
            pStmt.setString(2,pass);
            ResultSet rs = pStmt.executeQuery();
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
            PreparedStatement pStmt = connection.prepareStatement("INSERT INTO blacklist (id_nick,id_black) VALUES (?,?)");
            pStmt.setInt(1,getId(holderBlackList));
            pStmt.setInt(2,getId(userToBlackList));
            pStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    static synchronized boolean isInBlackList (String holderBlackList, String userForCheck) {
        boolean res = false;

        try {
            PreparedStatement pStmt = connection.prepareStatement("SELECT  main.nickname as 'black' FROM main\n" +
                    "INNER JOIN blacklist ON main.id = blacklist.id_black\n" +
                    "INNER JOIN main m1 ON m1.id = blacklist.id_nick\n" +
                    "WHERE m1.nickname = ? and main.nickname = ?");
            pStmt.setString(1,holderBlackList);
            pStmt.setString(2,userForCheck);
            ResultSet rs = pStmt.executeQuery();
            res = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res;

    }

    static synchronized boolean isUserWithLogin(String login){
        boolean isUser = false;
        try {
            PreparedStatement pStmt = connection.prepareStatement("SELECT id FROM main where login = ?");
            pStmt.setString(1, login);
            ResultSet rs = pStmt.executeQuery();
            isUser = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isUser;
    }

    synchronized static boolean isUserWithNick(String nick){
        boolean isUser = false;
        try {
            PreparedStatement pStmt = connection.prepareStatement("SELECT id FROM main where nickname = ?");
            pStmt.setString(1, nick);
            ResultSet rs = pStmt.executeQuery();
            isUser = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return isUser;

    }


    static synchronized void deleteFromBlackList (String holderBlackList, String userDeleteFromBlackList){
        try {
            PreparedStatement pStmt = connection.prepareStatement("DELETE FROM blacklist WHERE id_nick = ? and id_black = ?");
            pStmt.setInt(1,getId(holderBlackList));
            pStmt.setInt(2,getId(userDeleteFromBlackList));
            pStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static int getId(String nickname) throws SQLException {
        int res = -1;
        PreparedStatement pStmt = connection.prepareStatement("SELECT id FROM main WHERE nickname = ?");
        pStmt.setString(1,nickname);
        ResultSet rs = pStmt.executeQuery();
        if(rs.next()){
            res = rs.getInt(1);
        }
        return res;
    }

    static void writeRegDataToSQLite(String login, String password, String nickname, String controlword) {
        try {
            PreparedStatement pStmt = connection.prepareStatement("INSERT INTO main (login, password, nickname, controlword)\n" +
                    "VALUES (?, ?,?,?)");
            pStmt.setString(1,login);
            pStmt.setString(2,password);
            pStmt.setString(3,nickname);
            pStmt.setString(4,controlword);
            pStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    static String getPassword(String login, String controlWord){
        String password = null;
        try {
            PreparedStatement pStmt = connection.prepareStatement("SELECT password FROM main where login = ? and  controlword = ?;");
            pStmt.setString(1,login);
            pStmt.setString(2,controlWord);
            ResultSet rs = pStmt.executeQuery();
            if(rs.next()){
                password = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return password;
    }



    static void clearBlackList(String holderBlackList){
        try {
            PreparedStatement pStmt =connection.prepareStatement("DELETE FROM blacklist WHERE id_nick = ?");
            pStmt.setInt(1,getId(holderBlackList));
            pStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void writeMessageToSQLite(String nickName, String message){
        try {
            PreparedStatement pStmt =connection.prepareStatement("INSERT INTO messages (nickname, message) VALUES (?,?)");
            pStmt.setString(1,nickName);
            pStmt.setString(2,message);
            pStmt.execute();
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


    static synchronized String[] getAllMessages(){
        ArrayList<String> arrayList = new ArrayList<>();
        String sql = "SELECT nickname, message FROM messages;";
        try {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                arrayList.add(rs.getString(1)+" "+rs.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String[] msgArr =new String[arrayList.size()];
        return  arrayList.toArray(msgArr);
    }

    static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
