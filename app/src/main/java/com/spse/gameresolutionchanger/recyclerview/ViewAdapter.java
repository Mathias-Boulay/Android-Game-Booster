package com.spse.gameresolutionchanger.recyclerview;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.spse.gameresolutionchanger.GameApp;
import com.spse.gameresolutionchanger.GameAppManager;
import com.spse.gameresolutionchanger.MainActivity;
import com.spse.gameresolutionchanger.R;

import java.util.ArrayList;

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.ViewHolder> {
    private final MainActivity mActivity;
    private final Dialog mDialog;
    private final int position;
    private final ArrayList<String> mPackages;

    /**
     * View Holder is a subclass to easily access content from his "parent"
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView appLogo;
        private final TextView appTitle;
        private final TextView appPackage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            //TODO a click listener shit

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.addGameUI(appPackage.getText().toString(), position);
                mDialog.dismiss();
            }
        });
            appLogo = itemView.findViewById(R.id.imageViewGameIcon);
            appTitle = itemView.findViewById(R.id.textViewGameAppTitle);
            appPackage = itemView.findViewById(R.id.textViewGameAppPackageName);
        }

        //Getters
        public TextView getAppPackage() {
            return appPackage;
        }
        public ImageView getAppLogo() {
            return appLogo;
        }
        public TextView getAppTitle() {
            return appTitle;
        }
        public void setFromGameApp(GameApp gameApp){
            appPackage.setText(gameApp.getPackageName());
            appTitle.setText(gameApp.getGameName());
            appLogo.setImageDrawable(gameApp.getIcon());
        }

    }


    public ViewAdapter(ArrayList<String> mPackages, MainActivity activity, Dialog dialog, int position){
        this.mPackages = mPackages;
        this.mDialog = dialog;
        this.mActivity = activity;
        this.position = position;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_app_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull  ViewHolder holder, int position) {
        GameApp gameApp = GameAppManager.getGameApp(holder.getAppPackage().getContext(), mPackages.get(position));
        if(gameApp != null) holder.setFromGameApp(gameApp);
    }

    @Override
    public int getItemCount() {
        return mPackages.size();
    }
}
