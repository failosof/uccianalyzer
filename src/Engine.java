import java.io.*;
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
    private Analysis currentAnalysis;

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

    public Analysis think(String position, int linesNum) throws Exception{
        assert process != null : "engine is not run";
        assert state == State.IDLE : "engine is already thinking";
        assert 0 < linesNum && linesNum < 500 : "bad number of lines";

        state = State.THINKING;
        cmd("setoption", "MultiPV", String.valueOf(linesNum));
        cmd("position", "fen", position); // set fen position
        cmd("go", "depth", "infinite"); // run engine thinking
        currentAnalysis = new Analysis(log, linesNum);
        return currentAnalysis;
    }

    public void stop() throws Exception{
        assert process != null : "engine is not run";

        if (state == State.THINKING) {
            cmd("stop"); // interrupt thinking
            currentAnalysis.stop();
        }
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
}
