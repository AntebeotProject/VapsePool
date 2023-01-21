set JAVA_HOME=C:\Program Files\Java\jdk-17.0.5
REM i686-w64-mingw32-gcc
set CC=x86_64-w64-mingw32-gcc
%CC% -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -shared -o gostd_java.dll GOSTD_native.c gost.c
"%JAVA_HOME%\bin\javac" GOSTD_native.java
"%JAVA_HOME%\bin\java" GOSTD_native hewwo