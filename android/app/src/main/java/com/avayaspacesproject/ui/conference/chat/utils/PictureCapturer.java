package com.avayaspacesproject.ui.conference.chat.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.avaya.spacescsdk.utils.UcLog;
import com.esna.extra.BoolRef;
import com.esna.os.DroidTweaks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PictureCapturer {

    static final private String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    private final String ID = "PicCapturer." + Integer.toHexString(hashCode());

    private int resultCodeTakePicture = 0x7A4E;

    private ArrayList<DeferredCallParams> pendingRequests = new ArrayList();

    private File target = null;

    public void capture(Object fragmentOrActivity, boolean imageNotVideo) {

        Fragment frag = null;
        Activity act = null;

        if (fragmentOrActivity instanceof Fragment) {
            frag = (Fragment) fragmentOrActivity;
        } else if (fragmentOrActivity instanceof Activity) {
            act = (Activity) fragmentOrActivity;
        } else {
            return;
        }


        String[] permissions = PERMISSIONS;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int access = ContextCompat.checkSelfPermission((act != null) ? act : frag.getActivity(), permission);
            if (access != PackageManager.PERMISSION_GRANTED) {
                int request = DroidTweaks.getPermissionRequestCode();
                DeferredCallParams parcel = new DeferredCallParams();
                parcel.fragmentOrActivity = fragmentOrActivity;
                parcel.imageNotVideo = imageNotVideo;
                parcel.requestCode = request;
                synchronized (pendingRequests) {
                    pendingRequests.add(parcel);
                }

                UcLog.l(ID, "Asking permission: " + permission + ", code " + request);
                if (act != null) {
                    ActivityCompat.requestPermissions(act, new String[]{permission}, request);
                } else {
                    frag.requestPermissions(new String[]{permission}, request);
                }
                return;
            }
        }

        Context c = (act != null) ? act.getApplicationContext() : frag.getActivity().getApplicationContext();

        Intent i = captureInt(imageNotVideo, c);

        if (i != null && i.resolveActivity(c.getPackageManager()) != null) {
            if (act != null)
                act.startActivityForResult(i, resultCodeTakePicture);
            else
                frag.startActivityForResult(i, resultCodeTakePicture);
        }
    }

    private Intent captureInt(boolean imageNotVideo, Context context) {
        final File pics = context.getExternalFilesDir(imageNotVideo ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_MOVIES);

        int num = 1;

        String format = imageNotVideo ? "SPACES_%04d.JPG" : "SPACES_%04d.MP4";
        // TODO: Actually we should use some temporary storage and then rename it to meet media type reported

        while ((target = new File(pics, String.format(format, num))).exists()) {
            num++;
        }

        try {
            if (!target.createNewFile()) {
                throw new IOException("Cannot create");
            }
            target.delete();
        } catch (IOException e) {
            return null;
        }

        Intent iCamera = new Intent(imageNotVideo ? MediaStore.ACTION_IMAGE_CAPTURE : MediaStore.ACTION_VIDEO_CAPTURE);

        Uri photoUri;
        // https://stackoverflow.com/questions/39242026/fileuriexposedexception-in-android-n-with-camera
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            photoUri = Uri.fromFile(target);
        } else {
            photoUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", target);
        }
        iCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        iCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return iCamera;
    }

    public File getFile() {
        return target;
    }

    public boolean onRequestPermissionsResult(Activity act, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean consumed = false;

        DeferredCallParams parcelFound = null;

        synchronized (pendingRequests) {
            for (DeferredCallParams parcel : pendingRequests) {
                if (parcel.requestCode == requestCode) {
                    parcelFound = parcel;
                    pendingRequests.remove(parcel);
                    break;
                }
            }
        }

        if (parcelFound == null) {
            return consumed;
        }

        consumed = true;

        final DeferredCallParams parcel = parcelFound;

        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                UcLog.w(ID, "Permission denied: " + permissions[i] + ", code " + requestCode);
//                UCToast.show(act, UCToast.POPUP_KIND_ERROR, act.getString(R.string.cancelled),
//                        act.getString(R.string.insufficient_permissions), UCToast.TOAST_DURATION_LONG);
                return consumed;
            }
        }

        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                capture(parcel.fragmentOrActivity, parcel.imageNotVideo);
            }
        });
        return consumed;
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data, BoolRef success) {

        if (requestCode == resultCodeTakePicture) {
            boolean ok = resultCode == Activity.RESULT_OK;

            if (success != null) {
                success.val = ok;
            }

            if (ok) {
                UcLog.i(ID, "Media captured");
            } else {
                UcLog.i(ID, "Failed to capture media");
                target = null;
            }
            return true;
        }
        return false;
    }

    private static class DeferredCallParams {
        public Object fragmentOrActivity;
        public boolean imageNotVideo;
        public int requestCode;
    }

}
