package uk.co.marcnorth.jnn_snake;

import uk.co.marcnorth.jnn.GeneticAlgorithm;

public class App {
  
  public App() {
    int gameCount = 36;
    
    GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm(
      Snake.class,
      new int[] {14, 28, 3},
      gameCount,
      gameCount/4,
      gameCount/4,
      gameCount/4,
      0.3
    );

    Renderer renderer = Renderer.create(geneticAlgorithm);
    renderer.startApplication();
    
    geneticAlgorithm.runGenerations(100000);
  }
  
  public static void main(String[] args) {
    new App();
  }
  
}
