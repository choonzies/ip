package tasks;

public class ToDoTask extends Task {
    private static final String SYMBOL = "T";

    public ToDoTask(String description) {
        super(description);
    }

    @Override
    public String toString() {
        return String.format("[%s][%s] %s", this.SYMBOL, super.getStatusIcon(), super.description);
    }
}
