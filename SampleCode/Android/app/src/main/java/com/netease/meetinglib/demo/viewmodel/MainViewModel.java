package com.netease.meetinglib.demo.viewmodel;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.netease.meetinglib.demo.SdkAuthenticator;
import com.netease.meetinglib.demo.SdkInitializer;
import com.netease.meetinglib.demo.data.MeetingDataRepository;
import com.netease.meetinglib.sdk.NEMeetingInfo;
import com.netease.meetinglib.sdk.NEMeetingOnInjectedMenuItemClickListener;
import com.netease.meetinglib.sdk.NEMeetingService;
import com.netease.meetinglib.sdk.NEMeetingStatus;
import com.netease.meetinglib.sdk.NEMeetingStatusListener;
import com.netease.meetinglib.sdk.control.NEControlListener;
import com.netease.meetinglib.sdk.control.NEControlMenuItemClickListener;

public class MainViewModel extends ViewModel implements NEMeetingStatusListener, SdkInitializer.InitializeListener, SdkAuthenticator.AuthStateChangeListener {

    private static final int MSG_ACTIVE = 1;

    private MeetingDataRepository mRepository = MeetingDataRepository.getInstance();

    private MutableLiveData<Integer> stateLiveData = new MutableLiveData<>();
    private MutableLiveData<Boolean> minimizedLiveData = new MutableLiveData<>();
    private MutableLiveData<String> meetingTimeLiveData = new MutableLiveData<String>() {
        @Override
        protected void onActive() {
            super.onActive();
            handler.sendEmptyMessage(MSG_ACTIVE);
        }

        @Override
        protected void onInactive() {
            handler.removeMessages(MSG_ACTIVE);
        }
    };
    private MutableLiveData<NEMeetingInfo> meetingInfoLiveData = new MutableLiveData<>();

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_ACTIVE) {
                calculateElapsedTime();
                return;
            }
            super.handleMessage(msg);
        }
    };

    public MainViewModel() {
        SdkInitializer.getInstance().addListener(this);
        SdkAuthenticator.getInstance().setAuthStateChangeListener(this::onAuthStateChanged);
    }

    public MutableLiveData<Boolean> getMeetingMinimizedLiveData() {
        return minimizedLiveData;
    }

    public MutableLiveData<String> getMeetingTimeLiveData() {
        return meetingTimeLiveData;
    }

    public void  observeStateLiveData(LifecycleOwner owner, Observer<Integer> observer) {
        stateLiveData.observe(owner, observer);
    }

    public void setOnInjectedMenuItemClickListener(NEMeetingOnInjectedMenuItemClickListener listener) {
        mRepository.setOnInjectedMenuItemClickListener(listener);
    }

    public void setOnControlCustomMenuItemClickListener(NEControlMenuItemClickListener listener) {
        mRepository.setOnControlCustomMenuItemClickListener(listener);
    }
    public void registerControlListener(NEControlListener listener) {
        mRepository.registerControlListener(listener);
    }
    public void unRegisterControlListener(NEControlListener listener) {
        mRepository.unRegisterControlListener(listener);
    }
    public NEMeetingService getMeetingService() {
        return mRepository.getMeetingService();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getMeetingService().removeMeetingStatusListener(this);
        SdkInitializer.getInstance().removeListener(this);
        SdkAuthenticator.getInstance().setAuthStateChangeListener(null);
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onInitialized(int total) {
        getMeetingService().addMeetingStatusListener(this);
    }

    @Override
    public void onMeetingStatusChanged(Event event) {
        Boolean value = event.status == NEMeetingStatus.MEETING_STATUS_INMEETING_MINIMIZED;
        if (minimizedLiveData.getValue() != value) {
            minimizedLiveData.setValue(value);
        }
        if (event.status == NEMeetingStatus.MEETING_STATUS_DISCONNECTING) {
            meetingInfoLiveData.setValue(null);
        } else if (event.status == NEMeetingStatus.MEETING_STATUS_INMEETING){
            getMeetingService().getCurrentMeetingInfo((code, msg, info) -> {
                meetingInfoLiveData.setValue(info);
            });
        }
    }

    private void calculateElapsedTime() {
        NEMeetingInfo info = meetingInfoLiveData.getValue();
        if (info != null) {
            long duration = (System.currentTimeMillis() - info.startTime) / 1000;
            long hours = duration / 3600;
            long minutes = (duration % 3600) / 60;
            long seconds = duration % 60;
            meetingTimeLiveData.setValue(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            handler.sendEmptyMessageDelayed(MSG_ACTIVE, 1000);
        }
    }

    @Override
    public void onAuthStateChanged(int state) {
        stateLiveData.setValue(state);
    }
}