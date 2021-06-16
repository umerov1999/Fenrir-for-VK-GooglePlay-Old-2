package dev.ragnarok.fenrir.mvp.view;

import java.util.List;

import dev.ragnarok.fenrir.model.Audio;
import dev.ragnarok.fenrir.mvp.core.IMvpView;
import dev.ragnarok.fenrir.mvp.view.base.IAccountDependencyView;

public interface IAudiosLocalServerView extends IMvpView, IErrorView, IAccountDependencyView {
    void displayList(List<Audio> audios);

    void notifyListChanged();

    void notifyItemChanged(int index);

    void notifyDataAdded(int position, int count);

    void displayLoading(boolean loading);

    void displayOptionsDialog(boolean isReverse, boolean isDiscography);
}
