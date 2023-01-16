// #include <inttypes.h>
typedef long long __int64;
#include"GOSTD_native.h"
#include<stdlib.h>
#include<string.h>

JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_gostd
  (JNIEnv * env, jobject sObject, jstring cmd) {
	puts("[DEBUG GOSTD] GET BYTEARRAY FROM STRING");
	const char *inCStr = (*env)->GetStringUTFChars(env, cmd, NULL);
	if (NULL == inCStr) return NULL;
	char output[32]; // 32 size of hash string. 
	gostd(output, inCStr, strlen(inCStr));
	jbyteArray ret = (*env)->NewByteArray(env,sizeof(output));
	(*env)->SetByteArrayRegion(env, ret, 0, sizeof(output), output);
	return ret;	
}
JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_gostdFromBytes
  (JNIEnv * env, jobject obj, jbyteArray iArr) {
	puts("[DEBUG GOSTD] GET BYTEARRAY FROM BYTEARRAY");
	jsize len  = (*env)->GetArrayLength(env,iArr); 
	unsigned char * buf = malloc( sizeof(unsigned char) * len );
	(*env)->GetByteArrayRegion (env, iArr, 0, len, (jbyte*)buf);
	char output[32];
	gostd(output, buf, len);
	free(buf);
	jbyteArray ret = (*env)->NewByteArray(env,sizeof(output));
	(*env)->SetByteArrayRegion(env, ret, 0, sizeof(output), output);
	return ret;
}

//
// get digest as char*
// get jBuf as char*
// do hash
// create jByteArray for return
// set byteArray
// free buf and buf digest. we also can to try without malloc. just char arr[..]
#define DOHASH(func) { \
	\
	jsize len_digest  = (*env)->GetArrayLength(env,digest); \
	unsigned char * buf_digest = malloc( sizeof(unsigned char) * len_digest  );\
	(*env)->GetByteArrayRegion (env, digest, 0, digest, (jbyte*)buf_digest );\
	\
	unsigned char * buf = malloc( sizeof(unsigned char) * len );\
	(*env)->GetByteArrayRegion (env, jbuf, 0, len, (jbyte*)buf);\
	\
	func(buf, len*8, buf_digest);\
	\
	jbyteArray ret = (*env)->NewByteArray(env, len*8);\
	\
	(*env)->SetByteArrayRegion(env, ret, 0, len*8, buf);\
	\
	free(buf); free(buf_digest);\
	return ret;\
}

JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_GOSTR3411_12012_1256
  (JNIEnv * env, jobject obj, jbyteArray jbuf, jint len, jbyteArray digest) {
	puts("[DEBUG GOSTD] HASH_256 (we can to use sph_gost instead)");
	DOHASH(hash_256);
}

JNIEXPORT jbyteArray JNICALL Java_GOSTD_1native_GOSTR3411_12012_1512
  (JNIEnv * env, jobject obj, jbyteArray jbuf, jint len, jbyteArray digest) {
	puts("[DEBUG GOSTD] HASH_512 (we can to use sph_gost instead)");
	DOHASH(hash_512);
}