package fabscreen.libraries.legacy.data.api;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * BaseDownloader:
 */
public class HttpDownloader implements DownloadProgressListener {
    private DownloadService mDownloadService;
    private String mSaveFilePath;

    private BehaviorSubject<Integer> mDownloadProgressSubject = BehaviorSubject.createDefault(0);

    public HttpDownloader(String filePath) {
        mSaveFilePath = filePath;
    }

    public Observable<Integer> getDownloadProgressObservable() {
        return mDownloadProgressSubject.hide();
    }

    @Override
    public void progress(long read, long contentLength, boolean isDone) {
        final int progress = (int) (100 * read / contentLength);
        mDownloadProgressSubject.onNext(progress);
    }

    public Observable<ResponseBody> startDownload(@NonNull String url) {
        int targetFolderSlashIndex = url.lastIndexOf("/");
        if (targetFolderSlashIndex == -1 ) {
            // error?
            ;
        }
        String baseUrl = url.substring(0, targetFolderSlashIndex + 1);
        String targetFile = url.substring(targetFolderSlashIndex + 1);
        final DownloadInterceptor downloadInterceptor = new DownloadInterceptor(this);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.addInterceptor(downloadInterceptor);
        Retrofit retrofit = new Retrofit.Builder()
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .build();

        mDownloadService = retrofit.create(DownloadService.class);

        return mDownloadService.download(targetFile).observeOn(Schedulers.io()).doOnNext(responseBody -> {
            Logger.d("Start downloading %s...", targetFile);
            InputStream is = null;
            FileOutputStream fos = null;

            is = responseBody.byteStream();

            File file = new File(mSaveFilePath, "update.bin");
            fos = new FileOutputStream(file);

            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                fos.write(buf, 0, n);
            }

            fos.flush();
            fos.close();
            is.close();
        }).doOnError(e -> {
            if (mDownloadService != null) {
                mDownloadService = null;
            }

            // Reset download progress
            mDownloadProgressSubject.onComplete();
            mDownloadProgressSubject = null;
            mDownloadProgressSubject = BehaviorSubject.createDefault(0);
        });
    }

    /**
     * ProgressResponseBody:
     */
    static class ProgressResponseBody extends ResponseBody {
        private ResponseBody responseBody;
        private DownloadProgressListener listener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody body, DownloadProgressListener listener) {
            this.responseBody = body;
            this.listener = listener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += (bytesRead != -1) ? bytesRead : 0;
                    if (listener != null) {
                        listener.progress(totalBytesRead, responseBody.contentLength(), bytesRead != -1);
                    }
                    return bytesRead;
                }
            };
        }
    }

    /**
     * DownloadInterceptor:
     * <p>
     * [reference](https://square.github.io/okhttp/interceptors/)
     */
    private static class DownloadInterceptor implements Interceptor {
        private DownloadProgressListener listener;

        DownloadInterceptor(DownloadProgressListener listener) {
            this.listener = listener;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originResponse = chain.proceed(chain.request());

            return originResponse
                    .newBuilder()
                    .body(new ProgressResponseBody(originResponse.body(), listener))
                    .build();
        }
    }

    /**
     * DownloadService:
     */
    interface DownloadService {
        @GET
        Observable<ResponseBody> download(@Url String url);
    }
}
