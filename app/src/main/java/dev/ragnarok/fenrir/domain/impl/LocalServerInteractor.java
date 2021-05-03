package dev.ragnarok.fenrir.domain.impl;

import java.util.ArrayList;
import java.util.List;

import dev.ragnarok.fenrir.api.interfaces.INetworker;
import dev.ragnarok.fenrir.api.model.VKApiVideo;
import dev.ragnarok.fenrir.db.model.entity.VideoEntity;
import dev.ragnarok.fenrir.domain.ILocalServerInteractor;
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity;
import dev.ragnarok.fenrir.domain.mappers.Dto2Model;
import dev.ragnarok.fenrir.model.Audio;
import dev.ragnarok.fenrir.model.Video;
import io.reactivex.rxjava3.core.Single;

import static dev.ragnarok.fenrir.util.Utils.listEmptyIfNull;

public class LocalServerInteractor implements ILocalServerInteractor {

    private final INetworker networker;

    public LocalServerInteractor(INetworker networker) {
        this.networker = networker;
    }

    @Override
    public Single<List<Video>> getVideos(int offset, int count) {
        return networker.localServerApi()
                .getVideos(offset, count)
                .flatMap(items -> {
                    List<VKApiVideo> dtos = listEmptyIfNull(items.getItems());
                    List<VideoEntity> dbos = new ArrayList<>(dtos.size());
                    List<Video> videos = new ArrayList<>(dbos.size());

                    for (VKApiVideo dto : dtos) {
                        dbos.add(Dto2Entity.mapVideo(dto));
                        videos.add(Dto2Model.transform(dto));
                    }

                    return Single.just(videos);
                });
    }

    @Override
    public Single<List<Audio>> getAudios(int offset, int count) {
        return networker.localServerApi()
                .getAudios(offset, count)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)).setIsLocalServer(true));
                    return ret;
                });
    }

    @Override
    public Single<List<Audio>> getDiscography(int offset, int count) {
        return networker.localServerApi()
                .getDiscography(offset, count)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)).setIsLocalServer(true));
                    return ret;
                });
    }

    @Override
    public Single<List<Video>> searchVideos(String q, int offset, int count) {
        return networker.localServerApi()
                .searchVideos(q, offset, count)
                .flatMap(items -> {
                    List<VKApiVideo> dtos = listEmptyIfNull(items.getItems());
                    List<VideoEntity> dbos = new ArrayList<>(dtos.size());
                    List<Video> videos = new ArrayList<>(dbos.size());

                    for (VKApiVideo dto : dtos) {
                        dbos.add(Dto2Entity.mapVideo(dto));
                        videos.add(Dto2Model.transform(dto));
                    }

                    return Single.just(videos);
                });
    }

    @Override
    public Single<List<Audio>> searchAudios(String q, int offset, int count) {
        return networker.localServerApi()
                .searchAudios(q, offset, count)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)).setIsLocalServer(true));
                    return ret;
                });
    }

    @Override
    public Single<List<Audio>> searchDiscography(String q, int offset, int count) {
        return networker.localServerApi()
                .searchDiscography(q, offset, count)
                .map(items -> listEmptyIfNull(items.getItems()))
                .map(out -> {
                    List<Audio> ret = new ArrayList<>();
                    for (int i = 0; i < out.size(); i++)
                        ret.add(Dto2Model.transform(out.get(i)).setIsLocalServer(true));
                    return ret;
                });
    }

    @Override
    public Single<Integer> update_time(String hash) {
        return networker.localServerApi()
                .update_time(hash)
                .map(resultId -> resultId);
    }

    @Override
    public Single<Integer> delete_media(String hash) {
        return networker.localServerApi()
                .delete_media(hash)
                .map(resultId -> resultId);
    }

    @Override
    public Single<String> get_file_name(String hash) {
        return networker.localServerApi()
                .get_file_name(hash)
                .map(resultId -> resultId);
    }

    @Override
    public Single<Integer> update_file_name(String hash, String name) {
        return networker.localServerApi()
                .update_file_name(hash, name)
                .map(resultId -> resultId);
    }
}
