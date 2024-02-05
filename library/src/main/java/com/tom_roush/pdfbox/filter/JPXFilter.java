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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.gemalto.jp2.JP2Decoder;
import com.gemalto.jp2.JP2Encoder;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.io.IOUtils;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDJPXColorSpace;

/**
 * JPXFilter是PDF文件中的一种解码器（Decode）或过滤器（Filter），用于解码使用JPEG 2000压缩的图像数据。
 * <p>
 * JPEG 2000是一种图像压缩标准，相比于原始的JPEG标准，它提供了更高的压缩效率和更多的功能，例如无损压缩、区域性解码等。JPEG 2000使用了一种名为wavelet的数学变换，可以提供比DCT（离散余弦变换，被原始的JPEG标准使用）更好的压缩性能。
 * <p>
 * 在PDF文件的语法中，使用JPXFilter的数据流会标记为“/Filter /JPXDecode”。例如，一个使用JPXFilter的图像XObject可能会有这样的定义：“/Filter /JPXDecode”。
 * <p>
 * 需要注意的是，虽然JPEG 2000的压缩性能很好，但是它的计算复杂度也相对较高，解码JPEG 2000图像需要更多的计算资源。因此，对于需要快速显示的应用，例如网页，原始的JPEG可能是更好的选择。
 * <p>
 * Decompress data encoded using the wavelet-based JPEG 2000 standard,
 * reproducing the original data.
 * <p>
 * Requires the JP2ForAndroid library to be available from com.gemalto.jp2:jp2-android:1.0.3, see
 * <a href="https://github.com/ThalesGroup/JP2ForAndroid">JP2ForAndroid</a>.
 *
 * @author John Hewson
 * @author Timo Boehme
 */
public final class JPXFilter extends Filter {
    private static final int CACHE_SIZE = 1024;

    /**
     * {@inheritDoc}
     */
    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded, COSDictionary
            parameters, int index, DecodeOptions options) throws IOException {
        DecodeResult result = new DecodeResult(new COSDictionary());
        result.getParameters().addAll(parameters);
        Bitmap image = readJPX(encoded, options, result);

        int arrLen = image.getWidth() * image.getHeight();
        int[] pixels = new int[arrLen];
        image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        // here we use a buffer to write batch to `decoded`, which makes it 10x faster than write byte one by one
        byte[] buffer = new byte[CACHE_SIZE * 3];
        int pos = 0;

        for (int i = 0; i < arrLen; i++) {
            if (pos + 3 >= buffer.length) {
                decoded.write(buffer, 0, pos);
                pos = 0;
            }
            int color = pixels[i];
            buffer[pos] = (byte) Color.red(color);
            buffer[pos + 1] = (byte) Color.green(color);
            buffer[pos + 2] = (byte) Color.blue(color);
            pos += 3;
        }
        decoded.write(buffer, 0, pos);
        return result;
    }

    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index) throws IOException {
        return decode(encoded, decoded, parameters, index, DecodeOptions.DEFAULT);
    }

    // try to read using JP2ForAndroid
    private Bitmap readJPX(InputStream input, DecodeOptions options, DecodeResult result) throws IOException {
        try {
            Class.forName("com.gemalto.jp2.JP2Decoder");
        } catch (ClassNotFoundException ex) {
            throw new MissingImageReaderException("Cannot read JPX image: JP2Android is not installed.");
        }

        JP2Decoder decoder = new JP2Decoder(input);

        // TODO: uncomment after upgrading JP2ForAndroid
        // decoder.setSourceRegion(options.getSourceRegion());

        Bitmap image = decoder.decode();

        COSDictionary parameters = result.getParameters();

        // "If the image stream uses the JPXDecode filter, this entry is optional
        // and shall be ignored if present"
        //
        // note that indexed color spaces make the BPC logic tricky, see PDFBOX-2204
//        int bpc = image.getColorModel().getPixelSize() / image.getRaster().getNumBands();
//        parameters.setInt(COSName.BITS_PER_COMPONENT, bpc); TODO: PdfBox-Android

        // "Decode shall be ignored, except in the case where the image is treated as a mask"
        if (!parameters.getBoolean(COSName.IMAGE_MASK, false)) {
            parameters.setItem(COSName.DECODE, null);
        }

        // override dimensions, see PDFBOX-1735
        parameters.setInt(COSName.WIDTH, image.getWidth());
        parameters.setInt(COSName.HEIGHT, image.getHeight());

        // extract embedded color space
        if (!parameters.containsKey(COSName.COLORSPACE) && Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            result.setColorSpace(new PDJPXColorSpace(image.getColorSpace()));
        }

        return image;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        byte[] jpeBytes = new JP2Encoder(bitmap).encode();
        IOUtils.copy(new ByteArrayInputStream(jpeBytes), encoded);
        encoded.flush();
    }
}
