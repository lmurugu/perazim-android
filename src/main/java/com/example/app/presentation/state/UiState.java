package com.example.app.presentation.state;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UiState<T> {
    public enum Status { LOADING, SUCCESS, EMPTY, ERROR }

    private final Status status;
    private final T data;
    private final String errorMessage;

    private UiState(Status status, @Nullable T data, @Nullable String errorMessage) {
        this.status = status;
        this.data = data;
        this.errorMessage = errorMessage;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null);
    }

    public static <T> UiState<T> success(@NonNull T data) {
        return new UiState<>(Status.SUCCESS, data, null);
    }

    public static <T> UiState<T> empty() {
        return new UiState<>(Status.EMPTY, null, null);
    }

    public static <T> UiState<T> error(@NonNull String message) {
        return new UiState<>(Status.ERROR, null, message);
    }

    public Status getStatus() { return status; }
    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isEmpty() { return status == Status.EMPTY; }
    public boolean isError() { return status == Status.ERROR; }
    public @Nullable T getData() { return data; }
    public @Nullable String getErrorMessage() { return errorMessage; }
}
