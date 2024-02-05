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

import com.tom_roush.pdfbox.cos.COSDictionary;

/**
 * RunLengthDecodeFilter是PDF文件中的一种解码器（Decode）或过滤器（Filter），用于解码使用游程长度编码（Run-length encoding）压缩的数据。
 * <p>
 * 游程长度编码是一种简单的无损数据压缩方法，它将连续的相同数据值（称为游程）替换为该值和游程的长度。例如，字符串"AAAABBBCCD"可以被游程长度编码为"4A3B2C1D"。这种方法在处理具有大量重复数据的图像或声音文件时特别有效。
 * <p>
 * 在PDF文件的语法中，使用RunLengthDecodeFilter的数据流会标记为“/Filter /RunLengthDecode”。例如，一个使用RunLengthDecodeFilter的图像XObject可能会有这样的定义：“/Filter /RunLengthDecode”。
 * <p>
 * 需要注意的是，虽然游程长度编码非常简单，但它的压缩效率通常不如更复杂的压缩算法（如FlateDecode或LZWDecode）。因此，在PDF文件中，RunLengthDecodeFilter通常只用于压缩简单的数据或小的数据块。
 * Decompresses data encoded using a byte-oriented run-length encoding algorithm,
 * reproducing the original text or binary data
 *
 * @author Ben Litchfield
 */
final class RunLengthDecodeFilter extends Filter {
    private static final int RUN_LENGTH_EOD = 128;

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        int dupAmount;
        byte[] buffer = new byte[128];
        while ((dupAmount = encoded.read()) != -1 && dupAmount != RUN_LENGTH_EOD) {
            if (dupAmount <= 127) {
                int amountToCopy = dupAmount + 1;
                int compressedRead;
                while (amountToCopy > 0) {
                    compressedRead = encoded.read(buffer, 0, amountToCopy);
                    // EOF reached?
                    if (compressedRead == -1) {
                        break;
                    }
                    decoded.write(buffer, 0, compressedRead);
                    amountToCopy -= compressedRead;
                }
            } else {
                int dupByte = encoded.read();
                // EOF reached?
                if (dupByte == -1) {
                    break;
                }
                for (int i = 0; i < 257 - dupAmount; i++) {
                    decoded.write(dupByte);
                }
            }
        }
        return new DecodeResult(parameters);
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        Log.w("PdfBox-Android", "RunLengthDecodeFilter.encode is not implemented yet, skipping this stream.");
    }
}
