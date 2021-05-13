package augusste.psi;

public class Message {

    public static final String TERM = "\u0007\u0008";

    public static final String  SERVER_MOVE = "102 MOVE";
    public static final String  SERVER_TURN_LEFT = "103 TURN LEFT";
    public static final String  SERVER_TURN_RIGHT = "104 TURN RIGHT";
    public static final String  SERVER_PICK_UP = "105 GET MESSAGE";
    public static final String  SERVER_LOGOUT = "106 LOGOUT";
    public static final String  SERVER_KEY_REQUEST = "107 KEY REQUEST";
    public static final String  SERVER_OK = "200 OK";
    public static final String  SERVER_LOGIN_FAILED = "300 LOGIN FAILED";
    public static final String  SERVER_SYNTAX_ERROR = "301 SYNTAX ERROR";
    public static final String  SERVER_LOGIC_ERROR = "302 LOGIC ERROR";
    public static final String  SERVER_KEY_OUT_OF_RANGE_ERROR = "303 KEY OUT OF RANGE";

    public static final int     CLIENT_USERNAME = 18;
    public static final int     CLIENT_KEY_ID = 3;
    public static final int     CLIENT_CONFIRMATION = 5;
    public static final int     CLIENT_OK = 10;
    public static final int     CLIENT_RECHARGING = 10;
    public static final int     CLIENT_FULL_POWER = 10;
    public static final int     CLIENT_MESSAGE = 98;

    public static final int     TIMEOUT = 1;
    public static final int     TIMEOUT_RECHARGING = 5;
}
