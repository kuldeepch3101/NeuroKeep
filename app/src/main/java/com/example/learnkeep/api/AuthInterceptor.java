package com.example.learnkeep.api;

import com.example.learnkeep.SessionManager;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class AuthInterceptor implements Interceptor {
    private SessionManager session;
    public AuthInterceptor(SessionManager session){
        this.session = session;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = session.getToken();
        Request request = chain.request()
                .newBuilder()
                .addHeader("Authorization", token)
                .build();
        return chain.proceed(request);
    }
}
