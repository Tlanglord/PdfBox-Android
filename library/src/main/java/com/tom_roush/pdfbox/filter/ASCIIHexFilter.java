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
import com.tom_roush.pdfbox.util.Hex;

/**
 * ASCII hexadecimal，或称为ASCII十六进制编码，是一种将二进制数据转换为ASCII字符的编码方法。在这种编码方法中，每个字节的二进制数据被转换为两个十六进制字符。
 * <p>
 * 十六进制是一种基数为16的数制，使用0-9和A-F（或a-f）共16个字符来表示数值。在ASCII十六进制编码中，每个字节的高4位和低4位分别对应一个十六进制字符。例如，二进制数据“10100011”会被转换为十六进制的“A3”。
 * <p>
 * ASCII十六进制编码的优点是可读性好，可以直观地看出原始的二进制数据。缺点是编码后的数据量较大，每个字节的数据需要两个字符来表示，数据量是原始二进制数据的两倍。
 * <p>
 * 在PDF文件中，ASCII十六进制编码常用于对二进制流进行编码，例如图像数据和嵌入的字体文件。在PDF的语法中，十六进制字符串用尖括号（<和>）括起来，例如“<901FA3>”
 * Decodes data encoded in an ASCII hexadecimal form, reproducing the original binary data.
 *
 * @author Ben Litchfield
 */
final class ASCIIHexFilter extends Filter {
    private static final int[] REVERSE_HEX = {
            /*   0 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  10 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  20 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  30 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  40 */  -1, -1, -1, -1, -1, -1, -1, -1, 0, 1,
            /*  50 */   2, 3, 4, 5, 6, 7, 8, 9, -1, -1,
            /*  60 */  -1, -1, -1, -1, -1, 10, 11, 12, 13, 14,
            /*  70 */  15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  80 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /*  90 */  -1, -1, -1, -1, -1, -1, -1, 10, 11, 12,
            /* 100 */  13, 14, 15, -1, -1, -1, -1, -1, -1, -1,
            /* 110 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 120 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 130 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 140 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 150 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 160 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 170 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 180 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 190 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 200 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 210 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 220 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 230 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 240 */  -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            /* 250 */  -1, -1, -1, -1, -1, -1
    };

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        int value, firstByte, secondByte;
        while ((firstByte = encoded.read()) != -1) {
            // always after first char
            while (isWhitespace(firstByte)) {
                firstByte = encoded.read();
            }
            if (firstByte == -1 || isEOD(firstByte)) {
                break;
            }

            if (REVERSE_HEX[firstByte] == -1) {
                Log.e("PdfBox-Android", "Invalid hex, int: " + firstByte + " char: " + (char) firstByte);
            }
            value = REVERSE_HEX[firstByte] * 16;
            secondByte = encoded.read();

            if (secondByte == -1 || isEOD(secondByte)) {
                // second value behaves like 0 in case of EOD
                decoded.write(value);
                break;
            }
            if (REVERSE_HEX[secondByte] == -1) {
                Log.e("PdfBox-Android", "Invalid hex, int: " + secondByte + " char: " + (char) secondByte);
            }
            value += REVERSE_HEX[secondByte];
            decoded.write(value);
        }
        decoded.flush();
        return new DecodeResult(parameters);
    }

    // whitespace
    //   0  0x00  Null (NUL)
    //   9  0x09  Tab (HT)
    //  10  0x0A  Line feed (LF)
    //  12  0x0C  Form feed (FF)
    //  13  0x0D  Carriage return (CR)
    //  32  0x20  Space (SP)
    private boolean isWhitespace(int c) {
        return c == 0 || c == 9 || c == 10 || c == 12 || c == 13 || c == 32;
    }

    private boolean isEOD(int c) {
        return c == '>';
    }

    @Override
    public void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        int byteRead;
        while ((byteRead = input.read()) != -1) {
            Hex.writeHexByte((byte) byteRead, encoded);
        }
        encoded.flush();
    }
}
