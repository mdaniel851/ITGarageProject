/*
 * Timer is a simple class made for logging times and returning the differences.
 */

package servers;

public class Timer {
	private long lastTime;
	private long timeOut;

	public Timer() {
		timeOut = 30000L;
		lastTime = System.currentTimeMillis();
	}

	public Timer(int t) {
		timeOut = (t * 1000);
		lastTime = System.currentTimeMillis();
	}

	public void start() {
		lastTime = System.currentTimeMillis();
	}

	public int elapsedTime() {
		return (int) ((System.currentTimeMillis() - lastTime) / 1000L);
	}

	public boolean waitedLongEnough() {
		return System.currentTimeMillis() - lastTime > timeOut;
	}
}
