package com.blur.backend;

import com.google.gson.annotations.SerializedName;

public enum Status {
    @SerializedName("created")
    CREATED,
    @SerializedName("active")
    ACTIVE,
    @SerializedName("done")
    DONE,
    @SerializedName("cancelled")
    CANCELLED,
    @SerializedName("error")
    ERROR;
}
