package dev.ragnarok.fenrir.picasso.transforms;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.squareup.picasso.Transformation;

public class RoundTransformation implements Transformation {

    private static final String TAG = RoundTransformation.class.getSimpleName();

    @NonNull
    @Override
    public String key() {
        return TAG + "()";
    }

    @Override
    public Bitmap transform(Bitmap source) {
        if (source == null) {
            return null;
        }
        return ImageHelper.getRoundedBitmap(source);
    }
}
