package glasscam.viewer.gridview.modelview;

import android.app.RecoverableSecurityException;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.lifecycle.ViewModel;

import com.hazia.glasscam.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.file.holder.DocumentHolder;
import glasscam.file.holder.FileHolder;
import glasscam.file.holder.UriHolder;
import glasscam.image.ImageManager;
import glasscam.utils.Log;
import glasscam.utils.StringUtils;
import glasscam.viewer.dngconvert.DngConvertingActivity;
import glasscam.viewer.gridview.enums.RequestModes;
import glasscam.viewer.gridview.enums.ViewStates;
import glasscam.viewer.gridview.models.ButtonDoAction;
import glasscam.viewer.gridview.models.ButtonFileTypeModel;
import glasscam.viewer.gridview.models.ButtonOptionsModel;
import glasscam.viewer.gridview.models.FilesHolderModel;
import glasscam.viewer.gridview.models.FilesSelectedModel;
import glasscam.viewer.gridview.models.FinishActivityModel;
import glasscam.viewer.gridview.models.GridImageViewModel;
import glasscam.viewer.gridview.models.IntentModel;
import glasscam.viewer.gridview.models.IntentSenderModel;
import glasscam.viewer.gridview.models.PopupMenuModel;
import glasscam.viewer.gridview.models.ViewStateModel;
import glasscam.viewer.screenslide.views.ScreenSlideFragment;
import glasscam.viewer.stack.DngStackActivity;

public class GridViewFragmentModelView extends ViewModel
{
    private final String TAG = GridViewFragmentModelView.class.getSimpleName();
    private final ViewStateModel viewStateModel;
    private final FilesHolderModel filesHolderModel;
    private boolean isRootDir = true;
    private final List<BaseHolder> filesSelectedList = new ArrayList<>();
    private final List<UriHolder> urisToDelte = new ArrayList<>();
    private final ButtonFileTypeModel buttonFiletype;
    private final ButtonDoAction buttonDoAction;
    private final ButtonOptionsModel buttonOptions;

    public FileListController.FormatTypes formatsToShow = FileListController.FormatTypes.all;
    private FileListController.FormatTypes lastFormat = FileListController.FormatTypes.all;
    private RequestModes requestMode = RequestModes.none;
    private BaseHolder folderToShow;
    private final IntentModel intentModel;
    private final FinishActivityModel finishActivityModel;
    private final FinishActivityModel alterDialogModel;
    private final FilesSelectedModel filesSelectedModel;
    private ScreenSlideFragment.ButtonClick onGridItemClick;
    private final PopupMenuModel popupMenuModel;

    private final IntentSenderModel intentSenderModel;
    private final FileListController fileListController;
    private final ImageManager imageManager;


    public GridViewFragmentModelView(FileListController fileListController, ImageManager imageManager)
    {
        viewStateModel = new ViewStateModel();
        filesHolderModel = new FilesHolderModel();
        this.fileListController = fileListController;
        this.imageManager = imageManager;
        buttonFiletype = new ButtonFileTypeModel(this);
        buttonDoAction = new ButtonDoAction();
        buttonOptions = new ButtonOptionsModel(onDeltedButtonClick,onRawToDngClick,onDngStackClick,this);
        if (isRootDir) {
            buttonOptions.setVisibility(false);
            buttonFiletype.setVisibility(false);
        }
        buttonFiletype.setText("ALL");
        intentModel = new IntentModel();
        finishActivityModel = new FinishActivityModel();
        alterDialogModel = new FinishActivityModel();
        filesSelectedModel = new FilesSelectedModel();
        intentSenderModel = new IntentSenderModel();
        popupMenuModel = new PopupMenuModel(buttonOptions);
    }


    public void setButtonClick(ScreenSlideFragment.ButtonClick onGridItemClick)
    {
        this.onGridItemClick = onGridItemClick;
    }

    public List<GridImageViewModel> getGridImageViewModels()
    {
        return filesHolderModel.getGridImageViewModels();
    }

    public ViewStateModel getViewStateModel() {
        return viewStateModel;
    }

    public FilesHolderModel getFilesHolderModel() {
        return filesHolderModel;
    }

    public IntentModel getIntentModel() {
        return intentModel;
    }

    public FinishActivityModel getFinishActivityModel() {
        return finishActivityModel;
    }

    public FinishActivityModel getAlterDialogModel() {
        return alterDialogModel;
    }

    public ButtonDoAction getButtonDoAction() {
        return buttonDoAction;
    }

