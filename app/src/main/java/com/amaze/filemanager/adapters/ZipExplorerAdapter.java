package com.amaze.filemanager.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amaze.filemanager.R;
import com.amaze.filemanager.asynchronous.services.ExtractService;
import com.amaze.filemanager.filesystem.HybridFileParcelable;
import com.amaze.filemanager.fragments.ZipExplorerFragment;
import com.amaze.filemanager.ui.ZipObjectParcelable;
import com.amaze.filemanager.ui.icons.Icons;
import com.amaze.filemanager.ui.views.CircleGradientDrawable;
import com.amaze.filemanager.ui.views.RoundedImageView;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.Utils;
import com.amaze.filemanager.utils.color.ColorUtils;
import com.amaze.filemanager.utils.provider.UtilitiesProviderInterface;
import com.amaze.filemanager.utils.theme.AppTheme;
import com.github.junrar.rarfile.FileHeader;

import java.util.ArrayList;

/**
 * Created by Arpit on 25-01-2015.
 */
public class ZipExplorerAdapter extends RecyclerArrayAdapter<String, RecyclerView.ViewHolder> {

    private Context c;
    private UtilitiesProviderInterface utilsProvider;
    private Drawable folder, unknown;
    private ArrayList<FileHeader> enter;
    private ArrayList<ZipObjectParcelable> enter1;
    private ZipExplorerFragment zipExplorerFragment;
    private LayoutInflater mInflater;
    private SparseBooleanArray myChecked = new SparseBooleanArray();
    private boolean zipMode = false;  // flag specify whether adapter is based on a Rar file or not

    public ZipExplorerAdapter(Context c, UtilitiesProviderInterface utilsProvider, ArrayList<FileHeader> enter, ZipExplorerFragment zipExplorerFragment) {
        this.utilsProvider = utilsProvider;
        this.enter = enter;
        for (int i = 0; i < enter.size(); i++)
            myChecked.put(i, false);

        mInflater = (LayoutInflater) c.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this.c = c;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        unknown = c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
        this.zipExplorerFragment = zipExplorerFragment;
    }

