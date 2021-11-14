import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

import tester.*;
import javalib.impworld.*;

import java.awt.Color;
import javalib.worldimages.*;

/* Notes:
 * - Our canvas size will be 1000 x 600 pixels
 * - We will use a Vertex to represent a square, a Edge to connect nodes,
 *   and Maze to run the board
 * - We won't allow any dimensions smaller than 2x2
 * - The board will be generated but you need to click a key to choose
 *   Manual (m), BFS (b), DFS (d), toggle solution (a), arrow keys (only if
 *   manual is clicked), bias in x direction (x), bias in y direction (y)
 *   otherwise nothing will happen
 */

/* Extra Credit:
 * 1) The walls get knocked down one by one, check out lines (228 - 273) 
 *    to see whether you want to turn it off or not
 * 2) Biases it in the x direction if you click "x", or y direction
 *    if you click "y"
 * 3) You can click "a" to toggle the answer instead of it just showing when you complete it
 */

/* How to Play:
 * - In big bang, choose the dimension of your maze, preferably a multiple of 1000, 600
 * - The board will show up and you have to click a key to do a specified action (look at notes)
 * - That's all and have fun!
 */

// represents each Square of the board
class Vertex {
  Posn posn;
  Color color;
  ArrayList<Edge> outEdges; // edges from this Vertex

  // constructor for Vertex
  Vertex(Posn posn, Color color) {
    this.posn = posn;
    this.color = color;
    this.outEdges = new ArrayList<Edge>();
  }

  // EFFECT: changes the Vertex's color
  void changeColor(Color color) {
    this.color = color;
  }

  // draws the cell
  WorldImage drawVertex(Posn cellSize) {
    return new RectangleImage(cellSize.x, cellSize.y, "solid", this.color);
  }
}

// represents an Edge connecting weights
class Edge implements Comparable<Edge> {
  Vertex from;
  Vertex to;
  int weight;

  // constructor for Edge
  Edge(Vertex from, Vertex to) {
    this(from, to, new Random().nextInt(100)); // 0-99
  }

  // constructor for Edge, used for examples with a given weight
  Edge(Vertex from, Vertex to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // compares the edges weights and returns negative if current < given,
  // 0 if current == given, and positive if current > given
  public int compareTo(Edge e) {
    return this.weight - e.weight;
  }
}

// represents the Maze game
class Maze extends World {
  Color lightBlue = new Color(135, 210, 250); // constant lightBlue color
  Posn dimension; // (x, y) so x squares to the right and y down
  Posn cellSize; // size of each cell
  ArrayList<ArrayList<Vertex>> allVertices;
  ArrayList<Edge> allEdges; // holds every single edge
  ArrayList<Edge> usedEdges; // edges from kruskals
  ArrayList<Vertex> alreadySeen; // holds a list of the already seen Vertices
  ArrayList<Vertex> answer; // holds a list of the already seen Vertices
  ArrayList<Vertex> drawnBoxes; // holds a list of the already drawn Vertices
  boolean manual; // checks whether we can do the manual controls
  boolean showAnswer; // checks whether we should show the answer
  Vertex pointer; // holds the pointer for manual
  Vertex current; // holds the thing the current vertex is at for the search loop
  ArrayList<Edge> currentUsedEdge; // holds the current edges for the wall knockdown
  Integer currentUsedEdgeIndex; // holds the index of the current Edge for the usedEdges
  // for the EC wall knockdown

