import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BattleShip {

    private static void printUsage(String msg) {
        System.out.println(msg);
        System.out.println("Usage: ");
        System.out.println("java Test <fileName>");
        System.out.println("Leave <fileName> blank to read from stdin [Enter 'EOD' for ending the inputs]");
        System.out.println("Input:\n" +
                "The first line of the input contains the width and height of the battle area respectively.\n" +
                "The second line of the input contains the number of battleships that each player gets.\n" +
                "The third line of the input contains the type of the battleship, its dimensions (width and height) and coordinates\n" +
                "for Player-1.\n" +
                "The fourth line of the input contains the type of the battleship, its dimensions (width and height) and coordinates\n" +
                "for Player-2.\n" +
                "The fifth line contains the sequence of the target locations of missiles fired by Player-1.\n" +
                "The sixth line contains the sequence of the target locations of missiles fired by Player-2.");
        System.out.println("5 E\n" +
                "2\n" +
                "Q 1 1 A1 B2\n" +
                "P 2 1 D4 C3\n" +
                "A1 B2 B2 B3\n" +
                "A1 B2 B3 A1 D1 E1 D4 D4 D5 D5");
        System.exit(1);
    }

    private static class Player {
        String name;
        List<List<Ship>> arena;
        private final Set<Ship> ships = new HashSet<>();
        List<Coordinate> bullets = new ArrayList<>();

        Player(String name, int arenaX, int arenaY) {
            this.name = name;
            this.arena = new ArrayList<>(arenaY);
            for (int j = 0; j < arenaY; j++) {
                List<Ship> row = new ArrayList<>(arenaX);
                for (int i = 0; i < arenaX; i++) {
                    row.add(i, null);
                }
                this.arena.add(row);
            }
        }

        void addToList(Ship ship) {
            this.ships.add(ship);
            for (int j = ship.start.y; j <= ship.end.y; j++) {
                List<Ship> row = arena.get(j);
                for (int i=ship.start.x; i <= ship.end.x; i++) {
                    row.set(i, ship);
                }
            }
        }

        void removeFromList(Ship ship, Coordinate bullet) {
            if (arena.get(bullet.y).get(bullet.x) != null) {
                arena.get(bullet.y).set(bullet.x, null);
                ship.size--;
            }
            if (0 >= ship.size) {
                this.ships.remove(ship);
            }
        }

        void printArena() {
            for (List<Ship> row: arena) {
                for (Ship ship: row) {
                    if (ship == null) {
                        System.out.print(0);
                    } else {
                        System.out.print(ship.strength);
                    }
                }
                System.out.println();
            }
            System.out.println();
        }
    }

    private static class Coordinate {
        int x;
        int y;

        public Coordinate(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class Ship {
        Coordinate start;
        Coordinate end;
        int strength = 1;
        int size;
        Ship(Coordinate start, Coordinate end) {
            this.start = start;
            this.end = end;
            this.size = (end.y - start.y + 1) * (end.x - start.x + 1);
        }

        Ship(Coordinate start, Coordinate end, int strength) {
            this.start = start;
            this.end = end;
            this.strength = strength;
            this.size = (end.y - start.y + 1) * (end.x - start.x + 1);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }

        @Override
        public boolean equals(Object obj) {
            Ship ship = (Ship) obj;
            return this.start.x == ship.start.x && this.start.y == ship.start.y && this.end.x == ship.end.x && this.end.y == ship.end.y;
        }
    }

    private static int validateX(String[] sizes) {
        try {
            return Integer.valueOf(sizes[0]);
        } catch (NumberFormatException e) {
            printUsage(e.getMessage() + " invalid format given for the grid size x");
        }
        return -1;
    }

    private static int validateNumberOfShips(List<String> lines) {
        try {
            return Integer.valueOf(lines.get(1).split(" ")[0]);
        } catch (NumberFormatException e) {
            printUsage(e.getMessage() + " invalid format given for the number of ships");
        }
        return -1;
    }

    private static String[] getSizes(List<String> lines) {
        String[] sizes = lines.get(0).split(" ");
        if (sizes.length < 2) {
            printUsage("Invalid input for the grid sizes (line1)");
        }
        return sizes;
    }

    private static List<String> readInputLines(String[] args) {
        List<String> lines = new ArrayList<>();
        try {
            if (args.length < 1 || "stdin".equals(args[0])) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("EOD") || line.startsWith("eod")) {
                            break;
                        }
                        lines.add(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Stream<String> stream = Files.lines(Paths.get(args[0]));
                lines = stream.collect(Collectors.toList());
            }
        } catch (IOException e) {
            e.printStackTrace();
            printUsage(e.getMessage());
        }
        return lines;
    }

    private static boolean printHitOrMiss(int i, Player smaller, Player larger) {
        if (i >= smaller.bullets.size()) {
            System.out.println("Player-" + smaller.name + " has no more missiles left to launch");
            return false;
        }
        Coordinate bullet = smaller.bullets.get(i);
        String output = "Player-" + smaller.name + " fires a missile with target " + getTupleFromCoordinate(bullet);
        if (hits(larger, bullet)) {
            System.out.println(output + " which got hit");
            return true;
        } else {
            System.out.println(output + " which got miss");
        }
        return false;
    }

    private static boolean hits(Player player, Coordinate bullet) {
        Ship ship;
        if ((ship = player.arena.get(bullet.y).get(bullet.x)) != null) {
            if (ship.strength >= 2) {
                ship.strength--;
            } else {
                player.removeFromList(ship, bullet);
            }
            return true;
        }
        return false;
    }

    private static void addBulletsToPlayer(Player player, String[] bullets) {
        for (int i = 0; i < bullets.length; i++) {
            player.bullets.add(getCoordinateFromTuple(bullets[i]));
        }
    }

    private static Coordinate getCoordinateFromTuple(String part) {
        return new Coordinate(part.charAt(1) - '0' - 1, getCoordinate(part));
    }

    private static String getTupleFromCoordinate(Coordinate coordinate) {
        char y = (char) ('A' + coordinate.y);
        return Character.toString(y) + (coordinate.x + 1);
    }

    private static void printShips(Set<Ship> pShipsA) {
        for (Ship ship : pShipsA) {
            System.out.println("(" + ship.start.x + ", " + ship.start.y + "), (" + ship.end.x + ", " + ship.end.y + ")");
        }
        System.out.println();
    }

    private static int getSize(String s) {
        return getCoordinate(s) + 1;
    }

    private static int getCoordinate(String s) {
        char ch = s.charAt(0);
        return Character.isLowerCase(ch) ? ch - 'a' : ch - 'A';
    }

    public static void main(String[] args) {
        List<String> lines = readInputLines(args);
        String[] sizes = getSizes(lines);
        int x = validateX(sizes);
        int y = getSize(lines.get(0).split(" ")[1]);
        int nships = validateNumberOfShips(lines);
        Player playerA = new Player("1", x, y);
        Player playerB = new Player("2", x, y);
        int i;
        for (i = 2; i < nships + 2; i++) {
            if (i >= lines.size()) {
                printUsage("Not enough lines to read!");
            }
            String[] parts = lines.get(i).split(" ");
            if (parts.length < 5) {
                printUsage("Less than 5 parts on line " + (i + 1));
            }
            String shiptype = parts[0];
            int sizex = Integer.valueOf(parts[1]);
            int sizey = Integer.valueOf(parts[2]);
            Coordinate aStart = getCoordinateFromTuple(parts[3]);
            Coordinate aEnd = new Coordinate(aStart.x + sizex - 1, aStart.y + sizey - 1);
            Coordinate bStart = getCoordinateFromTuple(parts[4]);
            Coordinate bEnd = new Coordinate(bStart.x + sizex - 1, bStart.y + sizey - 1);
            if (shiptype.contains("Q") || shiptype.contains("q")) {
                playerA.addToList(new Ship(aStart, aEnd, 2));
                playerB.addToList(new Ship(bStart, bEnd, 2));
            } else if (shiptype.contains("P") || shiptype.contains("p")) {
                playerA.addToList(new Ship(aStart, aEnd));
                playerB.addToList(new Ship(bStart, bEnd));
            }
        }
        System.out.println("PlayerA:\n");
        playerA.printArena();
        System.out.println("PlayerB:\n");
        playerB.printArena();
        addBulletsToPlayer(playerA, lines.get(i).split(" "));
        addBulletsToPlayer(playerB, lines.get(i + 1).split(" "));
//        System.out.println("x: " + x + " y: " + y + " nships: " + nships);
//        printShips(playerA.ships);
//        printShips(playerB.ships);
        i = 0;
        int j = 0;
        while (i < playerA.bullets.size() || j < playerB.bullets.size()) {
            checkForWinner(playerA, playerB);
            while (printHitOrMiss(i++, playerA, playerB)) {
                checkForWinner(playerA, playerB);
            }
            while (printHitOrMiss(j++, playerB, playerA)) {
                checkForWinner(playerA, playerB);
            }
        }
        System.out.println("PlayerA:\n");
        playerA.printArena();
        System.out.println("PlayerB:\n");
        playerB.printArena();
        System.out.println("PlayerA Ships:\n");
        printShips(playerA.ships);
        System.out.println("PlayerB Ships:\n");
        printShips(playerB.ships);
        if (playerA.ships.size() > 0 && playerB.ships.size() > 0) {
            System.out.println("The game ends in a draw!");
            System.exit(0);
        }
        if (playerA.ships.size() > 0) {
            System.out.println("Player-" + playerA.name + " won the battle");
            System.exit(0);
        }
        if (playerB.ships.size() > 0) {
            System.out.println("Player-" + playerB.name + " won the battle");
            System.exit(0);
        }
    }

    private static void checkForWinner(Player playerA, Player playerB) {
        if (playerA.ships.size() < 1) {
            String winner = playerB.name;
            System.out.println("Player-" + winner + " won the battle");
            System.exit(0);
        }
        if (playerB.ships.size() < 1) {
            String winner = playerB.name;
            System.out.println("Player-" + winner + " won the battle");
            System.exit(0);
        }
    }
}
