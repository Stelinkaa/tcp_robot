package augusste.psi;

public class Robot {

    public int coordX;
    public int coordY;
    public Direction direction;

    Robot(int x, int y)
    {
        this.coordX = x;
        this.coordY = y;
    }

    public void move(){
        if(direction == Direction.LEFT) coordX--;
        else if (direction == Direction.DOWN) coordY--;
        else if (direction == Direction.RIGHT) coordY++;
        else coordX++;
    }

    public boolean onTreasure(){
        return coordX==0 && coordY==0;
    }

    public void turnLeft(){
        if(direction == Direction.LEFT) direction = Direction.DOWN;
        else if (direction == Direction.DOWN) direction = Direction.RIGHT;
        else if (direction == Direction.RIGHT) direction = Direction.UP;
        else direction = Direction.LEFT;
    }

    public void turnRight(){
        if(direction == Direction.LEFT) direction = Direction.UP;
        else if (direction == Direction.DOWN) direction = Direction.LEFT;
        else if (direction == Direction.RIGHT) direction = Direction.DOWN;
        else direction = Direction.RIGHT;
    }
}
