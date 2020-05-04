package uk.co.marcnorth.jnn_snake;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.scene.canvas.Canvas;
import uk.co.marcnorth.jnn.GeneticAlgorithm;

public class Renderer {

  public static final int CANVAS_WIDTH = 512;
  public static final int CANVAS_HEIGHT = 512;
  public static final int PIXELS_PER_BLOCK = 16;
  
  private static Renderer instance;
  
  private final GeneticAlgorithm geneticAlgorithm;
  private final List<Snake> games = new ArrayList<>();
  
  private Renderer(GeneticAlgorithm geneticAlgorithm) {
    this.geneticAlgorithm = geneticAlgorithm;
  }
  
  public int getGameCount() {
    return this.geneticAlgorithm.getNumberOfNetworks();
  }
  
  public void startApplication() {
    Thread applicationThread = new Thread(() -> {
      Application.launch(RendererApplication.class);
    });
    applicationThread.setDaemon(true);
    applicationThread.start();
  }
  
  public GeneticAlgorithm getGeneticAlgorithm() {
    return this.geneticAlgorithm;
  }

  public synchronized void addGame(Snake game) {
    this.games.add(game);
  }
  
  public List<Snake> games() {
    return this.games;
  }
  
  public void clearGames() {
    this.games.clear();
  }
  
  public Position worldToScreen(Position pos, Canvas canvas) {
    return new Position(
      (int)((double)(pos.x + Snake.RADIUS) / (2 * Snake.RADIUS + 1) * canvas.getWidth()),
      (int)((double)(-pos.y + Snake.RADIUS) / (2 * Snake.RADIUS + 1) * canvas.getHeight())
    );
  }

  public int worldToScreen(int length, Canvas canvas) {
    return (int)((double)length / ((2 * Snake.RADIUS + 1) * PIXELS_PER_BLOCK) * canvas.getWidth());
  }

  public double worldToScreen(double length, Canvas canvas) {
    return (double)length / ((2 * Snake.RADIUS + 1) * PIXELS_PER_BLOCK) * canvas.getWidth();
  }
  
  public static synchronized Renderer create(GeneticAlgorithm geneticAlgorithm) {
    instance = new Renderer(geneticAlgorithm);
    return instance;
  }
  
  public static synchronized Renderer getInstance() {
    if (instance == null)
      throw new RuntimeException("Instance not created");
    return instance;
  }
  
}