  // constructor for maze, the one that should be used for a real game
  Maze(Posn dimension) {
    if (dimension.x <= 1 || dimension.y <= 1) {
      throw new IllegalArgumentException("Maze has to be bigger than 1 dimension!");
    }
    this.dimension = dimension;
    this.cellSize = new Posn(1000 / this.dimension.x, 600 / this.dimension.y);
    this.allVertices = new ArrayList<ArrayList<Vertex>>();
    this.allEdges = new ArrayList<Edge>();
    this.usedEdges = new ArrayList<Edge>();
    this.alreadySeen = new ArrayList<Vertex>();
    this.answer = new ArrayList<Vertex>();
    this.drawnBoxes = new ArrayList<Vertex>();
    this.manual = false;
    this.showAnswer = false;
    if (this.allVertices.size() == 0) {
      this.createBoard();
    }
    this.pointer = this.findVertex(new Posn(0, 0));
    this.current = this.findVertex(new Posn(0, 0));
    this.currentUsedEdge = new ArrayList<Edge>();
    this.currentUsedEdgeIndex = 0;
    this.setVertexEdges();
    this.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))
        .changeColor(Color.magenta);
    this.kruskals();
    // sets the answer to the dfs search in case they do manual
    this.searchHelp(new Stack<Vertex>());
  }

  // constructor for maze
  Maze(ArrayList<ArrayList<Vertex>> allVertices) {
    if (allVertices.size() <= 1 || allVertices.get(0).size() <= 1) {
      throw new IllegalArgumentException("Maze has to be bigger than 1 dimension!");
    }
    this.dimension = new Posn(allVertices.size(), allVertices.get(0).size());
    this.cellSize = new Posn(1000 / this.dimension.x, 600 / this.dimension.y);
    this.allVertices = allVertices;
    this.allEdges = new ArrayList<Edge>();
    this.usedEdges = new ArrayList<Edge>();
    this.alreadySeen = new ArrayList<Vertex>();
    this.answer = new ArrayList<Vertex>();
    this.drawnBoxes = new ArrayList<Vertex>();
    this.manual = false;
    this.showAnswer = false;
    if (this.allVertices.size() == 0) {
      this.createBoard();
    }
    this.pointer = this.findVertex(new Posn(0, 0));
    this.current = this.findVertex(new Posn(0, 0));
    this.currentUsedEdge = new ArrayList<Edge>();
    this.currentUsedEdgeIndex = 0;
    this.setVertexEdges();
    this.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))
        .changeColor(Color.magenta);
    this.kruskals();
    // sets the answer to the dfs search in case they do manual
    this.searchHelp(new Stack<Vertex>());
  }

  // constructor for maze, used for examples
  Maze(Posn dimension, Random random) {
    if (dimension.x <= 1 || dimension.y <= 1) {
      throw new IllegalArgumentException("Maze has to be bigger than 1 dimension!");
    }
    this.dimension = dimension;
    this.cellSize = new Posn(1000 / this.dimension.x, 600 / this.dimension.y);
    this.allVertices = new ArrayList<ArrayList<Vertex>>();
    this.allEdges = new ArrayList<Edge>();
    this.usedEdges = new ArrayList<Edge>();
    this.alreadySeen = new ArrayList<Vertex>();
    this.answer = new ArrayList<Vertex>();
    this.drawnBoxes = new ArrayList<Vertex>();
    this.manual = false;
    this.showAnswer = false;
    this.currentUsedEdge = new ArrayList<Edge>();
    this.currentUsedEdgeIndex = 0;
    // this examples doesn't have a pointer initialized because no board has been
    // created
  }

  // constructor for maze, used for examples
  Maze(ArrayList<ArrayList<Vertex>> allVertices, Random random) {
    if (allVertices.size() <= 1 || allVertices.get(0).size() <= 1) {
      throw new IllegalArgumentException("Maze has to be bigger than 1 dimension!");
    }
    this.dimension = new Posn(allVertices.size(), allVertices.get(0).size());
    this.cellSize = new Posn(1000 / this.dimension.x, 600 / this.dimension.y);
    this.allVertices = allVertices;
    this.allEdges = new ArrayList<Edge>();
    this.usedEdges = new ArrayList<Edge>();
    this.alreadySeen = new ArrayList<Vertex>();
    this.answer = new ArrayList<Vertex>();
    this.drawnBoxes = new ArrayList<Vertex>();
    this.manual = false;
    this.showAnswer = false;
    this.pointer = this.findVertex(new Posn(0, 0));
    this.current = this.findVertex(new Posn(0, 0));
    this.currentUsedEdge = new ArrayList<Edge>();
    this.currentUsedEdgeIndex = 0;
  }

  // draws the scene of the maze
  public WorldScene makeScene() {
    WorldScene background = new WorldScene(1000, 600);

    // places the gray background
    background.placeImageXY(new RectangleImage(1000, 600, "solid", Color.gray), 500, 300);
    // places the green box on top left
    background.placeImageXY(
        new RectangleImage(this.cellSize.x, this.cellSize.y, "solid", Color.green),
        this.cellSize.x / 2, this.cellSize.y / 2);
    // places the magenta box on the bottom right
    background.placeImageXY(
        new RectangleImage(this.cellSize.x, this.cellSize.y, "solid", Color.magenta),
        (this.dimension.x - 1) * this.cellSize.x + this.cellSize.x / 2,
        (this.dimension.y - 1) * this.cellSize.y + this.cellSize.y / 2);
    // creates the horizontal lines to the grid
    for (int y = 0; y <= 600; y += this.cellSize.y) {
      WorldImage line = new RectangleImage(1000, 2, "solid", Color.black);
      background.placeImageXY(line, 500, y);
    }
    // creates the vertical lines to the grid
    for (int x = 0; x < 1000; x += this.cellSize.x) {
      WorldImage line = new RectangleImage(2, 600, "solid", Color.black);
      background.placeImageXY(line, x, 300);
    }

    // draws each wall being knocked down (COMMENT THIS OUT FOR THE FOR LOOP
    // UNDERNEATH TO START
    // WITH WALLS KNOCKED DOWN, HIGHLIGHT ALL THE NEEDED CODE AND CLICK CTRL+/ OR
    // CMD+/)
    for (Edge e : this.currentUsedEdge) {
      WorldImage horizontal = new RectangleImage(this.cellSize.x - 2, 2, "solid", Color.gray);
      WorldImage vertical = new RectangleImage(2, this.cellSize.y - 2, "solid", Color.gray);
      if (e.from.posn.x < e.to.posn.x) {
        background.placeImageXY(vertical, e.to.posn.x * this.cellSize.x,
            e.from.posn.y * this.cellSize.y + this.cellSize.y / 2);
      }
      if (e.from.posn.x > e.to.posn.x) {
        background.placeImageXY(vertical, e.from.posn.x * this.cellSize.x,
            e.from.posn.y * this.cellSize.y + this.cellSize.y / 2);
      }
      if (e.from.posn.y < e.to.posn.y) {
        background.placeImageXY(horizontal, e.from.posn.x * this.cellSize.x + this.cellSize.x / 2,
            e.to.posn.y * this.cellSize.y);
      }
      if (e.from.posn.y > e.to.posn.y) {
        background.placeImageXY(horizontal, e.from.posn.x * this.cellSize.x + this.cellSize.x / 2,
            e.from.posn.y * this.cellSize.y);
      }
    }

    // (UNCOMMENT THIS TO START WITH WALLS ALREADY KNOCKED DOWN)
    // draws over the lines with gray if there's an edge connecting two nodes
    //    for (Edge e : this.usedEdges) {
    //      WorldImage horizontal = new RectangleImage(this.cellSize.x - 2, 2, "solid", Color.gray);
    //      WorldImage vertical = new RectangleImage(2, this.cellSize.y - 2, "solid", Color.gray);
    //      if (e.from.posn.x < e.to.posn.x) {
    //        background.placeImageXY(vertical, e.to.posn.x * this.cellSize.x,
    //            e.from.posn.y * this.cellSize.y + this.cellSize.y / 2);
    //      }
    //      if (e.from.posn.x > e.to.posn.x) {
    //        background.placeImageXY(vertical, e.from.posn.x * this.cellSize.x,
    //            e.from.posn.y * this.cellSize.y + this.cellSize.y / 2);
    //      }
    //      if (e.from.posn.y < e.to.posn.y) {
    //        background.placeImageXY(horizontal, 
    // e.from.posn.x * this.cellSize.x + this.cellSize.x / 2,
    //            e.to.posn.y * this.cellSize.y);
    //      }
    //      if (e.from.posn.y > e.to.posn.y) {
    //        background.placeImageXY(horizontal, 
    // e.from.posn.x * this.cellSize.x + this.cellSize.x / 2,
    //            e.from.posn.y * this.cellSize.y);
    //      }
    //    }

    // loops through drawnBoxes and add it onto background
    for (Vertex v : this.drawnBoxes) {
      background.placeImageXY(
          new RectangleImage(this.cellSize.x - 2, this.cellSize.y - 2, "solid", this.lightBlue),
          v.posn.x * this.cellSize.x + this.cellSize.x / 2,
          v.posn.y * this.cellSize.y + this.cellSize.y / 2);
    }
    // draws the Vertex the search loop is currently on
    background.placeImageXY(
        new RectangleImage(this.cellSize.x - 2, this.cellSize.y - 2, "solid", Color.green),
        this.current.posn.x * this.cellSize.x + this.cellSize.x / 2,
        this.current.posn.y * this.cellSize.y + this.cellSize.y / 2);
    // toggles the answer
    if (this.showAnswer) {
      for (Vertex v : this.answer) {
        background.placeImageXY(
            new RectangleImage(this.cellSize.x - 2, this.cellSize.y - 2, "solid", Color.BLUE),
            v.posn.x * this.cellSize.x + this.cellSize.x / 2,
            v.posn.y * this.cellSize.y + this.cellSize.y / 2);
      }
    }
    // if manual is turned on it draws the manual position
    if (this.manual) {
      background.placeImageXY(
          new RectangleImage(this.cellSize.x - 2, this.cellSize.y - 2, "solid",
              new Color(255, 92, 0)),
          this.pointer.posn.x * this.cellSize.x + this.cellSize.x / 2,
          this.pointer.posn.y * this.cellSize.y + this.cellSize.y / 2);
    }
    // shows "YOU WIN!" when you completed it on manual
    if (this.pointer == this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))) {
      background.placeImageXY(new TextImage("YOU WIN!", 50, FontStyle.BOLD, Color.green), 500, 300);
    }
    return background;
  }

  // does something to the game based on the key pressed
  public void onKeyEvent(String key) {
    if (key.equals("r")) { // resets the board
      this.allVertices = new ArrayList<ArrayList<Vertex>>();
      this.allEdges = new ArrayList<Edge>();
      this.usedEdges = new ArrayList<Edge>();
      this.alreadySeen = new ArrayList<Vertex>();
      this.answer = new ArrayList<Vertex>();
      this.drawnBoxes = new ArrayList<Vertex>();
      this.manual = false;
      this.showAnswer = false;
      this.createBoard();
      this.pointer = this.findVertex(new Posn(0, 0));
      this.current = this.findVertex(new Posn(0, 0));
      this.currentUsedEdge = new ArrayList<Edge>();
      this.currentUsedEdgeIndex = 0;
      this.setVertexEdges();
      this.findVertex(new Posn(0, 0)).changeColor(Color.green);
      this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))
          .changeColor(Color.magenta);
      this.kruskals();
      // sets the answer to the dfs search in case they do manual
      this.searchHelp(new Stack<Vertex>());
    }
    else if (key.equals("b")) { // breadth first search
      this.alreadySeen = this.bfs();
    }
    else if (key.equals("d")) { // depth first search
      this.alreadySeen = this.dfs();
    }
    else if (key.equals("m")) { // turns on manual so you can use arrow keys
      this.manual = true;
    }
    else if (key.equals("a")) { // toggles the answer
      this.showAnswer = !this.showAnswer;
    }
    else if (this.manual && key.equals("up") && this.pointer.posn.y > 0) {
      Edge connector = this.findEdge(this.pointer.outEdges, // up arrow
          this.findVertex(new Posn(this.pointer.posn.x, this.pointer.posn.y - 1)), this.pointer);
      if (this.usedEdges.contains(connector)) {
        this.pointer = this.findVertex(new Posn(this.pointer.posn.x, this.pointer.posn.y - 1));
      }
    }
    else if (this.manual && key.equals("down") && this.pointer.posn.y < this.dimension.y - 1) {
      Edge connector = this.findEdge(this.pointer.outEdges, this.pointer, // down arrow
          this.findVertex(new Posn(this.pointer.posn.x, this.pointer.posn.y + 1)));
      if (this.usedEdges.contains(connector)) {
        this.pointer = this.findVertex(new Posn(this.pointer.posn.x, this.pointer.posn.y + 1));
      }
    }
    else if (this.manual && key.equals("left") && this.pointer.posn.x > 0) {
      Edge connector = this.findEdge(this.pointer.outEdges, // left arrow
          this.findVertex(new Posn(this.pointer.posn.x - 1, this.pointer.posn.y)), this.pointer);
      if (this.usedEdges.contains(connector)) {
        this.pointer = this.findVertex(new Posn(this.pointer.posn.x - 1, this.pointer.posn.y));
      }
    }
    else if (this.manual && key.equals("right") && this.pointer.posn.x < this.dimension.x - 1) {
      Edge connector = this.findEdge(this.pointer.outEdges, this.pointer, // right arrow
          this.findVertex(new Posn(this.pointer.posn.x + 1, this.pointer.posn.y)));
      if (this.usedEdges.contains(connector)) {
        this.pointer = this.findVertex(new Posn(this.pointer.posn.x + 1, this.pointer.posn.y));
      }
    }
    else if (key.equals("x")) { // bias in x
      this.allVertices = new ArrayList<ArrayList<Vertex>>();
      this.allEdges = new ArrayList<Edge>();
      this.usedEdges = new ArrayList<Edge>();
      this.alreadySeen = new ArrayList<Vertex>();
      this.answer = new ArrayList<Vertex>();
      this.drawnBoxes = new ArrayList<Vertex>();
      this.manual = false;
      this.showAnswer = false;
      this.createBoard();
      this.pointer = this.findVertex(new Posn(0, 0));
      this.current = this.findVertex(new Posn(0, 0));
      this.currentUsedEdge = new ArrayList<Edge>();
      this.currentUsedEdgeIndex = 0;
      this.setVertexEdgesX();
      this.findVertex(new Posn(0, 0)).changeColor(Color.green);
      this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))
          .changeColor(Color.magenta);
      this.kruskals();
      this.searchHelp(new Stack<Vertex>());
    }
    else if (key.equals("y")) { // bias in y
      this.allVertices = new ArrayList<ArrayList<Vertex>>();
      this.allEdges = new ArrayList<Edge>();
      this.usedEdges = new ArrayList<Edge>();
      this.alreadySeen = new ArrayList<Vertex>();
      this.answer = new ArrayList<Vertex>();
      this.drawnBoxes = new ArrayList<Vertex>();
      this.manual = false;
      this.showAnswer = false;
      this.createBoard();
      this.pointer = this.findVertex(new Posn(0, 0));
      this.current = this.findVertex(new Posn(0, 0));
      this.currentUsedEdge = new ArrayList<Edge>();
      this.currentUsedEdgeIndex = 0;
      this.setVertexEdgesY();
      this.findVertex(new Posn(0, 0)).changeColor(Color.green);
      this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1))
          .changeColor(Color.magenta);
      this.kruskals();
      this.searchHelp(new Stack<Vertex>());
    }
    else {
      return;
    }
  }

  // does something every tick
  public void onTick() {
    if (this.currentUsedEdgeIndex < this.usedEdges.size()) {
      this.currentUsedEdge.add(this.usedEdges.get(this.currentUsedEdgeIndex));
      this.currentUsedEdgeIndex += 1;
    }

    if (this.alreadySeen.size() < 1) {
      return;
    }
    else {
      Vertex temp = this.alreadySeen.remove(0);
      this.drawnBoxes.add(temp);
      this.current = temp;
    }
  }

  // returns the bfs whole path taken
  ArrayList<Vertex> bfs() {
    return this.searchHelp(new Queue<Vertex>());
  }

  // EFFECT: create a new board with Vertices
  // ArrayList<ArrayList<Vertex>> holds the row of columns
  // ArrayList<Vertex> holds the list of Vertices in each column
  void createBoard() {
    for (int x = 0; x < this.dimension.x; x += 1) {
      ArrayList<Vertex> column = new ArrayList<Vertex>();
      for (int y = 0; y < this.dimension.y; y += 1) {
        column.add(new Vertex(new Posn(x, y), Color.gray));
      }
      this.allVertices.add(column);
    }
  }

  // returns the dfs whole path taken
  ArrayList<Vertex> dfs() {
    return this.searchHelp(new Stack<Vertex>());
  }

  // gets the edge given two vertices
  // the first Vertex is always from and v2 is always to
  Edge findEdge(ArrayList<Edge> edges, Vertex v1, Vertex v2) {
    for (Edge edge : edges) {
      if (edge.from == v1 && edge.to == v2) {
        return edge;
      }
    }
    return null;
  }

  // returns the representative of the vertex during kruskals
  Vertex findRepresentative(HashMap<Vertex, Vertex> representatives, Vertex v) {
    Vertex child = v;
    Vertex representative = representatives.get(v);
    while (child != representative) {
      child = representative;
      representative = representatives.get(representative);
    }
    return child;
  }

  // returns the Vertex at the given Posn
  Vertex findVertex(Posn posn) {
    return this.allVertices.get(posn.x).get(posn.y);
  }

  // EFFECT: changes usedEdges to have the edges used in Kruskals
  void kruskals() {
    HashMap<Vertex, Vertex> representatives = new HashMap<Vertex, Vertex>();
    ArrayList<Edge> edgesInTree = new ArrayList<Edge>();
    Collections.sort(this.allEdges); // contains the sorted list of all edges

    // initializes every node's representative to itself
    for (ArrayList<Vertex> column : this.allVertices) {
      for (Vertex v : column) {
        representatives.put(v, v);
      }
    }

    // there needs to be n-1 Edges in the tree added to connect all the n Vertices
    while (edgesInTree.size() < representatives.size() - 1 && this.allEdges.size() > 0) {
      Edge firstEdge = this.allEdges.remove(0);
      if (this.findRepresentative(representatives, firstEdge.from) == this
          .findRepresentative(representatives, firstEdge.to)) {
        continue;
      }
      else {
        // record this edge in edgesInTree
        edgesInTree.add(firstEdge);
        this.union(representatives, this.findRepresentative(representatives, firstEdge.from),
            this.findRepresentative(representatives, firstEdge.to));
      }
    }
    this.usedEdges = edgesInTree;
  }

  // EFFECTS: sets this.answer to the path
  void reconstruct(HashMap<Vertex, Vertex> cameFrom, Vertex next) {
    ArrayList<Vertex> path = new ArrayList<Vertex>();
    path.add(next);
    while (!path.contains(this.findVertex(new Posn(0, 0)))) {
      next = cameFrom.get(next);
      path.add(next);
    }
    Collections.reverse(path);
    this.answer = path;
  }

  // helps set the correct path for the right search and returns the whole path
  // taken
  ArrayList<Vertex> searchHelp(ICollection<Vertex> worklist) {
    Vertex target = this.findVertex(new Posn(this.dimension.x - 1, this.dimension.y - 1));
    ArrayList<Vertex> seen = new ArrayList<Vertex>();
    // the cameFromEdge is where the key is destination Vertex
    // and the value is the vertex where it's coming from
    HashMap<Vertex, Vertex> cameFrom = new HashMap<Vertex, Vertex>();
    // initialize the worklist to contain the starting Vertex
    worklist.add(this.findVertex(new Posn(0, 0)));

    while (!worklist.isEmpty()) {
      Vertex next = worklist.remove();
      if (seen.contains(next)) {
        continue;
      }
      else if (next == target) {
        seen.add(next);
        this.reconstruct(cameFrom, next);
        break;
      }
      else {
        seen.add(next);
        for (Edge e : next.outEdges) {
          if (this.usedEdges.contains(e)) {
            if (e.from == next && !seen.contains(e.to)) { // the to is the vertex
              worklist.add(e.to);
              cameFrom.put(e.to, next);
            }
            else if (e.to == next && !seen.contains(e.from)) {
              worklist.add(e.from);
              cameFrom.put(e.from, next);
            }
          }
        }
      }
    }
    return seen;
  }

  // EFFECT: sets each Vertex's edges and also adds all of these edges to the
  // global list of Edges to be used for kruskals
  void setVertexEdges() {
    for (int i = 0; i < this.allVertices.size(); i += 1) {
      for (int j = 0; j < this.allVertices.get(i).size(); j += 1) {
        Vertex v = this.findVertex(new Posn(i, j));
        if (v.posn.x < this.dimension.x - 1) {
          Vertex right = this.findVertex(new Posn(v.posn.x + 1, v.posn.y));
          Edge edge = new Edge(v, right);
          v.outEdges.add(edge); // adds the edge left Vertex
          right.outEdges.add(edge); // adds the edge to the right Vertex
          this.allEdges.add(edge);
        }
        if (v.posn.y < this.dimension.y - 1) {
          Vertex down = this.findVertex(new Posn(v.posn.x, v.posn.y + 1));
          Edge edge = new Edge(v, down);
          v.outEdges.add(edge); // adds the edge to the top Vertex
          down.outEdges.add(edge); // adds the bottom Vertex
          this.allEdges.add(edge);
        }
      }
    }
  }

  // EFFECT: sets each Vertex's edges and also adds all of these edges to the
  // global list of Edges to be used for kruskals
  // biases in X direction
  void setVertexEdgesX() {
    for (int i = 0; i < this.allVertices.size(); i += 1) {
      for (int j = 0; j < this.allVertices.get(i).size(); j += 1) {
        Vertex v = this.findVertex(new Posn(i, j));
        if (v.posn.x < this.dimension.x - 1) {
          Vertex right = this.findVertex(new Posn(v.posn.x + 1, v.posn.y));
          Edge edge = new Edge(v, right);
          v.outEdges.add(edge); // adds the edge left Vertex
          right.outEdges.add(edge); // adds the edge to the right Vertex
          this.allEdges.add(edge);
        }
        if (v.posn.y < this.dimension.y - 1) {
          Vertex down = this.findVertex(new Posn(v.posn.x, v.posn.y + 1));
          Edge edge = new Edge(v, down, new Random().nextInt(100) + 100);
          v.outEdges.add(edge); // adds the edge to the top Vertex
          down.outEdges.add(edge); // adds the bottom Vertex
          this.allEdges.add(edge);
        }
      }
    }
  }

  // EFFECT: sets each Vertex's edges and also adds all of these edges to the
  // global list of Edges to be used for kruskals
  // biases in Y direction
  void setVertexEdgesY() {
    for (int i = 0; i < this.allVertices.size(); i += 1) {
      for (int j = 0; j < this.allVertices.get(i).size(); j += 1) {
        Vertex v = this.findVertex(new Posn(i, j));
        if (v.posn.x < this.dimension.x - 1) {
          Vertex right = this.findVertex(new Posn(v.posn.x + 1, v.posn.y));
          Edge edge = new Edge(v, right, new Random().nextInt(100) + 100);
          v.outEdges.add(edge); // adds the edge left Vertex
          right.outEdges.add(edge); // adds the edge to the right Vertex
          this.allEdges.add(edge);
        }
        if (v.posn.y < this.dimension.y - 1) {
          Vertex down = this.findVertex(new Posn(v.posn.x, v.posn.y + 1));
          Edge edge = new Edge(v, down);
          v.outEdges.add(edge); // adds the edge to the top Vertex
          down.outEdges.add(edge); // adds the bottom Vertex
          this.allEdges.add(edge);
        }
      }
    }
  }

  // sets the fromRep's representative to toRep
  void union(HashMap<Vertex, Vertex> representatives, Vertex fromRep, Vertex toRep) {
    representatives.put(fromRep, toRep);
  }
}

// Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}

// represents a Collection for depth-first search
class Stack<T> implements ICollection<T> {
  ArrayList<T> contents;

  Stack() {
    this.contents = new ArrayList<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public T remove() {
    return this.contents.remove(0);
  }

  public void add(T item) {
    this.contents.add(0, item);
  }
}

// represents a Collection for breadth-first search
class Queue<T> implements ICollection<T> {
  ArrayList<T> contents;

  Queue() {
    this.contents = new ArrayList<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public T remove() {
    return this.contents.remove(0);
  }

  public void add(T item) {
    this.contents.add(item);
  }
}

/* Vertex:
 * Edge:
 * Maze:
 *  onKeyEvent(String)
 *  onTick()
 *  findEdge(ArrayList<Edge>, Vertex, Vertex)
 */
class ExamplesMaze {
  // 2x2
  Vertex vertexZero0;
  Vertex vertexZero1;
  Vertex vertexOne0;
  Vertex vertexOne1;

  Edge edgeZero0To10;
  Edge edgeZero0To01;
  Edge edgeZero1To11;
  Edge edgeOne0To11;

  ArrayList<Vertex> columnZero;
  ArrayList<Vertex> columnOne;
  ArrayList<ArrayList<Vertex>> allVertices2x2;

  Maze maze2x2;

