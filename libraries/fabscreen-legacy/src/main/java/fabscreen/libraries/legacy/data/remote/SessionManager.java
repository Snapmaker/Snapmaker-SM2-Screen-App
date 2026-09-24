package fabscreen.libraries.legacy.data.remote;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import fabscreen.libraries.legacy.data.Preferences;
import fabscreen.libraries.legacy.lib.JsonHelper;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class SessionManager {
    File mConfigFile;
    // TODO: make the map persistent in preferences
    // All sessions that are granted
    private List<Session> mGrantedSessionList = new ArrayList<>();

    private Session mCurrentSession = NULL_SESSION;
    private PublishSubject<Session> mCurrentSessionSubject = PublishSubject.create();

    SessionManager(File configFile) {
        mConfigFile = configFile;

        loadGrantedSessions();
    }

    //region Session operations

    static Session createSession(String identifier) {
        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();

        // create new session
        Session session = new Session();
        session.token = token;
        session.identifier = identifier;

        return session;
    }

    private void accessSession(Session session) {
        session.lastActive = SystemClock.elapsedRealtime();
    }

    //endregion

    @NonNull
    Session getSession(Preferences preferences, @NonNull String token) {
        if (mCurrentSession != NULL_SESSION && mCurrentSession.token.equals(token)) {
            return mCurrentSession;
        }

        for (Session session : mGrantedSessionList) {
            if (session.token.equals(token)) {
                return session;
            }
        }

        return NULL_SESSION;
    }

    //region granted session operations

    private void loadGrantedSessions() {
        SessionConfig sessionConfig = JsonHelper.fromJsonFile(mConfigFile, SessionConfig.class);

        if (sessionConfig != null) {
            mGrantedSessionList.clear();
            mGrantedSessionList.addAll(sessionConfig.sessions);
        } else {
            mGrantedSessionList.clear();
        }

        Logger.i("SessionManager, load sessions...done");
    }

    private void saveGrantedSessions() {
        SessionConfig sessionConfig = new SessionConfig();
        int savedCount = Math.min(mGrantedSessionList.size(), 10);
        for (int i = mGrantedSessionList.size() - savedCount; i < mGrantedSessionList.size(); i++) {
            sessionConfig.sessions.add(mGrantedSessionList.get(i));
        }

        JsonHelper.toJsonFile(mConfigFile, sessionConfig, SessionConfig.class);
        Logger.i("SessionManager, save sessions...done");
    }

    /**
     * Add Session to saved session list.
     *
     * @param session session to be added
     */
    private void addGrantedSession(@NonNull Session session) {
        if (!session.granted) {
            return;
        }

        // remove and then add, put the session at the end of list
        mGrantedSessionList.remove(session);
        mGrantedSessionList.add(session);

        // TODO: persistent
        // TODO: remove old sessions
        saveGrantedSessions();
    }

    private void removeGrantedSession(@NonNull Session session) {
        mGrantedSessionList.remove(session);
    }

    //endregion

    //region current session

    Session getCurrentSession() {
        return mCurrentSession;
    }

    Observable<Session> getCurrentSessionObservable() {
        return mCurrentSessionSubject.hide();
    }

    void setCurrentSession(@NonNull Session session) {
        mCurrentSession = session;
        Logger.d("set Current Session " + session.getToken());
        mCurrentSession.lastActive = SystemClock.elapsedRealtime();

        mCurrentSessionSubject.onNext(session);
    }

    void grantCurrentSession() {
        if (mCurrentSession.granted) {
            return;
        }

        mCurrentSession.granted = true;
        addGrantedSession(mCurrentSession);
    }

    void denyCurrentSession() {
        mCurrentSession.granted = false;

        setCurrentSession(NULL_SESSION);
    }

    void accessCurrentSession() {
        accessSession(mCurrentSession);
    }

    void checkCurrentSessionActive() {
        long now = SystemClock.elapsedRealtime();

        // 5 seconds inactive
        if (mCurrentSession != NULL_SESSION && now - mCurrentSession.lastActive > 5000) {
            Logger.w("Session inactive detected(%dms), closing current session %s",
                    now - mCurrentSession.lastActive,
                    mCurrentSession.getToken());
            setCurrentSession(NULL_SESSION);
        }
    }

    //endregion
    public enum State {
        STATE_INACTIVE,
        STATE_ACTIVE,
    }

    public static class Session {
        String token;
        String identifier;
        boolean granted = false;
        long lastActive;

        public String getToken() {
            return token;
        }

        public String getIdentifier() {
            return identifier;
        }

        public boolean isGranted() {
            return granted;
        }
    }

    public static final Session NULL_SESSION = createSession("NULL");

    private static class SessionConfig {
        ArrayList<Session> sessions = new ArrayList<>();
    }
}
