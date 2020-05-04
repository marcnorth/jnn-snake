package uk.co.marcnorth.jnn_snake;

public class Position {
  
  public int x;
  public int y;
  
  public Position() {}
  
  public Position(int x, int y) {
    this.x = x;
    this.y = y;
  }
  
  public boolean equals(Object other) {
    if (other instanceof Position) {
      Position pos = (Position)other;
      return pos.x == this.x && pos.y == this.y;
    }
    return false;
  }
  
}