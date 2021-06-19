package dev.ragnarok.fenrir.mvp.presenter.conversations;

import static dev.ragnarok.fenrir.util.Objects.nonNull;
import static dev.ragnarok.fenrir.util.Utils.safeCountOf;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import dev.ragnarok.fenrir.R;
import dev.ragnarok.fenrir.api.Apis;
import dev.ragnarok.fenrir.api.model.VKApiAttachment;
import dev.ragnarok.fenrir.api.model.VKApiLink;
import dev.ragnarok.fenrir.api.model.response.AttachmentsHistoryResponse;
import dev.ragnarok.fenrir.domain.mappers.Dto2Model;
import dev.ragnarok.fenrir.model.Link;
import dev.ragnarok.fenrir.mvp.reflect.OnGuiCreated;
import dev.ragnarok.fenrir.mvp.view.conversations.IChatAttachmentLinksView;
import dev.ragnarok.fenrir.util.Pair;
import io.reactivex.rxjava3.core.Single;

public class ChatAttachmentLinksPresenter extends BaseChatAttachmentsPresenter<Link, IChatAttachmentLinksView> {

    public ChatAttachmentLinksPresenter(int peerId, int accountId, @Nullable Bundle savedInstanceState) {
        super(peerId, accountId, savedInstanceState);
    }

    @Override
    void onDataChanged() {
        super.onDataChanged();
        resolveToolbar();
    }

    @Override
    Single<Pair<String, List<Link>>> requestAttachments(int peerId, String nextFrom) {
        return Apis.get().vkDefault(getAccountId())
                .messages()
                .getHistoryAttachments(peerId, VKApiAttachment.TYPE_LINK, nextFrom, 1, 50, null)
                .map(response -> {
                    List<Link> docs = new ArrayList<>(safeCountOf(response.items));

                    if (nonNull(response.items)) {
                        for (AttachmentsHistoryResponse.One one : response.items) {
                            if (nonNull(one) && nonNull(one.entry) && one.entry.attachment instanceof VKApiLink) {
                                VKApiLink dto = (VKApiLink) one.entry.attachment;
                                docs.add(Dto2Model.transform(dto).setMsgId(one.messageId).setMsgPeerId(peerId));
                            }
                        }
                    }

                    return Pair.Companion.create(response.next_from, docs);
                });
    }

    @OnGuiCreated
    private void resolveToolbar() {
        callView(v -> v.setToolbarTitle(getString(R.string.attachments_in_chat)));
        callView(v -> v.setToolbarSubtitle(getString(R.string.links_count, safeCountOf(data))));
    }
}
