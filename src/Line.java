public class Line {
    public int number;
    public int depth;
    public int mate;
    public float score;
    public String moves;

    public String toString() {
        var sb = new StringBuilder();
        sb.append(number);
        sb.append(". ");
        sb.append("depth: ");
        sb.append(depth);
        if (mate > 0 ) {
            sb.append("#");
            sb.append(mate);
        } else {
            sb.append(" ( ");
            sb.append(score); // xxx idk how to convert
            sb.append(" )");
        }
        sb.append(": ");
        sb.append(moves);
        return sb.toString();
    }
}