  /*
  00 10 20 
  01 11 21 
  02 12 22
  */

  // 3x3
  Vertex vertex00;
  Vertex vertex01;
  Vertex vertex02;
  Vertex vertex10;
  Vertex vertex11;
  Vertex vertex12;
  Vertex vertex20;
  Vertex vertex21;
  Vertex vertex22;

  Edge edge00To10; // 00
  Edge edge00To01;

  Edge edge01To11; // 01
  Edge edge01To02;

  Edge edge02To12; // 02

  Edge edge10To20; // 10
  Edge edge10To11;

  Edge edge11To21; // 11
  Edge edge11To12;

  Edge edge12To22; // 12

  Edge edge20To21; // 20

  Edge edge21To22; // 21

  ArrayList<Vertex> column0;
  ArrayList<Vertex> column1;
  ArrayList<Vertex> column2;
  ArrayList<ArrayList<Vertex>> allVertices3x3;

  Maze maze3x3;
  Maze maze3x3v2;

  void initData() {
    // 2x2
    this.vertexZero0 = new Vertex(new Posn(0, 0), Color.green);
    this.vertexZero1 = new Vertex(new Posn(0, 1), Color.gray);
    this.vertexOne0 = new Vertex(new Posn(1, 0), Color.gray);
    this.vertexOne1 = new Vertex(new Posn(1, 1), Color.magenta);

    this.edgeZero0To10 = new Edge(this.vertexZero0, this.vertexOne0, 10);
    this.edgeZero0To01 = new Edge(this.vertexZero0, this.vertexZero1, 50);
    this.edgeZero1To11 = new Edge(this.vertexZero1, this.vertexOne1, 30);
    this.edgeOne0To11 = new Edge(this.vertexOne0, this.vertexOne1, 40);

    this.vertexZero0.outEdges.add(this.edgeZero0To10);
    this.vertexZero0.outEdges.add(this.edgeZero0To01);

    this.vertexOne0.outEdges.add(this.edgeZero0To10);
    this.vertexOne0.outEdges.add(this.edgeOne0To11);

    this.vertexZero1.outEdges.add(this.edgeZero0To01);
    this.vertexZero1.outEdges.add(this.edgeZero1To11);

    this.vertexOne1.outEdges.add(this.edgeZero1To11);
    this.vertexOne1.outEdges.add(this.edgeOne0To11);

    this.columnZero = new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, this.vertexZero1));
    this.columnOne = new ArrayList<Vertex>(Arrays.asList(this.vertexOne0, this.vertexOne1));
    this.allVertices2x2 = new ArrayList<ArrayList<Vertex>>(
        Arrays.asList(this.columnZero, this.columnOne));
    this.maze2x2 = new Maze(this.allVertices2x2, new Random());

