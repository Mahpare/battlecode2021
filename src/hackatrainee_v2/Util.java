package hackatrainee_v2;

public class Util {
	public static boolean getBit(int number, int bit) {
		return ((number >> (23 - bit)) & 1) == 1;
	}
}
