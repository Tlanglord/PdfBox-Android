/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tom_roush.pdfbox.filter;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.io.IOUtils;

/**
 * Flate，全称FlateDecode，是一种在PDF文件中广泛使用的无损压缩算法。它基于DEFLATE算法，DEFLATE算法是由LZ77洗牌和哈夫曼编码（Huffman coding）两种算法组合而成的。
 * <p>
 * Flate压缩算法在PDF文件中的应用非常广泛，可以用于压缩页面描述、图像、字体和其他类型的数据。由于它是无损的，所以压缩后的数据可以完全恢复到原始的状态。
 * <p>
 * Flate压缩算法的效率非常高，可以提供良好的压缩比，同时解压缩的速度也非常快。这使得它成为PDF文件中最常用的压缩算法之一。
 * <p>
 * 在PDF文件的语法中，使用Flate压缩的数据流会标记为“/FlateDecode”。例如，一个使用Flate压缩的图像XObject可能会有这样的定义：“/Filter /FlateDecode”。
 * Decompresses data encoded using the zlib/deflate compression method,
 * reproducing the original text or binary data.
 *
 * @author Ben Litchfield
 * @author Marcel Kammer
 */
final class FlateFilter extends Filter {
    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        final COSDictionary decodeParams = getDecodeParams(parameters, index);

        try {
            decompress(encoded, Predictor.wrapPredictor(decoded, decodeParams));
        } catch (DataFormatException e) {
            // if the stream is corrupt a DataFormatException may occur
            Log.e("PdfBox-Android", "FlateFilter: stop reading corrupt stream due to a DataFormatException");

            // re-throw the exception
            throw new IOException(e);
        }
        return new DecodeResult(parameters);
    }

    // Use Inflater instead of InflateInputStream to avoid an EOFException due to a probably
    // missing Z_STREAM_END, see PDFBOX-1232 for details
    private void decompress(InputStream in, OutputStream out) throws IOException, DataFormatException {
        byte[] buf = new byte[2048];
        // skip zlib header
        in.read();
        in.read();
        int read = in.read(buf);
        if (read > 0) {
            // use nowrap mode to bypass zlib-header and checksum to avoid a DataFormatException
            Inflater inflater = new Inflater(true);
            inflater.setInput(buf, 0, read);
            byte[] res = new byte[1024];
            boolean dataWritten = false;
            try {
                while (true) {
                    int resRead = 0;
                    try {
                        resRead = inflater.inflate(res);
                    } catch (DataFormatException exception) {
                        if (dataWritten) {
                            // some data could be read -> don't throw an exception
                            Log.w("PdfBox-Android", "FlateFilter: premature end of stream due to a DataFormatException");
                            break;
                        } else {
                            // nothing could be read -> re-throw exception
                            throw exception;
                        }
                    }
                    if (resRead != 0) {
                        out.write(res, 0, resRead);
                        dataWritten = true;
                        continue;
                    }
                    if (inflater.finished() || inflater.needsDictionary() || in.available() == 0) {
                        break;
                    }
                    read = in.read(buf);
                    inflater.setInput(buf, 0, read);
                }
            } finally {
                inflater.end();
            }
        }
        out.flush();
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        int compressionLevel = getCompressionLevel();
        Deflater deflater = new Deflater(compressionLevel);
        DeflaterOutputStream out = new DeflaterOutputStream(encoded, deflater);
        IOUtils.copy(input, out);
        out.close();
        encoded.flush();
        deflater.end();
    }
}