    // 3x3
    this.vertex00 = new Vertex(new Posn(0, 0), Color.green);
    this.vertex01 = new Vertex(new Posn(0, 1), Color.gray);
    this.vertex02 = new Vertex(new Posn(0, 2), Color.gray);

    this.vertex10 = new Vertex(new Posn(1, 0), Color.gray);
    this.vertex11 = new Vertex(new Posn(1, 1), Color.gray);
    this.vertex12 = new Vertex(new Posn(1, 2), Color.gray);

    this.vertex20 = new Vertex(new Posn(2, 0), Color.gray);
    this.vertex21 = new Vertex(new Posn(2, 1), Color.gray);
    this.vertex22 = new Vertex(new Posn(2, 2), Color.magenta);

    this.edge00To01 = new Edge(this.vertex00, this.vertex01, 15);
    this.edge00To10 = new Edge(this.vertex00, this.vertex10, 8);

    this.edge01To11 = new Edge(this.vertex01, this.vertex11, 93);
    this.edge01To02 = new Edge(this.vertex01, this.vertex02, 31);

    this.edge02To12 = new Edge(this.vertex02, this.vertex12, 49);

    this.edge10To20 = new Edge(this.vertex10, this.vertex20, 76);
    this.edge10To11 = new Edge(this.vertex10, this.vertex11, 38);

    this.edge11To21 = new Edge(this.vertex11, this.vertex21, 63);
    this.edge11To12 = new Edge(this.vertex11, this.vertex12, 76);

    this.edge12To22 = new Edge(this.vertex12, this.vertex22, 38);

