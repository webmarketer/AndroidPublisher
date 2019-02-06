package com.webmarketer.publisher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.gearback.methods.HttpPostRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Random;

public class Advertise extends FrameLayout {

    View view;
    ImageView adImageView, adFavicon, adLogo;
    TextView adTitle, adLink, adDesc;
    int type = 0;
    int borderWidth = 0;
    int borderColor = 0;
    Context con;
    Activity activity;
    final Handler errorHandler = new Handler();
    AdvertiseHolder advertise;

    public Advertise(Context context, AttributeSet attrs) {
        super(context, attrs);
        con = context;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Advertise, 0, 0);
        try {
            type = a.getInt(R.styleable.Advertise_type, 0);
            borderWidth = a.getDimensionPixelSize(R.styleable.Advertise_borderWidth, 0);
            borderColor = a.getColor(R.styleable.Advertise_borderColor, -1);
        } finally {
            a.recycle();
        }
    }

    private void init(String wm_ad_client, String wm_ad_group_token, String app) {
        FetchAds("http://www.webmarketer.ir/app_upload/applications/ads/api/json/?adboxid=" + TokenGenerator() + "&adgroup=" + wm_ad_group_token + "&adclient=" + wm_ad_client + "&adcount=1&linkcolor=&btcolor=&border=&bordercolor=&urlcolor=&textcolor=&pagination=", "app=" + app);
    }

    public void initialize(Activity activity, String wm_ad_client) {
        this.activity = activity;
        String wm_ad_group_token;
        if (type == 0) {
            wm_ad_group_token = "LEADERBOARD";
        }
        else if (type == 1) {
            wm_ad_group_token = "MPU";
        }
        else {
            wm_ad_group_token = "TEXT";
        }
        init(wm_ad_client, wm_ad_group_token, activity.getPackageName());
    }

    private void FetchAds(final String url, final String params) {
        HttpPostRequest postRequest = new HttpPostRequest(null, new HttpPostRequest.TaskListener() {
            @Override
            public void onFinished(String result) {
                if (!result.equals("")) {
                    try {
                        JSONObject adHolder = new JSONObject(result);
                        JSONArray ads = adHolder.getJSONArray("ads");
                        JSONObject ad = ads.getJSONObject(0);
                        AdItem item = new AdItem(ad.getString("title"), ad.getString("description"), ad.getString("image"), ad.getString("url"), ad.getString("domain"), ad.getString("favicon"));
                        advertise = new AdvertiseHolder(adHolder.getString("group"), adHolder.getString("width"), adHolder.getString("height"), adHolder.getString("show_title"), adHolder.getString("description_chars"), item);
                        if (type == 0 || type == 1) {
                            view = inflate(getContext(), R.layout.image_ad_layout, null);
                            RelativeLayout holder = view.findViewById(R.id.adContainer);
                            adImageView = view.findViewById(R.id.adImage);
                            adLogo = view.findViewById(R.id.adLogo);
                            adImageView.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String url = advertise.getAdItem().getUrl() + "&" + params;
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(url));
                                    try {
                                        con.startActivity(intent);
                                    }
                                    catch (ActivityNotFoundException e) {
                                        Log.d("Error", e.toString());
                                    }
                                }
                            });
                            GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(adImageView);
                            if (!activity.isFinishing()) {
                                Glide.with(getContext().getApplicationContext()).load(advertise.getAdItem().getImage()).into(imageViewTarget);
                            }

                            ViewTreeObserver vto = view.getViewTreeObserver();
                            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                                    } else {
                                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                    int viewWidth  = view.getMeasuredWidth();

                                    double rate = Double.parseDouble(advertise.getWidth())/Double.parseDouble(advertise.getHeight());
                                    double height = viewWidth/rate;
                                    double logoHeight = height/6;
                                    if (type == 1) {
                                        logoHeight = height/10;
                                    }
                                    adImageView.getLayoutParams().width = viewWidth;
                                    adImageView.getLayoutParams().height = (int)height;

                                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams((int)logoHeight, (int)logoHeight);
                                    lp.setMargins((int)logoHeight/2, (int)logoHeight/2, 0, 0);
                                    lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                                    adLogo.setLayoutParams(lp);
                                }
                            });

                            if (borderWidth != 0) {
                                GradientDrawable border = new GradientDrawable();
                                border.setColor(0xFFFFFFFF); //white background
                                holder.setPadding(borderWidth, borderWidth, borderWidth, borderWidth);
                                if (borderColor != -1) {
                                    border.setStroke(borderWidth, borderColor);
                                }
                                else {
                                    border.setStroke(borderWidth, 0x26000000);
                                }
                                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.setBackgroundDrawable(border);
                                } else {
                                    holder.setBackground(border);
                                }
                            }
                        }
                        else {
                            view = inflate(getContext(), R.layout.text_ad_layout, null);
                            adTitle = view.findViewById(R.id.adTitle);
                            adLink = view.findViewById(R.id.adLink);
                            adDesc = view.findViewById(R.id.adDesc);
                            adFavicon = view.findViewById(R.id.adFavicon);
                            LinearLayout holder = view.findViewById(R.id.adContainer);
                            adTitle.setText(advertise.getAdItem().getTitle());
                            adLink.setText(advertise.getAdItem().getDomain());
                            adDesc.setText(advertise.getAdItem().getDescription());
                            GlideDrawableImageViewTarget imageViewTarget = new GlideDrawableImageViewTarget(adFavicon);
                            if (!activity.isFinishing()) {
                                Glide.with(getContext().getApplicationContext()).load(advertise.getAdItem().getFavicon()).into(imageViewTarget);
                            }
                            view.setOnClickListener(new OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setData(Uri.parse(advertise.getAdItem().getUrl()));
                                    con.startActivity(i);
                                }
                            });
                            if (borderWidth != 0) {
                                GradientDrawable border = new GradientDrawable();
                                border.setColor(0xFFFFFFFF); //white background
                                holder.setPadding(borderWidth, borderWidth, borderWidth, borderWidth);
                                if (borderColor != -1) {
                                    border.setStroke(borderWidth, borderColor);
                                }
                                else {
                                    border.setStroke(borderWidth, 0x26000000);
                                }
                                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                    holder.setBackgroundDrawable(border);
                                } else {
                                    holder.setBackground(border);
                                }
                            }
                        }
                        addView(view);
                    } catch (JSONException e) {
                        Log.e("JSON Parser", "Error parsing data " + e.toString());
                    }
                }
                else {
                    errorHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            FetchAds(url, params);
                        }
                    }, 30000);
                }
            }
        });
        postRequest.execute(url, params);
    }

    private String RandomWord(int num) {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        String randomLetters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int randomLength = generator.nextInt(randomLetters.length());
        //char tempChar;
        for (int i = 0; i < num; i++){
            //tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(randomLetters.charAt(randomLength));
        }
        return randomStringBuilder.toString();
    }
    private String TokenGenerator() {
        String newtoken = "";
        Calendar c = Calendar.getInstance();
        newtoken = RandomWord(1) + (c.get(Calendar.YEAR) - 2000) + RandomWord(2) + c.get(Calendar.MONTH) + RandomWord(2) + c.get(Calendar.DAY_OF_MONTH) + RandomWord(2) + c.get(Calendar.HOUR) + RandomWord(2) + c.get(Calendar.MINUTE) + RandomWord(2) + c.get(Calendar.SECOND) + RandomWord(2);
        return newtoken;
    }
    class AdItem {
        private String title;
        private String description;
        private String image;
        private String url;
        private String domain;
        private String favicon;

        public AdItem(String title, String description, String image, String url, String domain, String favicon) {
            this.image = image;
            this.url = url;
            this.title = title;
            this.description = description;
            this.domain = domain;
            this.favicon = favicon;
        }
        public String getImage() {
            return image;
        }
        public String getUrl() {
            return url;
        }
        public String getTitle() {
            return title;
        }
        public String getDescription() {
            return description;
        }
        public String getDomain() {
            return domain;
        }
        public String getFavicon() {
            return favicon;
        }
    }
    class AdvertiseHolder {

        private String group;
        private String width;
        private String height;
        private String show_title;
        private String description_chars;
        private AdItem adItem;

        public AdvertiseHolder(String group, String width, String height, String show_title, String description_chars, AdItem adItem) {
            this.group = group;
            this.width = width;
            this.height = height;
            this.show_title = show_title;
            this.description_chars = description_chars;
            this.adItem = adItem;
        }

        public String getGroup() {return group;}
        public String getWidth() {return width;}
        public String getHeight() {return height;}
        public String getShow_title() {return show_title;}
        public String getDescription_chars() {return description_chars;}
        public AdItem getAdItem() {return adItem;}
    }
}
