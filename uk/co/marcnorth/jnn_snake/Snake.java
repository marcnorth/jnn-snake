package uk.co.marcnorth.jnn_snake;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import uk.co.marcnorth.jnn.NeuralNetwork;
import uk.co.marcnorth.jnn.NeuralNetworkTask;

public class Snake implements NeuralNetworkTask {
  
  public static final int RADIUS = 15;
  
  private static boolean throttling = true;
  private static int throttlingIndex = 3;
  private static int[] throttleValues = new int[] {
    5,
    10,
    50,
    100,
    500,
    1000,
    5000,
  };
  private boolean isOver = false;
  private enum Direction {
    UP,
    RIGHT,
    DOWN,
    LEFT;
    public Direction next() {
      return Direction.values()[(this.ordinal()+1) % Direction.values().length];
    }
    public Direction previous() {
      return Direction.values()[(this.ordinal()-1 + Direction.values().length) % Direction.values().length];
    }
  };
  private Direction direction;
  private Position applePos = new Position();
  private List<Position> snake;
  private int score;
  private int moves;
  private int movesNoScore;
  private final int MAX_MOVES = 100000;
  private final int MAX_MOVES_NO_SCORE = 25000;
  private Random rand = new Random();
  private NeuralNetwork network;
  
  public double getNetworkScore(NeuralNetwork network) {
    this.network = network;
    Renderer.getInstance().addGame(this);
    setup();
    runGameLoop();
    return score;
  }

  private void setup() {
    score = 0;
    snake = new ArrayList<Position>();
    snake.add(new Position(0, 0));
    direction = Direction.RIGHT;
    moves = 0;
    movesNoScore = 0;
    createApple();
  }

  private void runGameLoop() {
    while (!this.isOver) {
      moves++;
      movesNoScore++;
      changeDirection();
      moveForwards();
      checkIsOver();
      throttle();
    }
    
  }
  
  private void changeDirection() {
    double[] output = network.feedForward(generateNetworkInput());
    if (output[0] > output[1] && output[0] > output[2]) {
      return;
    } else if (output[1] > output[2]) {
      direction = direction.next();
    } else {
      direction = direction.previous();
    }
  }
  
  private void moveForwards() {
    Position head = getHead();
    snake.add(0, new Position(
      head.x,
      head.y
    ));
    head = getHead();
    if (direction == Direction.RIGHT)
      head.x++;
    else if (direction == Direction.LEFT)
      head.x--;
    else if (direction == Direction.UP)
      head.y++;
    else if (direction == Direction.DOWN)
      head.y--;
    if (head.equals(applePos)) {
      score++;
      movesNoScore = 0;
      createApple();
    } else {
      snake.remove(snake.size() - 1);
    }
  }
  