    public FilesSelectedModel getFilesSelectedModel() {
        return filesSelectedModel;
    }

    public ButtonFileTypeModel getButtonFiletype() {
        return buttonFiletype;
    }

    public ButtonOptionsModel getButtonOptions() {
        return buttonOptions;
    }

    public IntentSenderModel getIntentSenderModel() {
        return intentSenderModel;
    }

    public PopupMenuModel getPopupMenuModel() {
        return popupMenuModel;
    }

    public boolean isRootDir() {
        return isRootDir;
    }

    public void setViewMode(ViewStates viewState)
    {
        Log.d(TAG,"setViewMode:  isRootDir" + isRootDir);
        viewStateModel.setCurrentViewState(viewState);
        if (isRootDir)
        {
            buttonFiletype.setVisibility(false);
            buttonOptions.setVisibility(false);
            filesSelectedModel.setVisibility(false);
        }
        else
        {
            switch (viewState)
            {
                case normal:
                    if (formatsToShow == FileListController.FormatTypes.raw && lastFormat != FileListController.FormatTypes.raw) {
                        formatsToShow = lastFormat;
                        if (filesHolderModel.getFormatType() != formatsToShow)
                            filesHolderModel.setFormatType(formatsToShow);
                    }
                    //resetFilesSelected();
                    requestMode = RequestModes.none;
                    buttonFiletype.setVisibility(true);
                    buttonOptions.setVisibility(true);
                    buttonDoAction.setVisibility(false);
                    filesSelectedModel.setVisibility(false);
                    break;
                case selection:
                    resetFilesSelected();
                    filesSelectedModel.setVisibility(true);
                    updateFilesSelected();
                    switch (requestMode) {
                        case none:
                            buttonFiletype.setVisibility(true);
                            buttonOptions.setVisibility(true);
                            buttonDoAction.setVisibility(false);
                            buttonDoAction.setOnClickListener(null);
                            break;
                        case delete:
                            buttonFiletype.setVisibility(false);
                            buttonOptions.setVisibility(false);
                            buttonDoAction.setText("Delete");
                            buttonDoAction.setOnClickListener(onDeltedButtonClick);
                            buttonDoAction.setVisibility(true);
                            break;
                        case rawToDng:
                            lastFormat = formatsToShow;
                            formatsToShow = FileListController.FormatTypes.raw;
                            if (filesHolderModel.getFormatType() != formatsToShow)
                                filesHolderModel.setFormatType(formatsToShow);
                            buttonOptions.setVisibility(false);
                            buttonFiletype.setVisibility(false);
                            buttonDoAction.setText("RawToDng");
                            buttonDoAction.setOnClickListener(onRawToDngClick);
                            buttonDoAction.setVisibility(true);
                            break;
                        case stack:
                            /*lastFormat = formatsToShow;
                            formatsToShow = FileListController.FormatTypes.jpg;
                            if (filesHolderModel.getFormatType() != formatsToShow)
                                filesHolderModel.setFormatType(formatsToShow);
                            buttonOptions.setVisibility(false);
                            buttonFiletype.setVisibility(false);
                            buttonDoAction.setText("Stack");
                            buttonDoAction.setOnClickListener(onStackClick);
                            buttonDoAction.setVisibility(true);*/
                            break;
                        case dngstack:
                            lastFormat = formatsToShow;
                            formatsToShow = FileListController.FormatTypes.dng;
                            if (folderToShow == null)
                                folderToShow = filesHolderModel.getFiles().get(0);
                            if (filesHolderModel.getFormatType() != formatsToShow)
                                filesHolderModel.setFormatType(formatsToShow);
                            buttonOptions.setVisibility(false);
                            buttonFiletype.setVisibility(false);
                            buttonDoAction.setText("DngStack");
                            buttonDoAction.setOnClickListener(onDngStackClick);
                            buttonDoAction.setVisibility(true);
                            break;
                    }
                    break;
            }
        }
    }

    /*public final View.OnClickListener onStackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (requestMode == RequestModes.none)
            {
                requestMode = RequestModes.stack;
                setViewMode(ViewStates.selection);
            }
            else if (requestMode == RequestModes.stack)
            {
                ArrayList<String> ar = new ArrayList<>();
                for (GridImageViewModel f : filesHolderModel.getGridImageViewModels()) {
                    if (f.getChecked() && f.getImagePath().getName().toLowerCase().endsWith(StringUtils.FileEnding.JPG))
                    {
                        if (f.getImagePath() instanceof FileHolder)
                            ar.add(((FileHolder)f.getImagePath()).getFile().getAbsolutePath());
                        else if (f.getImagePath() instanceof UriHolder)
                            ar.add(((UriHolder)f.getImagePath()).getMediaStoreUri().toString());
                    }

                }
                for (GridImageViewModel f : filesHolderModel.getGridImageViewModels()) {
                    f.setChecked(false);
                }
                setViewMode(ViewStates.normal);
                intentModel.setAr(ar);
                intentModel.setIntentClass(StackActivity.class);
            }
            else
            {
                requestMode = RequestModes.none;
                setViewMode(ViewStates.normal);
            }
        }
    };*/

