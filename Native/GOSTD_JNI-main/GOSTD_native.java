//import static System.out;
public class GOSTD_native {
static {
    System.loadLibrary("gostd_java");
}

    public static void main(String[] args) {
	String full = "";
	for (String arg : args) {
		full+=arg + " ";
	}
	byte[] bytes = new GOSTD_native().gostd(full);
	System.out.println();
	for(int idx =0; idx < bytes.length; idx++) {
		System.out.print(bytes[idx]);
		if(idx +1 != bytes.length) System.out.print(",");
	}
	System.out.println();
	System.out.println("test byte[]");
	byte[] bytes1 = new GOSTD_native().gostdFromBytes(new byte[] {1,2,3} );
	for(int idx =0; idx < bytes1.length; idx++) {
		System.out.print(bytes1[idx]);
		if(idx +1 != bytes1.length) System.out.print(",");
	}
	System.out.println();
    }
  //native method with no body
  public native byte[] gostd(String input); // like works
  public native byte[] gostdFromBytes(byte[] input); // like works

  public native byte[] GOSTR3411_2012_256 (byte[] buf, int len, byte[] digest); // not tested yet
  public native byte[] GOSTR3411_2012_512 (byte[] buf, int len, byte[] digest); // not tested yet

}