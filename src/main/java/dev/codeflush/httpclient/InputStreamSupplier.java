package dev.codeflush.httpclient;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamSupplier {

    InputStream getInputStream() throws IOException;
}