    public final View.OnClickListener onDngStackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (requestMode == RequestModes.none)
            {
                requestMode = RequestModes.dngstack;
                setViewMode(ViewStates.selection);
            }
            else if (requestMode == RequestModes.dngstack)
            {
                // DngStack's native side (TIFFOpen in DngStacker.cpp) needs a real filesystem
                // path - it can't read a content:// URI, which is what every file has under
                // SAF or the MediaStore fallback (the two storage modes minSdk 30 actually
                // uses; see MIGRATION_NOTES.md). Only FileHolder (a direct java.io.File)
                // gives a usable path, so refuse the whole selection rather than silently
                // dropping SAF-backed files or handing the native code a URI string it can't
                // open.
                ArrayList<String> ar = new ArrayList<>();
                boolean hasUnsupportedFile = false;
                for (GridImageViewModel f : getFilesHolderModel().getGridImageViewModels()) {
                    if (f.getChecked() && f.getImagePath().getName().toLowerCase().endsWith(StringUtils.FileEnding.DNG))
                    {
                        if (f.getImagePath() instanceof FileHolder)
                            ar.add(((FileHolder)f.getImagePath()).getFile().getAbsolutePath());
                        else
                            hasUnsupportedFile = true;
                    }

                }
                for (GridImageViewModel f : getFilesHolderModel().getGridImageViewModels()) {
                    f.setChecked(false);
                }
                setViewMode(ViewStates.normal);
                if (hasUnsupportedFile) {
                    Toast.makeText(GlassCamApplication.getContext(), R.string.dngstack_unsupported_storage, Toast.LENGTH_LONG).show();
                    return;
                }
                if (ar.isEmpty()) {   // nothing to stack is not a storage problem: say what is missing
                    Toast.makeText(GlassCamApplication.getContext(), R.string.dngstack_no_dng, Toast.LENGTH_LONG).show();
                    return;
                }
                intentModel.setAr(ar);
                intentModel.setIntentClass(DngStackActivity.class);
            }
            else
            {
                requestMode = RequestModes.none;
                setViewMode(ViewStates.normal);
            }
        }
    };

    public final View.OnClickListener onGobBackClick = new View.OnClickListener() {
        @Override
        public void onClick(View view)
        {
            if (viewStateModel.getCurrentViewState() == ViewStates.normal)
            {
                if (getFilesHolderModel().getFiles() != null && getFilesHolderModel().getFiles().size() > 0
                        && getFilesHolderModel().getFiles().get(0) instanceof FileHolder)
                {
                    FileHolder fileHolder = (FileHolder) getFilesHolderModel().getFiles().get(0);
                    File topPath = fileHolder.getFile().getParentFile().getParentFile();
                    if (topPath.getName().equals("DCIM") && !isRootDir)
                    {
                        getFilesHolderModel().loadDefault();
                        isRootDir = true;
                        Log.d(TAG, "onGobBackClick dcim folder rootdir:" +isRootDir);
                        setViewMode(viewStateModel.getCurrentViewState());
                    }
                    else if (isRootDir)
                    {
                        finishActivityModel.setOb(null);
                    }
                    else
                    {
                        isRootDir = false;
                        Log.d(TAG, "onGobBackClick load folder rootdir:" +isRootDir);
                        filesHolderModel.loadDefault();
                        //viewerActivityInterface.LoadFolder(viewerActivityInterface.getFiles().get(0),formatsToShow);
                        setViewMode(viewStateModel.getCurrentViewState());
                    }
                }
                else if (filesHolderModel.getFiles() != null && filesHolderModel.getFiles().size() > 0 && (filesHolderModel.getFiles().get(0) instanceof UriHolder|| filesHolderModel.getFiles().get(0) instanceof DocumentHolder)) {
                    if (filesHolderModel.getFiles().get(0).IsFolder())
                        finishActivityModel.setOb(null);
                    else {
                        filesHolderModel.loadDefault();
                        isRootDir = true;
                        setViewMode(ViewStates.normal);
                    }
                }
                else
                {
                    filesHolderModel.loadDefault();
                    //viewerActivityInterface.LoadDCIMDirs();
                    Log.d(TAG, "onGobBackClick dcim folder rootdir:" +isRootDir);
                    isRootDir = true;

                    setViewMode(viewStateModel.getCurrentViewState());
                    if (filesHolderModel.getFiles().size() == 0)
                        finishActivityModel.setOb(null);
                }
            }
            else if (viewStateModel.getCurrentViewState() == ViewStates.selection)
            {
                for (int i = 0; i< filesHolderModel.getGridImageViewModels().size(); i++)
                {
                    GridImageViewModel f = filesHolderModel.getGridImageViewModels().get(i);
                    f.setChecked(false);
                }
                setViewMode(ViewStates.normal);
            }
        }
    };

    public final View.OnClickListener onRawToDngClick = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            if (requestMode == RequestModes.none)
            {
                requestMode = RequestModes.rawToDng;
                setViewMode(ViewStates.selection);
            }
            else if (requestMode == RequestModes.rawToDng)
            {
                ArrayList<String> ar = new ArrayList<>();
                for (GridImageViewModel f : getFilesHolderModel().getGridImageViewModels()) {
                    if (f.getChecked() &&
                            (f.getImagePath().getName().toLowerCase().endsWith(StringUtils.FileEnding.RAW) ||f.getImagePath().getName().toLowerCase().endsWith(StringUtils.FileEnding.BAYER))) {
                        if (f.getImagePath() instanceof FileHolder)
                            ar.add(((FileHolder) f.getImagePath()).getFile().getAbsolutePath());
                        else if (f.getImagePath() instanceof UriHolder)
                            ar.add(((UriHolder) f.getImagePath()).getMediaStoreUri().toString());
                        else if (f.getImagePath() instanceof DocumentHolder)
                            ar.add(((DocumentHolder)f.getImagePath()).getDocumentFile().getUri().toString());
                    }

                }
                for (GridImageViewModel f : getFilesHolderModel().getGridImageViewModels()) {
                    f.setChecked(false);
                }
                setViewMode(ViewStates.normal);
                intentModel.setAr(ar);
                intentModel.setIntentClass(DngConvertingActivity.class);
            }
            else
            {
                requestMode = RequestModes.none;
                setViewMode(ViewStates.normal);
            }
        }
    };

    public final View.OnClickListener onDeltedButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            if (requestMode == RequestModes.none)
            {
                requestMode = RequestModes.delete;
                setViewMode(ViewStates.selection);
            }
            else if (requestMode == RequestModes.delete)
            {
                //check if files are selected
                boolean hasfilesSelected = false;
                for (GridImageViewModel f : getFilesHolderModel().getGridImageViewModels()) {
                    if (f.getChecked()) {
                        hasfilesSelected = true;
                        break;
                    }
                }
                //if no files selected skip dialog
                if (!hasfilesSelected)
                    return;
                //else show dialog
                alterDialogModel.setOb(null);
                setViewMode(ViewStates.normal);
            }
            else
            {
                requestMode = RequestModes.none;
                setViewMode(ViewStates.normal);
            }
        }
    };

    public void deleteFiles()
    {
        imageManager.cancelImageLoadTasks();
        urisToDelte.clear();
        // route each file by its own holder type: the first file's type used to decide for the
        // whole selection, so a leading UriHolder cast a FileHolder/DocumentHolder and threw
        List<BaseHolder> direct = new java.util.ArrayList<>();
        for (BaseHolder baseHolder : filesSelectedList) {
            if (baseHolder.getHolderType() == FileHolder.class || baseHolder.getHolderType() == DocumentHolder.class)
                direct.add(baseHolder);
            else if (baseHolder instanceof UriHolder)
                urisToDelte.add((UriHolder) baseHolder);
        }
        if (!direct.isEmpty())
            fileListController.DeleteFiles(direct);
        deleteUriFile();
    }

    public void deleteNextFile()
    {
        deleteUriFile();
    }

    private void deleteUriFile()
    {
        if (urisToDelte.size() > 0)
            try {

                fileListController.DeleteFile(urisToDelte.get(0));
                urisToDelte.remove(0);
                deleteUriFile();
            }
            catch(SecurityException ex){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ex instanceof RecoverableSecurityException)
                    {
                        RecoverableSecurityException rex = (RecoverableSecurityException)ex;
                        intentSenderModel.setIntentSender(rex.getUserAction().getActionIntent().getIntentSender());
                    }
                }
            }
    }
    private void resetFilesSelected()
    {
        for (int i = 0; i< filesHolderModel.getGridImageViewModels().size(); i++)
        {
            GridImageViewModel f = getFilesHolderModel().getGridImageViewModels().get(i);
            f.setChecked(false);
        }
        filesSelectedList.clear();
        updateFilesSelected();   // every other mutator of the list does; the count stayed stale here
    }

    private void updateFilesSelected()
    {
        filesSelectedModel.setFilesSelectedCount(filesSelectedList.size());
    }

    public void setFormatsToShow(FileListController.FormatTypes formatsToShow)
    {
        this.formatsToShow = formatsToShow;
        if (filesHolderModel.getFormatType() != formatsToShow)
            filesHolderModel.setFormatType(formatsToShow);
    }

    public View.OnTouchListener gridItem_onTouchListener = new View.OnTouchListener() {

        private GridImageViewModel last = null;
        private float startX;
        private float startY;
        private float lastDistance;
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (viewStateModel.getCurrentViewState() == ViewStates.selection) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || lastDistance > 30)
                {
                    startX = event.getX();
                    startY = event.getY();
                    lastDistance = 0;
                }
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    startX = 0;
                    startY = 0;
                    last = null;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {

                    float distanceX;
                    float distanceY;
                    if (startX > event.getX())
                        distanceX = startX - event.getX();
                    else
                        distanceX = event.getX() - startX;

                    if (startY > event.getY())
                        distanceY = startY - event.getY();
                    else
                        distanceY = event.getY() - startY;
                    if (distanceX > distanceY)
                        lastDistance += distanceX;
                    else
                        lastDistance+= distanceY;

                    if (distanceX > distanceY) {
                        GridView view = (GridView) v;
                        int position = view.pointToPosition((int) event.getX(), (int) event.getY());
                        GridImageViewModel mod = filesHolderModel.getGridImageViewModels().get(position);

                        if (mod != last) {
                            if (filesHolderModel.getGridImageViewModels().get(position).getChecked()) {
                                filesHolderModel.getGridImageViewModels().get(position).setChecked(false);
                                filesSelectedList.remove(filesHolderModel.getFiles().get(position));
                            } else {
                                filesHolderModel.getGridImageViewModels().get(position).setChecked(true);
                                filesSelectedList.add(filesHolderModel.getGridImageViewModels().get(position).getImagePath());
                            }
                            last = mod;
                            updateFilesSelected();
                        }
                    }
                }
            }
            return false;
        }
    };

    public AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (viewStateModel.getCurrentViewState())
            {
                case normal:
                    //handel normal griditem click to open screenslide when its not a folder
                    if (!filesHolderModel.getFiles().get(position).IsFolder())
                    {
                        onGridItemClick.onButtonClick(position, view);
                    }
                    else //handel folder click
                    {
                        //hold the current folder to show if a format is empty
                        folderToShow = filesHolderModel.getFiles().get(position);
                        filesHolderModel.LoadFolder(folderToShow,formatsToShow);
                        isRootDir = false;
                        setViewMode(viewStateModel.getCurrentViewState());

                    }
                    break;
                case selection:
                    if (filesHolderModel.getGridImageViewModels().get(position).getChecked()) {
                        filesHolderModel.getGridImageViewModels().get(position).setChecked(false);
                        filesSelectedList.remove(filesHolderModel.getFiles().get(position));
                    } else {
                        filesHolderModel.getGridImageViewModels().get(position).setChecked(true);
                        filesSelectedList.add(filesHolderModel.getGridImageViewModels().get(position).getImagePath());
                    }
                    updateFilesSelected();
                    break;
            }
        }
    };


    public void refreshCurrentFolder()
    {
        filesHolderModel.LoadFolder(folderToShow,formatsToShow);
    }

    /**
     * Batch 3 (2026-09-16): opened from the camera's gallery cell, the viewer should land in the
     * folder that holds the newest capture rather than at the folder root. Runs after the default
     * list is loaded; a no-op when the root is empty or already holds files.
     */
    public void openNewestFolder()
    {
        java.util.List<glasscam.file.holder.BaseHolder> files = filesHolderModel.getFiles();
        if (files == null || files.isEmpty() || !files.get(0).IsFolder())
            return;
        folderToShow = files.get(0);
        filesHolderModel.LoadFolder(folderToShow, formatsToShow);
        isRootDir = false;
        setViewMode(viewStateModel.getCurrentViewState());
    }
}
