import java.util.List;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("specify engine filename in first param and fen in second dude");
            System.err.println("use third parameter to specify pv number");
            System.exit(1);
        }

        var pvs = 1;
        if (args.length > 2) {
            pvs = Integer.parseInt(args[3]);
        }

        try (var engine = new Engine(args[0])) {
            engine.run();
            engine.think(args[1], pvs);
            List<Line> lines;
            while((lines = engine.lines()) != null) {
                var sb = new StringBuilder();
                for (var line : lines) {
                    sb.append(line);
                    if (pvs > 1 ){
                        sb.append("\n");
                    }
                }
                System.out.printf("\r%s", sb);
            }
            engine.stop();
        } catch (Exception ex) {
            System.err.printf("oops, shit happens: %s\n", ex.getMessage());
            ex.printStackTrace();
        }
    }
}