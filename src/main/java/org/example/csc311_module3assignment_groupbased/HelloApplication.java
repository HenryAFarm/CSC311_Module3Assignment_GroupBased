package org.example.csc311_module3assignment_groupbased;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class HelloApplication extends Application {
    private static final int STEP = 5;
    private static final double OPEN_THRESHOLD = 0.80;

    // Optional overrides for reference/testing. When enabled and valid, the start
    // position will be placed at START_OVERRIDE_{X,Y} instead of scanning.
    // These are only used if the override point fits entirely on open path pixels
    // for the robot footprint. They can be left enabled for a specific maze and
    // will fall back to dynamic detection on other images.
    private static final boolean USE_START_OVERRIDE = true;
    private static final int START_OVERRIDE_X = 0;
    private static final int START_OVERRIDE_Y = 272;

    // Optional exit override (reference only). When enabled pickExitFrom may use
    // this region as a preferred exit area; kept as metadata for now.
    private static final boolean USE_EXIT_OVERRIDE = true;
    private static final int EXIT_OVERRIDE_X = 605;
    private static final int EXIT_OVERRIDE_Y_MIN = 227;
    private static final int EXIT_OVERRIDE_Y_MAX = 274;

    @Override
    public void start(Stage stage) {
        MazeWorld world1 = new MazeWorld("/images/maze.png");
        MazeWorld world2 = new MazeWorld("/images/maze2.png");

        TabPane tabs = new TabPane();
        Tab t1 = new Tab("Maze 1");
        BorderPane bp1 = new BorderPane();
        bp1.setCenter(world1.arena());
        bp1.setBottom(world1.controls());
        t1.setContent(bp1);
        t1.setClosable(false);

        Tab t2 = new Tab("Maze 2");
        BorderPane bp2 = new BorderPane();
        bp2.setCenter(world2.arena());
        bp2.setBottom(world2.controls());
        t2.setContent(bp2);
        t2.setClosable(false);

        tabs.getTabs().addAll(t1, t2);

        int width = Math.max(world1.width(), world2.width());
        int height = Math.max(world1.height(), world2.height());

        Scene scene = new Scene(tabs, width, height + 40);
        scene.setOnKeyPressed(event -> {
            Tab sel = tabs.getSelectionModel().getSelectedItem();
            if (sel == t1) world1.moveByKey(event.getCode());
            else world2.moveByKey(event.getCode());
        });

        // focus the currently selected world's arena when switching tabs
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, oldT, newT) -> {
            if (newT == t1) world1.arena().requestFocus();
            else world2.arena().requestFocus();
        });

        stage.setTitle("Step 3 - Car Maze (two mazes)");
        stage.setScene(scene);
        stage.show();
        // initial focus
        world1.arena().requestFocus();
    }

    private enum Direction {
        UP(0, -STEP),
        DOWN(0, STEP),
        LEFT(-STEP, 0),
        RIGHT(STEP, 0);

        final int dx;
        final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private abstract static class MazeActor {
        protected double x;
        protected double y;

        MazeActor(double x, double y) {
            this.x = x;
            this.y = y;
        }

        abstract Node node();

        abstract int width();

        abstract int height();

        void moveTo(double nx, double ny) {
            x = nx;
            y = ny;
            node().setLayoutX(nx);
            node().setLayoutY(ny);
        }
    }

    private static final class CarActor extends MazeActor {
        private final Pane carView;
        private final int w;
        private final int h;

        CarActor(double x, double y) {
            super(x, y);
            // Car dimensions roughly match previous robot sprite
            this.w = 26;
            this.h = 26;
            carView = new Pane();
            carView.setPrefSize(w, h);

            javafx.scene.shape.Rectangle body = new javafx.scene.shape.Rectangle(6, 6, 14, 10);
            body.setArcWidth(6);
            body.setArcHeight(6);
            body.setFill(Color.DARKRED);

            javafx.scene.shape.Rectangle cabin = new javafx.scene.shape.Rectangle(9, 2, 8, 6);
            cabin.setFill(Color.LIGHTBLUE);

            javafx.scene.shape.Circle wheel1 = new javafx.scene.shape.Circle(8, 18, 3);
            wheel1.setFill(Color.BLACK);
            javafx.scene.shape.Circle wheel2 = new javafx.scene.shape.Circle(18, 18, 3);
            wheel2.setFill(Color.BLACK);

            javafx.scene.shape.Polygon nose = new javafx.scene.shape.Polygon();
            nose.getPoints().addAll(20.0, 11.0, 25.0, 13.0, 20.0, 15.0);
            nose.setFill(Color.DARKRED.darker());

            carView.getChildren().addAll(body, cabin, wheel1, wheel2, nose);
            carView.setLayoutX(x);
            carView.setLayoutY(y);
        }

        @Override
        Node node() {
            return carView;
        }

        @Override
        int width() {
            return w;
        }

        @Override
        int height() {
            return h;
        }

        void setHeading(Direction dir) {
            if (dir == null) return;
            switch (dir) {
                case RIGHT:
                    carView.setRotate(0);
                    break;
                case DOWN:
                    carView.setRotate(90);
                    break;
                case LEFT:
                    carView.setRotate(180);
                    break;
                case UP:
                    carView.setRotate(270);
                    break;
            }
        }
    }

    private static final class MazeWorld {
        private final Image mazeImage;
        private final PixelReader pixels;
        private final ImageView mazeView;
        private final Pane arena;
        private Label status;
        private final MazeActor robot;
        private final HBox controls;
        private Timeline autoTimeline;
        private int startX;
        private int startY;

        MazeWorld(String mazeResourcePath) {
            mazeImage = new Image(HelloApplication.class.getResourceAsStream(mazeResourcePath));
            pixels = mazeImage.getPixelReader();
            mazeView = new ImageView(mazeImage);

            robot = new CarActor(0, 0);
            Label localStatus = new Label("Use arrow keys. Movement is blocked by maze walls.");
            int startX = 0, startY = 0;
            if (USE_START_OVERRIDE) {
                // Try to place the robot at the requested override. If that exact
                // spot is blocked, search nearby along Y (down then up) for the
                // first position that fully fits the robot footprint.
                int w = robot.width();
                int h = robot.height();
                if (isAreaOpen(START_OVERRIDE_X, START_OVERRIDE_Y, w, h)) {
                    startX = START_OVERRIDE_X;
                    startY = START_OVERRIDE_Y;
                    localStatus = new Label(String.format("Using forced start override (%d,%d)", startX, startY));
                } else {
                    // search offsets up to 60 pixels
                    boolean found = false;
                    for (int off = STEP; off <= 60; off += STEP) {
                        int tryYDown = START_OVERRIDE_Y + off;
                        int tryYUp = START_OVERRIDE_Y - off;
                        if (tryYDown + h <= mazeImage.getHeight() && isAreaOpen(START_OVERRIDE_X, tryYDown, w, h)) {
                            startX = START_OVERRIDE_X;
                            startY = tryYDown;
                            localStatus = new Label(String.format("Using nearby start override (%d,%d)", startX, startY));
                            found = true;
                            break;
                        }
                        if (tryYUp >= 0 && isAreaOpen(START_OVERRIDE_X, tryYUp, w, h)) {
                            startX = START_OVERRIDE_X;
                            startY = tryYUp;
                            localStatus = new Label(String.format("Using nearby start override (%d,%d)", startX, startY));
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        int[] start = findStartPosition(robot.width(), robot.height());
                        startX = start[0];
                        startY = start[1];
                        localStatus = new Label("Override blocked; using dynamic start.");
                    }
                }
            } else {
                int[] start = findStartPosition(robot.width(), robot.height());
                startX = start[0];
                startY = start[1];
                localStatus = new Label("Use arrow keys. Movement is blocked by maze walls.");
            }
            // Align start to STEP grid to avoid BFS/grid mismatch later
            startX = (int) (Math.round(startX / (double) STEP) * STEP);
            startY = (int) (Math.round(startY / (double) STEP) * STEP);
            robot.moveTo(startX, startY);
            // persist start coordinates for resets
            this.startX = startX;
            this.startY = startY;
            this.status = localStatus;

            Button autoSolve = new Button("Auto Solve");
            autoSolve.setOnAction(event -> autoSolve());
            Button reset = new Button("Reset");
            reset.setOnAction(ev -> resetToStart());
            controls = new HBox(10, autoSolve, reset, status);
            arena = new Pane(mazeView, robot.node());
            arena.setPrefSize(mazeImage.getWidth(), mazeImage.getHeight());
            arena.setFocusTraversable(true);
            arena.setOnMouseMoved(this::handleMouseMove);
        }

        Pane arena() {
            return arena;
        }

        HBox controls() {
            return controls;
        }

        int width() {
            return (int) mazeImage.getWidth();
        }

        int height() {
            return (int) mazeImage.getHeight();
        }

        void moveByKey(KeyCode code) {
            Direction direction;
            switch (code) {
                case UP:
                    direction = Direction.UP;
                    break;
                case DOWN:
                    direction = Direction.DOWN;
                    break;
                case LEFT:
                    direction = Direction.LEFT;
                    break;
                case RIGHT:
                    direction = Direction.RIGHT;
                    break;
                default:
                    direction = null;
                    break;
            }
            if (direction != null) {
                stopAutoIfRunning();
                tryMove(direction);
            }
        }

        private void tryMove(Direction direction) {
            double nx = robot.x + direction.dx;
            double ny = robot.y + direction.dy;
            if (canOccupy(nx, ny)) {
                robot.moveTo(nx, ny);
                if (robot instanceof CarActor) {
                    ((CarActor) robot).setHeading(direction);
                }
                status.setText(String.format("Robot at (%.0f, %.0f)", robot.x, robot.y));
            } else {
                status.setText("Blocked by wall.");
            }
        }

        private boolean canOccupy(double x, double y) {
            int inset = 3;
            return isAreaOpen((int) Math.floor(x) + inset, (int) Math.floor(y) + inset,
                    robot.width() - (2 * inset), robot.height() - (2 * inset));
        }

        private boolean isWalkable(int px, int py) {
            Color c = pixels.getColor(px, py);
            return c.getRed() >= OPEN_THRESHOLD
                    && c.getGreen() >= OPEN_THRESHOLD
                    && c.getBlue() >= OPEN_THRESHOLD;
        }

        private int[] findStartPosition(int robotWidth, int robotHeight) {
            int width = (int) mazeImage.getWidth();
            int height = (int) mazeImage.getHeight();
            for (int y = 0; y <= height - robotHeight; y++) {
                for (int x = 0; x <= width - robotWidth; x++) {
                    if (isAreaOpen(x, y, robotWidth, robotHeight)) {
                        return new int[]{x, y};
                    }
                }
            }
            throw new IllegalStateException("No open starting area found in maze image");
        }

        private boolean isAreaOpen(int x, int y, int width, int height) {
            int maxX = (int) mazeImage.getWidth() - 1;
            int maxY = (int) mazeImage.getHeight() - 1;
            if (x < 0 || y < 0 || x + width - 1 > maxX || y + height - 1 > maxY) {
                return false;
            }

            int stepX = Math.max(1, width / 6);
            int stepY = Math.max(1, height / 6);
            for (int sampleX = 0; sampleX <= width - 1; sampleX += stepX) {
                for (int sampleY = 0; sampleY <= height - 1; sampleY += stepY) {
                    if (!isWalkable(x + sampleX, y + sampleY)) {
                        return false;
                    }
                }
            }
            // Sampling passed — accept the area as open (avoid over-strict perimeter checks)
            return true;
        }

        private void handleMouseMove(MouseEvent event) {
            status.setText(String.format("Mouse (%.0f, %.0f) | Robot (%.0f, %.0f)",
                    event.getX(), event.getY(), robot.x, robot.y));
        }

        private void autoSolve() {
            stopAutoIfRunning();
            int startX = ((int) robot.x / STEP) * STEP;
            int startY = ((int) robot.y / STEP) * STEP;

            Map<String, String> parent = new HashMap<>();
            Map<String, Direction> moveTaken = new HashMap<>();
            Map<String, int[]> coordOf = new HashMap<>();
            Map<String, Integer> depthOf = new HashMap<>();

            floodFill(startX, startY, parent, moveTaken, coordOf, depthOf);

            int[] exit = pickExitFrom(coordOf, depthOf, robot.width(), robot.height());
            List<Direction> path = buildPath(startX, startY, exit[0], exit[1], parent, moveTaken);
            if (path.isEmpty()) {
                status.setText("No path to exit found.");
                return;
            }

            Queue<Direction> moves = new LinkedList<>(path);
            autoTimeline = new Timeline(new KeyFrame(Duration.millis(30), event -> {
                Direction step = moves.poll();
                if (step == null) {
                    stopAutoIfRunning();
                    status.setText("Auto-solve complete.");
                    PauseTransition pause = new PauseTransition(Duration.millis(600));
                    pause.setOnFinished(ev -> resetToStart());
                    pause.play();
                    return;
                }
                tryMove(step);
            }));
            autoTimeline.setCycleCount(path.size() + 1);
            autoTimeline.play();
        }

        private void stopAutoIfRunning() {
            if (autoTimeline != null) {
                autoTimeline.stop();
                autoTimeline = null;
            }
        }

        private void resetToStart() {
            stopAutoIfRunning();
            robot.moveTo(startX, startY);
            if (robot instanceof CarActor) {
                // clear heading (optional)
                ((CarActor) robot).setHeading(null);
            }
            status.setText(String.format("Reset to start (%d,%d)", startX, startY));
            arena.requestFocus();
        }

        private void floodFill(int sx, int sy, Map<String, String> parent,
                               Map<String, Direction> moveTaken, Map<String, int[]> coordOf,
                               Map<String, Integer> depthOf) {
            Queue<int[]> frontier = new LinkedList<>();
            String start = key(sx, sy);
            frontier.add(new int[]{sx, sy});
            parent.put(start, null);
            coordOf.put(start, new int[]{sx, sy});
            depthOf.put(start, 0);

            while (!frontier.isEmpty()) {
                int[] current = frontier.poll();
                String currentKey = key(current[0], current[1]);
                int currentDepth = depthOf.getOrDefault(currentKey, 0);
                for (Direction direction : Direction.values()) {
                    int nx = current[0] + direction.dx;
                    int ny = current[1] + direction.dy;
                    String nextKey = key(nx, ny);
                    if (parent.containsKey(nextKey)) {
                        continue;
                    }
                    if (!canOccupy(nx, ny)) {
                        continue;
                    }
                    parent.put(nextKey, currentKey);
                    moveTaken.put(nextKey, direction);
                    coordOf.put(nextKey, new int[]{nx, ny});
                    depthOf.put(nextKey, currentDepth + 1);
                    frontier.add(new int[]{nx, ny});
                }
            }
        }

        private int[] pickExitFrom(Map<String, int[]> coordOf, Map<String, Integer> depthOf, int robotWidth, int robotHeight) {
            int imgW = (int) mazeImage.getWidth();
            int imgH = (int) mazeImage.getHeight();
            List<int[]> borderCandidates = new ArrayList<>();

            for (int[] cell : coordOf.values()) {
                int x = cell[0];
                int y = cell[1];
                boolean touchesBorder = x <= 0 || y <= 0 || x + robotWidth >= imgW || y + robotHeight >= imgH;
                if (touchesBorder) {
                    borderCandidates.add(cell);
                }
            }

            if (!borderCandidates.isEmpty()) {
                int bestDepth = Integer.MIN_VALUE;
                int[] best = null;
                int bestTie = Integer.MIN_VALUE;
                for (int[] cell : borderCandidates) {
                    String k = key(cell[0], cell[1]);
                    int d = depthOf.getOrDefault(k, Integer.MIN_VALUE);
                    int tie = cell[0] + cell[1];
                    if (d > bestDepth || (d == bestDepth && tie > bestTie)) {
                        bestDepth = d;
                        bestTie = tie;
                        best = cell;
                    }
                }
                return best;
            }

            // fallback: pick farthest reachable point
            int maxDepth = Integer.MIN_VALUE;
            int[] best = null;
            int bestTie = Integer.MIN_VALUE;
            for (int[] cell : coordOf.values()) {
                String k = key(cell[0], cell[1]);
                int d = depthOf.getOrDefault(k, Integer.MIN_VALUE);
                int tie = cell[0] + cell[1];
                if (d > maxDepth || (d == maxDepth && tie > bestTie)) {
                    maxDepth = d;
                    bestTie = tie;
                    best = cell;
                }
            }
            if (best == null) throw new IllegalStateException("No exit found");
            return best;
        }

        private List<Direction> buildPath(int sx, int sy, int gx, int gy,
                                          Map<String, String> parent, Map<String, Direction> moveTaken) {
            String start = key(sx, sy);
            String goal = key(gx, gy);
            if (!parent.containsKey(goal)) {
                return Collections.emptyList();
            }

            List<Direction> reversed = new ArrayList<>();
            String cursor = goal;
            while (!cursor.equals(start)) {
                reversed.add(moveTaken.get(cursor));
                cursor = parent.get(cursor);
            }
            Collections.reverse(reversed);
            return reversed;
        }

        private int[] findExitPosition(int robotWidth, int robotHeight) {
            int inset = 3;
            int hitWidth = robotWidth - (2 * inset);
            int hitHeight = robotHeight - (2 * inset);
            int imgW = (int) mazeImage.getWidth();
            int imgH = (int) mazeImage.getHeight();

            int y = imgH - hitHeight - inset - 1;
            for (int x = imgW - hitWidth - inset - 1; x >= 0; x--) {
                if (isAreaOpen(x + inset, y + inset, hitWidth, hitHeight)) {
                    return new int[]{(x / STEP) * STEP, (y / STEP) * STEP};
                }
            }

            for (y = imgH - hitHeight - inset - 1; y >= 0; y--) {
                for (int x = imgW - hitWidth - inset - 1; x >= 0; x--) {
                    if (isAreaOpen(x + inset, y + inset, hitWidth, hitHeight)) {
                        return new int[]{(x / STEP) * STEP, (y / STEP) * STEP};
                    }
                }
            }
            throw new IllegalStateException("No exit area found in maze image");
        }

        private List<Direction> findPath(int sx, int sy, int gx, int gy) {
            Queue<int[]> frontier = new LinkedList<>();
            Map<String, String> parent = new HashMap<>();
            Map<String, Direction> moveTaken = new HashMap<>();

            String start = key(sx, sy);
            String goal = key(gx, gy);
            frontier.add(new int[]{sx, sy});
            parent.put(start, null);

            while (!frontier.isEmpty()) {
                int[] current = frontier.poll();
                String currentKey = key(current[0], current[1]);
                if (currentKey.equals(goal)) {
                    break;
                }
                for (Direction direction : Direction.values()) {
                    int nx = current[0] + direction.dx;
                    int ny = current[1] + direction.dy;
                    String nextKey = key(nx, ny);
                    if (parent.containsKey(nextKey)) {
                        continue;
                    }
                    if (!canOccupy(nx, ny)) {
                        continue;
                    }
                    parent.put(nextKey, currentKey);
                    moveTaken.put(nextKey, direction);
                    frontier.add(new int[]{nx, ny});
                }
            }

            if (!parent.containsKey(goal)) {
                return Collections.emptyList();
            }

            List<Direction> reversed = new ArrayList<>();
            String cursor = goal;
            while (!cursor.equals(start)) {
                reversed.add(moveTaken.get(cursor));
                cursor = parent.get(cursor);
            }
            Collections.reverse(reversed);
            return reversed;
        }

        private String key(int x, int y) {
            return x + "," + y;
        }
    }
}