package engine;

/**
 * Implements a high score record.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class Score implements Comparable<Score> {

	/** Player's name. */
	private String name;
	/** Score points. */
	private int score;
	private String grade;
    private long time;
	
	private static final long S_RANK_TIME = 60000;
    private static final long A_RANK_TIME = 120000;
    private static final long B_RANK_TIME = 180000;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Player name, three letters.
	 * @param score
	 *            Player score.
	 */
	public Score(final String name, final int score) {
		this.name = name;
		this.score = score;
	}

	public Score(final String name, final long time) {
        this.name = name;
        this.time = time;
        setScoreAndGrade();
    }

	private void setScoreAndGrade() {
        if (this.time <= S_RANK_TIME) {
            this.grade = "S";
            this.score = 10000;
        } else if (this.time <= A_RANK_TIME) {
            this.grade = "A";
            this.score = 7500;
        } else if (this.time <= B_RANK_TIME) {
            this.grade = "B";
            this.score = 5000;
        } else {
            this.grade = "C";
            this.score = 2500;
        }
    }
	/**
	 * Getter for the player's name.
	 * 
	 * @return Name of the player.
	 */
	public final String getName() {
		return this.name;
	}

	/**
	 * Getter for the player's score.
	 * 
	 * @return High score.
	 */
	public final int getScore() {
		return this.score;
	}

	public final String getGrade() {
        return this.grade;
    }

    public final long getTime() {
        return this.time;
    }
	/**
	 * Orders the scores descending by score.
	 * 
	 * @param score
	 *            Score to compare the current one with.
	 * @return Comparison between the two scores. Positive if the current one is
	 *         smaller, positive if its bigger, zero if its the same.
	 */
	public final int compareTo(final Score other) {
        return Integer.compare(other.getScore(), this.score);
    }
	
	@Override
	public final int compareTo(final Score score) {
		int comparison = this.score < score.getScore() ? 1 : this.score > score
				.getScore() ? -1 : 0;
		return comparison;
	}

}
