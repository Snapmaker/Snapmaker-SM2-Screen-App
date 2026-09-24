package fabscreen.libraries.legacy.server.http.handlers;

import static fabscreen.libraries.legacy.data.remote.SessionManager.NULL_SESSION;

import com.orhanobut.logger.Logger;
import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.framework.body.StringBody;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;

import org.json.JSONException;
import org.json.JSONObject;

import fabscreen.libraries.legacy.data.Preferences;
import fabscreen.libraries.legacy.data.remote.SessionManager;
import fabscreen.libraries.legacy.lib.LogHelper;


@RestController
public class AuthRequestHandler extends BaseRequestHandler {
    private static final String URI_CONNECT = "/api/v1/connect";
    private static final String URI_DISCONNECT = "/api/v1/disconnect";

    @PostMapping(path = URI_CONNECT)
    void requestConnect(HttpRequest request, HttpResponse response) {
        String token = request.getParameter("token");
        Preferences preferences = getModel().getPreferences();

        // only one pending session is allowed, if there is one, then refuse current one
        SessionManager.Session currentSession = getModel().getRemoteController().getCurrentSession();
        if (currentSession != NULL_SESSION && !currentSession.getToken().equals(token)) {
            response.setBody(new StringBody("Failed to connect, there is someone trying to connect to Touchscreen."));
            response.setStatus(HttpResponse.SC_FORBIDDEN);
            return;
        }

        // Get related session or create a new session
        SessionManager.Session session;
        if (token == null) {
            session = getModel().getRemoteController().createSession();
        } else {
            session = getModel().getRemoteController().getSession(preferences, token);

            // if token is invalid or session doesn't exist, then create a new session
            if (session == NULL_SESSION) {
                session = getModel().getRemoteController().createSession();
            }
        }

        getModel().getRemoteController().setCurrentSession(session);

        Logger.i("Receive remote access request, session #%s", session.getToken());

        JSONObject data = new JSONObject();
        try {
            data.put("token", session.getToken());
            data.put("readonly", false);
            data.put("series", getModel().getMachineController().getMachineModelSeries());
            data.put("headType", getModel().getMachineController().getHeadType());
            data.put("hasEnclosure", getModel().getMachineController().isEnclosureReady());

            response.setBody(new JsonBody(data));
        } catch (JSONException e) {
            LogHelper.log(e);
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping(path = URI_DISCONNECT)
    void requestDisconnect(HttpRequest request, HttpResponse response) {
        if (!ensureConnection(request, response)) return;

        String token = request.getParameter("token");
        if (token == null) {
            response.setBody(new StringBody("Failed to disconnect."));
            response.setStatus(HttpResponse.SC_FORBIDDEN);
            return;
        }

        SessionManager.Session currentSession = getModel().getRemoteController().getCurrentSession();

        if (currentSession != NULL_SESSION && currentSession.getToken().equals(token)) {
            getModel().getRemoteController().setCurrentSession(NULL_SESSION);
            response.setStatus(HttpResponse.SC_OK);
        } else {
            response.setStatus(HttpResponse.SC_NOT_FOUND);
        }
    }
}
