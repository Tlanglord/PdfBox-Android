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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.io.IOUtils;

/**
 * DCT，全称离散余弦变换（Discrete Cosine Transform），是一种在图像和音频压缩中广泛使用的数学变换。它将数据从空间域（或时间域）转换到频率域，使得数据的大部分能量集中在少数几个频率分量上。这使得我们可以丢弃一些能量较小的频率分量，从而达到压缩数据的目的。
 * <p>
 * 在图像压缩中，最著名的使用DCT的算法就是JPEG。JPEG算法将图像分割为8x8的块，然后对每个块进行DCT，将像素值从空间域转换到频率域。然后，JPEG算法会丢弃一些能量较小的频率分量，只保留能量较大的分量。这样，即使丢弃了一部分数据，图像的视觉质量仍然可以保持得相当好。
 * <p>
 * 在PDF文件中，DCT编码常用于对彩色和灰度图像进行压缩。PDF的图像XObject可以指定一个解码器（Decode），用于解码压缩的图像数据。对于DCT编码的图像，解码器就是“DCTDecode”，这实际上就是JPEG解码。
 * Decompresses data encoded using a DCT (discrete cosine transform)
 * technique based on the JPEG standard.
 *
 * @author John Hewson
 */
final class DCTFilter extends Filter {
    private static final int POS_TRANSFORM = 11;
    private static final String ADOBE = "Adobe";

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded, COSDictionary
            parameters, int index, DecodeOptions options) throws IOException {
        // Already ready, just read it back out
        IOUtils.copy(encoded, decoded);

        return new DecodeResult(parameters);
    }

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        return decode(encoded, decoded, parameters, index, DecodeOptions.DEFAULT);
    }

//    private Integer getAdobeTransform(IIOMetadata metadata) TODO: PdfBox-Android

//    private int getAdobeTransformByBruteForce(ImageInputStream iis) throws IOException TODO: PdfBox-Android

//    private WritableRaster fromYCCKtoCMYK(Raster raster) TODO: PdfBox-Android

//    private WritableRaster fromYCbCrtoCMYK(Raster raster) TODO: PdfBox-Android

//    private WritableRaster fromBGRtoRGB(Raster raster) TODO: PdfBox-Android

//    private String getNumChannels(ImageReader reader) TODO: PdfBox-Android

    // clamps value to 0-255 range
    private int clamp(float value) {
        return (int) ((value < 0) ? 0 : ((value > 255) ? 255 : value));
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        throw new UnsupportedOperationException("DCTFilter encoding not implemented, use the JPEGFactory methods instead");
    }
}
