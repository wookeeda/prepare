public class Call {

    int id;
    int timestamp;
    int start;
    int end;
    String direction;

    public void setStart(int start) {
        this.start = start;
        setDirection();
    }

    public void setEnd(int end) {
        this.end = end;
        setDirection();
    }

    public void setDirection() {
        if (this.start != 0 && this.end != 0) {
            if (this.start > this.end) {
                direction = "DOWN";
            } else {
                direction = "UP";
            }
        }
    }

    public int getStart() {
        return start;
    }
}