    this.edge20To21 = new Edge(this.vertex20, this.vertex21, 21);

    this.edge21To22 = new Edge(this.vertex21, this.vertex22, 74);

    // adding Edges to Vertex
    this.vertex00.outEdges.addAll(Arrays.asList(this.edge00To10, this.edge00To01));
    this.vertex01.outEdges.addAll(Arrays.asList(this.edge00To01, this.edge01To11, this.edge01To02));
    this.vertex02.outEdges.addAll(Arrays.asList(this.edge01To02, this.edge02To12));

    this.vertex10.outEdges.addAll(Arrays.asList(this.edge00To10, this.edge10To20, this.edge10To11));
    this.vertex11.outEdges
        .addAll(Arrays.asList(this.edge01To11, this.edge10To11, this.edge11To21, this.edge11To12));
    this.vertex12.outEdges.addAll(Arrays.asList(this.edge02To12, this.edge11To12, this.edge12To22));

    this.vertex20.outEdges.addAll(Arrays.asList(this.edge10To20, this.edge20To21));
    this.vertex21.outEdges.addAll(Arrays.asList(this.edge11To21, this.edge20To21, this.edge21To22));
    this.vertex22.outEdges.addAll(Arrays.asList(this.edge12To22, this.edge21To22));

    this.column0 = new ArrayList<Vertex>(
        Arrays.asList(this.vertex00, this.vertex01, this.vertex02));
    this.column1 = new ArrayList<Vertex>(
        Arrays.asList(this.vertex10, this.vertex11, this.vertex12));
    this.column2 = new ArrayList<Vertex>(
        Arrays.asList(this.vertex20, this.vertex21, this.vertex22));
    this.allVertices3x3 = new ArrayList<ArrayList<Vertex>>(
        Arrays.asList(this.column0, this.column1, this.column2));

    this.maze3x3 = new Maze(this.allVertices3x3, new Random());
  }

  // Vertex
  // tests for changeColor(Color)
  void testChangeColor(Tester t) {
    this.initData();
    t.checkExpect(this.vertex00.color, Color.green);
    this.vertex00.changeColor(Color.black);
    t.checkExpect(this.vertex00.color, Color.black);
  }

  // tests for drawVertex()
  void testDrawVertex(Tester t) {
    this.initData();
    t.checkExpect(this.vertex00.drawVertex(new Posn(20, 20)),
        new RectangleImage(20, 20, "solid", Color.green));
    t.checkExpect(this.vertex22.drawVertex(new Posn(20, 20)),
        new RectangleImage(20, 20, "solid", Color.magenta));
    t.checkExpect(this.vertex01.drawVertex(new Posn(20, 20)),
        new RectangleImage(20, 20, "solid", Color.gray));
    t.checkExpect(this.vertex10.drawVertex(new Posn(20, 20)),
        new RectangleImage(20, 20, "solid", Color.gray));
  }

  // Edge
  // tests for compareTo(Edge)
  void testCompareTo(Tester t) {
    this.initData();
    t.checkExpect(this.edge00To01.compareTo(this.edge00To10), 7);
    t.checkExpect(this.edge00To10.compareTo(this.edge00To01), -7);
    t.checkExpect(this.edge00To01.compareTo(this.edge00To01), 0);
  }

