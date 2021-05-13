package augusste.psi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import static java.lang.Character.isDigit;

public class ThreadFun implements Runnable{

    Socket clientSocket;
    BufferedReader input;
    DataOutputStream outputStream;
    State state;
    Robot robot;


    ThreadFun(Socket client) throws IOException {
        this.clientSocket = client;
        this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));
        this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
        this.state = State.BEGIN;
    }

    @Override
    public void run() {


        try {
            auth();
        } catch (IOException e) {
            closeConnection();
            return;
        }

        String response;



        try{

            while (state != State.TREASURE)
            {

                response = getNext(Message.CLIENT_RECHARGING);

                if (response.equals("end"))
                    break;

                if (state == State.BEGIN)
                    firstMoves();

                robotMove();

            }

            getTreasure();
        } catch (Exception e){
            closeConnection();
            return;
        }

        closeConnection();
    }

    private String getNext(int maxLen) throws IOException {
        String ret = "";
        int value = 0;

        try {
            clientSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        value = input.read();
        while ((char)value != '\u0007')
        {
            if (value == -1){
                if (ret.equals(""))
                    return "empty";
                return "end";
            }

            ret += (char)value;
            if (ret.length() > maxLen)
                return "end";
        }

        value = input.read();
        if ((char)value != '\u0008')
            return "end";

        if (ret.equals("RECHARGING"))
            if (!robotCharge())
                return "end";
            else return getNext(maxLen);

        return ret;
    }

    private boolean auth() throws IOException {

        try {
            clientSocket.setSoTimeout(1);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        String name = null;
        name = getNext(Message.CLIENT_USERNAME);

        if (name.equals("end") || name.equals("empty"))
            return false;


        int clientKeyID = Integer.parseInt(getNext(Message.CLIENT_KEY_ID));
        int serverKey = 0;
        int clientKey = 0;

        switch (clientKeyID){
            case 0: serverKey = 23019; clientKey = 32037;
            case 1: serverKey = 32037; clientKey = 29295;
            case 2: serverKey = 18789; clientKey = 13603;
            case 3: serverKey = 16443; clientKey = 29533;
            case 4: serverKey = 18189; clientKey = 21952;
            default:
                try {
                    outputStream.writeChars(Message.SERVER_KEY_OUT_OF_RANGE_ERROR + Message.TERM);
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        int nameVal = 0;
        for(int i = 0; i < name.length(); i++)
            nameVal += Integer.parseInt(String.valueOf(name.charAt(i)));

        nameVal *= 1000;
        int hash = nameVal % 65536;

        hash += serverKey;
        hash %= 65536;

        try {
            outputStream.writeChars(String.valueOf(hash) + Message.TERM);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String clientConfirm = getNext(Message.CLIENT_CONFIRMATION);
        int clientHash = Integer.parseInt(clientConfirm) + 65536 - clientKey;
        clientHash %= 65536;

        if(clientHash == hash)
        {
            try {
                outputStream.writeChars(Message.SERVER_OK + Message.TERM);
            } catch (IOException e) {
                e.printStackTrace();
            }
            state = State.BEGIN;
            return true;
        }

        try {
            outputStream.writeChars(Message.SERVER_LOGIN_FAILED + Message.TERM);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean firstMoves() throws IOException {

        outputStream.writeBytes(Message.SERVER_MOVE + Message.TERM);

        String first = getNext(Message.CLIENT_OK);

        if (!checkMovementResponse(first))
            return false;

        outputStream.writeBytes(Message.SERVER_MOVE + Message.TERM);

        String second = getNext(Message.CLIENT_OK);

        if (!checkMovementResponse(second))
            return false;

        int firstX, firstY, secondX, secondY;

        first = first.substring(3);
        String[] arrF = first.split(" ");

        firstX = Integer.parseInt(arrF[0]);
        firstY = Integer.parseInt(arrF[1]);

        second = second.substring(3);
        String[] arrS = second.split(" ");

        secondX = Integer.parseInt(arrS[0]);
        secondY = Integer.parseInt(arrS[1]);

        if (firstX == secondX && firstY < secondY) robot.direction = Direction.DOWN;
        else if (firstX == secondX && firstY > secondY) robot.direction = Direction.UP;
        else if (firstX < secondX && firstY == secondY) robot.direction = Direction.LEFT;
        else if (firstX > secondX && firstY == secondY) robot.direction = Direction.RIGHT;
        else {
            outputStream.writeBytes(Message.SERVER_TURN_RIGHT + Message.TERM + Message.SERVER_MOVE + Message.TERM);
            String response = getNext(Message.CLIENT_OK);
            if (!checkMovementResponse(response))
                return false;

            response = response.substring(3);
            String[] arr = response.split(" ");
            int x = Integer.parseInt(arr[0]);
            int y = Integer.parseInt(arr[1]);

            if (secondX == x && secondY < y) robot.direction = Direction.DOWN;
            else if (secondX == x && secondY > y) robot.direction = Direction.UP;
            else if (secondX < x && secondY == y) robot.direction = Direction.LEFT;
            else if (secondX > x && secondY == y) robot.direction = Direction.RIGHT;
        }
        state = State.MOVE;
        return true;

    }

    public void robotMove() throws IOException {

        if (robot.coordX < 0){
            if(robot.direction == Direction.RIGHT)
                outputStream.writeBytes(Message.SERVER_MOVE + Message.TERM);
            else if (robot.direction == Direction.DOWN)
                outputStream.writeBytes(Message.SERVER_TURN_LEFT + Message.TERM);
            else outputStream.writeBytes(Message.SERVER_TURN_RIGHT + Message.TERM);
        }
        else if (robot.coordX > 0){
            if (robot.direction == Direction.LEFT)
                outputStream.writeBytes(Message.SERVER_MOVE + Message.TERM);
            else if (robot.direction == Direction.UP)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
            else outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
        }
        else if (robot.coordY < 0){
            if (robot.direction == Direction.UP)
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
            else if(robot.direction == Direction.RIGHT)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
            else outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
        }
        else if (robot.coordY > 0){
            if (robot.direction == Direction.DOWN)
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
            else if(robot.direction == Direction.LEFT)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
            else outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
        }

        String response = getNext(Message.CLIENT_OK);

        if (!checkMovementResponse(response))
            closeConnection();

        response = response.substring(3);

        String[] arr = response.split(" ");

        int x = Integer.parseInt(arr[0]);
        int y = Integer.parseInt(arr[1]);

        if (robot.coordY == y && robot.coordX == x)
            obstacle();
        else {
            robot.coordY = y;
            robot.coordX = x;
        }

        if (robot.coordX == 0 && robot.coordY == 0)
            state = State.TREASURE;

    }

    private boolean checkMovementResponse(String response){
        if (response.length() > Message.CLIENT_OK)
            return false;

        if(!(response.charAt(0) == 'O' && response.charAt(1)=='K' && response.charAt(2)==' '))
            return false;

        response = response.substring(3);

        String[] arr = response.split(" ");

        if (arr.length > 2)
            return false;

        for (String num : arr) {
            for (int i = 0; i < num.length(); i++)
                if (!((i == 0 && num.charAt(i) == '-') ||
                        (isDigit(num.charAt(i)))))
                    return false;
        }

        return true;
    }

    private void turnRobot() throws IOException {

        if(robot.direction == Direction.RIGHT){
            if(robot.coordY > 0)
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
            else if (robot.coordY <= 0)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
        }
        else if (robot.direction == Direction.DOWN){
            if (robot.coordX < 0)
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
            else if (robot.coordX >= 0)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
        }
        else if (robot.direction == Direction.LEFT){
            if (robot.coordY < 0)
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
            else if (robot.coordY >= 0)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
        }
        else if (robot.direction == Direction.UP){
            if (robot.coordX < 0)
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
            else if (robot.coordX >= 0)
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
        }
    }

    private void getTreasure(){
        try {
            outputStream.writeChars(Message.SERVER_PICK_UP + Message.TERM);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            clientSocket.setSoTimeout(1);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        String mess = "";
        int value = 0;

        while(!mess.endsWith(Message.TERM))
        {
            try {
                value = input.read();
                if(value == -1) closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // este nieco ked bude dlhsie
            mess += (char)value;
            if (mess.length() > 100)
                closeConnection();
        }
        mess = mess.substring(0, mess.length()-2);

        state = State.TREASURE;
        try {
            outputStream.writeBytes(Message.SERVER_LOGOUT + Message.TERM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean robotCharge(){

        try {
            clientSocket.setSoTimeout(5*1000);

            String response = getNext(Message.CLIENT_RECHARGING);
            if (!response.equals("FULL POWER")){
                outputStream.writeChars(Message.SERVER_SYNTAX_ERROR + Message.TERM);
                return false;
            }
        } catch (SocketException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private void closeConnection(){
        try {
            clientSocket.close();
            outputStream.close();
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void obstacle() throws IOException {

        String response;

        if(robot.direction == Direction.DOWN)
        {
            if (robot.coordX < 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX > 0)
            {
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX == 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
        }
        else if (robot.direction == Direction.UP){
            if (robot.coordX > 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX < 0)
            {
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX == 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnRight();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
        }
        else if (robot.direction == Direction.RIGHT){
            if (robot.coordY < 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordY > 0)
            {
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX == 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
        }
        else if (robot.direction == Direction.LEFT){
            if (robot.coordY > 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordY < 0)
            {
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
            else if (robot.coordX == 0){
                outputStream.writeChars(Message.SERVER_TURN_LEFT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                robot.turnLeft();
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
                outputStream.writeChars(Message.SERVER_TURN_RIGHT + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                outputStream.writeChars(Message.SERVER_MOVE + Message.TERM);
                response = getNext(Message.CLIENT_OK);
                setCoordinates(response);
            }
        }

    }

    private void setCoordinates(String coords){
        coords = coords.substring(3);

        String[] arr = coords.split(" ");

        int x = Integer.parseInt(arr[0]);
        int y = Integer.parseInt(arr[1]);

        robot.coordX = x;
        robot.coordY = y;
    }
}
