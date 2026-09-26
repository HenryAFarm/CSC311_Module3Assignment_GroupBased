package org.example.csc311_module3assignment_groupbased;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.animation.AnimationTimer;

import java.util.*;


public class RobotMazeScreen {

    private static final double MOVE_DISTANCE = 5;

    public static Pane createContent() {

        Image mazeImage = new Image(
                RobotMazeScreen.class.getResourceAsStream("/images/maze.png")
        );

        Image robotImage = new Image(
                RobotMazeScreen.class.getResourceAsStream("/images/robot.png")
        );

        ImageView maze = new ImageView(mazeImage);
        ImageView robot = new ImageView(robotImage);

        // Starting position
        robot.setX(15);
        robot.setY(260);

        PixelReader pixels = mazeImage.getPixelReader();

        Pane root = new Pane();
        root.getChildren().addAll(maze, robot);

        root.setFocusTraversable(true);

        root.setOnKeyPressed(event -> {

            double newX = robot.getX();
            double newY = robot.getY();

            if (event.getCode() == KeyCode.LEFT) {
                newX -= MOVE_DISTANCE;
            }
            else if (event.getCode() == KeyCode.RIGHT) {
                newX += MOVE_DISTANCE;
            }
            else if (event.getCode() == KeyCode.UP) {
                newY -= MOVE_DISTANCE;
            }
            else if (event.getCode() == KeyCode.DOWN) {
                newY += MOVE_DISTANCE;
            }
            else {
                return;
            }

            if (canMove(robot, newX, newY, pixels, mazeImage)) {
                robot.setX(newX);
                robot.setY(newY);
            }
        });

        List<double[]> path = findPath(robot, pixels, mazeImage);

        // robot moves
        AnimationTimer timer = new AnimationTimer() {

            private int index = 0;
            private long lastMove = 0;

            @Override
            public void handle(long now) {

                if (index >= path.size()) {
                    stop();
                    return;
                }

                // Move 50 milli sec
                if (now - lastMove < 50_000_000) {
                    return;
                }

                double[] position = path.get(index);

                robot.setX(position[0]);
                robot.setY(position[1]);

                index++;
                lastMove = now;
            }
        };

        timer.start();


        root.requestFocus();


        return root;
    }

    private static boolean canMove(
            ImageView robot,
            double x,
            double y,
            PixelReader pixels,
            Image mazeImage) {

        double left = x + 1;
        double right = x + robot.getImage().getWidth() - 1;
        double top = y + 1;
        double bottom = y + robot.getImage().getHeight() - 1;

        return isPath(left, top, pixels, mazeImage)
                && isPath(right, top, pixels, mazeImage)
                && isPath(left, bottom, pixels, mazeImage)
                && isPath(right, bottom, pixels, mazeImage);
    }

    private static boolean isPath(
            double x,
            double y,
            PixelReader pixels,
            Image mazeImage) {

        int pixelX = (int) x;
        int pixelY = (int) y;

        if (pixelX < 0 || pixelY < 0
                || pixelX >= mazeImage.getWidth()
                || pixelY >= mazeImage.getHeight()) {

            return false;
        }

        return pixels.getColor(pixelX, pixelY).getBrightness() > 0.8;
    }

    private static List<double[]> findPath(
            ImageView robot,
            PixelReader pixels,
            Image mazeImage) {

        Queue<double[]> queue = new LinkedList<>();
        Map<String, String> previous = new HashMap<>();
        Map<String, double[]> positions = new HashMap<>();

        double startX = robot.getX();
        double startY = robot.getY();

        String start = startX + "," + startY;

        queue.add(new double[]{startX, startY});
        positions.put(start, new double[]{startX, startY});

        double[] directions = {
                MOVE_DISTANCE, 0,
                -MOVE_DISTANCE, 0,
                0, MOVE_DISTANCE,
                0, -MOVE_DISTANCE
        };

        String end = null;

        while (!queue.isEmpty()) {

            double[] current = queue.remove();

            double x = current[0];
            double y = current[1];

            // Right side of maze = exit
            if (x > mazeImage.getWidth() - 50) {
                end = x + "," + y;
                break;
            }

            for (int i = 0; i < directions.length; i += 2) {

                double newX = x + directions[i];
                double newY = y + directions[i + 1];

                if (!canMove(robot, newX, newY, pixels, mazeImage)) {
                    continue;
                }

                String key = newX + "," + newY;

                if (positions.containsKey(key)) {
                    continue;
                }

                positions.put(key, new double[]{newX, newY});
                previous.put(key, x + "," + y);

                queue.add(new double[]{newX, newY});
            }
        }

        List<double[]> path = new ArrayList<>();

        if (end == null) {
            return path;
        }

        String current = end;

        while (!current.equals(start)) {

            path.add(positions.get(current));
            current = previous.get(current);
        }

        Collections.reverse(path);

        return path;
    }

}