  // Maze
  // tests Maze constructor creation
  void testMazeCreation(Tester t) {
    this.initData();
    // dimension constructors
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze",
        new Posn(0, 0));
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze",
        new Posn(0, 1));
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze",
        new Posn(1, 0));
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze",
        new Posn(1, 1));
    // allVertices constructors
    ArrayList<ArrayList<Vertex>> list00 = new ArrayList<ArrayList<Vertex>>();
    ArrayList<ArrayList<Vertex>> list11 = new ArrayList<ArrayList<Vertex>>(
        Arrays.asList(new ArrayList<Vertex>(Arrays.asList(this.vertex00))));
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze", list00);
    t.checkConstructorException(
        new IllegalArgumentException("Maze has to be bigger than 1 dimension!"), "Maze", list11);
  }

  // tests for makeScene(){
  void testMakeScene(Tester t) {
    this.initData();
    WorldScene background = new WorldScene(1000, 600);
    this.maze2x2.setVertexEdges();
    this.maze2x2.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.maze2x2.findVertex(new Posn(this.maze2x2.dimension.x - 1, this.maze2x2.dimension.y - 1))
        .changeColor(Color.magenta);
    this.maze2x2.kruskals();
    // sets the answer to the dfs search in case they do manual
    this.maze2x2.searchHelp(new Stack<Vertex>());

    // places the gray background
    background.placeImageXY(new RectangleImage(1000, 600, "solid", Color.gray), 500, 300);
    // places the green box on top left
    background.placeImageXY(
        new RectangleImage(this.maze2x2.cellSize.x, this.maze2x2.cellSize.y, "solid", Color.green),
        this.maze2x2.cellSize.x / 2, this.maze2x2.cellSize.y / 2);
    // places the magenta box on the bottom right
    background.placeImageXY(
        new RectangleImage(this.maze2x2.cellSize.x, this.maze2x2.cellSize.y, "solid",
            Color.magenta),
        (this.maze2x2.dimension.x - 1) * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
        (this.maze2x2.dimension.y - 1) * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);

    // creates the horizontal lines to the grid
    for (int y = 0; y <= 600; y += this.maze2x2.cellSize.y) {
      WorldImage line = new RectangleImage(1000, 2, "solid", Color.black);
      background.placeImageXY(line, 500, y);
    }

    // creates the vertical lines to the grid
    for (int x = 0; x < 1000; x += this.maze2x2.cellSize.x) {
      WorldImage line = new RectangleImage(2, 600, "solid", Color.black);
      background.placeImageXY(line, x, 300);
    }

    // draws each wall being knocked down (COMMENT THIS OUT
    // FOR THE FOR LOOP UNDERNEATH TO START WITH WALLS KNOCKED DOWN)
    for (Edge e : this.maze2x2.currentUsedEdge) {
      WorldImage horizontal = new RectangleImage(this.maze2x2.cellSize.x - 2, 2, "solid",
          Color.gray);
      WorldImage vertical = new RectangleImage(2, this.maze2x2.cellSize.y - 2, "solid", Color.gray);
      if (e.from.posn.x < e.to.posn.x) {
        background.placeImageXY(vertical, e.to.posn.x * this.maze2x2.cellSize.x,
            e.from.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);
      }
      if (e.from.posn.x > e.to.posn.x) {
        background.placeImageXY(vertical, e.from.posn.x * this.maze2x2.cellSize.x,
            e.from.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);
      }
      if (e.from.posn.y < e.to.posn.y) {
        background.placeImageXY(horizontal,
            e.from.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
            e.to.posn.y * this.maze2x2.cellSize.y);
      }
      if (e.from.posn.y > e.to.posn.y) {
        background.placeImageXY(horizontal,
            e.from.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
            e.from.posn.y * this.maze2x2.cellSize.y);
      }
    }

    // loops through drawnBoxes and add it onto background
    for (Vertex v : this.maze2x2.drawnBoxes) {
      background.placeImageXY(
          new RectangleImage(this.maze2x2.cellSize.x - 2, this.maze2x2.cellSize.y - 2, "solid",
              this.maze2x2.lightBlue),
          v.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
          v.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);
    }

    // draws the Vertex the search loop is currently on
    background.placeImageXY(
        new RectangleImage(this.maze2x2.cellSize.x - 2, this.maze2x2.cellSize.y - 2, "solid",
            Color.green),
        this.maze2x2.current.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
        this.maze2x2.current.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);

    // toggles the solution
    if (this.maze2x2.showAnswer) {
      for (Vertex v : this.maze2x2.answer) {
        background.placeImageXY(
            new RectangleImage(this.maze2x2.cellSize.x - 2, this.maze2x2.cellSize.y - 2, "solid",
                Color.BLUE),
            v.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
            v.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);
      }
    }

    // if manual is turned on it draws the manual position
    if (this.maze2x2.manual) {
      background.placeImageXY(
          new RectangleImage(this.maze2x2.cellSize.x - 2, this.maze2x2.cellSize.y - 2, "solid",
              new Color(255, 92, 0)),
          this.maze2x2.pointer.posn.x * this.maze2x2.cellSize.x + this.maze2x2.cellSize.x / 2,
          this.maze2x2.pointer.posn.y * this.maze2x2.cellSize.y + this.maze2x2.cellSize.y / 2);
    }

    // shows "YOU WIN!" when you completed it on manual
    if (this.maze2x2.pointer == this.maze2x2
        .findVertex(new Posn(this.maze2x2.dimension.x - 1, this.maze2x2.dimension.y - 1))) {
      background.placeImageXY(new TextImage("YOU WIN!", 50, FontStyle.BOLD, Color.green), 500, 300);
    }

    t.checkExpect(this.maze2x2.makeScene(), background);
  }

  // tests for onKeyEvent(String)
  void testOnKeyEvent(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.maze2x2.findVertex(new Posn(this.maze2x2.dimension.x - 1, this.maze2x2.dimension.y - 1))
        .changeColor(Color.magenta);
    this.maze2x2.kruskals();
    this.maze2x2.searchHelp(new Stack<Vertex>());

    this.maze2x2.allVertices = new ArrayList<ArrayList<Vertex>>(); // r
    this.maze2x2.allEdges = new ArrayList<Edge>();
    this.maze2x2.usedEdges = new ArrayList<Edge>();
    this.maze2x2.answer = new ArrayList<Vertex>();
    t.checkExpect(this.maze2x2.allVertices.size(), 0);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    t.checkExpect(this.maze2x2.usedEdges.size(), 0);
    t.checkExpect(this.maze2x2.answer.size(), 0);
    this.maze2x2.onKeyEvent("r");
    t.checkExpect(this.maze2x2.allVertices.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 1);
    t.checkExpect(this.maze2x2.usedEdges.size(), 3);

    t.checkExpect(this.maze2x2.showAnswer, false); // a
    this.maze2x2.onKeyEvent("a");
    t.checkExpect(this.maze2x2.showAnswer, true);
    this.maze2x2.onKeyEvent("a");

    t.checkExpect(this.maze2x2.manual, false);
    this.maze2x2.onKeyEvent("m");
    t.checkExpect(this.maze2x2.manual, true);

    t.checkExpect(this.maze2x2.pointer, this.maze2x2.findVertex(new Posn(0, 0)));
    this.maze2x2.onKeyEvent("right");
    t.checkExpect(this.maze2x2.pointer, this.maze2x2.findVertex(new Posn(1, 0)));
    this.maze2x2.onKeyEvent("down");
    t.checkExpect(this.maze2x2.pointer, this.maze2x2.findVertex(new Posn(1, 1)));
    this.maze2x2.onKeyEvent("up");
    t.checkExpect(this.maze2x2.pointer, this.maze2x2.findVertex(new Posn(1, 0)));
    this.maze2x2.onKeyEvent("left");
    t.checkExpect(this.maze2x2.pointer, this.maze2x2.findVertex(new Posn(0, 0)));

    t.checkExpect(this.maze2x2.alreadySeen.size(), 0);
    this.maze2x2.onKeyEvent("b");
    t.checkExpect(this.maze2x2.alreadySeen.size(), 4);

    this.maze2x2.alreadySeen = new ArrayList<Vertex>();
    this.maze2x2.onKeyEvent("d");
    t.checkExpect(this.maze2x2.alreadySeen.size(), 4);

    this.maze2x2.allVertices = new ArrayList<ArrayList<Vertex>>(); // x
    this.maze2x2.allEdges = new ArrayList<Edge>();
    this.maze2x2.usedEdges = new ArrayList<Edge>();
    this.maze2x2.answer = new ArrayList<Vertex>();
    t.checkExpect(this.maze2x2.allVertices.size(), 0);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    t.checkExpect(this.maze2x2.usedEdges.size(), 0);
    t.checkExpect(this.maze2x2.answer.size(), 0);
    this.maze2x2.onKeyEvent("x");
    t.checkExpect(this.maze2x2.allVertices.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 1);
    t.checkExpect(this.maze2x2.usedEdges.size(), 3);

    this.maze2x2.allVertices = new ArrayList<ArrayList<Vertex>>(); // y
    this.maze2x2.allEdges = new ArrayList<Edge>();
    this.maze2x2.usedEdges = new ArrayList<Edge>();
    this.maze2x2.answer = new ArrayList<Vertex>();
    t.checkExpect(this.maze2x2.allVertices.size(), 0);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    t.checkExpect(this.maze2x2.usedEdges.size(), 0);
    t.checkExpect(this.maze2x2.answer.size(), 0);
    this.maze2x2.onKeyEvent("y");
    t.checkExpect(this.maze2x2.allVertices.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 1);
    t.checkExpect(this.maze2x2.usedEdges.size(), 3);

  }

  // tests for onTick()
  void testOnTick(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.currentUsedEdgeIndex, 0);
    t.checkExpect(this.maze2x2.drawnBoxes, new ArrayList<Vertex>());
    t.checkExpect(this.maze2x2.current, this.vertexZero0);
    this.maze2x2.onTick();
    this.maze2x2.onTick();
    t.checkExpect(this.maze2x2.currentUsedEdgeIndex, 2);
    t.checkExpect(this.maze2x2.drawnBoxes, new ArrayList<Vertex>());
    t.checkExpect(this.maze2x2.current, this.vertexZero0);
  }

  // tests for bfs(Vertex, Vertex)
  void testBfs(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.answer, new ArrayList<Vertex>());
    this.maze2x2.bfs();
    t.checkOneOf(this.maze2x2.answer,
        new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, this.vertexZero1, this.vertexOne1)));
  }

  // tests for createBoard
  void testCreateBoard(Tester t) {
    this.initData();
    Maze twox2 = new Maze(new Posn(2, 2), new Random());
    t.checkExpect(twox2.allVertices, new ArrayList<ArrayList<Vertex>>());
    twox2.createBoard();
    t.checkExpect(twox2.allVertices.size(), 2);
  }

  // tests for dfs(Vertex, Vertex)
  void testDfs(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.answer, new ArrayList<Vertex>());
    this.maze2x2.dfs();
    t.checkExpect(this.maze2x2.answer,
        new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, 
                                            this.vertexZero1, this.vertexOne1)));
  }

  // tests for findEdge(ArrayList<Edge>, Vertex, Vertex)
  void testFindEdge(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    // sets the answer to the dfs search in case they do manual
    this.maze2x2.searchHelp(new Stack<Vertex>());
    t.checkExpect(this.maze2x2.findEdge(this.maze2x2.findVertex(new Posn(0, 0)).outEdges,
        this.vertexZero0, this.vertexOne0), this.edgeZero0To10);
  }

  // tests for findRepresentative(HashMap<Vertex, Vertex>, Vertex)
  void testFindRepresentative(Tester t) {
    this.initData();
    // 00 | 01 <- 10 <- 11
    HashMap<Vertex, Vertex> hash1 = new HashMap<Vertex, Vertex>();
    hash1.put(this.vertexZero0, this.vertexZero0);
    hash1.put(this.vertexZero1, this.vertexZero1);
    hash1.put(this.vertexOne0, this.vertexZero1);
    hash1.put(this.vertexOne1, this.vertexOne0);
    t.checkExpect(this.maze2x2.findRepresentative(hash1, vertexZero0), this.vertexZero0);
    t.checkExpect(this.maze2x2.findRepresentative(hash1, vertexOne0), this.vertexZero1);
    t.checkExpect(this.maze2x2.findRepresentative(hash1, vertexOne1), this.vertexZero1);
  }

  // tests for findVertex(Posn)
  void testFindVertex(Tester t) {
    this.initData();
    t.checkExpect(this.maze3x3.findVertex(new Posn(0, 0)), this.vertex00);
    t.checkExpect(this.maze3x3.findVertex(new Posn(1, 1)), this.vertex11);
    t.checkExpect(this.maze3x3.findVertex(new Posn(2, 2)), this.vertex22);
  }

  // tests for kruskals()
  void testKruskals(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.maze2x2.findVertex(new Posn(this.maze2x2.dimension.x - 1, this.maze2x2.dimension.y - 1))
        .changeColor(Color.magenta);
    this.maze3x3.setVertexEdges();
    this.maze3x3.findVertex(new Posn(0, 0)).changeColor(Color.green);
    this.maze3x3.findVertex(new Posn(this.maze3x3.dimension.x - 1, this.maze3x3.dimension.y - 1))
        .changeColor(Color.magenta);
    t.checkExpect(this.maze2x2.usedEdges.size(), 0);
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.usedEdges.size(), 3);
    t.checkExpect(this.maze3x3.usedEdges.size(), 0);
    this.maze3x3.kruskals();
    t.checkExpect(this.maze3x3.usedEdges.size(), 8);
  }

  // tests for reconstruct(HashMap<Vertex, Edge>, Vertex)
  void testReconstruct(Tester t) {
    this.initData();
    HashMap<Vertex, Vertex> hash1 = new HashMap<Vertex, Vertex>();
    hash1.put(this.vertexOne0, this.vertexZero0);
    hash1.put(this.vertexOne1, this.vertexOne0);
    t.checkExpect(this.maze2x2.answer.size(), 0);
    this.maze2x2.reconstruct(hash1, this.vertexOne1);
    t.checkExpect(this.maze2x2.answer,
        new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, this.vertexOne0, this.vertexOne1)));
  }

  /*
  // tests for searchHelp(Vertex, Vertex, ICollection<Vertex>)
  void testSearchHelp(Tester t) {
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.searchHelp(new Stack<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, this.vertexZero1, this.vertexOne1)));
    this.initData();
    this.maze2x2.setVertexEdges();
    this.maze2x2.kruskals();
    t.checkExpect(this.maze2x2.searchHelp(new Queue<Vertex>()),
        new ArrayList<Vertex>(Arrays.asList(this.vertexZero0, this.vertexOne0, this.vertexOne1)));\
  }
  */

  // tests for setVertexEdges()
  void testSetVertexEdges(Tester t) {
    this.initData();
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    this.maze2x2.setVertexEdges();
    t.checkExpect(this.maze2x2.allEdges.size(), 4);
  }

  // tests for setVertexEdges()
  void testSetVertexEdgesX(Tester t) {
    this.initData();
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    this.maze2x2.setVertexEdgesX();
    t.checkExpect(this.maze2x2.allEdges.size(), 4);
  }

  // tests for setVertexEdges()
  void testSetVertexEdgesY(Tester t) {
    this.initData();
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(0, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 0)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.findVertex(new Posn(1, 1)).outEdges.size(), 2);
    t.checkExpect(this.maze2x2.allEdges.size(), 0);
    this.maze2x2.setVertexEdgesY();
    t.checkExpect(this.maze2x2.allEdges.size(), 4);
  }

  // tests for union(HashMap<Vertex, Vertex>, Vertex, Vertex)
  void testUnion(Tester t) {
    this.initData();
    // 00 | 01 <- 10 <- 11
    HashMap<Vertex, Vertex> hash1 = new HashMap<Vertex, Vertex>();
    hash1.put(this.vertexZero0, this.vertexZero0);
    hash1.put(this.vertexZero1, this.vertexZero1);
    hash1.put(this.vertexOne0, this.vertexZero1);
    hash1.put(this.vertexOne1, this.vertexOne0);
    t.checkExpect(this.maze2x2.findRepresentative(hash1, this.vertexZero0), this.vertexZero0);
    this.maze2x2.union(hash1, this.vertexZero0, this.vertexOne1);
    t.checkExpect(this.maze2x2.findRepresentative(hash1, this.vertexZero0), this.vertexZero1);
  }

  void testBigBang(Tester t) {
    this.initData();
    Maze m = new Maze(new Posn(100, 60));
    m.bigBang(1000, 600, .001);
  }
}