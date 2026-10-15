package glasscam.viewer.gridview.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

import java.util.ArrayList;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.utils.Log;
import glasscam.viewer.helper.BitmapHelper;

public class FilesHolderModel extends BaseObservable implements FileListController.NotifyFilesChanged {

    private final String TAG = FilesHolderModel.class.getSimpleName();
    private List<BaseHolder> files;
    private final FileListController fileListController;
    private final List<GridImageViewModel> gridImageViewModels;
    private final List<GridImageViewModel> visibleGridImageViewModels;
    private final BitmapHelper bitmapHelper;
    private FileListController.FormatTypes formatType = FileListController.FormatTypes.all;

    public FilesHolderModel()
    {
        gridImageViewModels = new ArrayList<>();
        visibleGridImageViewModels = new ArrayList<>();
        fileListController = GlassCamApplication.fileListController();
        fileListController.setNotifyFilesChanged(this);
        bitmapHelper = GlassCamApplication.bitmapHelper();
    }

    public void setFiles(List<BaseHolder> files) {
        this.files = files;
        gridImageViewModels.clear();
        visibleGridImageViewModels.clear();
        for (BaseHolder baseHolder : files)
        {
            GridImageViewModel model =new GridImageViewModel(bitmapHelper,baseHolder);
            gridImageViewModels.add(model);
            visibleGridImageViewModels.add(model);
        }
        notifyPropertyChanged(BR.files);
    }

    public void setFormatType(FileListController.FormatTypes formatType)
    {
        this.formatType = formatType;
        visibleGridImageViewModels.clear();
        if (formatType != FileListController.FormatTypes.all) {
            for (int i = 0; i < gridImageViewModels.size(); i++) {
                if (gridImageViewModels.get(i).getImagePath().getFileformat() == formatType)
                    visibleGridImageViewModels.add(gridImageViewModels.get(i));
            }
        }
        else {
            for (int i = 0; i < gridImageViewModels.size(); i++) {
                visibleGridImageViewModels.add(gridImageViewModels.get(i));
            }
        }
        notifyPropertyChanged(BR.files);
        notifyPropertyChanged(BR.formatType);
    }

    @Bindable
    public FileListController.FormatTypes getFormatType() {
        return formatType;
    }

    public List<GridImageViewModel> getGridImageViewModels()
    {
        return visibleGridImageViewModels;
    }

    @Bindable
    public List<BaseHolder> getFiles() {
        return files;
    }

    public void LoadFolder(BaseHolder fileHolder, FileListController.FormatTypes types)
    {
        fileListController.LoadFolder(fileHolder,types);
        setFiles(fileListController.getFiles());
    }

    public void loadDefault()
    {
        fileListController.loadDefaultFiles();
        setFiles(fileListController.getFiles());
    }

    public void deleteFiles(List<BaseHolder> baseHolders)
    {
        fileListController.DeleteFiles(baseHolders);
    }

    public void deleteFile(BaseHolder baseHolders)
    {
        fileListController.DeleteFile(baseHolders);
    }


    @Override
    public void onFilesChanged() {
        Log.d(TAG,"onFilesChanged");
        // an empty list is a real state (a folder emptied by a batch delete), not "nothing new":
        // skipping it left the deleted files on screen (fresh-eyes 2026-09-28)
        if (fileListController.getFiles() != null)
            setFiles(fileListController.getFiles());
    }

    @Override
    public void onFileDeleted(int id) {
        Log.d(TAG,"onFileDeleted " +id);
        // the grid binds visibleGridImageViewModels; removing only from the full list left the
        // deleted file's cell in the grid (fresh-eyes 2026-09-28)
        if (id >= 0 && id < gridImageViewModels.size()) {
            GridImageViewModel gone = gridImageViewModels.remove(id);
            visibleGridImageViewModels.remove(gone);
        }
        notifyPropertyChanged(BR.files);
    }
}
