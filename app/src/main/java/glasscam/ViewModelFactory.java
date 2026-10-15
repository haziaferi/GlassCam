package glasscam;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.function.Supplier;

/**
 * Tiny ViewModelProvider.Factory for the handful of ViewModels that take
 * constructor arguments. Previously wired automatically by @HiltViewModel;
 * each call site now just supplies how to build its ViewModel.
 *
 * Usage: new ViewModelProvider(this, new ViewModelFactory(() -> new MyModelView(...))).get(MyModelView.class);
 */
public class ViewModelFactory implements ViewModelProvider.Factory {
    private final Supplier<? extends ViewModel> supplier;

    public ViewModelFactory(Supplier<? extends ViewModel> supplier) {
        this.supplier = supplier;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) supplier.get();
    }
}
