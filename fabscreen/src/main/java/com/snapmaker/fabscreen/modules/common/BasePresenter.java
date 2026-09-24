package com.snapmaker.fabscreen.modules.common;

import android.content.Context;

import fabscreen.libraries.legacy.data.Model;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class BasePresenter {
    private Context mContext;
    private Model mModel;
    private CompositeDisposable mCompositeDisposable;

    BasePresenter(Context context, Model model, CompositeDisposable compositeDisposable) {
        mContext = context;
        mModel = model;
        mCompositeDisposable = compositeDisposable;
    }

    protected Context getContext() {
        return mContext;
    }

    protected Model getModel() {
        return mModel;
    }

    protected CompositeDisposable getCompositeDisposable() {
        return mCompositeDisposable;
    }

    protected void addDisposable(Disposable disposable) {
        mCompositeDisposable.add(disposable);
    }
}
