package com.shoyu666.imagerecord.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import com.shoyu666.imagerecord.log.MLog;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
    public static final int IMAGE_W1 = 900;
    public static final int AUTHI_IAMGE_W = 400;
    private static final String TAG = "ImageUtil";

    public static Bitmap getColorMatrixBitMap(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static int getExifRotation(File imageFile) {
        int rotaTag = ExifInterface.ORIENTATION_UNDEFINED;
        if (imageFile == null) return 0;
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            // We only recognize a subset of orientation tag values
            rotaTag = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            android.util.Log.d(TAG, "getExifRotation" + rotaTag);
        } catch (IOException e) {
        }
        return rotaTag;
    }

    public static Bitmap getBitMap(Resources res, int draw) {
        try {
            BitmapDrawable bd = (BitmapDrawable) res.getDrawable(draw);
            return bd.getBitmap();
        } catch (Resources.NotFoundException e) {
            MLog.reportThrowable(e);
        }
        return null;
    }

    public static Bitmap getSampleBitmap(Context context, Uri uri, int borderWidth, int borderHeight, int rotate) throws IOException {
        int sample = getinSampleSize(context, uri, borderWidth, borderHeight, rotate);
        BitmapFactory.Options optionForDecode = new BitmapFactory.Options();
        optionForDecode.inSampleSize = sample;
        InputStream is = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, optionForDecode);
        return bitmap;
    }

    public Bitmap roundCornerImage(Bitmap raw, float round) {
        int width = raw.getWidth();
        int height = raw.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        canvas.drawARGB(0, 0, 0, 0);
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#000000"));
        final Rect rect = new Rect(0, 0, width, height);
        final RectF rectF = new RectF(rect);
        canvas.drawRoundRect(rectF, round, round, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(raw, rect, rect, paint);
        return result;
    }

    public static Bitmap rotaingImageView(int rotate, Bitmap bitmap) {
        int angle = 0;
        Bitmap resizedBitmap = null;
        switch (rotate) {
            case ExifInterface.ORIENTATION_UNDEFINED:
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                angle = 90;
                break;
            default:
                break;
        }
        if (angle != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return resizedBitmap;
    }

    public static BitmapFactory.Options getBoundOption(Context context, Uri sourceUri) {
        BitmapFactory.Options option;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(
                    sourceUri);
            option = new BitmapFactory.Options();
            option.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, option);
        } catch (Exception e) {
            MLog.reportThrowable(e);
            option = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return option;
    }

    public static int getinSampleSize(Context context, Uri sourceUri, int borderWidth, int borderHeight, int rotate) {
        int inSampleSize = 1;
        try {
            BitmapFactory.Options option = getBoundOption(context, sourceUri);
            int bitmapWidth = option.outWidth;
            int bitmapHeight = option.outHeight;
            switch (rotate) {
                case ExifInterface.ORIENTATION_UNDEFINED:
                    inSampleSize = sampleSize(bitmapWidth, bitmapHeight, borderWidth, borderHeight);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    inSampleSize = sampleSize(bitmapWidth, bitmapHeight, borderHeight, borderWidth);
                    break;
                default:
                    inSampleSize = sampleSize(bitmapWidth, bitmapHeight, borderHeight, borderWidth);
                    break;
            }
            MLog.d(TAG, "getinSampleSize inSampleSize " + inSampleSize
                    + " bitmapWidth " + bitmapWidth + " bitmapHeight " + bitmapHeight);
            return inSampleSize;
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
        return inSampleSize;
    }

    public static int sampleSize(int bitmapWidth, int bitmapHeight, int borderWidth, int borderHeight) {
        int result = 1;
        int a;
        int b;
        if (bitmapWidth / bitmapHeight > borderWidth / borderHeight) {
            a = bitmapWidth;
            b = borderWidth;
        } else {
            a = bitmapHeight;
            b = borderHeight;
        }
        for (int i = 0; i < 10; i++) {
            if (a < b * 2) {
                break;
            }
            a = b / 2;
            result = result * 2;
        }
        return result;
    }

    public static void comparess(Context context, File target, File out) {
        Bitmap bitmap = null;
        FileOutputStream outStream = null;
        try {
            Uri image = Uri.fromFile(target);
            int sample = ImageUtil.getinSampleSize(context, image, MScreenUtils.getScreenWidth(context), MScreenUtils.getScreenHeight(context), ExifInterface.ORIENTATION_UNDEFINED);
            BitmapFactory.Options optionForDecode = new BitmapFactory.Options();
            optionForDecode.inSampleSize = sample;
            bitmap = BitmapFactory.decodeFile(target.getAbsolutePath(), optionForDecode);
            outStream = new FileOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.close();
            bitmap.recycle();
        } catch (IOException e) {
            MLog.reportThrowable(e);
        } finally {
            if (bitmap != null) {
                try {
                    bitmap.recycle();
                } catch (Exception e) {
                    MLog.reportThrowable(e);
                    MLog.reportThrowable(e);
                }
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (Exception e) {
                    MLog.reportThrowable(e);
                    MLog.reportThrowable(e);
                }
            }
        }
    }

    /**
     * 保存文件(视频/图片)到相冊
     *
     * @param context
     * @param isVideo
     * @param saveFile
     * @param createTime
     */
    public static void insertIntoMediaStore(Context context, boolean isVideo, File saveFile, long createTime) {
        try {
            ContentResolver mContentResolver = context.getContentResolver();
            if (createTime == 0)
                createTime = System.currentTimeMillis();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
            //值一样，但是还是用常量区分对待
            values.put(isVideo ? MediaStore.Video.VideoColumns.DATE_TAKEN
                    : MediaStore.Images.ImageColumns.DATE_TAKEN, createTime);
            values.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis());
            values.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis());
            if (!isVideo)
                values.put(MediaStore.Images.ImageColumns.ORIENTATION, 0);
            values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
            values.put(MediaStore.MediaColumns.SIZE, saveFile.length());
            values.put(MediaStore.MediaColumns.MIME_TYPE, isVideo ? getVideoMimeType("video/3gp") : getVideoMimeType("image/jpeg"));
            //插入
            mContentResolver.insert(isVideo
                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            MLog.reportThrowable(e);
        }
    }

    // 获取video的mine_type,暂时只支持mp4,3gp
    private static String getVideoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("mp4") || lowerPath.endsWith("mpeg4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith("3gp")) {
            return "video/3gp";
        }
        return "video/mp4";
    }
}
