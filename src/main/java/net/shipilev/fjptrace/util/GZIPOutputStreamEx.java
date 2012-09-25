package net.shipilev.fjptrace.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

public class GZIPOutputStreamEx extends GZIPOutputStream {
    public GZIPOutputStreamEx(OutputStream out) throws IOException {
        super(out, 16*1024*1024);
        def.setLevel(Deflater.BEST_SPEED);
    }
}
