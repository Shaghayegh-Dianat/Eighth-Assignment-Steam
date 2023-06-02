package Server.User;

import Server.Entities.UserEntity;
import Shared.Dto.LoginDto;
import Shared.Dto.RegisterDto;
import Shared.Response;
import Shared.Title;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.UUID;

public class UserService {
    Connection connection;
    Statement statement;
    String query;
    String url = "jdbc:postgresql://localhost:5432/postgres";
    String user = "postgres";
    String pass = "";
    public UserService() {
        try {
            this.connection = DriverManager.getConnection(url, user, pass);
            System.out.println("Connected to the PostgreSQL database for users' data!");
            this.statement = connection.createStatement();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Response login(LoginDto data) {
        Response response = new Response();
        response.setTitle(Title.login);
        UserEntity foundUser = findByUsername(data.username);
        if (foundUser.getId().length() > 0){
            if (BCrypt.checkpw(data.password, foundUser.getPassword())){
                response.setStatus("successful");
                foundUser.setPassword(null);
                response.setData(foundUser);
            } else {
                response.setMessage("WrongUsername");
            }
        } else {
            response.setMessage("WrongPassword");
        }
        return response;
    }

    public Response register(RegisterDto data) {
        Response response = new Response();
        response.setTitle(Title.signup);
        UserEntity userEntity = new UserEntity();

        if (findByUsername(data.username).getUsername() != null) {
            response.setMessage("duplicateUsername");
            return response;
        }

        userEntity.setId(UUID.randomUUID().toString());
        userEntity.setUsername(data.username);
        userEntity.setPassword(BCrypt.hashpw(data.password, BCrypt.gensalt()));
        userEntity.setDateOfBirth(data.dateOfBirth);
        if (!insertIntoTable(userEntity)) {
            response.setMessage("dataBaseError");
            return response;
        }
        response.setStatus("successful");

        userEntity.setPassword(null);
        response.setData(userEntity);
        return response;
    }

    public Response logOut(){
        Response response = new Response();
        response.setTitle(Title.logOut);
        response.setStatus("successful");
        return response;
    }

    public boolean insertIntoTable(UserEntity data) {
        this.query = "INSERT INTO \"users\" (id, username, password, date_of_birth) VALUES (?, ?, ?, ?);";
        try {
            PreparedStatement insertStatement = this.connection.prepareStatement(query);
            insertStatement.setString(1, data.getId());
            insertStatement.setString(2, data.getUsername());
            insertStatement.setString(3, data.getPassword());
            insertStatement.setDate(4, new Date(data.getDateOfBirth().getTime()));
            insertStatement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public UserEntity findByUsername(String username) {
        UserEntity userEntity = new UserEntity();
        this.query = "SELECT * FROM \"users\" WHERE username = ?;";
        try {
            PreparedStatement selectStatement = this.connection.prepareStatement(query);
            selectStatement.setString(1, username);
            ResultSet rs = selectStatement.executeQuery();
            if (rs.next()){
                userEntity.setId(rs.getString("id"));
                userEntity.setUsername(rs.getString("username"));
                userEntity.setPassword(rs.getString("password"));
                userEntity.setDateOfBirth(rs.getDate("date_of_birth"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userEntity;
    }
}
