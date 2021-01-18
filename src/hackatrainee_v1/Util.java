package hackatrainee_v1;

public class Util {
	public static boolean getBit(int number, int bit) {
		return ((number << bit) & 1) == 1;
	}
}