    public ZipExplorerAdapter(Context c, UtilitiesProviderInterface utilsProvider, ArrayList<ZipObjectParcelable> enter, ZipExplorerFragment zipExplorerFragment, boolean l) {
        this.utilsProvider = utilsProvider;
        this.enter1 = enter;
        for (int i = 0; i < enter.size(); i++) {
            myChecked.put(i, false);
        }
        zipMode = true;
        this.c = c;
        if (c == null) return;
        folder = c.getResources().getDrawable(R.drawable.ic_grid_folder_new);
        unknown = c.getResources().getDrawable(R.drawable.ic_doc_generic_am);
        this.zipExplorerFragment = zipExplorerFragment;
        mInflater = (LayoutInflater) c.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * called as to toggle selection of any item in adapter
     *
     * @param position  the position of the item
     * @param imageView the circular {@link CircleGradientDrawable} that is to be animated
     */
    private void toggleChecked(int position, ImageView imageView) {
        zipExplorerFragment.stopAnim();
        stoppedAnimation = true;
        if (myChecked.get(position)) {
            // if the view at position is checked, un-check it
            myChecked.put(position, false);
            Animation checkOutAnimation = AnimationUtils.loadAnimation(c, R.anim.check_out);
            if (imageView != null) {

                imageView.setAnimation(checkOutAnimation);
            } else {
                // TODO: we don't have the check icon object probably because of config change
            }
        } else {
            // if view is un-checked, check it
            myChecked.put(position, true);

            Animation iconAnimation = AnimationUtils.loadAnimation(c, R.anim.check_in);
            if (imageView != null) {

                imageView.setAnimation(iconAnimation);
            } else {
                // TODO: we don't have the check icon object probably because of config change
            }
        }

        notifyDataSetChanged();
        if (!zipExplorerFragment.selection || zipExplorerFragment.mActionMode == null) {
            zipExplorerFragment.selection = true;
            /*zipExplorerFragment.mActionMode = zipExplorerFragment.getActivity().startActionMode(
                   zipExplorerFragment.mActionModeCallback);*/
            zipExplorerFragment.mActionMode = zipExplorerFragment.mainActivity.getAppbar().getToolbar().startActionMode(zipExplorerFragment.mActionModeCallback);
        }
        zipExplorerFragment.mActionMode.invalidate();
        if (getCheckedItemPositions().size() == 0) {
            zipExplorerFragment.selection = false;
            zipExplorerFragment.mActionMode.finish();
            zipExplorerFragment.mActionMode = null;
        }
    }

    public void toggleChecked(boolean b, String path) {
        int k = 0;
        // if(enter.get(0).getEntry()==null)k=1;
        for (int i = k; i < (zipMode ? enter1.size() : enter.size()); i++) {
            myChecked.put(i, b);
            notifyItemChanged(i);
        }
    }

    public ArrayList<Integer> getCheckedItemPositions() {
        ArrayList<Integer> checkedItemPositions = new ArrayList<>();

        for (int i = 0; i < myChecked.size(); i++) {
            if (myChecked.get(i)) {
                (checkedItemPositions).add(i);
            }
        }

        return checkedItemPositions;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        RoundedImageView pictureIcon;
        ImageView genericIcon, apkIcon;
        TextView txtTitle;
        TextView txtDesc;
        public TextView date;
        TextView perm;
        View rl;
        ImageView checkImageView;

        ViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.firstline);
            pictureIcon = (RoundedImageView) view.findViewById(R.id.picture_icon);
            genericIcon = (ImageView) view.findViewById(R.id.generic_icon);
            rl = view.findViewById(R.id.second);
            perm = (TextView) view.findViewById(R.id.permis);
            date = (TextView) view.findViewById(R.id.date);
            txtDesc = (TextView) view.findViewById(R.id.secondLine);
            apkIcon = (ImageView) view.findViewById(R.id.apk_icon);
            checkImageView = (ImageView) view.findViewById(R.id.check_icon);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View v = mInflater.inflate(R.layout.rowlayout, parent, false);
            v.findViewById(R.id.picture_icon).setVisibility(View.INVISIBLE);
            return new ViewHolder(v);

        }
        View v = mInflater.inflate(R.layout.rowlayout, parent, false);
        ViewHolder vh = new ViewHolder(v);
        ImageButton about = (ImageButton) v.findViewById(R.id.properties);
        about.setVisibility(View.INVISIBLE);
        return vh;
    }

    private int offset = 0;
    public boolean stoppedAnimation = false;
    private Animation localAnimation;

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ((ViewHolder) holder).rl.clearAnimation();
    }

    @Override
    public boolean onFailedToRecycleView(RecyclerView.ViewHolder holder) {
        ((ViewHolder) holder).rl.clearAnimation();
        return super.onFailedToRecycleView(holder);
    }

    private void animate(ZipExplorerAdapter.ViewHolder holder) {
        holder.rl.clearAnimation();
        localAnimation = AnimationUtils.loadAnimation(zipExplorerFragment.getActivity(), R.anim.fade_in_top);
        localAnimation.setStartOffset(this.offset);
        holder.rl.startAnimation(localAnimation);
        this.offset = (30 + this.offset);
    }

    public void generate(ArrayList<FileHeader> arrayList) {
        offset = 0;
        stoppedAnimation = false;
        notifyDataSetChanged();
        enter = arrayList;
    }

    public void generate(ArrayList<ZipObjectParcelable> arrayList, boolean zipMode) {
        offset = 0;
        stoppedAnimation = false;
        notifyDataSetChanged();
        enter1 = arrayList;
    }

    /**
     * onBindViewHolder for zip files
     *
     * @param vholder   the ElementViewHolder reference for instantiating views
     * @param position1 the position of the view to bind
     */
    private void onBindView(RecyclerView.ViewHolder vholder, final int position1) {
        final ZipExplorerAdapter.ViewHolder holder = ((ZipExplorerAdapter.ViewHolder) vholder);
        if (!this.stoppedAnimation) {
            animate(holder);
        }
        final ZipObjectParcelable rowItem = enter1.get(position1);
        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.checkImageView.setBackground(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));
        } else
            holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));

        if (rowItem.getEntry() == null) {
            holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.ic_arrow_left_white_24dp));
            gradientDrawable.setColor(Utils.getColor(c, R.color.goback_item));
            holder.txtTitle.setText("..");
            holder.txtDesc.setText("");
            holder.date.setText(R.string.goback);
        } else {
            holder.genericIcon.setImageDrawable(Icons.loadMimeIcon(rowItem.getName(), false, zipExplorerFragment.res));
            final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
            if (zipExplorerFragment.showLastModified)
                holder.date.setText(Utils.getDate(rowItem.getTime(), zipExplorerFragment.year));
            if (rowItem.isDirectory()) {
                holder.genericIcon.setImageDrawable(folder);
                gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
                if (stringBuilder.toString().length() > 0) {
                    stringBuilder.deleteCharAt(rowItem.getName().length() - 1);
                    try {
                        holder.txtTitle.setText(stringBuilder.toString().substring(stringBuilder.toString().lastIndexOf("/") + 1));
                    } catch (Exception e) {
                        holder.txtTitle.setText(rowItem.getName().substring(0, rowItem.getName().lastIndexOf("/")));
                    }
                }
            } else {
                if (zipExplorerFragment.showSize)
                    holder.txtDesc.setText(Formatter.formatFileSize(c, rowItem.getSize()));
                holder.txtTitle.setText(rowItem.getName().substring(rowItem.getName().lastIndexOf("/") + 1));
                if (zipExplorerFragment.coloriseIcons) {
                    ColorUtils.colorizeIcons(c, Icons.getTypeOfFile(rowItem.getName()),
                            gradientDrawable, Color.parseColor(zipExplorerFragment.iconskin));
                } else gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
            }
        }


        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (rowItem.getEntry() != null) {

                    toggleChecked(position1, holder.checkImageView);
                }
                return true;
            }
        });
        holder.genericIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (rowItem.getEntry() != null) {
                    toggleChecked(position1, holder.checkImageView);
                }
            }
        });
        Boolean checked = myChecked.get(position1);
        if (checked != null) {
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);

            }
            holder.rl.setSelected(false);
            if (checked) {
                //holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                holder.checkImageView.setVisibility(View.VISIBLE);
                gradientDrawable.setColor(Utils.getColor(c, R.color.goback_item));
                holder.rl.setSelected(true);
            } else holder.checkImageView.setVisibility(View.INVISIBLE);
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if (rowItem.getEntry() == null)
                    zipExplorerFragment.goBack();
                else {
                    if (zipExplorerFragment.selection) {
                        toggleChecked(position1, holder.checkImageView);
                    } else {
                        final StringBuilder stringBuilder = new StringBuilder(rowItem.getName());
                        if (rowItem.isDirectory())
                            stringBuilder.deleteCharAt(rowItem.getName().length() - 1);

                        if (rowItem.isDirectory()) {
                            zipExplorerFragment.changeZipPath(stringBuilder.toString());
                        } else {
                            String fileName = zipExplorerFragment.f.getName().substring(0,
                                    zipExplorerFragment.f.getName().lastIndexOf("."));
                            String archiveCacheDirPath = zipExplorerFragment.getActivity().getExternalCacheDir().getPath() +
                                    "/" + fileName;

                            HybridFileParcelable file = new HybridFileParcelable(archiveCacheDirPath + "/"
                                    + rowItem.getName().replaceAll("\\\\", "/"));
                            file.setMode(OpenMode.FILE);
                            // this file will be opened once service finishes up it's extraction
                            zipExplorerFragment.files.add(file);
                            // setting flag for binder to know
                            zipExplorerFragment.isOpen = true;

                            Toast.makeText(zipExplorerFragment.getContext(),
                                    zipExplorerFragment.getContext().getResources().getString(R.string.please_wait),
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(zipExplorerFragment.getContext(), ExtractService.class);
                            ArrayList<String> a = new ArrayList<>();

                            // adding name of entry to extract from zip, before opening it
                            a.add(rowItem.getName());
                            intent.putExtra(ExtractService.KEY_PATH_ZIP, zipExplorerFragment.f.getPath());
                            intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                            intent.putExtra(ExtractService.KEY_PATH_EXTRACT,
                                    zipExplorerFragment.getActivity().getExternalCacheDir().getPath());
                            ServiceWatcherUtil.runService(zipExplorerFragment.getContext(), intent);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vholder, final int position1) {
        if (zipMode) {
            onBindView(vholder, position1);
            return;
        }
        final ZipExplorerAdapter.ViewHolder holder = ((ZipExplorerAdapter.ViewHolder) vholder);
        if (!this.stoppedAnimation) {
            animate(holder);
        }
        if (position1 < 0) return;
        final FileHeader rowItem = enter.get(position1);
        zipExplorerFragment.elementsRar.add(position1, headerRequired(rowItem));

        GradientDrawable gradientDrawable = (GradientDrawable) holder.genericIcon.getBackground();

        holder.genericIcon.setImageDrawable(Icons.loadMimeIcon(rowItem.getFileNameString(), false, zipExplorerFragment.res));
        holder.txtTitle.setText(rowItem.getFileNameString().substring(rowItem.getFileNameString().lastIndexOf("\\") + 1));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.checkImageView.setBackground(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));
        } else
            holder.checkImageView.setBackgroundDrawable(new CircleGradientDrawable(zipExplorerFragment.accentColor,
                    utilsProvider.getAppTheme(), zipExplorerFragment.getResources().getDisplayMetrics()));

        if (rowItem.isDirectory()) {
            holder.genericIcon.setImageDrawable(folder);
            gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
        } else {
            if (zipExplorerFragment.coloriseIcons) {
                ColorUtils.colorizeIcons(c, Icons.getTypeOfFile(rowItem.getFileNameString()),
                        gradientDrawable, Color.parseColor(zipExplorerFragment.iconskin));
            } else gradientDrawable.setColor(Color.parseColor(zipExplorerFragment.iconskin));
        }


        holder.rl.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                toggleChecked(position1, holder.checkImageView);
                return true;
            }
        });
        holder.genericIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                toggleChecked(position1, holder.checkImageView);
            }

        });
        Boolean checked = myChecked.get(position1);
        if (checked != null) {
            if (utilsProvider.getAppTheme().equals(AppTheme.LIGHT)) {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_white);
            } else {

                holder.rl.setBackgroundResource(R.drawable.safr_ripple_black);
            }
            holder.rl.setSelected(false);
            if (checked) {
                //holder.genericIcon.setImageDrawable(zipExplorerFragment.getResources().getDrawable(R.drawable.abc_ic_cab_done_holo_dark));
                holder.checkImageView.setVisibility(View.VISIBLE);
                gradientDrawable.setColor(Utils.getColor(c, R.color.goback_item));
                holder.rl.setSelected(true);
            } else holder.checkImageView.setVisibility(View.INVISIBLE);
        }
        holder.rl.setOnClickListener(new View.OnClickListener() {

            public void onClick(View p1) {
                if (zipExplorerFragment.selection) {

                    toggleChecked(position1, holder.checkImageView);
                } else {

                    if (rowItem.isDirectory()) {
                        zipExplorerFragment.elementsRar.clear();
                        zipExplorerFragment.changeRarPath(rowItem.getFileNameString());
                    } else {
                        String fileName = zipExplorerFragment.f.getName().substring(0,
                                zipExplorerFragment.f.getName().lastIndexOf("."));
                        String archiveCacheDirPath = zipExplorerFragment.getActivity().getExternalCacheDir().getPath() +
                                "/" + fileName;

                        HybridFileParcelable file1 = new HybridFileParcelable(archiveCacheDirPath + "/"
                                + rowItem.getFileNameString().replaceAll("\\\\", "/"));
                        file1.setMode(OpenMode.FILE);

                        // this file will be opened once service finishes up it's extraction
                        zipExplorerFragment.files.add(file1);
                        // setting flag for binder to know
                        zipExplorerFragment.isOpen = true;

                        Toast.makeText(zipExplorerFragment.getContext(),
                                zipExplorerFragment.getContext().getResources().getString(R.string.please_wait),
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(zipExplorerFragment.getContext(), ExtractService.class);
                        ArrayList<String> a = new ArrayList<>();

                        // adding name of entry to extract from zip, before opening it
                        a.add(rowItem.getFileNameString());
                        intent.putExtra(ExtractService.KEY_PATH_ZIP, zipExplorerFragment.f.getPath());
                        intent.putExtra(ExtractService.KEY_ENTRIES_ZIP, a);
                        intent.putExtra(ExtractService.KEY_PATH_EXTRACT,
                                zipExplorerFragment.getActivity().getExternalCacheDir().getPath());
                        ServiceWatcherUtil.runService(zipExplorerFragment.getContext(), intent);
                    }
                }
            }
        });
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;

        return TYPE_ITEM;
    }

    private boolean isPositionHeader(int position) {
        return false;
    }


    private FileHeader headerRequired(FileHeader rowItem) {
        if(zipExplorerFragment.archive != null) {
            for (FileHeader fileHeader : zipExplorerFragment.archive.getFileHeaders()) {
                String req = fileHeader.getFileNameString();
                if (rowItem.getFileNameString().equals(req))
                    return fileHeader;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return zipMode ? enter1.size() : enter.size();
    }

}

