/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class GOSTD_native */

#ifndef _Included_GOSTD_native
#define _Included_GOSTD_native
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     GOSTD_native
 * Method:    gostd
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_gostd
  (JNIEnv *, jobject, jstring);

/*
 * Class:     GOSTD_native
 * Method:    gostdFromBytes
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_gostdFromBytes
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     GOSTD_native
 * Method:    GOSTR3411_2012_256
 * Signature: ([BI[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_GOSTR3411_12012_1256
  (JNIEnv *, jobject, jbyteArray, jint, jbyteArray);

/*
 * Class:     GOSTD_native
 * Method:    GOSTR3411_2012_512
 * Signature: ([BI[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_GOSTR3411_12012_1512
  (JNIEnv *, jobject, jbyteArray, jint, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif