import java.util.*;
import java.util.concurrent.BlockingQueue;


public class Analysis {
    private final BlockingQueue<String> log;
    private final int linesNumber;
    private List<Line> lastLines;
    private boolean running;

    public Analysis(BlockingQueue<String> log, int linesNumber) {
        this.log = log;
        this.linesNumber = linesNumber;
        this.lastLines = new ArrayList<>(linesNumber);
        this.running = true;
    }

    public List<Line> lines() throws Exception {
        if (!running) {
            return null;
        }

        var lines = new Line[linesNumber];
        long end = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < end) {
            var out = log.poll();
            if (out != null) {
                var line = parseLine(out);
                if (line != null && line.number <= linesNumber) {
                    lines[line.number-1] = line;
                }
            }
        }

        var linesList = new ArrayList<Line>();
        for (var line : lines) {
            if (line != null) {
                linesList.add(line);
            }
        }

        if (!linesList.isEmpty()) {
            lastLines = linesList;
        }

        return lastLines;
    }

    private Line parseLine(String out) {
        var parts = out.trim().split(" ");
        if (parts.length < 20 || !out.contains("info")) {
            return null;
        }

        var line = new Line();
        for (int i = 0; i < parts.length; i++) {
            switch (parts[i]) {
                case "depth":
                    line.depth = Integer.parseInt(parts[i+1]);
                    break;
                case "multipv":
                    line.number = Integer.parseInt(parts[i+1]);
                    break;
                case "score":
                    line.score = Float.parseFloat(parts[i+1]);
                    break;
                case "mate":
                    line.mate = Integer.parseInt(parts[i+1]);
                    break;
                case "pv":
                    var end = Math.min(parts.length, i+1 + 12);
                    var moves = Arrays.copyOfRange(parts, i+1, end);
                    line.moves = String.join(" ", moves);
                    break;
            }
        }
        return line;
    }

    public void stop() {
        running = false;
    }
}
