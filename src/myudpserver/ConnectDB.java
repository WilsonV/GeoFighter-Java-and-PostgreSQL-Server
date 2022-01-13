package myudpserver;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectDB {

  private Connection databaseConnection = null;

  public Connection getConnection() {

    try {
      Class.forName("org.postgresql.Driver");
      databaseConnection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/geofighter", "Gamer", "");

    } catch (Exception e) {
      System.out.println(e);
    }

    return databaseConnection;
  }

}