  private void throttle() {
    try {
      if (Snake.throttling)
        Thread.sleep(Snake.throttleValues[Snake.throttlingIndex]);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  private double[] generateNetworkInput() {
    double[] input = new double[14];
    
    Position front = this.relativePositionToAbsolute(new Position(0, 1));
    Position left = this.relativePositionToAbsolute(new Position(-1, 0));
    Position right = this.relativePositionToAbsolute(new Position(1, 0));
    input[0] = this.isDeadPosition(front) ? 1 : 0;
    input[1] = this.isDeadPosition(left) ? 1 : 0;
    input[2] = this.isDeadPosition(right) ? 1 : 0;
    
    Position relativeApplePos = this.relativePosition(this.applePos);
    input[3] = relativeApplePos.x == 0 ? 0 : relativeApplePos.x / Math.abs(relativeApplePos.x);
    input[4] = relativeApplePos.y == 0 ? 0 : relativeApplePos.y / Math.abs(relativeApplePos.y);
    
    // What is directly in each direction
    List<Position> body = getBody();
    for (int i = 0; i < 3; i ++) {
      Position check = null;
      int distance = 0;
      while (true) {
        distance++;
        switch (i) {
          case 0:
            check = relativePositionToAbsolute(new Position(-distance, 0));
            break;
          case 1:
            check = relativePositionToAbsolute(new Position(distance, 0));
            break;
          case 2:
            check = relativePositionToAbsolute(new Position(0, distance));
            break;
        }
        if (check.equals(this.applePos)) {
          input[5 + i] = 1;
          break;
        } else if (!isOnBoard(check)) {
          input[8 + i] = 1;
          break;
        } else if (body.contains(check)) {
          input[11 + i] = 1;
          break;
        }
      }
    }
    return input;
  }
  
  public Position getHead() {
    return snake.size() > 0 ? snake.get(0) : null;
  }

  public List<Position> getBody() {
    return snake.size() > 1 ? snake.subList(1, snake.size() - 1) : new ArrayList<Position>();
  }
  
  public void kill() {
    isOver = true;
  }
  
  private void createApple() {
    do {
      applePos.x = rand.nextInt(2 * Snake.RADIUS + 1) - Snake.RADIUS;
      applePos.y = rand.nextInt(2 * Snake.RADIUS + 1) - Snake.RADIUS;
    } while (snake.contains(applePos));
  }
  
  private void checkIsOver() {
    if (isOver)
      return;
    if (isDeadPosition(getHead())) {
      score -= 3;
      isOver = true;
    }
    if (this.moves >= this.MAX_MOVES || this.movesNoScore >= this.MAX_MOVES_NO_SCORE)
      this.isOver = true;
  }
  
  private boolean isDeadPosition(Position pos) {
    
    return !isOnBoard(pos) || getBody().contains(pos);
    
  }

  private boolean isOnBoard(Position pos) {
    
    return !(pos.x < - Snake.RADIUS || pos.x > Snake.RADIUS || pos.y < -Snake.RADIUS || pos.y > Snake.RADIUS);
    
  }
  
  private Position relativePosition(Position pos) {

    int x = 0;
    int y = 0;
    
    Position head = snake.get(0);

    switch (direction) {
    
      case UP:
        x = pos.x - head.x;
        y = pos.y - head.y;
        break;
        
      case DOWN:
        x = head.x - pos.x;
        y = head.y - pos.y;
        break;

      case RIGHT:
        x = head.y - pos.y;
        y = pos.x - head.x;
        break;

      case LEFT:
        x = pos.y - head.y;
        y = head.x - pos.x;
        break;
      
    }
    
    return new Position(x, y);
    
  }
  
  private Position relativePositionToAbsolute(Position pos) {

    int x = 0;
    int y = 0;
    
    Position head = snake.get(0);

    switch (direction) {
    
      case UP:
        x = head.x + pos.x;
        y = head.y + pos.y;
        break;
        
      case DOWN:
        x = head.x - pos.x;
        y = head.y - pos.y;
        break;

      case RIGHT:
        x = head.x + pos.y;
        y = head.y - pos.x;
        break;

      case LEFT:
        x = head.x - pos.y;
        y = head.y + pos.x;
        break;
      
    }
    
    return new Position(x, y);
    
  }
  
  public boolean getIsOver() {
    
    return isOver;
    
  }
  
  public Position getApplePos() {
    
    return applePos;
    
  }
  
  public List<Position> getSnake() {
    
    return snake;
    
  }
  
  public int getScore() {
    
    return score;
    
  }
  
  public int getRadius() {
    
    return Snake.RADIUS;
    
  }

  public static void nextThrottling() {
    
    Snake.throttlingIndex = Math.min(throttlingIndex + 1, Snake.throttleValues.length - 1);
    
  }

  public static void previousThrottling() {

    Snake.throttlingIndex = Math.max(throttlingIndex - 1, 0);
    
  }

  public static void throttlingOff() {
    
    Snake.throttling = false;
    
  }

  public static void throttlingOn() {
    
    Snake.throttling = true;
    
  }
  
}