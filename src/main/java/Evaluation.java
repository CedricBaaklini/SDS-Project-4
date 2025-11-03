public record Evaluation(Result result, String reason) {
    @Override
    public String toString() {
        return result + " - " + reason;
    }
}
