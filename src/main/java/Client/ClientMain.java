package Client;

import Server.Entities.GameEntity;
import Server.Entities.UserEntity;
import Shared.Dto.GameDto;
import Shared.Dto.LoginDto;
import Shared.Dto.RegisterDto;
import Shared.Dto.UserDto;
import Shared.Title;
import Shared.Request;
import Shared.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;


public class ClientMain {
    private static Scanner in = new Scanner(System.in);
    public static ObjectMapper objectMapper = new ObjectMapper();
    public static DataInputStream dataInputStream;
    public static DataOutputStream dataOutputStream;

    private static String currentUserId = "";

    public static void main(String[] args) throws IOException {
        final int PORT = 3000;

        Socket socket = new Socket("localhost", PORT);

        InputStream inputStream = socket.getInputStream();
        dataInputStream = new DataInputStream(inputStream);

        OutputStream outputStream = socket.getOutputStream();
        dataOutputStream = new DataOutputStream(outputStream);
        objectMapper.registerModule(new JavaTimeModule());
        dataOutputStream.close();
        dataInputStream.close();
        start();

    }

    private static void receiveFile(String idGame) throws IOException {
        int bytes = 0;

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(idGame);

            long size = dataInputStream.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                // Here we write the file using write method
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes; // read upto file size
            }
            // Here we received file
            System.out.println("File is Received");
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataOutputStream.close();
        dataInputStream.close();

    }


    private static void start() throws IOException {
        System.out.println("**Steam**");
        System.out.println("1) logIn\n" +
                "2) signUp\n" +
                "3) Exit");

        try {
            int input = in.nextInt();
            in.nextLine();
            switch (input) {
                case 1:
                    login();
                    break;
                case 2:
                    signup();
                    break;
                case 3:
                    exit();
                    break;
                default:
                    System.out.println("Please inter a number from 1 to 3.");
                    start();
            }
        } catch (Exception e) {
            System.out.println("Please enter a number.");
            start();
        }
        dataOutputStream.close();
        dataInputStream.close();
    }

    private static void login() throws IOException {
        Response rs = new Response();
        System.out.println("Enter your username");
        String username = in.nextLine();
        System.out.println("Enter your password");
        String password = in.nextLine();
        System.out.println();

        LoginDto loginDto = new LoginDto();
        loginDto.username = username;
        loginDto.password = password;

        Request request = new Request();
        request.setTitle(Title.login);
        request.setData(loginDto);

        String command = null;
        try {
            command = objectMapper.writeValueAsString(request);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request

            String response = dataInputStream.readUTF();
            rs = objectMapper.readValue(response, Response.class);//receive response
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rs.getStatus().equals("successful")) {
            UserEntity userEntity = objectMapper.convertValue(rs.getData(), UserEntity.class);
            currentUserId = userEntity.getId();
            menu();
        } else {
            System.out.println("please retry to login");
            start();
        }
        dataOutputStream.close();
        dataInputStream.close();

    }

    private static void signup() throws IOException {
        Response rs = new Response();
        System.out.println("Make a username");
        String username = in.nextLine();
        System.out.println("Make a password");
        String password = in.nextLine();
        System.out.println("Enter your date of birth in format  dd-mm-yyyy");
        String strDateOfBirth = in.nextLine();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy");
        Date date = new Date();
        try {
            date = dateFormat.parse(strDateOfBirth);
            System.out.println("Date entered: " + date);
        } catch (Exception e) {
            System.out.println("Invalid date format");
            signup();
        }

        RegisterDto registerDto = new RegisterDto();
        registerDto.username = username;
        registerDto.password = password;
        registerDto.dateOfBirth = date;


        Request request = new Request();
        request.setTitle(Title.signup);
        request.setData(registerDto);


        try {
            String command = objectMapper.writeValueAsString(request);
            System.out.println(command);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request

            String response = dataInputStream.readUTF();
            rs = objectMapper.readValue(response, Response.class);//receive response
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rs.getStatus().equals("successful")) {
            UserEntity userEntity = objectMapper.convertValue(rs.getData(), UserEntity.class);
            currentUserId = userEntity.getId();
            menu();
        } else {
            System.out.println("please retry to login");
            start();
        }
        dataOutputStream.close();
        dataInputStream.close();
    }

    private static void exit() throws IOException {
        Request request = new Request();
        request.setTitle(Title.exit);
        String command = null;
        try {
            command = objectMapper.writeValueAsString(request);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataOutputStream.close();
        dataInputStream.close();
    }

    public static void menu() throws IOException {
        System.out.println("1) view all game\n" +
                "       view one game \n" +
                "       download file\n"+
                "2)back to start");


        try {
            int input = in.nextInt();
            in.nextLine();
            switch (input) {
                case 1:
                    view_all_game();
                    break;

                case 2:
                    start();

                default:
                    System.out.println("Please inter a number from 1 to 4.");
                    start();
            }
        } catch (Exception e) {
            System.out.println("Please enter a number.");
            start();
        }

    }

    public static void view_all_game() throws IOException {
        Response rs = new Response();

        UserDto userDto = new UserDto();

        Request request = new Request();
        request.setTitle(Title.findAllGames);
        request.setData(userDto);

        String command = null;
        try {
            command = objectMapper.writeValueAsString(request);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request

            String response = dataInputStream.readUTF();
            rs = objectMapper.readValue(response, Response.class);//receive response
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rs.getStatus().equals("successful")) {

            GameEntity[] games = objectMapper.convertValue(rs.getData(), GameEntity[].class);
            for (int i = 0; i < games.length; i++) {
                System.out.println(i + 1 + ")" + games[i].getTitle() + " Reviews: " + games[i].getReviews());
            }
            System.out.println("select one game to download or view more details(just enter the number)");
            int number=in.nextInt();
            in.nextLine();
            GameEntity selectedGame = games[number-1];
            System.out.println("Enter 1 to view details , 2 to download");
            int choice = in.nextInt();
            in.nextLine();
            if(choice==1){
                view_one_game(selectedGame.getId());
            } else if (choice==2) {
                download(selectedGame);
            }else{
                view_all_game();
            }

        } else {
            System.out.println("please retry to login");
            start();
        }
    }

    public static void view_one_game(String gameId) throws IOException {
        Response rs = new Response();
        GameDto gameDto = new GameDto();
        gameDto.userId=currentUserId;
        gameDto.gameId=gameId;
        Request request = new Request();

        request.setTitle(Title.findOneGame);
        request.setData(gameDto);

        String command = null;
        try {
            command = objectMapper.writeValueAsString(request);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request

            String response = dataInputStream.readUTF();
            rs = objectMapper.readValue(response, Response.class);//receive response
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rs.getStatus().equals("successful")) {

            GameEntity game = objectMapper.convertValue(rs.getData(), GameEntity.class);//convert json to gameEntity
            System.out.println( game.getTitle() + " Reviews: " + game.getReviews() +"ReleaseYear: " +game.getReleaseYear() + "Genre: "+ game.getGenre() +"Developer: "+ game.getDeveloper() +"Size:" + game.getSize()+"Price: "+game.getPrice());
        } else {
            System.out.println("please retry ");
        }

    }

    public static void download(GameEntity game) throws IOException {
        Response rs = new Response();
        GameDto gameDto = new GameDto();
        Request request = new Request();
        gameDto.userId=currentUserId;
        gameDto.gameId=game.getId();

        request.setTitle(Title.download);
        request.setData(gameDto);
        String command = null;
        try {
            command = objectMapper.writeValueAsString(request);
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();//sending request

            String response = dataInputStream.readUTF();
            rs = objectMapper.readValue(response, Response.class);//receive response
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rs.getStatus().equals("successful")) {

            receiveFile(game.getTitle());

        } else {
            System.out.println("please retry ");
        }

    }

}








