import java.io.File;
import java.nio.file.Path;

public class GOSTD_native {

    // if Java Win
    static {
        // If (is linux the.....)
        final String FullPathToNativeLibrary = new File("./Native/gostd_java.dll").getAbsolutePath();
        System.out.println("[INFO GOSTD_NATIVE] LOAD LIBRARY " + FullPathToNativeLibrary);
        System.load(FullPathToNativeLibrary);
    }
    public native byte[] gostd(String input);
    public native byte[] gostdFromBytes(byte[] input);
    // File(".${File.separator}Native${File.separator}gostd_java.dll").absolutePath.let { System.load(it) }
    // val test1:byte[] = gostdFromBytes(byteArrayOf(1,2,3))
    // println(test1)
    // val test = gostd("test")
    // println(test)
    public native byte[] GOSTR3411_2012_256(byte[] bytes, int i, byte[] bytes1); // not tested
    public native byte[] GOSTR3411_2012_512(byte[] bytes, int i, byte[] bytes1); // not tested
}
