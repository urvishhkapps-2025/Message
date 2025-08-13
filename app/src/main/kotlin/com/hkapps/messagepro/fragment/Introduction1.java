package com.hkapps.messagepro.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.hkapps.messagepro.R;
import com.hkapps.messagepro.utils.Utility;

public class Introduction1 extends Fragment {
    TextView textView, textView1;
    ImageView iv_img1;
    Animation anim_out;
    Animation anim_in;

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.fragment_introduction_1, null, false);
        textView = view.findViewById(R.id.tv_head);
        iv_img1 = view.findViewById(R.id.iv_img1);
        textView1 = view.findViewById(R.id.tv_str);
        textView.setText(getString(R.string.text_intro_title1));
        textView1.setText(getString(R.string.text_intro_sub_title1));
        Bitmap bitmapLocal1 = Utility.decodeSampledBitmapFromResource(getResources(), R.drawable.icon_banner_offer, 500, 500);
        iv_img1.setImageBitmap(bitmapLocal1);
        return view;
    }

    public void doAnimTitleSubTitle() {
        textView.clearAnimation();
        textView1.clearAnimation();
        iv_img1.clearAnimation();

        anim_out = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in);
        anim_out.setRepeatCount(Animation.ABSOLUTE);
        anim_out.setDuration(2000);

        textView.setAnimation(anim_out);
        textView1.setAnimation(anim_out);
        iv_img1.setAnimation(anim_out);

        anim_in = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_out);
        anim_in.setRepeatCount(Animation.ABSOLUTE);
        anim_in.setDuration(2000);
        anim_in.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                textView.startAnimation(anim_out);
                textView1.startAnimation(anim_out);
                iv_img1.startAnimation(anim_out);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }
}
