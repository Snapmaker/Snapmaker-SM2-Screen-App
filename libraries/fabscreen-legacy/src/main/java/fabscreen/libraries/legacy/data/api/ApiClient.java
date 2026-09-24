package fabscreen.libraries.legacy.data.api;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class ApiClient {

    private APIService mAPIService;

    public ApiClient(String url) {
        // Base url must end with slash.
        if (!url.endsWith("/")) {
            url += "/";
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mAPIService = retrofit.create(APIService.class);
    }

    public Observable<VersionResponse> getLatestVersion() {
        return mAPIService.getLatestVersion();
    }

    interface APIService {
        @GET("/v1/fabscreen/version")
        Observable<VersionResponse> getLatestVersion();
    }
}
