package com.tom_roush.pdfbox.pdmodel.font;

import android.util.Log;

import com.tom_roush.fontbox.FontBoxFont;
import com.tom_roush.fontbox.ttf.OTFParser;
import com.tom_roush.fontbox.ttf.OpenTypeFont;
import com.tom_roush.fontbox.ttf.TTFParser;
import com.tom_roush.fontbox.ttf.TrueTypeCollection;
import com.tom_roush.fontbox.ttf.TrueTypeFont;
import com.tom_roush.fontbox.type1.Type1Font;
import com.tom_roush.pdfbox.android.PDFBoxConfig;
import com.tom_roush.pdfbox.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by dongqiangqiang on 2024/2/1
 */
public class SuperFont {


    private static class FSFontInfo extends FontInfo {
        private final String postScriptName;
        private final FontFormat format;
        private final CIDSystemInfo cidSystemInfo;
        private final int usWeightClass;
        private final int sFamilyClass;
        private final int ulCodePageRange1;
        private final int ulCodePageRange2;
        private final int macStyle;
        private final PDPanoseClassification panose;
        private final File file;

        private FSFontInfo(File file, FontFormat format, String postScriptName,
                           CIDSystemInfo cidSystemInfo, int usWeightClass, int sFamilyClass,
                           int ulCodePageRange1, int ulCodePageRange2, int macStyle, byte[] panose) {
            this.file = file;
            this.format = format;
            this.postScriptName = postScriptName;
            this.cidSystemInfo = cidSystemInfo;
            this.usWeightClass = usWeightClass;
            this.sFamilyClass = sFamilyClass;
            this.ulCodePageRange1 = ulCodePageRange1;
            this.ulCodePageRange2 = ulCodePageRange2;
            this.macStyle = macStyle;
            this.panose = panose != null && panose.length >= PDPanoseClassification.LENGTH ?
                    new PDPanoseClassification(panose) : null;
        }

        @Override
        public String getPostScriptName() {
            return postScriptName;
        }

        @Override
        public FontFormat getFormat() {
            return format;
        }

        @Override
        public CIDSystemInfo getCIDSystemInfo() {
            return cidSystemInfo;
        }

        /**
         * {@inheritDoc}
         * <p>
         * The method returns null if there is there was an error opening the font.
         */
        @Override
        public synchronized FontBoxFont getFont() {
            // synchronized to avoid race condition on cache access,
            // which could result in an unreferenced but open font
//            FontBoxFont cached = parent.cache.getFont(this);
//            if (cached != null) {
//                return cached;
//            } else {
            FontBoxFont font;
            switch (format) {
                case PFB:
                    font = getType1Font(postScriptName, file);
                    break;
                case TTF:
                    font = getTrueTypeFont(postScriptName, file);
                    break;
                case OTF:
                    font = getOTFFont(postScriptName, file);
                    break;
                default:
                    throw new RuntimeException("can't happen");
            }
//                if (font != null) {
//                    parent.cache.addFont(this, font);
//                }
            return font;
        }

        @Override
        public int getFamilyClass() {
            return sFamilyClass;
        }

        @Override
        public int getWeightClass() {
            return usWeightClass;
        }

        @Override
        public int getCodePageRange1() {
            return ulCodePageRange1;
        }

        @Override
        public int getCodePageRange2() {
            return ulCodePageRange2;
        }

        @Override
        public int getMacStyle() {
            return macStyle;
        }

        @Override
        public PDPanoseClassification getPanose() {
            return panose;
        }

        @Override
        public String toString() {
            return super.toString() + " " + file;
        }

        private TrueTypeFont getTrueTypeFont(String postScriptName, File file) {
            try {
                TrueTypeFont ttf = readTrueTypeFont(postScriptName, file);

                if (PDFBoxConfig.isDebugEnabled()) {
                    Log.d("PdfBox-Android", "Loaded " + postScriptName + " from " + file);
                }
                return ttf;
            } catch (IOException e) {
                Log.w("PdfBox-Android", "Could not load font file: " + file, e);
            }
            return null;
        }

        private TrueTypeFont readTrueTypeFont(String postScriptName, File file) throws IOException {
            if (file.getName().toLowerCase().endsWith(".ttc")) {
                @SuppressWarnings("squid:S2095")
                // ttc not closed here because it is needed later when ttf is accessed,
                // e.g. rendering PDF with non-embedded font which is in ttc file in our font directory
                TrueTypeCollection ttc = new TrueTypeCollection(file);
                TrueTypeFont ttf;
                try {
                    ttf = ttc.getFontByName(postScriptName);
                } catch (IOException ex) {
                    ttc.close();
                    throw ex;
                }
                if (ttf == null) {
                    ttc.close();
                    throw new IOException("Font " + postScriptName + " not found in " + file);
                }
                return ttf;
            } else {
                TTFParser ttfParser = new TTFParser(false, true);
                return ttfParser.parse(file);
            }
        }

        private OpenTypeFont getOTFFont(String postScriptName, File file) {
            try {
                if (file.getName().toLowerCase().endsWith(".ttc")) {
                    @SuppressWarnings("squid:S2095")
                    // ttc not closed here because it is needed later when ttf is accessed,
                    // e.g. rendering PDF with non-embedded font which is in ttc file in our font directory
                    TrueTypeCollection ttc = new TrueTypeCollection(file);
                    TrueTypeFont ttf;
                    try {
                        ttf = ttc.getFontByName(postScriptName);
                    } catch (IOException ex) {
                        Log.e("PdfBox-Android", ex.getMessage(), ex);
                        ttc.close();
                        return null;
                    }
                    if (ttf == null) {
                        ttc.close();
                        throw new IOException("Font " + postScriptName + " not found in " + file);
                    }
                    return (OpenTypeFont) ttf;
                }

                OTFParser parser = new OTFParser(false, true);
                OpenTypeFont otf = parser.parse(file);

                if (PDFBoxConfig.isDebugEnabled()) {
                    Log.d("PdfBox-Android", "Loaded " + postScriptName + " from " + file);
                }
                return otf;
            } catch (IOException e) {
                Log.w("PdfBox-Android", "Could not load font file: " + file, e);
            }
            return null;
        }

        private Type1Font getType1Font(String postScriptName, File file) {
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                Type1Font type1 = Type1Font.createWithPFB(input);

                if (PDFBoxConfig.isDebugEnabled()) {
                    Log.d("PdfBox-Android", "Loaded " + postScriptName + " from " + file);
                }
                return type1;
            } catch (IOException e) {
                Log.w("PdfBox-Android", "Could not load font file: " + file, e);
            } finally {
                IOUtils.closeQuietly(input);
            }
            return null;
        }
    }

}
