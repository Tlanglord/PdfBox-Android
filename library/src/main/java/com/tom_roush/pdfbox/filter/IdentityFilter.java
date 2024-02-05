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
 * IdentityFilter是PDF文件中的一种特殊的解码器（Decode）或过滤器（Filter），它表示不对数据进行任何压缩或编码。换句话说，使用IdentityFilter的数据流就是原始的、未经修改的数据。
 * <p>
 * 在PDF文件的语法中，使用IdentityFilter的数据流会标记为“/Filter /Identity”。例如，一个使用IdentityFilter的图像XObject可能会有这样的定义：“/Filter /Identity”。
 * <p>
 * IdentityFilter通常用于那些已经被其他方式压缩或编码的数据，或者那些不需要压缩的数据。例如，如果一个图像已经被JPEG编码，那么就可以直接将这个JPEG文件作为一个数据流，使用IdentityFilter，而不需要再进行额外的压缩。
 * <p>
 * The IdentityFilter filter passes the data through without any modifications.
 * It is defined in section 7.6.5 of the PDF 1.7 spec and also stated in table 26.
 *
 * @author Adam Nichols
 */
final class IdentityFilter extends Filter {
    @Override
    public DecodeResult decode(InputStream encoded, OutputStream decoded,
                               COSDictionary parameters, int index)
            throws IOException {
        IOUtils.copy(encoded, decoded);
        decoded.flush();
        return new DecodeResult(parameters);
    }

    @Override
    protected void encode(InputStream input, OutputStream encoded, COSDictionary parameters)
            throws IOException {
        IOUtils.copy(input, encoded);
        encoded.flush();
    }
}