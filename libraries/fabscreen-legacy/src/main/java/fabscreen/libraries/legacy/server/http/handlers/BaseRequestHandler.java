package fabscreen.libraries.legacy.server.http.handlers;


import static fabscreen.libraries.legacy.data.remote.SessionManager.NULL_SESSION;

import android.os.SystemClock;

import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;

import fabscreen.libraries.legacy.BaseApplication;
import fabscreen.libraries.legacy.data.Model;
import fabscreen.libraries.legacy.data.remote.SessionManager;

public class BaseRequestHandler {
    protected Model getModel() {
        return BaseApplication.getInstance().getModel();
    }

    protected boolean ensureConnection(HttpRequest request, HttpResponse response) {
        String token = request.getParameter("token");
        if (token == null) {
            response.setStatus(HttpResponse.SC_BAD_REQUEST);
            return false;
        }

        SessionManager.Session currentSession = getModel().getRemoteController().getCurrentSession();

        // check if current session
        if (currentSession != NULL_SESSION && currentSession.getToken().equals(token)) {
            if (currentSession.isGranted()) {
                // session is granted, success
                getModel().getRemoteController().accessCurrentSession();
                return true;
            } else {
                getModel().getRemoteController().accessCurrentSession();
                // If session is pending granted, return (204)
                response.setStatus(HttpResponse.SC_NO_CONTENT);
                return false;
            }
        } else {
            // If Auth doesn't exist, deny request (401)
            response.setBody(new StringBody("Machine is not connected yet."));
            response.setStatus(HttpResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    protected void logResponseCostTime(HttpRequest request, long startTime) {
        long intervalTime = SystemClock.elapsedRealtime() - startTime;
        if (intervalTime > 500) {
            Logger.e("API: %s using %d ms", request.getPath(), intervalTime);
        } else if (intervalTime > 50) {
            Logger.w("API: %s using %d ms", request.getPath(), intervalTime);
        }

    }
}
