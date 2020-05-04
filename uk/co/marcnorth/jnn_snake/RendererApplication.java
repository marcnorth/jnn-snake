package uk.co.marcnorth.jnn_snake;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import uk.co.marcnorth.jnn.GeneticAlgorithmListener;

public class RendererApplication extends Application implements GeneticAlgorithmListener {
  
  private Renderer renderer;
  
  private Image snakeImage;
  private Image appleImage;
  private Image headImage;
  private boolean isRendering = true;
  private Stage stage;
  private Scene scene;
  private Region canvasContainer;
  private Text text;
  private Canvas[] canvases;
  private AnimationTimer frameTimer;
  private CountDownLatch renderLoopCountDown;
  
  public void init() {
    renderer = Renderer.getInstance();
    loadImages();
  }
  
  private void loadImages() {
    headImage = new Image(this.getClass().getResource("images/head.png").toString());
    snakeImage = new Image(this.getClass().getResource("images/snake.png").toString());
    appleImage = new Image(this.getClass().getResource("images/apple.png").toString());
  }
  
  @Override
  public void start(Stage stage) throws Exception {
    this.stage = stage;
    setupStage();
    setupEvents();
    startRenderLoop();
  }

  @Override
  public void stop() {
    System.exit(0);
  }
  
  private void setupStage() {
    stage.setTitle("Snake!");
    createScene();
    stage.setScene(scene);
  }
  
  private void createScene() {
    Pane root = new Pane();
    scene = new Scene(root, 750, 800);
    createTextNode();
    createCanvasNodes();
    root.getChildren().addAll(
      text,
      canvasContainer
    );
  }
  
  private void createTextNode() {
    text = new Text(10, 30, "");
    text.setFont(Font.font ("Verdana", 25));
    text.setFill(Color.BLACK);
  }
  
  private void createCanvasNodes() {
    int numberOfCanvases = renderer.getGameCount();
    canvasContainer = new GridPane();
    canvasContainer.setLayoutY(50);
    canvasContainer.setPrefWidth(750);
    canvasContainer.setPrefHeight(750);
    canvases = new Canvas[numberOfCanvases];
    int numberOfColumns = (int) Math.floor(Math.sqrt((double)numberOfCanvases));
    int numberOfRows = (int) Math.ceil((double)numberOfCanvases / (double)numberOfColumns);
    for (int i = 0; i < canvases.length; i++) {
      canvases[i] = new Canvas(canvasContainer.getPrefWidth() / numberOfColumns - 4, canvasContainer.getPrefHeight() / numberOfRows - 4);
      StackPane canvasParent = new StackPane(canvases[i]);
      canvasParent.setStyle("-fx-padding: 2; -fx-background-color: black, white; -fx-background-insets: 0, 2;");
      ((GridPane)canvasContainer).add(canvasParent, i % numberOfColumns, Math.floorDiv(i, numberOfColumns));
    }
  }
  
  private void setupEvents() {
    scene.setOnKeyPressed((e) -> {
      if (e.getCode() == KeyCode.DOWN) {
        Snake.nextThrottling();
      } else if (e.getCode() == KeyCode.UP) {
        Snake.previousThrottling();
      } else if (e.getCode() == KeyCode.SPACE) {
        for (Snake game : renderer.games())
          game.kill();
      } else if (e.getCode() == KeyCode.ENTER) {
        if (isRendering) {
          Snake.throttlingOff();
          isRendering = false;
          frameTimer.stop();
        } else {
          Snake.throttlingOn();
          isRendering = true;
          frameTimer.start();
        }
      }
    });
  }
  
  private void startRenderLoop() {
    stage.show();
    renderer.getGeneticAlgorithm().addListener(this);
    frameTimer = new AnimationTimer() {
      public void handle(long currentNanoTime) {
        renderLoopCountDown = new CountDownLatch(canvases.length);
        for (int i = 0; i < renderer.games().size(); i++) {
          renderGameToCanvas(renderer.games().get(i), canvases[i]);
          renderLoopCountDown.countDown();
        }
      }
    };
    setGenerationText(1, 0);
    frameTimer.start();
  }
  
  private void renderGameToCanvas(Snake game, Canvas canvas) {
    GraphicsContext context = canvas.getGraphicsContext2D();
    context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    context.setFill(Color.WHITE);
    context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    // Draw snake
    List<Position>snake = game.getSnake();
    for (int s = snake.size() - 1; s > 0; s--) {
      Position pos = renderer.worldToScreen(snake.get(s), canvas);
      context.drawImage(snakeImage, pos.x, pos.y, renderer.worldToScreen(snakeImage.getWidth(), canvas), renderer.worldToScreen(snakeImage.getHeight(), canvas));
    }
    Position headPos = renderer.worldToScreen(snake.get(0), canvas);
    context.drawImage(headImage, headPos.x, headPos.y, renderer.worldToScreen(headImage.getWidth(), canvas), renderer.worldToScreen(headImage.getHeight(), canvas));
    // Draw apple
    Position applePos = renderer.worldToScreen(game.getApplePos(), canvas);
    context.drawImage(appleImage, applePos.x, applePos.y, renderer.worldToScreen(appleImage.getWidth(), canvas), renderer.worldToScreen(appleImage.getHeight(), canvas));
    // Write score
    context.setFill(Color.BLACK);
    context.fillText(String.valueOf(game.getScore()), 10, 10);
    if (game.getIsOver()) {
      context.setFill(Color.BLACK);
      context.setGlobalAlpha(0.5);
      context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
      context.setGlobalAlpha(1);
    }
  }
  
  @Override
  public void onGenerationStart() {
    setGenerationText(renderer.getGeneticAlgorithm().getCurrentGenerationNumber(), renderer.getGeneticAlgorithm().getHighestScoreOfAnyGeneration());
    if (isRendering)
      frameTimer.start();
  }

  @Override
  public void onGenerationEnd() {
    try {
      frameTimer.stop();
      renderLoopCountDown.await();
      renderer.clearGames();
    } catch (InterruptedException e) {
      System.err.println("Unexpected interrupt");
      System.exit(0);
    }
  }
  
  private void setGenerationText(int generationNumber, double bestScore) {
    text.setText("Generation: " + generationNumber + " | Highest score: " + String.format("%.0f", bestScore > Double.MIN_VALUE ? bestScore : 0));
  }
  
}