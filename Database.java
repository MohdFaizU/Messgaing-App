package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database {

  private String dbUrl;
  private String dbUser;
  private String dbPassword;

  public Database(String dbUrl, String dbUser, String dbPassword) {
    this.dbUrl = dbUrl;
    this.dbUser = dbUser;
    this.dbPassword = dbPassword;
  }

  public String getDbUrl() {
    return dbUrl;
  }

  public String getDbUser() {
    return dbUser;
  }

  public String getDbPassword() {
    return dbPassword;
  }

  public boolean SuccessfulLogin(String user_name, String user_password) {
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query =
        "SELECT * FROM users WHERE user_name = ? AND user_password = ?";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, user_name);
        stmt.setString(2, user_password);
        try (ResultSet rs = stmt.executeQuery()) {
          return rs.next();
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
  }

  public List<Message> getMessageList(String currentUser) {
    List<Message> messages = new ArrayList<>();
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query =
        "SELECT sender_id, message FROM messages WHERE recipient_id = ?";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, currentUser);
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String senderId = rs.getString("sender_id");
            String messageText = rs.getString("message");
            Message message = new Message(senderId, messageText);
            messages.add(message);
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return messages;
  }

  public List<String> getUserList() {
    List<String> users = new ArrayList<>();
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query = "SELECT user_name, status FROM users";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String userName = rs.getString("user_name");
            String status = rs.getString("status");
            users.add(userName + " (" + status + ")");
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return users;
  }

  public void setStatus(String username, String status) {
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query = "UPDATE users SET status = ? WHERE user_name = ?";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, status);
        stmt.setString(2, username);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public boolean SuccessfulSend(
    int messageid,
    String recipient,
    String message,
    String sender
  ) {
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query =
        "INSERT INTO messages (message_id, recipient_id, sender_id, message) VALUES (?,?, ?, ?)";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setInt(1, messageid);
        stmt.setString(2, recipient);
        stmt.setString(3, sender);
        stmt.setString(4, message);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean backupmessages(File file) {
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      String query = "SELECT * FROM messages ORDER BY message_id;";
      try (
        PreparedStatement stmt = conn.prepareStatement(query);
        FileWriter writer = new FileWriter(file)
      ) {
        try (ResultSet rs = stmt.executeQuery()) {
          while (rs.next()) {
            String messageid = rs.getString("message_id");
            String sender = rs.getString("sender_id");
            String recipient = rs.getString("recipient_id");
            String message = rs.getString("message");
            writer.write(
              "INSERT INTO messages VALUES (" +
              messageid +
              ",'" +
              sender +
              "','" +
              recipient +
              "','" +
              message +
              "');\n"
            );
          }
        }
      }
    } catch (SQLException | IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean restoremessages(File file) {
    try (
      Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    ) {
      Statement statement = conn.createStatement();
      String query = new String(Files.readAllBytes(file.toPath()));
      String[] sqlStatements = query.split(";");
      for (String sqlStatement : sqlStatements) {
        statement.execute(sqlStatement);
      }
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
}
