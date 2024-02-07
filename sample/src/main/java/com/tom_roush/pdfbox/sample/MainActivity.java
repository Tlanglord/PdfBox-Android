package com.tom_roush.pdfbox.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.tom_roush.pdfbox.android.PDFBoxConfig;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentCatalog;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDComboBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDListBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "permission";

    File root;
    AssetManager assetManager;
    Bitmap pageImage;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onCreate: audio permission  granted");
            } else {
                Log.d(TAG, "onCreate: audio permission  not granted");
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions[0].equals(Manifest.permission.RECORD_AUDIO) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: audio permission  granted");
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                boolean b = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
                if (b) {
                    Log.d(TAG, "onRequestPermissionsResult: audio permission  denied but not permanently");

                    // 在这里显示一个对话框，引导用户到设置页面手动开启权限
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("权限申请")
                            .setMessage("未开启录音权限，不能使用录音功能")
                            .setPositiveButton("打开设置", (dialog, which) -> {
                                openAppSettings();
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    // 在这里显示一个对话框，引导用户到设置页面手动开启权限
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("权限申请")
                            .setMessage("未开启录音权限，不能使用录音功能")
                            .setPositiveButton("打开设置", (dialog, which) -> {
                                openAppSettings();
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        }


    }

    private void openAppSettings() {
        // 打开应用设置页面
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Initializes variables used for convenience
     */
    private void setup() {
        PDFBoxConfig.setFontLoadLevel(PDFBoxConfig.FontLoadLevel.FULL);
        // Enable Android asset loading
        PDFBoxResourceLoader.init(getApplicationContext());
        // Find the root of the external storage.

        root = getApplicationContext().getCacheDir();
        assetManager = getAssets();
        tv = (TextView) findViewById(R.id.statusTextView);
    }

    /**
     * Creates a new PDF from scratch and saves it to a file
     */
    public void createPdf(View v) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // Create a new font object selecting one of the PDF base fonts
        PDFont font = PDType1Font.HELVETICA;
        // Or a custom font
//        try
//        {
//            // Replace MyFontFile with the path to the asset font you'd like to use.
//            // Or use LiberationSans "com/tom_roush/pdfbox/resources/ttf/LiberationSans-Regular.ttf"
//            font = PDType0Font.load(document, assetManager.open("MyFontFile.TTF"));
//        }
//        catch (IOException e)
//        {
//            Log.e("PdfBox-Android-Sample", "Could not load font", e);
//        }


        try {
            // Replace MyFontFile with the path to the asset font you'd like to use.
            // Or use LiberationSans "com/tom_roush/pdfbox/resources/ttf/LiberationSans-Regular.ttf"
            font = PDType0Font.load(document, assetManager.open("AaJianHaoTi-2.ttf"));
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Could not load font", e);
        }

        PDPageContentStream contentStream;

        try {
            // Define a content stream for adding to the PDF
            contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();

            // Load in the images
            InputStream in = assetManager.open("falcon.jpg");
            InputStream alpha = assetManager.open("trans.png");

            // Draw a green rectangle
//            contentStream.addRect(5, 500, 100, 100);
//            contentStream.setNonStrokingColor(0, 255, 125);
//            contentStream.fill();

            // Draw the falcon base image
            PDImageXObject ximage = JPEGFactory.createFromStream(document, in);
            contentStream.drawImage(ximage, 20, 20);

            // Draw the red overlay image
//            Bitmap alphaImage = BitmapFactory.decodeStream(alpha);
//            PDImageXObject alphaXimage = LosslessFactory.createFromImage(document, alphaImage);
//            contentStream.drawImage(alphaXimage, 20, 20);

            // Make sure that the content stream is closed:
            contentStream.close();

            // Save the final pdf document to a file
            String path = root.getAbsolutePath() + "/ChCreated.pdf";
            document.save(path);
            document.close();
            tv.setText("Successfully wrote PDF to " + path);

        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF", e);
        }
    }

    /**
     * Loads an existing PDF and renders it to a Bitmap
     */
    public void renderFile(View v) {
        // Render the page and save it to an image file
        try {
            // Load in an already created PDF
            PDDocument document = PDDocument.load(assetManager.open("goole_tt.pdf"));
            // Create a renderer for the document
            PDFRenderer renderer = new PDFRenderer(document);

            // Render the image to an RGB Bitmap
            pageImage = renderer.renderImage(0, 1, ImageType.RGB);

            // Save the render result to an image
            String path = root.getAbsolutePath() + "/render.jpg";
            File renderFile = new File(path);
            FileOutputStream fileOut = new FileOutputStream(renderFile);
            pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
            fileOut.close();
            tv.setText("Successfully rendered image to " + path);
            // Optional: display the render result on screen
            displayRenderedImage();
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e);
        }
    }

    /**
     * Fills in a PDF form and saves the result
     */
    public void fillForm(View v) {
        try {
            // Load the document and get the AcroForm
            PDDocument document = PDDocument.load(assetManager.open("FormTest.pdf"));
            PDDocumentCatalog docCatalog = document.getDocumentCatalog();
            PDAcroForm acroForm = docCatalog.getAcroForm();

            // Fill the text field
            PDTextField field = (PDTextField) acroForm.getField("TextField");
            field.setValue("Filled Text Field");
            // Optional: don't allow this field to be edited
            field.setReadOnly(true);

            PDField checkbox = acroForm.getField("Checkbox");
            ((PDCheckBox) checkbox).check();

            PDField radio = acroForm.getField("Radio");
            ((PDRadioButton) radio).setValue("Second");

            PDField listbox = acroForm.getField("ListBox");
            List<Integer> listValues = new ArrayList<>();
            listValues.add(1);
            listValues.add(2);
            ((PDListBox) listbox).setSelectedOptionsIndex(listValues);

            PDField dropdown = acroForm.getField("Dropdown");
            ((PDComboBox) dropdown).setValue("Hello");

            String path = root.getAbsolutePath() + "/FilledForm.pdf";
            tv.setText("Saved filled form to " + path);
            document.save(path);
            document.close();
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while filling form fields", e);
        }
    }

    /**
     * Strips the text from a PDF and displays the text on screen
     */
    public void stripText(View v) {
        String parsedText = null;
        PDDocument document = null;
        try {
            document = PDDocument.load(assetManager.open("Hello.pdf"));
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while loading document to strip", e);
        }

        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(0);
            pdfStripper.setEndPage(1);
            parsedText = "Parsed text: " + pdfStripper.getText(document);
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while stripping text", e);
        } finally {
            try {
                if (document != null) document.close();
            } catch (IOException e) {
                Log.e("PdfBox-Android-Sample", "Exception thrown while closing document", e);
            }
        }
        tv.setText(parsedText);
    }

    /**
     * Creates a simple pdf and encrypts it
     */
    public void createEncryptedPdf(View v) {
        String path = root.getAbsolutePath() + "/crypt.pdf";

        int keyLength = 128; // 128 bit is the highest currently supported

        // Limit permissions of those without the password
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(false);

        // Sets the owner password and user password
        StandardProtectionPolicy spp = new StandardProtectionPolicy("12345", "hi", ap);

        // Setups up the encryption parameters
        spp.setEncryptionKeyLength(keyLength);
        spp.setPermissions(ap);
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        PDFont font = PDType1Font.HELVETICA;
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();

        document.addPage(page);

        try {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();
            contentStream.close();

            // Save the final pdf document to a file
            document.protect(spp); // Apply the protections to the PDF
            document.save(path);
            document.close();
            tv.setText("Successfully wrote PDF to " + path);

        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while creating PDF for encryption", e);
        }
    }

    public void showDecryptedPdf(View v) {
//        String path = root.getAbsolutePath() + "/crypt.pdf";

        try {
            // Load in an already created PDF
//            PDDocument document = PDDocument.load(new File(path),"hi");

            PDDocument document = PDDocument.load(assetManager.open("single_password.pdf"), "azerty");


            // Create a renderer for the document
            PDFRenderer renderer = new PDFRenderer(document);
            // Render the image to an RGB Bitmap
            pageImage = renderer.renderImage(0, 1, ImageType.RGB);

            // Save the render result to an image
            String path2 = root.getAbsolutePath() + "/decrypt_render.jpg";
            File renderFile = new File(path2);
            FileOutputStream fileOut = new FileOutputStream(renderFile);
            pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
            fileOut.close();
            tv.setText("Successfully rendered image to " + path2);
            // Optional: display the render result on screen
            displayRenderedImage();
        } catch (IOException e) {
            Log.e("PdfBox-Android-Sample", "Exception thrown while rendering file", e);
        }

    }

    /**
     * Helper method for drawing the result of renderFile() on screen
     */
    private void displayRenderedImage() {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.renderedImageView);
                        imageView.setImageBitmap(pageImage);
                    }
                });
            }
        }.start();
    }
}