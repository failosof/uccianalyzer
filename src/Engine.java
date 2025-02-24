import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class Engine implements Closeable {
    private final String filename;
    private Process process;
    private State state;
    private BufferedWriter input;
    private BufferedReader output;
    private Thread outputReadingThread;
    private final BlockingQueue<String> log;
    private List<Line> lastLines;
    private int linesNumber;

    private enum State {
        INIT,
        IDLE,
        THINKING,
    }

    public Engine(String filename) throws Exception {
        var file = new File(filename);
        if (!file.exists() || file.isDirectory()) {
            throw new Exception("engine program not found");
        }

        if (!file.canExecute()) {
            throw new Exception("engine program is not executable");
        }

        this.filename = filename;
        this.state = State.INIT;
        log = new SynchronousQueue<>();
    }

    public void run() throws Exception {
        assert process == null : "engine is already ran";
        assert state == State.INIT : "egnine is already initialized";

        process = Runtime.getRuntime().exec(filename);

        // wtf this names lol, java moment
        input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        output = new BufferedReader(new InputStreamReader(process.getInputStream()));

        outputReadingThread = Thread.startVirtualThread(this::readOutput);

        // specify the protocol to the engine and check if boot successful
        cmd("ucci");
        if (!hasOutput("ucciok")) {
            throw new Exception("engine boot error");
        }

        // check engine is ready and can receive commands
        cmd("isready");
        if (!hasOutput("readyok")) {
            throw new Exception("engine is misbehaving");
        }

        state = State.IDLE;
    }

    public void close() {
        try {
            try {
                outputReadingThread.interrupt();
            } catch (Exception ignored) {}

            log.clear();
            cmd("quit"); // exit from an engine
            input.close();
            output.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            process.destroy();
        }
    }

    public void think(String position, int linesNum) throws Exception{
        assert process != null : "engine is not run";
        assert state == State.IDLE : "engine is already thinking";
        assert 0 < linesNum && linesNum < 500 : "bad number of lines";

        state = State.THINKING;
        cmd("setoption", "MultiPV", String.valueOf(linesNum));
        cmd("position", "fen", position); // set fen position
        cmd("go", "depth", "infinite"); // run engine thinking
        this.linesNumber = linesNum;
    }

    public void stop() throws Exception{
        assert process != null : "engine is not run";

        if (state == State.THINKING) {
            cmd("stop"); // interrupt thinking
            state = State.IDLE;
        }
    }

    public List<Line> lines() throws Exception {
        if (state != State.THINKING) {
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

    private void cmd(String ...command) throws Exception{
        input.write(String.join(" ", command));
        input.write('\n');
        input.flush();
    }

    private void readOutput() {
        try {
            String line;
            while ((line = output.readLine()) != null) {
                log.put(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean hasOutput(String val) throws Exception {
        String out;
        while ((out = log.poll(1, TimeUnit.SECONDS)) != null) {
            if (out.equals(val)) {
                return true;
            }
        }
        return false;
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
}
