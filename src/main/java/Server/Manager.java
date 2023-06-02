package Server;

import Server.Game.GameService;
import Server.Entities.UserEntity;
import Server.User.UserService;
import Shared.Dto.GameDto;
import Shared.Dto.LoginDto;
import Shared.Dto.RegisterDto;
import Shared.Dto.UserDto;
import Shared.Request;
import Shared.Response;
import Shared.Title;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;

public class Manager implements Runnable {
    String currentUserId;
    Socket socket;
    InputStream inputStream;
    DataInputStream dataInputStream;
    OutputStream outputStream;
    DataOutputStream dataOutputStream;
    boolean Exit = false;
    String fileToSend;
    UserService userService = new UserService();
    GameService gameService = new GameService();
    ObjectMapper mapper = new ObjectMapper();

    public Manager(Socket socket) {
        this.socket = socket;
        try {
            this.inputStream = socket.getInputStream();
            this.dataInputStream = new DataInputStream(inputStream);

            this.outputStream = socket.getOutputStream();
            this.dataOutputStream = new DataOutputStream(outputStream);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = this.dataInputStream.readUTF();
                Request clientRequest = this.mapper.readValue(command, Request.class);
                Response response = manage(clientRequest);
                if (Exit){
                    break;
                }
                String res = this.mapper.writeValueAsString(response);
                dataOutputStream.writeUTF(res);
                dataOutputStream.flush();
                if (this.fileToSend != null){
                    sendFile();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outputStream.close();
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public Response manage(Request request) {
        GameDto gameDto;
        LoginDto loginDto;
        RegisterDto registerDto;
        UserDto userDto;
        switch (request.getTitle()) {
            case login:
                loginDto = this.mapper.convertValue(request.getData(), LoginDto.class);
                return login(loginDto);
            case logOut:
                userDto = this.mapper.convertValue(request.getData(), UserDto.class);
                return logout(userDto);
            case signup:
                registerDto = this.mapper.convertValue(request.getData(), RegisterDto.class);
                return register(registerDto);
            case findAllGames:
                userDto = this.mapper.convertValue(request.getData(), UserDto.class);
                return findAllGames(userDto);
            case findOneGame:
                gameDto = this.mapper.convertValue(request.getData(), GameDto.class);
                return findOneGame(gameDto);
            case download:
                gameDto = this.mapper.convertValue(request.getData(), GameDto.class);
                return download(gameDto);
            case exit:
                this.Exit = true;
                break;
        }
        return new Response();
    }

    private Response login(LoginDto loginDto) {
        Response response = this.userService.login(loginDto);

        if (response.getStatus().equals("successful")){
            this.currentUserId = ((UserEntity) response.getData()).getId();
        }
        return response;
    }

    private Response logout(UserDto userDto){
        Response response = this.userService.logOut();
        if (this.currentUserId.equals(userDto.id)) {
            this.currentUserId = "";
        } else {
            response.setMessage("UnAllowed");
        }
        return response;
    }

    private Response register(RegisterDto registerDto){
        Response response = this.userService.register(registerDto);

        if (response.getStatus().equals("successful")){
            this.currentUserId = ((UserEntity) response.getData()).getId();
        }
        return response;
    }

    private Response findAllGames(UserDto userDto){
        if (this.currentUserId.equals(userDto.id)){
            return this.gameService.findAll();
        } else {
            Response response = new Response();
            response.setTitle(Title.findAllGames);
            response.setMessage("UnAllowed");
            return response;
        }
    }

    private Response findOneGame(GameDto gameDto) {
        if (this.currentUserId.equals(gameDto.userId)){
            return this.gameService.findOne(gameDto);
        } else {
            Response response = new Response();
            response.setTitle(Title.findOneGame);
            response.setMessage("UnAllowed");
            return response;
        }
    }

    private Response download(GameDto gameDto){
        if (this.currentUserId.equals(gameDto.userId)){
            Response response = this.gameService.download(gameDto);
            if (response.getStatus().equals("successful")){
                this.fileToSend = (String) response.getData();
                response.setData(null);
            }
            return response;
        } else {
            Response response = new Response();
            response.setTitle(Title.download);
            response.setMessage("UnAllowed");
            return response;
        }
    }

    private void sendFile(){
        int bytes = 0;
        try {
            File file = new File(this.fileToSend);
            FileInputStream fileInputStream = new FileInputStream(file);

            this.dataOutputStream.writeLong(file.length());
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer)) != -1) {
                this.dataOutputStream.write(buffer, 0, bytes);
                this.dataOutputStream.flush();
            }
            fileInputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.fileToSend = null;
        }
    }
}
