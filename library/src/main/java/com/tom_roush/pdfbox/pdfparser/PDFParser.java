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
package com.tom_roush.pdfbox.pdfparser;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSDocument;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.io.IOUtils;
import com.tom_roush.pdfbox.io.RandomAccessRead;
import com.tom_roush.pdfbox.io.ScratchFile;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;

public class PDFParser extends COSParser
{
    private static final String TAG = "PDFParser";

    /**
     * Constructor.
     * Unrestricted main memory will be used for buffering PDF streams.
     *
     * @param source source representing the pdf.
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source) throws IOException
    {
        this(source, "", ScratchFile.getMainMemoryOnlyInstance());
    }

    /**
     * Constructor.
     *
     * @param source input representing the pdf.
     * @param scratchFile use a {@link ScratchFile} for temporary storage.
     *
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source, ScratchFile scratchFile) throws IOException
    {
        this(source, "", scratchFile);
    }

    /**
     * Constructor.
     * Unrestricted main memory will be used for buffering PDF streams.
     *
     * @param source input representing the pdf.
     * @param decryptionPassword password to be used for decryption.
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source, String decryptionPassword) throws IOException
    {
        this(source, decryptionPassword, ScratchFile.getMainMemoryOnlyInstance());
    }

    /**
     * Constructor.
     *
     * @param source input representing the pdf.
     * @param decryptionPassword password to be used for decryption.
     * @param scratchFile use a {@link ScratchFile} for temporary storage.
     *
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source, String decryptionPassword, ScratchFile scratchFile)
        throws IOException
    {
        this(source, decryptionPassword, null, null, scratchFile);
    }

    /**
     * Constructor.
     * Unrestricted main memory will be used for buffering PDF streams.
     *
     * @param source input representing the pdf.
     * @param decryptionPassword password to be used for decryption.
     * @param keyStore key store to be used for decryption when using public key security 
     * @param alias alias to be used for decryption when using public key security
     *
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source, String decryptionPassword, InputStream keyStore,
        String alias) throws IOException
    {
        this(source, decryptionPassword, keyStore, alias, ScratchFile.getMainMemoryOnlyInstance());
    }

    /**
     * Constructor.
     *
     * @param source input representing the pdf.
     * @param decryptionPassword password to be used for decryption.
     * @param keyStore key store to be used for decryption when using public key security 
     * @param alias alias to be used for decryption when using public key security
     * @param scratchFile buffer handler for temporary storage; it will be closed on
     *        {@link COSDocument#close()}
     *
     * @throws IOException If something went wrong.
     */
    public PDFParser(RandomAccessRead source, String decryptionPassword, InputStream keyStore,
        String alias, ScratchFile scratchFile) throws IOException
    {
        super(source, decryptionPassword, keyStore, alias);
        fileLen = source.length();
        init(scratchFile);
    }

    private void init(ScratchFile scratchFile)
    {
        String eofLookupRangeStr = System.getProperty(SYSPROP_EOFLOOKUPRANGE);
        if (eofLookupRangeStr != null)
        {
            try
            {
                setEOFLookupRange(Integer.parseInt(eofLookupRangeStr));
            }
            catch (NumberFormatException nfe)
            {
                Log.w("PdfBox-Android", "System property " + SYSPROP_EOFLOOKUPRANGE
                    + " does not contain an integer value, but: '" + eofLookupRangeStr + "'");
            }
        }
        document = new COSDocument(scratchFile);
    }

    /**
     * This will get the PD document that was parsed.  When you are done with
     * this document you must call close() on it to release resources.
     *
     * @return The document at the PD layer.
     *
     * @throws IOException If there is an error getting the document.
     */
    public PDDocument getPDDocument() throws IOException
    {
        PDDocument doc = new PDDocument(getDocument(), source, getAccessPermission());
        doc.setEncryptionDictionary(getEncryption());
        return doc;
    }

    /**
     * The initial parse will first parse only the trailer, the xrefstart and all xref tables to have a pointer (offset)
     * to all the pdf's objects. It can handle linearized pdfs, which will have an xref at the end pointing to an xref
     * at the beginning of the file. Last the root object is parsed.
     *
     * @throws InvalidPasswordException If the password is incorrect.
     * @throws IOException If something went wrong.
     */
    protected void initialParse() throws IOException
    {
        Log.d(TAG, "initialParse: ");
        COSDictionary trailer = retrieveTrailer();

        COSBase base = parseTrailerValuesDynamically(trailer);
        if (!(base instanceof COSDictionary))
        {
            throw new IOException("Expected root dictionary, but got this: " + base);
        }
        COSDictionary root = (COSDictionary) base;
        // in some pdfs the type value "Catalog" is missing in the root object
        if (isLenient() && !root.containsKey(COSName.TYPE))
        {
            root.setItem(COSName.TYPE, COSName.CATALOG);
        }
        // parse all objects, starting at the root dictionary
        // 解析所有objects
        parseDictObjects(root, (COSName[]) null);
        // parse all objects of the info dictionary
        COSBase infoBase = trailer.getDictionaryObject(COSName.INFO);
        if (infoBase instanceof COSDictionary)
        {
            parseDictObjects((COSDictionary) infoBase, (COSName[]) null);
        }
        // check pages dictionaries
        checkPages(root);
        if (!(root.getDictionaryObject(COSName.PAGES) instanceof COSDictionary))
        {
            throw new IOException("Page tree root must be a dictionary");
        }
        document.setDecrypted();
        initialParseDone = true;
    }

    /**
     * This will parse the stream and populate the COSDocument object.  This will close
     * the keystore stream when it is done parsing.
     *
     * @throws InvalidPasswordException If the password is incorrect.
     * @throws IOException If there is an error reading from the stream or corrupt data
     * is found.
     */
    public void parse() throws IOException
    {
        // set to false if all is processed
        boolean exceptionOccurred = true;
        try
        {
            // PDFBOX-1922 read the version header and rewind
            if (!parsePDFHeader() && !parseFDFHeader())
            {
                throw new IOException( "Error: Header doesn't contain versioninfo" );
            }

            if (!initialParseDone)
            {
                initialParse();
            }
            exceptionOccurred = false;
        }
        finally
        {
            if (exceptionOccurred && document != null)
            {
                IOUtils.closeQuietly(document);
                document = null;
            }
        }
    }

}
